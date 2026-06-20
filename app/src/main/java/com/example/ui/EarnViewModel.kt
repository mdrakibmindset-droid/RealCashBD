package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MyApplication
import com.example.data.EarningLog
import com.example.data.SystemNotification
import com.example.data.User
import com.example.data.WithdrawRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Authenticated(val user: User) : AuthState
    data class Error(val message: String) : AuthState
}

class EarnViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MyApplication).repository

    // --- Authentication State ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    // Observe active user data reactively from room
    val currentUser: StateFlow<User?> = _currentUserId
        .flatMapLatest { id ->
            if (id != null) repository.getUserFlow(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Ensure we also support fetching device ID (or mock a fixed one for simplicity)
    val deviceId = "MOCK_DEVICE_ID_123456"

    // --- Actions/Flows ---
    val allUsers: StateFlow<List<User>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWithdrawRequests: StateFlow<List<WithdrawRequest>> = repository.getAllWithdrawRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin summaries
    val totalUsersCount: StateFlow<Int> = repository.getTotalUsersCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingWithdrawCount: StateFlow<Int> = repository.getPendingWithdrawCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalApprovedWithdrawPoints: StateFlow<Int> = repository.getTotalApprovedWithdrawPoints()
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Active User Specific flows
    val userWithdrawRequests: StateFlow<List<WithdrawRequest>> = _currentUserId
        .flatMapLatest { id ->
            if (id != null) repository.getWithdrawRequestsForUser(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userEarningLogs: StateFlow<List<EarningLog>> = _currentUserId
        .flatMapLatest { id ->
            if (id != null) repository.getEarningLogsForUser(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userNotifications: StateFlow<List<SystemNotification>> = _currentUserId
        .flatMapLatest { id ->
            if (id != null) repository.getNotificationsForUser(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Message States (for showing Toast or snackbar messages)
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    // Simulated task state
    private val _isVideoWatching = MutableStateFlow(false)
    val isVideoWatching = _isVideoWatching.asStateFlow()

    private val _videoCountdown = MutableStateFlow(0)
    val videoCountdown = _videoCountdown.asStateFlow()

    // ==========================================
    // AUTHENTICATION LOGIC
    // ==========================================

    fun login(email: String, passwordStr: String) {
        if (email.isBlank() || passwordStr.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("সবগুলো ঘর সঠিকভাবে পূরণ করুন!") }
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val user = repository.getUserByEmail(email.trim().lowercase())
            if (user == null) {
                _authState.value = AuthState.Error("ব্যবহারকারী পাওয়া যায়নি!")
                _toastMessage.emit("ভুল ইমেল বা পাসওয়ার্ড!")
                return@launch
            }

            if (user.passwordStr != passwordStr) {
                _authState.value = AuthState.Error("ভুল পাসওয়ার্ড!")
                _toastMessage.emit("ভুল পাসওয়ার্ড! আবার চেষ্টা করুন।")
                return@launch
            }

            if (user.status == "BANNED") {
                _authState.value = AuthState.Error("আপনার অ্যাকাউন্টটি নিষ্ক্রিয় (Banned) করা হয়েছে!")
                _toastMessage.emit("নিষ্ক্রিয় অ্যাকাউন্ট! এডমিনের সাথে যোগাযোগ করুন।")
                return@launch
            }

            // Mocked device check to simulate "One Device Login" security constraint
            if (user.deviceId.isNotEmpty() && user.deviceId != deviceId && user.role != "ADMIN") {
                // If it's a new device, we let them login but update the deviceId as security notification
                val updatedWithNewDevice = user.copy(deviceId = deviceId)
                repository.updateUser(updatedWithNewDevice)
                repository.sendGlobalNotification(
                    "নতুন লগইন সনাক্ত করা হয়েছে 🛡️",
                    "ব্যবহারকারী ${user.name} একটি নতুন ডিভাইসে সফল লগইন করেছেন।"
                )
            }

            _currentUserId.value = user.id
            _authState.value = AuthState.Authenticated(user)
            _toastMessage.emit("সফল লগইন! স্বাগতম ${user.name} 🎉")
        }
    }

    fun register(name: String, email: String, passwordStr: String, referredBy: String) {
        if (name.isBlank() || email.isBlank() || passwordStr.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("সবগুলো ঘর সঠিকভাবে পূরণ করুন!") }
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.createUser(
                name = name.trim(),
                email = email.trim().lowercase(),
                passwordStr = passwordStr,
                referredByCode = referredBy.trim().uppercase().ifEmpty { null },
                deviceId = deviceId
            )

            result.fold(
                onSuccess = { userId ->
                    val user = repository.getUserById(userId)
                    if (user != null) {
                        _currentUserId.value = userId
                        _authState.value = AuthState.Authenticated(user)
                        _toastMessage.emit("অ্যাকাউন্ট তৈরি সফল হয়েছে! 🎉")
                    } else {
                        _authState.value = AuthState.Error("অ্যাকাউন্ট তৈরিতে ত্রুটি হয়েছে!")
                    }
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "রেজিস্ট্রেশন ব্যর্থ!")
                    _toastMessage.emit(error.message ?: "রেজিস্ট্রেশন ব্যর্থ!")
                }
            )
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("আপনার ইমেল প্রদান করুন!") }
            return
        }
        viewModelScope.launch {
            val user = repository.getUserByEmail(email.trim().lowercase())
            if (user != null) {
                _toastMessage.emit("পাসওয়ার্ড পুনরুদ্ধারের কোডটি ${user.email}-এ পাঠানো হয়েছে! (পাসওয়ার্ডটি ছিল: ${user.passwordStr})")
            } else {
                _toastMessage.emit("এই ইমেইল দিয়ে কোনো অ্যাকাউন্ট রেকর্ড করা নেই!")
            }
        }
    }

    fun logout() {
        _currentUserId.value = null
        _authState.value = AuthState.Idle
        viewModelScope.launch { _toastMessage.emit("লগআউট সফল হয়েছে।") }
    }

    // ==========================================
    // EARNING LOGIC & TASKS
    // ==========================================

    fun claimDailyCheckIn() {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            val result = repository.claimDailyCheckIn(userId)
            result.fold(
                onSuccess = { points ->
                    _toastMessage.emit("অভিনন্দন! আপনি দৈনিক বোনাস হিসেবে $points পয়েন্ট পেয়েছেন। 🎉")
                },
                onFailure = { error ->
                    _toastMessage.emit(error.message ?: "দৈনিক বোনাস গ্রহণে সমস্যা হয়েছে।")
                }
            )
        }
    }

    fun spinWheelAndEarn(earnedPoints: Int) {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            val user = repository.getUserById(userId) ?: return@launch
            if (user.spinsLeftToday <= 0) {
                _toastMessage.emit("আপনার আজকের স্পিন সীমা শেষ! আগামীকাল আবার চেষ্টা করুন।")
                return@launch
            }

            val updatedUser = user.copy(
                points = user.points + earnedPoints,
                spinsLeftToday = user.spinsLeftToday - 1
            )
            repository.updateUser(updatedUser)

            repository.insertEarningLog(
                EarningLog(
                    userId = userId,
                    description = "লাকি স্পিন হুইল 🎡",
                    points = earnedPoints
                )
            )
            _toastMessage.emit("অভিনন্দন! আপনি স্পিন করে $earnedPoints পয়েন্ট জিতেছেন। 🎡")
        }
    }

    fun scratchCardAndEarn(earnedPoints: Int) {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            val user = repository.getUserById(userId) ?: return@launch
            if (user.scratchesLeftToday <= 0) {
                _toastMessage.emit("আপনার আজকের স্ক্র্যাচ সীমা শেষ! আগামীকাল আবার চেষ্টা করুন।")
                return@launch
            }

            val updatedUser = user.copy(
                points = user.points + earnedPoints,
                scratchesLeftToday = user.scratchesLeftToday - 1
            )
            repository.updateUser(updatedUser)

            repository.insertEarningLog(
                EarningLog(
                    userId = userId,
                    description = "স্ক্র্যাচ কার্ড অফার 🎁",
                    points = earnedPoints
                )
            )
            _toastMessage.emit("অভিনন্দন! স্ক্র্যাচ বোতাম চেপে আপনি $earnedPoints পয়েন্ট পেয়েছেন। 🎁")
        }
    }

    fun startVideoWatch() {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            val user = repository.getUserById(userId) ?: return@launch
            if (user.videosLeftToday <= 0) {
                _toastMessage.emit("আপনার আজকের ভিডিও দেখার সীমা শেষ! আগামীকাল ক্লিক করুন।")
                return@launch
            }

            _isVideoWatching.value = true
            _videoCountdown.value = 8 // 8 seconds watch simulation

            _toastMessage.emit("ভিডিও লোড হচ্ছে, অ্যাড শেষ হওয়া পর্যন্ত অপেক্ষা করুন... 📺")
        }
    }

    fun completeVideoWatch() {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            val user = repository.getUserById(userId) ?: return@launch
            _isVideoWatching.value = false
            
            val videoRewardPoints = 200
            val updatedUser = user.copy(
                points = user.points + videoRewardPoints,
                videosLeftToday = user.videosLeftToday - 1
            )
            repository.updateUser(updatedUser)

            repository.insertEarningLog(
                EarningLog(
                    userId = userId,
                    description = "ভিডিও বিজ্ঞাপন বোনাস 📺",
                    points = videoRewardPoints
                )
            )
            _toastMessage.emit("অভিনন্দন! ভিডিও দেখে আপনি $videoRewardPoints পয়েন্ট পেয়েছেন। 💰")
        }
    }

    fun tickVideoCountdown() {
        val current = _videoCountdown.value
        if (current > 0) {
            _videoCountdown.value = current - 1
        }
    }

    fun convertPointsToMoney() {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            val user = repository.getUserById(userId) ?: return@launch
            if (user.points < 1000) {
                _toastMessage.emit("পয়েন্ট পরিবর্তন করতে কমপক্ষে ১০০০ পয়েন্ট থাকা আবশ্যক! (১০০০ পয়েন্ট = ১০ টাকা)")
                return@launch
            }

            val conversionPoints = (user.points / 1000) * 1000
            val convertedCash = (conversionPoints / 100).toDouble() // 1000 points = 10 BDT

            val updatedUser = user.copy(
                points = user.points - conversionPoints
                // Note: since currency is calculated dynamically on-the-fly or simulated, point deduction is immediate
            )
            repository.updateUser(updatedUser)

            repository.insertEarningLog(
                EarningLog(
                    userId = userId,
                    description = "পয়েন্ট থেকে টাকা রূপান্তর 🔄",
                    points = -conversionPoints
                )
            )

            repository.insertNotification(
                SystemNotification(
                    title = "পয়েন্ট রূপান্তর সফল! 🔄",
                    message = "আপনি $conversionPoints পয়েন্ট পরিবর্তন করে $convertedCash টাকা ব্যালেন্সে যুক্ত করেছেন।",
                    targetUserId = userId
                )
            )

            _toastMessage.emit("সফল রূপান্তর! আপনি $conversionPoints পয়েন্ট রূপান্তর করে $convertedCash BDT নগদ পকেট ব্যালেন্সে যুক্ত করেছেন! 💰")
        }
    }

    // ==========================================
    // WITHDRAW REQUEST LOGIC
    // ==========================================

    fun requestWithdraw(method: String, number: String, points: Int) {
        val userId = _currentUserId.value ?: return
        val user = currentUser.value ?: return
        
        if (number.length < 11) {
            viewModelScope.launch { _toastMessage.emit("সঠিক মোবাইল নম্বর প্রদান করুন!") }
            return
        }

        val minPoints = when (method) {
            "Flexiload" -> 1000
            "bKash" -> 5000
            "Nagad" -> 5000
            "Rocket" -> 5000
            else -> 1000
        }

        if (points < minPoints) {
            viewModelScope.launch { _toastMessage.emit("$method এর জন্য ন্যূনতম উথড্র সীমা $minPoints পয়েন্ট!") }
            return
        }

        if (user.points < points) {
            viewModelScope.launch { _toastMessage.emit("আপনার একাউন্টে পর্যাপ্ত ব্যালেন্স নেই!") }
            return
        }

        val cashAmount = (points / 100).toDouble() // 1000 points = 10 Money

        viewModelScope.launch {
            val result = repository.createWithdrawRequest(
                userId = userId,
                userName = user.name,
                method = method,
                number = number,
                points = points,
                cashAmount = cashAmount
            )

            result.fold(
                onSuccess = {
                    _toastMessage.emit("টাকা তোলার আবেদন জমা নেওয়া হয়েছে! এডমিন পেন্ডিং থেকে এপ্রুভ করবেন। 🏦")
                },
                onFailure = { error ->
                    _toastMessage.emit(error.message ?: "উত্তোলন অনুরোধ ব্যর্থ হয়েছে।")
                }
            )
        }
    }

    // ==========================================
    // ADMIN FUNCTIONS
    // ==========================================

    fun approveWithdraw(requestId: Long) {
        viewModelScope.launch {
            val success = repository.approveWithdrawRequest(requestId)
            if (success) {
                _toastMessage.emit("পেমেন্ট অনুরোধ সফলভাবে অনুমোদিত এবং সম্পন্ন হয়েছে! ✅")
            } else {
                _toastMessage.emit("অনুমোদনে ব্যর্থ! অনুরোধটি খুঁজে পাওয়া যায়নি বা ইতিমধ্যেই অনুমোদিত।")
            }
        }
    }

    fun rejectWithdraw(requestId: Long) {
        viewModelScope.launch {
            val success = repository.rejectWithdrawRequest(requestId)
            if (success) {
                _toastMessage.emit("পেমেন্ট অনুরোধ প্রত্যাখ্যান করা হয়েছে এবং পয়েন্ট ফেরত দেওয়া হয়েছে! ❌")
            } else {
                _toastMessage.emit("প্রত্যাখ্যান ব্যর্থ!")
            }
        }
    }

    fun toggleUserBan(userId: Long, currentStatus: String) {
        viewModelScope.launch {
            val shouldBan = currentStatus == "ACTIVE"
            repository.changeUserStatus(userId, shouldBan)
            val actionName = if (shouldBan) "নিষ্ক্রিয় (Banned)" else "সক্রিয় (Active)"
            _toastMessage.emit("ব্যবহারকারীকে সফলভাবে $actionName করা হয়েছে।")
        }
    }

    fun adminSendGlobalNotification(title: String, message: String) {
        if (title.isBlank() || message.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("শিরোনাম ও বার্তা পূরণ করুন!") }
            return
        }

        viewModelScope.launch {
            repository.sendGlobalNotification(title.trim(), message.trim())
            _toastMessage.emit("বিজ্ঞপ্তিটি সফলভাবে সকল ব্যবহারকারীর কাছে পাঠানো হয়েছে! 📢")
        }
    }
}

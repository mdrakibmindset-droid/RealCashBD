package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class UserRepository(private val dao: AppDao) {

    // Users Flow
    fun getUserFlow(id: Long): Flow<User?> = dao.getUserByIdFlow(id)
    suspend fun getUserById(id: Long): User? = dao.getUserById(id)
    fun getAllUsersFlow(): Flow<List<User>> = dao.getAllUsersFlow()
    
    suspend fun getUserByEmail(email: String): User? = dao.getUserByEmail(email)
    suspend fun getUserByReferral(code: String): User? = dao.getUserByReferralCode(code)

    suspend fun createUser(
        name: String,
        email: String,
        passwordStr: String,
        referredByCode: String?,
        deviceId: String
    ): Result<Long> {
        val existing = dao.getUserByEmail(email)
        if (existing != null) {
            return Result.failure(Exception("ইমেইলটি ইতিমধ্যে ব্যবহার করা হয়েছে!"))
        }

        var refCode = generateReferralCode()
        // Ensure uniqueness
        var safety = 0
        while (dao.getUserByReferralCode(refCode) != null && safety < 10) {
            refCode = generateReferralCode()
            safety++
        }

        var validReferredBy: String? = null
        if (!referredByCode.isNullOrBlank()) {
            val referrer = dao.getUserByReferralCode(referredByCode.trim().uppercase())
            if (referrer != null) {
                if (referrer.status == "BANNED") {
                    return Result.failure(Exception("এই রেফার কোডটির একাউন্ট নিষ্ক্রিয় (Banned)!"))
                }
                validReferredBy = referrer.referralCode
            } else {
                return Result.failure(Exception("প্রদত্ত রেফার কোডটি সঠিক নয়!"))
            }
        }

        val newUser = User(
            name = name,
            email = email,
            passwordStr = passwordStr,
            referralCode = refCode,
            referredBy = validReferredBy,
            points = if (validReferredBy != null) 300 else 100, // 300 points welcome bonus for referral, 100 default
            deviceId = deviceId,
            role = "USER"
        )

        val id = dao.insertUser(newUser)

        // Award points to referrer if valid
        if (validReferredBy != null) {
            val referrer = dao.getUserByReferralCode(validReferredBy)
            if (referrer != null) {
                val updatedReferrer = referrer.copy(points = referrer.points + 500) // Referrer gets 500 points
                dao.updateUser(updatedReferrer)
                dao.insertEarningLog(
                    EarningLog(
                        userId = referrer.id,
                        description = "রেফারেল বোনাস ($name)",
                        points = 500
                    )
                )
                // Notify Referrer
                dao.insertNotification(
                    SystemNotification(
                        title = "নতুন রেফারেল বোনাস! 👥",
                        message = "আপনার রেফার কোড ব্যবহার করে $name একাউন্ট খুলেছেন। আপনি ৫০০ পয়েন্ট বোনাস পেয়েছেন!",
                        targetUserId = referrer.id
                    )
                )
            }
        }

        // Welcome Log
        dao.insertEarningLog(
            EarningLog(
                userId = id,
                description = "নতুন একাউন্ট বোনাস 🎉",
                points = if (validReferredBy != null) 300 else 100
            )
        )

        return Result.success(id)
    }

    suspend fun updateUser(user: User) {
        dao.updateUser(user)
    }

    suspend fun changeUserStatus(userId: Long, isBanned: Boolean) {
        dao.changeUserStatus(userId, if (isBanned) "BANNED" else "ACTIVE")
    }

    // Withdraw Operations
    fun getAllWithdrawRequests(): Flow<List<WithdrawRequest>> = dao.getAllWithdrawRequestsFlow()
    fun getWithdrawRequestsForUser(userId: Long): Flow<List<WithdrawRequest>> = dao.getWithdrawRequestsForUserFlow(userId)
    
    suspend fun createWithdrawRequest(
        userId: Long,
        userName: String,
        method: String,
        number: String,
        points: Int,
        cashAmount: Double
    ): Result<Long> {
        val user = dao.getUserById(userId) ?: return Result.failure(Exception("ব্যবহারকারী পাওয়া যায়নি!"))
        if (user.points < points) {
            return Result.failure(Exception("আপনার পর্যাপ্ত পয়েন্ট নেই! সর্বনিম্ন সীমা চেক করুন।"))
        }

        // Deduct points instantly
        val updatedUser = user.copy(points = user.points - points)
        dao.updateUser(updatedUser)

        val request = WithdrawRequest(
            userId = userId,
            userName = userName,
            paymentMethod = method,
            number = number,
            pointsAmount = points,
            cashAmount = cashAmount,
            status = "PENDING"
        )
        val reqId = dao.insertWithdrawRequest(request)

        // Record Log
        dao.insertEarningLog(
            EarningLog(
                userId = userId,
                description = "টাকা তোলার অনুরোধ ($method)",
                points = -points
            )
        )

        return Result.success(reqId)
    }

    suspend fun approveWithdrawRequest(requestId: Long): Boolean {
        val request = dao.getWithdrawRequestById(requestId) ?: return false
        if (request.status != "PENDING") return false

        val approvedRequest = request.copy(status = "APPROVED")
        dao.updateWithdrawRequest(approvedRequest)

        // Notify user
        dao.insertNotification(
            SystemNotification(
                title = "টাকা উত্তোলন সফল! 🏦",
                message = "আপনার ${request.pointsAmount} পয়েন্ট (${request.cashAmount} টাকা) উত্তোলন অনুরোধ সফলভাবে অনুমোদিত হয়েছে। আপনার $approvedRequest.paymentMethod অ্যাকাউন্টে টাকা পাঠানো হয়েছে।",
                targetUserId = request.userId
            )
        )
        return true
    }

    suspend fun rejectWithdrawRequest(requestId: Long): Boolean {
        val request = dao.getWithdrawRequestById(requestId) ?: return false
        if (request.status != "PENDING") return false

        val rejectedRequest = request.copy(status = "REJECTED")
        dao.updateWithdrawRequest(rejectedRequest)

        // Refund points to user!
        val user = dao.getUserById(request.userId)
        if (user != null) {
            val refundedUser = user.copy(points = user.points + request.pointsAmount)
            dao.updateUser(refundedUser)
            
            // Log Refund
            dao.insertEarningLog(
                EarningLog(
                    userId = request.userId,
                    description = "উত্তোলন প্রত্যাখ্যান রিফান্ড",
                    points = request.pointsAmount
                )
            )
        }

        // Notify user
        dao.insertNotification(
            SystemNotification(
                title = "টাকা উত্তোলন প্রত্যাখ্যান! ❌",
                message = "দুঃখিত, আপনার ${request.pointsAmount} পয়েন্টের উত্তোলন অনুরোধটি নামঞ্জুর করা হয়েছে এবং পয়েন্ট ফেরত দেওয়া হয়েছে। দয়া করে সঠিক নম্বর দিয়ে আবার চেষ্টা করুন।",
                targetUserId = request.userId
            )
        )
        return true
    }

    // Earnings History
    fun getEarningLogsForUser(userId: Long): Flow<List<EarningLog>> = dao.getEarningLogsForUserFlow(userId)

    // Notifications
    fun getNotificationsForUser(userId: Long): Flow<List<SystemNotification>> = dao.getNotificationsForUserFlow(userId)
    fun getAllNotificationsWithFlow(): Flow<List<SystemNotification>> = dao.getAllNotificationsFlow()
    
    suspend fun sendGlobalNotification(title: String, message: String) {
        dao.insertNotification(
            SystemNotification(
                title = title,
                message = message,
                targetUserId = null
            )
        )
    }

    // CheckIn / Activities
    suspend fun claimDailyCheckIn(userId: Long): Result<Int> {
        val user = dao.getUserById(userId) ?: return Result.failure(Exception("ব্যবহারকারী নেই!"))
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000
        
        // Simple daily check using date strings or dividing millisecond epoch by days to compare
        val dayNow = now / oneDayMs
        val dayLast = user.lastCheckIn / oneDayMs

        if (dayNow <= dayLast) {
            return Result.failure(Exception("আপনি আজকের বোনাস ইতিমধ্যে গ্রহণ করেছেন! অনুগ্রহ করে আগামীকাল আবার চেষ্টা করুন।"))
        }

        val dailyBonusPoints = 150
        val updatedUser = user.copy(
            points = user.points + dailyBonusPoints,
            lastCheckIn = now,
            spinsLeftToday = 10,  // Reset activities
            scratchesLeftToday = 10,
            videosLeftToday = 10
        )
        dao.updateUser(updatedUser)

        dao.insertEarningLog(
            EarningLog(
                userId = userId,
                description = "দৈনিক হাজিরা বোনাস 🎁",
                points = dailyBonusPoints
            )
        )

        return Result.success(dailyBonusPoints)
    }

    // Analytics
    fun getTotalUsersCount(): Flow<Int> = dao.getTotalUsersCountFlow()
    fun getPendingWithdrawCount(): Flow<Int> = dao.getPendingWithdrawCountFlow()
    fun getTotalApprovedWithdrawPoints(): Flow<Int?> = dao.getTotalApprovedWithdrawPointsFlow()

    suspend fun insertEarningLog(log: EarningLog) = dao.insertEarningLog(log)
    suspend fun insertNotification(notification: SystemNotification) = dao.insertNotification(notification)

    // Helper functions
    private fun generateReferralCode(): String {
        return UUID.randomUUID().toString().substring(0, 6).uppercase()
    }

    // Prepopulate DB (Demo ready)
    suspend fun prepopulateDb() {
        val existing = dao.getUserByEmail("admin@realcash.bd")
        if (existing == null) {
            // Create Admin
            dao.insertUser(
                User(
                    name = "Admin (ম্যানেজার)",
                    email = "admin@realcash.bd",
                    passwordStr = "admin123",
                    referralCode = "ADMIN001",
                    role = "ADMIN",
                    points = 999999
                )
            )

            // Create some users
            val u1Id = dao.insertUser(
                User(
                    name = "রাকিব হাসান",
                    email = "rakib@gmail.com",
                    passwordStr = "user123",
                    referralCode = "RAKIB01",
                    points = 2500,
                    registrationTime = System.currentTimeMillis() - 48 * 3600 * 1000
                )
            )
            val u2Id = dao.insertUser(
                User(
                    name = "সুমন ইসলাম",
                    email = "sumon@gmail.com",
                    passwordStr = "user123",
                    referralCode = "SUMON77",
                    referredBy = "RAKIB01",
                    points = 12000,
                    registrationTime = System.currentTimeMillis() - 24 * 3600 * 1000
                )
            )

            // Add logs for u1
            dao.insertEarningLog(EarningLog(userId = u1Id, description = "রেজিষ্ট্রেশন বোনাস", points = 100))
            dao.insertEarningLog(EarningLog(userId = u1Id, description = "স্পিন বোনাস", points = 50))
            dao.insertEarningLog(EarningLog(userId = u1Id, description = "ভিডিও ওয়াচ বোনাস", points = 150))
            dao.insertEarningLog(EarningLog(userId = u1Id, description = "রেফারেল বোনাস (সুমন)", points = 500))

            // Add logs for u2
            dao.insertEarningLog(EarningLog(userId = u2Id, description = "রেফারেল দিয়ে রেজিষ্ট্রেশন বোনাস", points = 300))
            dao.insertEarningLog(EarningLog(userId = u2Id, description = "দৈনিক বোনাস", points = 150))
            dao.insertEarningLog(EarningLog(userId = u2Id, description = "স্ক্র্যাচ কার্ড বোনাস", points = 250))
            dao.insertEarningLog(EarningLog(userId = u2Id, description = "স্পিন বোনাস", points = 300))

            // Add withdraw requests
            dao.insertWithdrawRequest(
                WithdrawRequest(
                    userId = u1Id,
                    userName = "রাকিব হাসান",
                    paymentMethod = "bKash",
                    number = "01712345678",
                    pointsAmount = 2000,
                    cashAmount = 20.0,
                    status = "APPROVED",
                    requestTime = System.currentTimeMillis() - 12 * 3600 * 1000
                )
            )

            dao.insertWithdrawRequest(
                WithdrawRequest(
                    userId = u2Id,
                    userName = "সুমন ইসলাম",
                    paymentMethod = "Nagad",
                    number = "01998765432",
                    pointsAmount = 5000,
                    cashAmount = 50.0,
                    status = "PENDING",
                    requestTime = System.currentTimeMillis() - 2 * 3600 * 1000
                )
            )

            // Notifications
            dao.insertNotification(
                SystemNotification(
                    title = "স্বাগতম রিয়েল ক্যাশ বিডি অ্যাপে! 🎉",
                    message = "আমাদের অ্যাপে কাজ করে প্রতিদিন ১০০ থেকে ৫০০ টাকা পর্যন্ত আয় করুন। ১০০০ পয়েন্ট হলে ফ্লেক্সিলোড এবং bKash/Nagad এ উথড্র করতে পারবেন। সঠিক নিয়মে কাজ করুন, ১০০% পেমেন্ট নিশ্চিত!",
                )
            )
            dao.insertNotification(
                SystemNotification(
                    title = "বিশেষ বোনাস অফার! 🌟",
                    message = "প্রতি সফল রেফারে ৫০০ পয়েন্ট বোনাস এবং ১০% আজীবন কমিশন! বেশি বেশি রেফার করুন এবং উপার্জন বাড়ান।",
                )
            )
        }
    }
}

package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// ROOM ENTITIES
// ==========================================

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true), Index(value = ["referralCode"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val passwordStr: String,
    val referralCode: String,
    val referredBy: String? = null,
    val points: Int = 0,
    val role: String = "USER", // "USER" or "ADMIN"
    val status: String = "ACTIVE", // "ACTIVE" or "BANNED"
    val registrationTime: Long = System.currentTimeMillis(),
    val lastCheckIn: Long = 0L,
    val spinsLeftToday: Int = 10,
    val scratchesLeftToday: Int = 10,
    val videosLeftToday: Int = 10,
    val deviceId: String = ""
)

@Entity(tableName = "withdraw_requests")
data class WithdrawRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val userName: String,
    val paymentMethod: String, // "bKash", "Nagad", "Rocket"
    val number: String,
    val pointsAmount: Int,
    val cashAmount: Double, // calculated value
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val requestTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "earning_logs")
data class EarningLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val description: String,
    val points: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_notifications")
data class SystemNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val targetUserId: Long? = null // null means sent to everyone
)

// ==========================================
// DATA ACCESS OBJECT (DAO)
// ==========================================

@Dao
interface AppDao {
    
    // --- User Queries ---
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE referralCode = :code LIMIT 1")
    suspend fun getUserByReferralCode(code: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): User?

    @Query("SELECT * FROM users WHERE role = 'USER' ORDER BY registrationTime DESC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun changeUserStatus(userId: Long, status: String)

    // --- Withdraw Requests Queries ---
    @Query("SELECT * FROM withdraw_requests ORDER BY requestTime DESC")
    fun getAllWithdrawRequestsFlow(): Flow<List<WithdrawRequest>>

    @Query("SELECT * FROM withdraw_requests WHERE userId = :userId ORDER BY requestTime DESC")
    fun getWithdrawRequestsForUserFlow(userId: Long): Flow<List<WithdrawRequest>>

    @Query("SELECT * FROM withdraw_requests WHERE id = :id LIMIT 1")
    suspend fun getWithdrawRequestById(id: Long): WithdrawRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawRequest(request: WithdrawRequest): Long

    @Update
    suspend fun updateWithdrawRequest(request: WithdrawRequest)

    // --- Earning Logs Queries ---
    @Query("SELECT * FROM earning_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getEarningLogsForUserFlow(userId: Long): Flow<List<EarningLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEarningLog(log: EarningLog): Long

    // --- System Notifications Queries ---
    @Query("SELECT * FROM system_notifications WHERE targetUserId IS NULL OR targetUserId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUserFlow(userId: Long): Flow<List<SystemNotification>>

    @Query("SELECT * FROM system_notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<SystemNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: SystemNotification): Long
    
    // --- Group Analytics Queries ---
    @Query("SELECT SUM(pointsAmount) FROM withdraw_requests WHERE status = 'APPROVED'")
    fun getTotalApprovedWithdrawPointsFlow(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM users")
    fun getTotalUsersCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM withdraw_requests WHERE status = 'PENDING'")
    fun getPendingWithdrawCountFlow(): Flow<Int>
}

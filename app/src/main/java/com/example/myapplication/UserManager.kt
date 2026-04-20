package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.CloudApiHelper.UserInfo

/**
 * 用户管理器
 * 用于保存和获取当前登录用户信息
 */
object UserManager {
    
    private const val PREF_NAME = "user_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_ROLE = "role"
    private const val KEY_CLASS_ID = "class_id"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_OLD_USER_ID = "old_user_id"  // 切换用户时用于清理旧缓存
    
    private lateinit var prefs: SharedPreferences
    
    /**
     * 初始化用户管理器
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 保存用户登录信息
     */
    fun saveUserLogin(userInfo: UserInfo) {
        with(prefs.edit()) {
            putLong(KEY_USER_ID, userInfo.userId)
            putString(KEY_USERNAME, userInfo.username)
            putInt(KEY_ROLE, userInfo.role)
            userInfo.classId?.let { putInt(KEY_CLASS_ID, it) }
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    /**
     * 获取当前用户ID
     */
    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, 0L)
    }
    
    /**
     * 获取旧用户ID（用于切换用户时清理缓存）
     */
    fun getOldUserId(): Long {
        return prefs.getLong(KEY_OLD_USER_ID, 0L)
    }
    
    /**
     * 更新旧用户ID为当前用户ID（切换用户时调用）
     */
    fun updateOldUserId() {
        val currentUserId = getUserId()
        if (currentUserId > 0) {
            prefs.edit().putLong(KEY_OLD_USER_ID, currentUserId).apply()
        }
    }
    
    /**
     * 获取当前用户名
     */
    fun getUsername(): String {
        return prefs.getString(KEY_USERNAME, "") ?: ""
    }
    
    /**
     * 获取当前用户角色
     * @return 0:学生, 1:教师
     */
    fun getUserRole(): Int {
        return prefs.getInt(KEY_ROLE, -1)
    }
    
    /**
     * 获取当前用户班级ID
     */
    fun getClassId(): Int? {
        return if (prefs.contains(KEY_CLASS_ID)) {
            prefs.getInt(KEY_CLASS_ID, 0)
        } else {
            null
        }
    }
    
    /**
     * 检查用户是否已登录
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * 获取完整的用户信息
     */
    fun getUserInfo(): UserInfo? {
        return if (isLoggedIn()) {
            UserInfo(
                userId = getUserId(),
                username = getUsername(),
                role = getUserRole(),
                classId = getClassId()
            )
        } else {
            null
        }
    }
    
    /**
     * 获取当前用户信息 (兼容旧代码)
     */
    fun getCurrentUser(): UserInfo? {
        return getUserInfo()
    }
    
    /**
     * 用户登出
     */
    fun logout() {
        with(prefs.edit()) {
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
            remove(KEY_ROLE)
            remove(KEY_CLASS_ID)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }
    
    /**
     * 检查当前用户是否是学生
     */
    fun isStudent(): Boolean {
        return getUserRole() == 0
    }
    
    /**
     * 检查当前用户是否是教师
     */
    fun isTeacher(): Boolean {
        return getUserRole() == 1
    }
    
    /**
     * 获取用户类型描述
     */
    fun getUserTypeDescription(): String {
        return when (getUserRole()) {
            0 -> "学生"
            1 -> "教师"
            else -> "未知用户"
        }
    }
}
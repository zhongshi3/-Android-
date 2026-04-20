package com.example.myapplication

/**
 * 用户验证工具类
 * 确保前端验证与云函数验证规则完全一致
 */
object UserValidationUtils {
    
    /**
     * 验证用户名格式（与云函数完全一致）
     * 规则：3-18位字母/数字/下划线
     */
    fun validateUsername(username: String): ValidationResult {
        return when {
            username.isEmpty() -> ValidationResult(false, "用户名不能为空")
            username.length < 3 -> ValidationResult(false, "用户名长度至少3位")
            username.length > 18 -> ValidationResult(false, "用户名长度不能超过18位")
            !username.matches(Regex("^[a-zA-Z0-9_]{3,18}$")) -> 
                ValidationResult(false, "用户名只能包含字母、数字和下划线")
            else -> ValidationResult(true, "用户名格式正确")
        }
    }
    
    /**
     * 验证用户名格式，返回布尔值
     */
    fun isValidUsername(username: String): Boolean {
        return username.matches(Regex("^[a-zA-Z0-9_]{3,18}$"))
    }
    
    /**
     * 验证密码格式
     * 规则：6-20位
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult(false, "密码不能为空")
            password.length < 6 -> ValidationResult(false, "密码长度至少6位")
            password.length > 20 -> ValidationResult(false, "密码长度不能超过20位")
            else -> ValidationResult(true, "密码格式正确")
        }
    }
    
    /**
     * 验证密码格式，返回布尔值
     */
    fun isValidPassword(password: String): Boolean {
        return password.length in 6..20
    }
    
    /**
     * 验证两次输入的密码是否一致
     */
    fun validatePasswordConfirmation(password: String, confirmPassword: String): ValidationResult {
        return if (password != confirmPassword) {
            ValidationResult(false, "两次输入的密码不一致")
        } else {
            ValidationResult(true, "密码一致")
        }
    }
    
    /**
     * 完整的注册验证
     */
    fun validateRegistration(username: String, password: String, confirmPassword: String): ValidationResult {
        // 验证用户名
        val usernameResult = validateUsername(username)
        if (!usernameResult.isValid) {
            return usernameResult
        }
        
        // 验证密码
        val passwordResult = validatePassword(password)
        if (!passwordResult.isValid) {
            return passwordResult
        }
        
        // 验证密码确认
        val confirmResult = validatePasswordConfirmation(password, confirmPassword)
        if (!confirmResult.isValid) {
            return confirmResult
        }
        
        return ValidationResult(true, "所有验证通过")
    }
    
    /**
     * 完整的登录验证
     */
    fun validateLogin(username: String, password: String): ValidationResult {
        // 验证用户名
        val usernameResult = validateUsername(username)
        if (!usernameResult.isValid) {
            return ValidationResult(false, "请输入有效的用户名")
        }
        
        // 验证密码
        val passwordResult = validatePassword(password)
        if (!passwordResult.isValid) {
            return ValidationResult(false, "请输入有效的密码")
        }
        
        return ValidationResult(true, "验证通过")
    }
}

/**
 * 验证结果数据类
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String
)
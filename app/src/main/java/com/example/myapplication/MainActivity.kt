package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent

class MainActivity : AppCompatActivity() {
    // 声明控件变量
    private lateinit var etAccount: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbAgreement: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var btnTeacherRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 隐藏ActionBar/TitleBar
        supportActionBar?.hide()
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 初始化用户管理器
        UserManager.init(this)

        // 初始化控件
        initView()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 登录按钮点击事件
        setLoginClickListener()
        // 教师注册按钮点击事件
        setTeacherRegisterClickListener()
    }

    //初始化

    private fun initView() {
        etAccount = findViewById(R.id.et_account)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnTeacherRegister = findViewById(R.id.btn_teacher_register)
    }


    private fun setLoginClickListener() {
        btnLogin.setOnClickListener {
            // 1. 获取输入内容
            val username = etAccount.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // 2. 校验输入
            if (!validateLoginInput(username, password)) {
                return@setOnClickListener
            }

            // 3. 显示加载状态
            btnLogin.isEnabled = false
            btnLogin.text = "登录中..."

            // 4. 调用云函数登录
            loginWithCloud(username, password)
        }
    }

    // 验证登录输入
    private fun validateLoginInput(username: String, password: String): Boolean {
        val validationResult = UserValidationUtils.validateLogin(username, password)
        if (!validationResult.isValid) {
            Toast.makeText(this, validationResult.message, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // 调用云函数登录
    private fun loginWithCloud(username: String, password: String) {
        CloudApiHelper.login(username, password, object : CloudApiHelper.LoginCallback {
            override fun onResult(response: CloudApiHelper.LoginResponse) {
                runOnUiThread {
                    btnLogin.isEnabled = true
                    btnLogin.text = "登录"

                    if (response.success) {
                        // 登录成功
                        val userInfo = response.userInfo
                        if (userInfo != null) {
                            // 检查并清理旧用户缓存（切换用户时）
                            clearOldUserCache(userInfo.userId)
                            
                            // 保存用户信息到UserManager
                            UserManager.saveUserLogin(userInfo)
                            println("用户信息已保存: userId=${userInfo.userId}, username=${userInfo.username}, role=${userInfo.role}")
                            
                            // 根据用户角色跳转到不同页面
                            when (userInfo.role) {
                                0 -> { // 学生
                                    Toast.makeText(
                                        this@MainActivity, 
                                        "欢迎回来，${userInfo.username}同学！", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val intent = Intent(this@MainActivity, StudentMainActivity::class.java)
                                    startActivity(intent)
                                    finish() // 关闭登录页，防止返回
                                }
                                1 -> { // 教师
                                    Toast.makeText(
                                        this@MainActivity, 
                                        "欢迎回来，${userInfo.username}老师！", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // TODO: 跳转到教师主页面
                                    val intent = Intent(this@MainActivity, TeacherMainActivity::class.java)
                                    startActivity(intent)
                                    finish() // 关闭登录页，防止返回
                                }
                                else -> {
                                    Toast.makeText(
                                        this@MainActivity, 
                                        "用户角色未知，请联系管理员", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            // 用户信息为空，默认跳转到学生页面
                            Toast.makeText(
                                this@MainActivity, 
                                "登录成功！欢迎使用离散数学学习助手", 
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this@MainActivity, StudentMainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        // 登录失败
                        Toast.makeText(
                            this@MainActivity, 
                            "登录失败：${response.message}", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
    
    /**
     * 清理旧用户缓存
     * 当切换用户时，清除前一个用户的答题状态缓存
     */
    private fun clearOldUserCache(newUserId: Long) {
        val oldUserId = UserManager.getOldUserId()
        
        // 只有当旧用户ID存在且与新用户不同时才清理
        if (oldUserId > 0 && oldUserId != newUserId) {
            println("切换用户，清理旧用户缓存: oldUserId=$oldUserId, newUserId=$newUserId")
            try {
                val cacheHelper = AnswerCacheHelper(this)
                cacheHelper.clearCache(oldUserId)
                cacheHelper.close()
                println("旧用户缓存清理完成")
            } catch (e: Exception) {
                println("清理旧用户缓存失败: ${e.message}")
            }
        }
        
        // 更新旧用户ID为当前用户ID
        UserManager.updateOldUserId()
    }

    private fun setTeacherRegisterClickListener() {
        btnTeacherRegister.setOnClickListener {
            // 跳转到教师注册页面
            val intent = Intent(this, TeacherRegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
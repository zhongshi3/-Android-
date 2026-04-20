package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject

class TeacherRegisterActivity : AppCompatActivity() {
    // 声明控件变量
    private lateinit var btnBack: ImageButton
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvUsernameHint: TextView
    private lateinit var tvPasswordHint: TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 隐藏ActionBar/TitleBar
        supportActionBar?.hide()
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_teacher_register)

        // 初始化控件
        initView()

        // 设置状态栏边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 设置返回按钮点击事件
        setBackClickListener()
        // 设置注册按钮点击事件
        setRegisterClickListener()
        // 设置用户名输入监听
        setupUsernameListener()
    }

    // 初始化控件
    private fun initView() {
        btnBack = findViewById(R.id.btn_back)
        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnRegister = findViewById(R.id.btn_register)
        tvUsernameHint = findViewById(R.id.tv_username_hint)
        tvPasswordHint = findViewById(R.id.tv_password_hint)
    }

    // 返回按钮点击事件
    private fun setBackClickListener() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    // 注册按钮点击事件
    private fun setRegisterClickListener() {
        btnRegister.setOnClickListener {
            // 1. 获取输入内容
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // 2. 校验输入
            if (!validateInput(username, password, confirmPassword)) {
                return@setOnClickListener
            }

            // 3. 显示加载状态
            btnRegister.isEnabled = false
            btnRegister.text = "注册中..."

            // 4. 调用云函数注册
            registerToCloud(username, password)
        }
    }

    // 验证输入
    private fun validateInput(username: String, password: String, confirmPassword: String): Boolean {
        val validationResult = UserValidationUtils.validateRegistration(username, password, confirmPassword)
        if (!validationResult.isValid) {
            Toast.makeText(this, validationResult.message, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // 调用云函数注册
    private fun registerToCloud(username: String, password: String) {
        CloudApiHelper.registerTeacher(username, password, object : CloudApiHelper.CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                runOnUiThread {
                    btnRegister.isEnabled = true
                    btnRegister.text = "注册"
                    Toast.makeText(this@TeacherRegisterActivity, "注册成功！", Toast.LENGTH_SHORT).show()
                    finish() // 返回登录页面
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    btnRegister.isEnabled = true
                    btnRegister.text = "注册"
                    Toast.makeText(this@TeacherRegisterActivity, error, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    // 设置用户名输入监听
    private fun setupUsernameListener() {
        etUsername.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val username = etUsername.text.toString().trim()
                if (username.isNotEmpty() && username.matches(Regex("^[a-zA-Z0-9_]{3,16}$"))) {
                    checkUsernameAvailability(username)
                }
            }
        }
    }

    // 检查用户名是否可用
    private fun checkUsernameAvailability(username: String) {
        tvUsernameHint.text = "检查用户名可用性..."
        tvUsernameHint.setTextColor(resources.getColor(android.R.color.holo_orange_dark, theme))

        CloudApiHelper.checkUsernameExists(username, object : CloudApiHelper.CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                runOnUiThread {
                    val exists = result.optBoolean("exists", false)
                    if (exists) {
                        tvUsernameHint.text = "用户名已被占用，请更换"
                        tvUsernameHint.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
                    } else {
                        // 注意：实际用户名重复检查在注册时由云函数完成
                        // 此处为初步检查，最终以注册结果为准
                        tvUsernameHint.text = "用户名格式正确，可用性以注册结果为准"
                        tvUsernameHint.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
                    }
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    tvUsernameHint.text = "检查失败，请重试"
                    tvUsernameHint.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
                }
            }
        })
    }
}
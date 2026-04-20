package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class UserProfileActivity : AppCompatActivity() {
    
    // 声明控件
    private lateinit var tvUsername: TextView
    private lateinit var tvUserType: TextView
    private lateinit var tvUserId: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnBack: ImageButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 隐藏ActionBar/TitleBar
        supportActionBar?.hide()
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)
        
        // 初始化用户管理器
        UserManager.init(this)
        
        // 初始化控件
        initViews()
        
        // 设置状态栏边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_profile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // 加载用户信息
        loadUserInfo()
        
        // 设置按钮点击事件
        setButtonClickListeners()
    }
    
    // 初始化视图
    private fun initViews() {
        tvUsername = findViewById(R.id.tv_username)
        tvUserType = findViewById(R.id.tv_user_type)
        tvUserId = findViewById(R.id.tv_user_id)
        btnLogout = findViewById(R.id.btn_logout)
        btnBack = findViewById(R.id.btn_back)
    }
    
    // 加载用户信息
    private fun loadUserInfo() {
        if (UserManager.isLoggedIn()) {
            // 显示用户信息
            tvUsername.text = "用户名：${UserManager.getUsername()}"
            tvUserType.text = "用户类型：${UserManager.getUserTypeDescription()}"
            tvUserId.text = "用户ID：${UserManager.getUserId()}"
        } else {
            // 用户未登录，直接跳转到登录页面
            Toast.makeText(this, "用户未登录，请先登录", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    // 设置按钮点击事件
    private fun setButtonClickListeners() {
        // 退出登录按钮
        btnLogout.setOnClickListener {
            logoutUser()
        }
        
        // 返回按钮
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    // 退出登录
    private fun logoutUser() {
        // 清除用户登录状态
        UserManager.logout()
        
        // 显示退出成功提示
        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
        
        // 跳转到登录页面
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        // 每次回到页面时重新检查登录状态
        if (!UserManager.isLoggedIn()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
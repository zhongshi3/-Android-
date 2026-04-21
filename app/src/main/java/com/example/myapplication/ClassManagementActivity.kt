package com.example.myapplication

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent

/**
 * 班级管理主页面
 * 提供班级统计、学生注册、新建班级等功能入口
 */
class ClassManagementActivity : AppCompatActivity() {
    
    private lateinit var btnBack: ImageButton
    
    private var teacherId: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supportActionBar?.hide()
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_class_management)
        
        // 初始化视图
        initViews()
        
        // 设置状态栏边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.class_management)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
        
        // 检查用户角色
        checkUserRole()
        
        // 设置按钮点击事件
        setButtonClickListeners()
    }
    
    /**
     * 初始化视图
     */
    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
    }
    
    /**
     * 设置按钮点击事件
     */
    private fun setButtonClickListeners() {
        // 班级答题统计 - 跳转到详情页面
        findViewById<android.view.View>(R.id.btn_class_statistics).setOnClickListener {
            val intent = Intent(this, ClassStatsDetailActivity::class.java)
            startActivity(intent)
        }

        // 学生注册
        findViewById<android.view.View>(R.id.btn_student_register).setOnClickListener {
            val intent = Intent(this, StudentRegisterActivity::class.java)
            startActivity(intent)
        }
        
        // 新建班级
        findViewById<android.view.View>(R.id.btn_create_class).setOnClickListener {
            val intent = Intent(this, CreateClassActivity::class.java)
            startActivity(intent)
        }
        
        // 返回按钮
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    /**
     * 检查用户角色
     * 只有教师可以访问班级管理功能
     */
    private fun checkUserRole() {
        val currentUser = UserManager.getCurrentUser()
        
        if (currentUser == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // 检查是否为教师
        if (currentUser.role != CloudApiHelper.Role.TEACHER) {
            Toast.makeText(this, "只有教师可以访问班级管理功能", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // 设置教师ID
        teacherId = currentUser.userId
    }
}

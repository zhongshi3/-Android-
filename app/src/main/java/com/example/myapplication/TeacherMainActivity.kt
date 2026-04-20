package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent

class TeacherMainActivity : AppCompatActivity() {
    // 声明功能按钮
    private lateinit var btnClassManage: Button
    private lateinit var btnQuestionManage: Button
    private lateinit var btnResourceManage: Button
    private lateinit var btnHomeworkReview: Button
    private lateinit var btnTeacherAvatar: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 隐藏ActionBar/TitleBar
        supportActionBar?.hide()
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_teacher_main)

        // 初始化按钮控件
        initViews()

        // 设置状态栏边距 - 只应用于主要布局容器
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.teacher_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 设置顶部padding为状态栏高度，确保内容不会与状态栏重叠
            v.setPadding(
                systemBars.left, 
                systemBars.top,  // 使用状态栏高度作为顶部padding
                systemBars.right, 
                systemBars.bottom
            )
            insets
        }

        // 设置按钮点击事件
        setButtonClickListeners()
    }

    // 初始化视图
    private fun initViews() {
        btnTeacherAvatar = findViewById(R.id.btn_teacher_avatar)
        btnClassManage = findViewById(R.id.btn_class_manage)
        btnQuestionManage = findViewById(R.id.btn_question_manage)
        btnResourceManage = findViewById(R.id.btn_resource_manage)
        btnHomeworkReview = findViewById(R.id.btn_homework_review)
    }

    // 设置按钮点击事件
    private fun setButtonClickListeners() {
        // 教师头像按钮点击事件
        btnTeacherAvatar.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        // 班级管理按钮点击事件
        btnClassManage.setOnClickListener {
            val intent = Intent(this, ClassManagementActivity::class.java)
            startActivity(intent)
        }

        // 题目管理按钮点击事件
        btnQuestionManage.setOnClickListener {
            Toast.makeText(this, "进入题目管理", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, QuestionManagementActivity::class.java)
            startActivity(intent)
        }

        // 资源管理按钮点击事件
        btnResourceManage.setOnClickListener {
            val intent = Intent(this, ResourceManagementActivity::class.java)
            startActivity(intent)
        }

        // 作业批改按钮点击事件
        btnHomeworkReview.setOnClickListener {
            // 导航到习题批改页面
            val intent = Intent(this, HomeworkGradingActivity::class.java)
            startActivity(intent)
        }
    }
}
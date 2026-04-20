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

class StudentMainActivity : AppCompatActivity() {
    // 声明控件
    private lateinit var btnUserAvatar: ImageButton
    private lateinit var btnKnowledgeBase: Button
    private lateinit var btnExerciseBook: Button
    private lateinit var btnErrorBook: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 隐藏ActionBar/TitleBar
        supportActionBar?.hide()
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_student_main)

        // 初始化按钮控件
        initViews()

        // 设置状态栏边距 - 只应用于主要布局容器
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.student_main)) { v, insets ->
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


//初始化所有按钮控件

    private fun initViews() {
        btnUserAvatar = findViewById<ImageButton>(R.id.btn_user_avatar)
        btnKnowledgeBase = findViewById(R.id.btn_knowledge_base)
        btnExerciseBook = findViewById(R.id.btn_exercise_book)
        btnErrorBook = findViewById(R.id.btn_error_book)
    }


    private fun setButtonClickListeners() {
        // 用户头像按钮点击
        btnUserAvatar.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        // 知识库按钮点击
        btnKnowledgeBase.setOnClickListener {
            Toast.makeText(this, "进入知识库", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, KnowledgeBaseActivity::class.java)
            startActivity(intent)
        }

        // 习题库按钮点击
        btnExerciseBook.setOnClickListener {
            Toast.makeText(this, "进入习题库", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, QuestionBankActivity::class.java)
            startActivity(intent)
        }

        // 错题本按钮点击
        btnErrorBook.setOnClickListener {
            val intent = Intent(this, ErrorBookNewActivity::class.java)
            startActivity(intent)
        }
    }
}
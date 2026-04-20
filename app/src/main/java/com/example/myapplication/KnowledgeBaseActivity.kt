package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent

class KnowledgeBaseActivity : AppCompatActivity() {
    // 返回按钮（ImageButton）
    private lateinit var btnBack: ImageButton
    private lateinit var btnKnowledgeGraph: Button
    private lateinit var btnClassResource: Button
    private lateinit var btnLearningProgress: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_knowledge_base)

        // 初始化控件
        initViews()


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.knowledge_base)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 设置按钮点击事件
        setButtonClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        btnKnowledgeGraph = findViewById(R.id.btn_knowledge_graph)
        btnClassResource = findViewById(R.id.btn_class_resource)
        btnLearningProgress = findViewById(R.id.btn_learning_progress)
    }

    private fun setButtonClickListeners() {
        // 返回按钮
        btnBack.setOnClickListener {
            finish()
        }

        // 功能按钮点击事件
        btnKnowledgeGraph.setOnClickListener {
            val intent = Intent(this, KnowledgeGraphActivity::class.java)
            startActivity(intent)
        }
        btnClassResource.setOnClickListener {
            val intent = Intent(this, ClassResourcesActivity::class.java)
            startActivity(intent)
        }
        btnLearningProgress.setOnClickListener {
            val intent = Intent(this, ProgressActivity::class.java)
            startActivity(intent)
        }
    }
}
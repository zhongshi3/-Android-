package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class QuestionManagementActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnUpload: Button
    private lateinit var rvQuestions: RecyclerView
    private lateinit var tvEmptyState: TextView

    private lateinit var dbHelper: QuestionDBHelper
    private lateinit var adapter: QuestionManagementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question_management)

        initViews()
        initData()
        setupListeners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.question_management)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        btnUpload = findViewById(R.id.btn_upload)
        rvQuestions = findViewById(R.id.rv_questions)
        tvEmptyState = findViewById(R.id.tv_empty_state)
    }

    private fun initData() {
        dbHelper = QuestionDBHelper(this)
        // 先同步云端数据，再加载
        syncAndLoadQuestions()
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnUpload.setOnClickListener {
            // 跳转到上传题目页面
            val intent = Intent(this, UploadQuestionActivity::class.java)
            startActivity(intent)
        }
    }
    
    /**
     * 同步云端数据后加载题目
     */
    private fun syncAndLoadQuestions() {
        val syncHelper = QuestionSyncHelper(this)
        
        syncHelper.syncQuestionsAsync(
            dbHelper = dbHelper,
            onStart = {
                // 可以显示加载进度
            },
            onSuccess = { result ->
                // 同步成功，加载本地数据
                loadQuestions()
                if (result.newQuestionsCount > 0) {
                    Toast.makeText(this, "已同步 ${result.newQuestionsCount} 道新题目", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { result ->
                // 同步失败，但仍尝试加载本地数据
                loadQuestions()
                Toast.makeText(this, "同步失败: ${result.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadQuestions() {
        val questions = dbHelper.getAllValidQuestions()
        
        if (questions.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
            adapter = QuestionManagementAdapter(questions,
                onItemClick = { question ->
                    // 点击题目可查看详情（教师端使用TeacherQuestionDetailActivity）
                    val intent = Intent(this, TeacherQuestionDetailActivity::class.java)
                    intent.putExtra("question_number", question.questionNumber)
                    startActivity(intent)
                },
                onDeleteClick = { question ->
                    // 删除确认对话框
                    showDeleteConfirmDialog(question.questionNumber)
                }
            )
            rvQuestions.layoutManager = LinearLayoutManager(this)
            rvQuestions.adapter = adapter
        }
    }
    
    private fun showDeleteConfirmDialog(questionNumber: Int) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除题目 #$questionNumber 吗？删除后学生将无法看到此题。")
            .setPositiveButton("确认删除") { _, _ ->
                deleteQuestion(questionNumber)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun deleteQuestion(questionNumber: Int) {
        val userId = getCurrentUserId()
        if (userId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }
        
        CloudApiHelper.deleteQuestion(userId, questionNumber, object : CloudApiHelper.DeleteQuestionCallback {
            override fun onResult(success: Boolean, message: String, data: CloudApiHelper.DeleteQuestionData?) {
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this@QuestionManagementActivity, "题目删除成功", Toast.LENGTH_SHORT).show()
                        // 重新同步并刷新页面
                        syncAndLoadQuestions()
                    } else {
                        Toast.makeText(this@QuestionManagementActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    
    private fun getCurrentUserId(): Long {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return prefs.getLong("user_id", -1)
    }

    private fun showEmptyState() {
        rvQuestions.visibility = View.GONE
        tvEmptyState.visibility = View.VISIBLE
    }
    
    private fun hideEmptyState() {
        rvQuestions.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        // 刷新数据
        loadQuestions()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}

package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ErrorBookNewActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvErrorCount: TextView
    private lateinit var rvErrors: RecyclerView
    private lateinit var tvEmptyState: TextView

    private lateinit var adapter: ErrorBookAdapter
    private var currentUserId: Long = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_error_book_new)


        initViews()
        setupListeners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.error_book_new)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvErrorCount = findViewById(R.id.tv_error_count)
        rvErrors = findViewById(R.id.rv_errors)
        tvEmptyState = findViewById(R.id.tv_empty_state)

        currentUserId = getCurrentUserId()
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadErrorQuestions() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        CloudApiHelper.getStudentErrorQuestions(currentUserId, object : CloudApiHelper.GetStudentErrorQuestionsCallback {
            override fun onResult(success: Boolean, message: String, data: List<CloudApiHelper.ErrorQuestionInfo>?) {
                runOnUiThread {
                    if (success && data != null) {
                        displayErrorQuestions(data)
                    } else {
                        Toast.makeText(this@ErrorBookNewActivity, message, Toast.LENGTH_SHORT).show()
                        displayErrorQuestions(emptyList())
                    }
                }
            }
        })
    }

    private fun displayErrorQuestions(errors: List<CloudApiHelper.ErrorQuestionInfo>) {
        tvErrorCount.text = "错题总数：${errors.size}"

        if (errors.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()

            adapter = ErrorBookAdapter(
                errors,
                onItemClick = { errorInfo ->
                    val intent = Intent(this, ErrorDetailActivity::class.java)
                    intent.putExtra("question_id", errorInfo.qId)
                    intent.putExtra("question_number", errorInfo.qId)
                    intent.putExtra("student_answer", errorInfo.studentAnswer)
                    intent.putExtra("teacher_msg", errorInfo.teacherMsg ?: "")
                    intent.putExtra("section_id", errorInfo.sectionId)
                    startActivity(intent)
                }
            )
            rvErrors.layoutManager = LinearLayoutManager(this)
            rvErrors.adapter = adapter
        }
    }

    private fun showEmptyState() {
        rvErrors.visibility = View.GONE
        tvEmptyState.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        rvErrors.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
    }

    private fun getCurrentUserId(): Long {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return prefs.getLong("user_id", -1)
    }

    override fun onResume() {
        super.onResume()
        loadErrorQuestions()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.CloudApiHelper.GetBatchStudentAnswersCallback
import com.example.myapplication.CloudApiHelper.StudentAnswer

class QuestionBankActivity : AppCompatActivity() {

    private lateinit var btnBack: View
    private lateinit var tvNotStartedCount: TextView
    private lateinit var tvNotCheckedCount: TextView
    private lateinit var tvCorrectCount: TextView
    private lateinit var tvWrongCount: TextView
    private lateinit var rvQuestions: RecyclerView
    private lateinit var tvEmptyState: TextView

    private lateinit var dbHelper: QuestionDBHelper
    private lateinit var answerCacheHelper: AnswerCacheHelper
    private lateinit var adapter: QuestionAdapter
    private lateinit var syncHelper: QuestionSyncHelper
    
    private var userId: Long = -1
    private var answerStatusMap: Map<Int, StudentAnswer> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question_bank)

        initViews()
        initData()
        setupListeners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.question_bank)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // 检查并触发习题同步
        checkAndSyncQuestions()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvNotStartedCount = findViewById(R.id.tv_not_started_count)
        tvNotCheckedCount = findViewById(R.id.tv_not_checked_count)
        tvCorrectCount = findViewById(R.id.tv_correct_count)
        tvWrongCount = findViewById(R.id.tv_wrong_count)
        rvQuestions = findViewById(R.id.rv_questions)
        tvEmptyState = findViewById(R.id.tv_empty_state)
    }

    private fun initData() {
        dbHelper = QuestionDBHelper(this)
        answerCacheHelper = AnswerCacheHelper(this)
        syncHelper = QuestionSyncHelper(this)
        userId = getCurrentUserId()

        // 从本地缓存加载答题状态
        if (userId > 0) {
            answerStatusMap = answerCacheHelper.getAnswers(userId)
        }


        // 检查数据库是否为空，如果为空则显示提示
        if (dbHelper.getAllValidQuestions().isEmpty()) {
            showEmptyState()
        } else {
            // 加载题目列表（只显示有效题目，节号不为0）
            loadQuestions()
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun checkAndSyncQuestions() {
        // 不再检查是否需要同步，而是总是检查更新
        // 显示加载提示
        Toast.makeText(this, "正在检查习题更新...", Toast.LENGTH_SHORT).show()
        
        syncHelper.syncQuestionsAsync(
            dbHelper = dbHelper,
            onStart = {
                // 同步开始，可以显示进度条
            },
            onSuccess = { result ->
                if (result.newQuestionsCount > 0 || result.deletedQuestionsCount > 0) {
                    Toast.makeText(
                        this,
                        "习题已更新: 新增${result.newQuestionsCount}题, 删除${result.deletedQuestionsCount}题",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // 刷新界面，隐藏空状态
                    hideEmptyState()
                    loadQuestions()
                } else if (dbHelper.getAllValidQuestions().isEmpty()) {
                    // 同步成功但没有数据，显示空状态
                    showEmptyState()
                    Toast.makeText(this, "暂无习题数据，请联系教师添加", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "习题已是最新", Toast.LENGTH_SHORT).show()
                    hideEmptyState()
                    loadQuestions()
                }
                
                // 同步完成后，批量查询云端答题状态
                syncAnswerStatusFromCloud()
            },
            onError = { result ->
                Toast.makeText(
                    this,
                    "习题更新失败: ${result.message}",
                    Toast.LENGTH_LONG
                ).show()
                
                // 如果数据库为空，显示空状态
                if (dbHelper.getAllValidQuestions().isEmpty()) {
                    showEmptyState()
                }
                
                // 仍然尝试查询答题状态（从缓存读取）
                loadQuestions()
            }
        )
    }
    
    /**
     * 从云端批量获取答题状态并更新本地缓存
     */
    private fun syncAnswerStatusFromCloud() {
        if (userId <= 0) {
            // 未登录，不查询云端状态
            updateStats()
            return
        }

        val questions = dbHelper.getAllValidQuestions()
        if (questions.isEmpty()) {
            updateStats()
            return
        }

        val qIds = questions.map { it.questionNumber }

        CloudApiHelper.getBatchStudentAnswers(userId, qIds, object : CloudApiHelper.GetBatchStudentAnswersCallback {
            override fun onResult(success: Boolean, message: String, data: Map<Int, StudentAnswer>?) {
                runOnUiThread {
                    if (success && data != null) {
                        // 保存到本地缓存
                        answerCacheHelper.saveAnswers(userId, data)
                        // 更新内存中的状态映射
                        answerStatusMap = data
                    }
                    // 刷新列表显示和统计数据
                    loadQuestions()
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
    
    private fun loadQuestions() {
        val questions = dbHelper.getAllValidQuestions()
        
        adapter = QuestionAdapter(questions, answerStatusMap) { question ->
            val intent = Intent(this, QuestionDetailActivity::class.java)
            intent.putExtra("question_number", question.questionNumber)
            startActivity(intent)
        }
        rvQuestions.layoutManager = LinearLayoutManager(this)
        rvQuestions.adapter = adapter
        
        updateStats()
    }
    
    private fun refreshData() {
        loadQuestions()
    }

    private fun updateStats() {
        val totalQuestions = dbHelper.getAllValidQuestions().size
        val answeredQuestions = answerStatusMap.size
        val notStartedCount = totalQuestions - answeredQuestions

        // 统计各状态
        var correctCount = 0
        var wrongCount = 0
        var notCheckedCount = 0

        answerStatusMap.values.forEach { answer ->
            when (answer.status) {
                0 -> notCheckedCount++  // 未批改
                1 -> correctCount++     // 正确
                2 -> wrongCount++        // 错误
            }
        }

        tvNotStartedCount.text = notStartedCount.toString()
        tvNotCheckedCount.text = notCheckedCount.toString()
        tvCorrectCount.text = correctCount.toString()
        tvWrongCount.text = wrongCount.toString()
    }

    override fun onResume() {
        super.onResume()
        // 刷新数据
        refreshData()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
        answerCacheHelper.close()
    }
}
package com.example.myapplication

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeworkGradingActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnAllQuestions: Button
    private lateinit var btnUngraded: Button
    private lateinit var tvTotalQuestions: TextView
    private lateinit var tvTotalUngraded: TextView
    private lateinit var rvQuestions: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var layoutLoading: LinearLayout

    private lateinit var adapter: HomeworkGradingAdapter
    private var currentUserId: Long = -1
    private var teacherId: Long = -1

    // 数据列表
    private var allQuestionsList: List<HomeworkGradingAdapter.QuestionGradingInfo> = emptyList()
    private var ungradedQuestionsList: List<HomeworkGradingAdapter.QuestionGradingInfo> = emptyList()

    // 当前筛选模式: true = 所有题目, false = 仅未完成批改
    private var showAllQuestions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_homework_grading)

        initViews()
        setupListeners()
        loadData()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homework_grading)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        btnAllQuestions = findViewById(R.id.btn_all_questions)
        btnUngraded = findViewById(R.id.btn_ungraded)
        tvTotalQuestions = findViewById(R.id.tv_total_questions)
        tvTotalUngraded = findViewById(R.id.tv_total_ungraded)
        rvQuestions = findViewById(R.id.rv_questions)
        tvEmptyState = findViewById(R.id.tv_empty_state)
        layoutLoading = findViewById(R.id.layout_loading)

        // 获取当前用户ID
        currentUserId = getCurrentUserId()
        teacherId = currentUserId

        // 初始化按钮高亮状态
        updateFilterButtons()
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnAllQuestions.setOnClickListener {
            showAllQuestions = true
            updateFilterButtons()
            filterAndDisplayQuestions()
        }

        btnUngraded.setOnClickListener {
            showAllQuestions = false
            updateFilterButtons()
            filterAndDisplayQuestions()
        }
    }

    private fun updateFilterButtons() {
        val blueColor = getColor(android.R.color.holo_blue_dark)
        val grayColor = getColor(android.R.color.darker_gray)

        if (showAllQuestions) {
            btnAllQuestions.backgroundTintList = ColorStateList.valueOf(blueColor)
            btnUngraded.backgroundTintList = ColorStateList.valueOf(grayColor)
        } else {
            btnAllQuestions.backgroundTintList = ColorStateList.valueOf(grayColor)
            btnUngraded.backgroundTintList = ColorStateList.valueOf(blueColor)
        }
    }

    private fun loadData() {
        showLoading()

        // 同时获取所有答题记录和未批改记录
        var allDataLoaded = false
        var ungradedDataLoaded = false
        var allAnsweredData: List<CloudApiHelper.QuestionAnsweredInfo> = emptyList()
        var ungradedData: List<CloudApiHelper.QuestionUngradedInfo> = emptyList()

        // 设置超时，防止API调用失败时界面一直等待
        android.os.Handler(mainLooper).postDelayed({
            if (!(allDataLoaded && ungradedDataLoaded)) {
                // 超时后仍然处理已加载的数据
                runOnUiThread {
                    processAllData(allAnsweredData, ungradedData)
                }
            }
        }, 10000) // 10秒超时

        // 获取所有答题记录
        CloudApiHelper.getTeacherAllAnsweredQuestions(teacherId, object : CloudApiHelper.GetTeacherAllAnsweredQuestionsCallback {
            override fun onResult(success: Boolean, message: String, data: List<CloudApiHelper.QuestionAnsweredInfo>?) {
                if (success) {
                    allAnsweredData = data ?: emptyList()
                } else {
                    runOnUiThread {
                        Toast.makeText(this@HomeworkGradingActivity, "获取答题记录失败: $message", Toast.LENGTH_SHORT).show()
                    }
                }
                allDataLoaded = true

                if (allDataLoaded && ungradedDataLoaded) {
                    runOnUiThread {
                        processAllData(allAnsweredData, ungradedData)
                    }
                }
            }
        })

        // 获取未批改记录
        CloudApiHelper.getTeacherUngradedQuestions(teacherId, object : CloudApiHelper.GetTeacherUngradedQuestionsCallback {
            override fun onResult(success: Boolean, message: String, data: List<CloudApiHelper.QuestionUngradedInfo>?) {
                if (success) {
                    ungradedData = data ?: emptyList()
                } else {
                    runOnUiThread {
                        Toast.makeText(this@HomeworkGradingActivity, "获取未批改题目失败: $message", Toast.LENGTH_SHORT).show()
                    }
                }
                ungradedDataLoaded = true

                if (allDataLoaded && ungradedDataLoaded) {
                    runOnUiThread {
                        processAllData(allAnsweredData, ungradedData)
                    }
                }
            }
        })
    }

    private fun processAllData(
        allAnsweredData: List<CloudApiHelper.QuestionAnsweredInfo>,
        ungradedData: List<CloudApiHelper.QuestionUngradedInfo>
    ) {
        android.util.Log.d("HomeworkGrading", "processAllData: allAnsweredData=${allAnsweredData.size}, ungradedData=${ungradedData.size}")
        android.util.Log.d("HomeworkGrading", "TeacherId: $teacherId")

        // 构建所有题目列表（从所有答题记录）
        val allQuestionMap = mutableMapOf<Int, HomeworkGradingAdapter.QuestionGradingInfo>()

        for (item in allAnsweredData) {
            val existing = allQuestionMap[item.qId]
            if (existing != null) {
                allQuestionMap[item.qId] = existing.copy(
                    totalCount = existing.totalCount + 1,
                    studentIds = existing.studentIds + item.userId
                )
            } else {
                allQuestionMap[item.qId] = HomeworkGradingAdapter.QuestionGradingInfo(
                    qId = item.qId,
                    totalCount = 1,
                    ungradedCount = 0,
                    studentIds = listOf(item.userId)
                )
            }
        }

        // 构建未批改题目列表（用于计算未批改人数）
        val ungradedMap = mutableMapOf<Int, MutableList<Long>>()
        for (item in ungradedData) {
            val list = ungradedMap.getOrPut(item.qId) { mutableListOf() }
            list.add(item.userId)
        }

        // 合并数据：更新未批改人数
        allQuestionsList = allQuestionMap.values.map { question ->
            val ungradedStudentIds = ungradedMap[question.qId] ?: emptyList()
            question.copy(
                ungradedCount = ungradedStudentIds.size,
                studentIds = question.studentIds  // 保持所有学生ID
            )
        }.sortedBy { it.qId }

        // 未批改列表
        ungradedQuestionsList = allQuestionsList.filter { it.ungradedCount > 0 }

        android.util.Log.d("HomeworkGrading", "allQuestionsList=${allQuestionsList.size}, ungradedQuestionsList=${ungradedQuestionsList.size}")

        // 更新统计
        val totalUngraded = ungradedQuestionsList.sumOf { it.ungradedCount }
        tvTotalQuestions.text = "总题目：${allQuestionsList.size}"
        tvTotalUngraded.text = "待批改总数：$totalUngraded"

        // 检查是否没有班级/学生
        if (allAnsweredData.isEmpty() && ungradedData.isEmpty()) {
            showEmptyState()
            tvEmptyState.text = "暂无答题记录\n请先创建班级并添加学生"
        } else {
            filterAndDisplayQuestions()
        }
    }

    private fun filterAndDisplayQuestions() {
        val displayList = if (showAllQuestions) allQuestionsList else ungradedQuestionsList

        if (displayList.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
            adapter = HomeworkGradingAdapter(
                displayList,
                showAllQuestions,
                onItemClick = { questionInfo ->
                    // 点击进入批改详情页面
                    val intent = Intent(this, GradingDetailActivity::class.java)
                    intent.putExtra("question_id", questionInfo.qId)
                    intent.putExtra("question_number", questionInfo.qId)
                    intent.putExtra("student_ids", questionInfo.studentIds.toLongArray())
                    startActivity(intent)
                }
            )
            rvQuestions.layoutManager = LinearLayoutManager(this)
            rvQuestions.adapter = adapter
        }
    }

    private fun showEmptyState() {
        rvQuestions.visibility = View.GONE
        tvEmptyState.visibility = View.VISIBLE
        layoutLoading.visibility = View.GONE
        tvEmptyState.text = if (showAllQuestions) "暂无答题记录" else "暂无待批改题目"
    }

    private fun hideEmptyState() {
        rvQuestions.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        layoutLoading.visibility = View.GONE
    }

    private fun showLoading() {
        layoutLoading.visibility = View.VISIBLE
        rvQuestions.visibility = View.GONE
        tvEmptyState.visibility = View.GONE
    }

    private fun getCurrentUserId(): Long {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return prefs.getLong("user_id", -1)
    }

    override fun onResume() {
        super.onResume()
        // 刷新数据
        loadData()
    }
}

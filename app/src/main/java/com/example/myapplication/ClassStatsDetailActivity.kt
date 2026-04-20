package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.myapplication.QuestionEntity

/**
 * 班级统计详情页面
 * 显示班级内学生的习题完成率和正确率统计
 */
class ClassStatsDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var spinnerClass: Spinner
    private lateinit var tvStudentCount: TextView
    private lateinit var tvCompletionRate: TextView
    private lateinit var progressCompletion: ProgressBar
    private lateinit var tvAccuracyRate: TextView
    private lateinit var progressAccuracy: ProgressBar
    private lateinit var rvChapterStats: RecyclerView
    private lateinit var layoutLoading: View
    private lateinit var tvEmpty: TextView

    private lateinit var adapter: ClassChapterStatsAdapter
    private lateinit var classDBHelper: ClassDBHelper
    private lateinit var questionDBHelper: QuestionDBHelper
    private lateinit var knowledgeDBHelper: KnowledgeGraphDBHelper

    private var teacherId: Long = 0
    private var classes = mutableListOf<CloudApiHelper.ClassInfo>()
    private var selectedClassId: Long = -1
    private var classStudentIds: List<Long> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_class_stats_detail)

        // 初始化数据库
        classDBHelper = ClassDBHelper(this)
        questionDBHelper = QuestionDBHelper(this)
        knowledgeDBHelper = KnowledgeGraphDBHelper(this)

        // 检查用户角色
        checkUserRole()

        // 初始化视图
        initViews()
        setupListeners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.class_stats_detail)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 加载班级数据
        loadClasses()
    }

    /**
     * 检查用户角色
     */
    private fun checkUserRole() {
        val currentUser = UserManager.getCurrentUser()

        if (currentUser == null) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show()
            finish()
        } else if (currentUser.role != 1) {
            Toast.makeText(this, "请以教师身份登录", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            teacherId = currentUser.userId
            android.util.Log.d("ClassStatsDetail", "教师登录: userId=$teacherId, role=${currentUser.role}, username=${currentUser.username}")
        }
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        spinnerClass = findViewById(R.id.spinner_class)
        tvStudentCount = findViewById(R.id.tv_student_count)
        tvCompletionRate = findViewById(R.id.tv_completion_rate)
        progressCompletion = findViewById(R.id.progress_completion)
        tvAccuracyRate = findViewById(R.id.tv_accuracy_rate)
        progressAccuracy = findViewById(R.id.progress_accuracy)
        rvChapterStats = findViewById(R.id.rv_chapter_stats)
        layoutLoading = findViewById(R.id.layout_loading)
        tvEmpty = findViewById(R.id.tv_empty)

        // 初始化answerCacheHelper
        answerCacheHelper = AnswerCacheHelper(this)

        // 设置RecyclerView
        adapter = ClassChapterStatsAdapter(emptyList())
        rvChapterStats.layoutManager = LinearLayoutManager(this)
        rvChapterStats.adapter = adapter
    }

    /**
     * 设置点击监听
     */
    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // 班级选择监听
        spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < classes.size) {
                    selectedClassId = classes[position].classId
                    loadClassStatistics(selectedClassId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 不做处理
            }
        }
    }

    /**
     * 加载班级列表
     */
    private fun loadClasses() {
        showLoading()

        android.util.Log.d("ClassStatsDetail", "loadClasses: teacherId = $teacherId")

        // 使用协程从云端获取班级
        lifecycleScope.launch {
            val response = withContext(Dispatchers.IO) {
                CloudApiHelper.getClassesByTeacher(teacherId)
            }

            android.util.Log.d("ClassStatsDetail", "loadClasses response: success=${response.success}, message=${response.message}, data.size=${response.data.size}")

            classes.clear()

            // 直接使用云端返回的班级数据（确保 classId 与云端一致）
            if (response.success && response.data.isNotEmpty()) {
                classes.addAll(response.data)
                android.util.Log.d("ClassStatsDetail", "添加了 ${response.data.size} 个班级")
            } else {
                android.util.Log.w("ClassStatsDetail", "获取班级失败或无数据: ${response.message}")
            }

            runOnUiThread {
                setupClassSpinner()
            }
        }
    }

    /**
     * 设置班级选择下拉框
     */
    private fun setupClassSpinner() {
        if (classes.isEmpty()) {
            hideLoading()
            tvEmpty.visibility = View.VISIBLE
            rvChapterStats.visibility = View.GONE
            return
        }

        val classNames = classes.map { it.className }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, classNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerClass.adapter = spinnerAdapter

        // 默认选择第一个班级
        if (classes.isNotEmpty()) {
            selectedClassId = classes[0].classId
            loadClassStatistics(selectedClassId)
        }
    }

    /**
     * 加载班级统计数据
     */
    private fun loadClassStatistics(classId: Long) {
        showLoading()

        // 获取该班级学生
        CloudApiHelper.getClassStudents(classId, object : CloudApiHelper.GetClassStudentsCallback {
            override fun onResult(success: Boolean, message: String, data: List<CloudApiHelper.StudentInfo>?) {
                runOnUiThread {
                    if (success && data != null) {
                        classStudentIds = data.map { it.userId }
                        tvStudentCount.text = "${classStudentIds.size} 人"

                        if (classStudentIds.isEmpty()) {
                            hideLoading()
                            showEmpty("该班级暂无学生\n请先添加学生")
                            return@runOnUiThread
                        }

                        // 获取班级所有学生的答题统计
                        loadStudentAnswersStats(classStudentIds)
                    } else {
                        hideLoading()
                        Toast.makeText(this@ClassStatsDetailActivity, "获取学生信息失败", Toast.LENGTH_SHORT).show()
                        showEmpty("获取学生信息失败")
                    }
                }
            }
        })
    }

    /**
     * 获取班级所有学生的答题统计数据
     */
    private fun loadStudentAnswersStats(studentIds: List<Long>) {
        // 获取所有题目信息
        val allQuestions = questionDBHelper.getAllValidQuestions()
        val totalQuestions = allQuestions.size

        if (totalQuestions == 0) {
            hideLoading()
            showEmpty("暂无题目数据")
            return
        }

        // 获取章节信息
        val chapters = knowledgeDBHelper.getChaptersByPartId(1) +
                       knowledgeDBHelper.getChaptersByPartId(2) +
                       knowledgeDBHelper.getChaptersByPartId(3) +
                       knowledgeDBHelper.getChaptersByPartId(4)

        // 获取章节与题目的映射
        val sectionQuestionsMap = mutableMapOf<Int, List<QuestionEntity>>()
        for (sectionId in allQuestions.map { it.sectionId }.distinct()) {
            sectionQuestionsMap[sectionId] = allQuestions.filter { it.sectionId == sectionId }
        }

        // 按章节统计
        val chapterStatsList = mutableListOf<ClassChapterStats>()
        var totalAnswered = 0
        var totalCorrect = 0

        for (chapter in chapters) {
            val sections = knowledgeDBHelper.getSectionsByChapterId(chapter.id)
            var chapterTotalQuestions = 0
            var chapterAnsweredQuestions = 0
            var chapterCorrectAnswers = 0

            for (section in sections) {
                val sectionQuestions = allQuestions.filter { it.sectionId == section.id }
                chapterTotalQuestions += sectionQuestions.size

                // 统计该章节的答题情况（每个学生的每道题只算一次）
                val answeredQIds = mutableSetOf<Int>()
                val correctQIds = mutableSetOf<Int>()

                for (studentId in studentIds) {
                    val answers = answerCacheHelper.getAnswers(studentId)
                    for ((qId, answer) in answers) {
                        if (sectionQuestions.any { it.questionNumber == qId }) {
                            answeredQIds.add(qId)
                            if (answer.status == 1) { // 正确
                                correctQIds.add(qId)
                            }
                        }
                    }
                }

                chapterAnsweredQuestions += answeredQIds.size
                chapterCorrectAnswers += correctQIds.size
            }

            val completionRate = if (chapterTotalQuestions > 0) {
                (chapterAnsweredQuestions * 100) / (chapterTotalQuestions * studentIds.size)
            } else 0

            val accuracyRate = if (chapterAnsweredQuestions > 0) {
                (chapterCorrectAnswers * 100) / chapterAnsweredQuestions
            } else 0

            if (chapterTotalQuestions > 0) {
                chapterStatsList.add(ClassChapterStats(
                    chapterId = chapter.id,
                    chapterName = "第${chapter.number}章 ${chapter.name}",
                    totalQuestions = chapterTotalQuestions,
                    answeredQuestions = chapterAnsweredQuestions,
                    correctAnswers = chapterCorrectAnswers,
                    completionRate = completionRate.coerceIn(0, 100),
                    accuracyRate = accuracyRate.coerceIn(0, 100)
                ))
            }

            totalAnswered += chapterAnsweredQuestions
            totalCorrect += chapterCorrectAnswers
        }

        // 计算总体统计
        val expectedAnswers = totalQuestions * studentIds.size
        val overallCompletionRate = if (expectedAnswers > 0) {
            (totalAnswered * 100) / expectedAnswers
        } else 0
        val overallAccuracyRate = if (totalAnswered > 0) {
            (totalCorrect * 100) / totalAnswered
        } else 0

        // 更新UI
        tvCompletionRate.text = "${overallCompletionRate}%"
        progressCompletion.progress = overallCompletionRate
        tvAccuracyRate.text = "${overallAccuracyRate}%"
        progressAccuracy.progress = overallAccuracyRate

        adapter.updateData(chapterStatsList)

        hideLoading()

        if (chapterStatsList.isEmpty()) {
            showEmpty("暂无答题数据\n请先让学生完成习题")
        } else {
            hideEmpty()
        }
    }

    /**
     * 显示加载中
     */
    private fun showLoading() {
        layoutLoading.visibility = View.VISIBLE
        rvChapterStats.visibility = View.GONE
        tvEmpty.visibility = View.GONE
    }

    /**
     * 隐藏加载中
     */
    private fun hideLoading() {
        layoutLoading.visibility = View.GONE
        rvChapterStats.visibility = View.VISIBLE
    }

    /**
     * 显示空状态
     */
    private fun showEmpty(message: String) {
        tvEmpty.text = message
        tvEmpty.visibility = View.VISIBLE
        rvChapterStats.visibility = View.GONE
    }

    /**
     * 隐藏空状态
     */
    private fun hideEmpty() {
        tvEmpty.visibility = View.GONE
        rvChapterStats.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        classDBHelper.close()
        questionDBHelper.close()
        knowledgeDBHelper.close()
        answerCacheHelper.close()
    }

    companion object {
        // 懒加载answerCacheHelper
        private lateinit var answerCacheHelper: AnswerCacheHelper
    }
}

package com.example.myapplication

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.net.HttpURLConnection
import java.net.URL

class TeacherQuestionDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvSection: TextView
    private lateinit var tvQuestionContent: TextView
    private lateinit var ivQuestionImage: ImageView
    private lateinit var tvStandardAnswer: TextView
    private lateinit var spinnerClass: Spinner
    private lateinit var progressLoading: ProgressBar
    private lateinit var layoutStats: View
    private lateinit var tvNotDoneCount: TextView
    private lateinit var tvNotCheckedCount: TextView
    private lateinit var tvWrongCount: TextView
    private lateinit var tvCorrectCount: TextView
    private lateinit var tvStatsSummary: TextView

    private lateinit var knowledgeGraphHelper: KnowledgeGraphDBHelper
    private lateinit var dbHelper: QuestionDBHelper

    private var questionNumber: Int = 0
    private var currentQuestion: QuestionEntity? = null
    private var teacherClasses: List<CloudApiHelper.ClassInfo> = emptyList()
    private var selectedClassId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_teacher_question_detail)

        questionNumber = intent.getIntExtra("question_number", 1)

        initViews()
        loadQuestion()
        setupListeners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvSection = findViewById(R.id.tv_section)
        tvQuestionContent = findViewById(R.id.tv_question_content)
        ivQuestionImage = findViewById(R.id.iv_question_image)
        tvStandardAnswer = findViewById(R.id.tv_standard_answer)
        spinnerClass = findViewById(R.id.spinner_class)
        progressLoading = findViewById(R.id.progress_loading)
        layoutStats = findViewById(R.id.layout_stats)
        tvNotDoneCount = findViewById(R.id.tv_not_done_count)
        tvNotCheckedCount = findViewById(R.id.tv_not_checked_count)
        tvWrongCount = findViewById(R.id.tv_wrong_count)
        tvCorrectCount = findViewById(R.id.tv_correct_count)
        tvStatsSummary = findViewById(R.id.tv_stats_summary)
    }

    private fun loadQuestion() {
        dbHelper = QuestionDBHelper(this)
        knowledgeGraphHelper = KnowledgeGraphDBHelper(this)
        currentQuestion = dbHelper.getQuestionByNumber(questionNumber)

        currentQuestion?.let { question ->
            // 获取节名
            val section = knowledgeGraphHelper.getSectionById(question.sectionId)
            val sectionName = section?.let { "${it.number}. ${it.name}" } ?: "未知章节"
            tvSection.text = "题号：${question.questionNumber}   所处章节：$sectionName"

            // 显示题目内容
            tvQuestionContent.text = question.content

            // 加载题目图片
            val imageUrl = question.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                loadQuestionImage(imageUrl)
            } else {
                ivQuestionImage.visibility = View.GONE
            }

            // 显示标准答案
            tvStandardAnswer.text = question.answer
        }

        // 加载教师班级列表
        loadTeacherClasses()
    }

    /**
     * 加载题目图片
     */
    private fun loadQuestionImage(imageUrl: String) {
        if (imageUrl.isBlank()) {
            ivQuestionImage.visibility = View.GONE
            return
        }

        ivQuestionImage.visibility = View.VISIBLE

        Thread {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()

                val inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                runOnUiThread {
                    if (bitmap != null) {
                        ivQuestionImage.setImageBitmap(bitmap)
                        ivQuestionImage.visibility = View.VISIBLE
                    } else {
                        ivQuestionImage.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    ivQuestionImage.visibility = View.GONE
                }
            }
        }.start()
    }

    /**
     * 加载教师管理的班级列表
     */
    private fun loadTeacherClasses() {
        val teacherId = getCurrentUserId()
        if (teacherId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        CloudApiHelper.getClasses(teacherId, object : CloudApiHelper.GetClassesCallback {
            override fun onResult(success: Boolean, message: String, classes: List<CloudApiHelper.ClassInfo>) {
                runOnUiThread {
                    if (success && classes.isNotEmpty()) {
                        teacherClasses = classes
                        setupClassSpinner(classes)
                    } else {
                        Toast.makeText(this@TeacherQuestionDetailActivity, "暂无管理的班级", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    /**
     * 设置班级选择下拉框
     */
    private fun setupClassSpinner(classes: List<CloudApiHelper.ClassInfo>) {
        val classNames = mutableListOf("请选择班级")
        classNames.addAll(classes.map { it.className })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, classNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerClass.adapter = adapter

        spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    selectedClassId = -1
                    layoutStats.visibility = View.GONE
                    return
                }
                val selectedClass = classes[position - 1]
                selectedClassId = selectedClass.classId
                // 查询该班级的答题统计
                queryClassAnswerStats(selectedClassId)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * 查询班级的答题统计
     */
    private fun queryClassAnswerStats(classId: Long) {
        progressLoading.visibility = View.VISIBLE
        layoutStats.visibility = View.GONE

        CloudApiHelper.getClassQuestionStats(
            classId = classId,
            qId = questionNumber,
            callback = object : CloudApiHelper.GetClassQuestionStatsCallback {
                override fun onResult(
                    success: Boolean,
                    message: String,
                    data: CloudApiHelper.ClassQuestionStats?
                ) {
                    runOnUiThread {
                        progressLoading.visibility = View.GONE

                        if (success && data != null) {
                            layoutStats.visibility = View.VISIBLE
                            displayStats(data)
                        } else {
                            Toast.makeText(this@TeacherQuestionDetailActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    /**
     * 显示统计数据
     */
    private fun displayStats(stats: CloudApiHelper.ClassQuestionStats) {
        // 更新数字显示
        tvNotDoneCount.text = stats.notDoneCount.toString()
        tvNotCheckedCount.text = stats.notCheckedCount.toString()
        tvWrongCount.text = stats.wrongCount.toString()
        tvCorrectCount.text = stats.correctCount.toString()

        // 计算百分比
        val total = stats.totalStudents
        val correctPercent = if (total > 0) (stats.correctCount * 100 / total) else 0

        // 更新统计说明
        tvStatsSummary.text = "班级总人数：$total | 已提交：${total - stats.notDoneCount} | 正确率：$correctPercent%"
    }

    private fun getCurrentUserId(): Long {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return prefs.getLong("user_id", -1)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
        knowledgeGraphHelper.close()
    }
}

package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class ErrorDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var tvQuestionContent: TextView
    private lateinit var ivQuestionImage: ImageView
    private lateinit var btnKnowledge: Button
    private lateinit var tvCorrectAnswer: TextView
    private lateinit var tvStudentAnswer: TextView
    private lateinit var tvAnswerResult: TextView
    private lateinit var layoutTeacherMsg: LinearLayout
    private lateinit var tvTeacherMsg: TextView

    private var questionId: Int = 0
    private var sectionId: Int = 0
    private lateinit var knowledgeDbHelper: KnowledgeGraphDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_error_detail)

        questionId = intent.getIntExtra("question_id", 0)
        sectionId = intent.getIntExtra("section_id", 0)

        knowledgeDbHelper = KnowledgeGraphDBHelper(this)
        initViews()
        setupListeners()
        loadQuestionData()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.error_detail)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_title)
        tvQuestionContent = findViewById(R.id.tv_question_content)
        ivQuestionImage = findViewById(R.id.iv_question_image)
        btnKnowledge = findViewById(R.id.btn_knowledge)
        tvCorrectAnswer = findViewById(R.id.tv_correct_answer)
        tvStudentAnswer = findViewById(R.id.tv_student_answer)
        tvAnswerResult = findViewById(R.id.tv_answer_result)
        layoutTeacherMsg = findViewById(R.id.layout_teacher_msg)
        tvTeacherMsg = findViewById(R.id.tv_teacher_msg)

        tvTitle.text = "第 ${questionId} 题"
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnKnowledge.setOnClickListener {
            if (sectionId > 0) {
                // 获取该节下的所有知识点
                val knowledgePoints = knowledgeDbHelper.getKnowledgePointsBySectionId(sectionId)
                if (knowledgePoints.isNotEmpty()) {
                    showKnowledgePointsDialog(knowledgePoints)
                } else {
                    Toast.makeText(this, "该节暂无知识点", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "该题目暂无关联知识点", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showKnowledgePointsDialog(knowledgePoints: List<KnowledgeGraphDBHelper.KnowledgePoint>) {
        val items = knowledgePoints.map { "${it.number}. ${it.name}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("选择知识点")
            .setItems(items) { _, which ->
                val selectedPoint = knowledgePoints[which]
                val intent = Intent(this, KnowledgeDetailActivity::class.java)
                intent.putExtra("nodeId", selectedPoint.id)
                intent.putExtra("nodeName", selectedPoint.name)
                intent.putExtra("nodeType", 3) // 3 = 知识点
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun loadQuestionData() {
        // 从本地数据库获取题目
        val dbHelper = QuestionDBHelper(this)
        val question = dbHelper.getQuestionByNumber(questionId)

        if (question != null) {
            tvQuestionContent.text = question.content
            tvCorrectAnswer.text = question.answer

            // 更新sectionId
            sectionId = question.sectionId

            // 根据是否有知识点来设置按钮状态
            if (sectionId <= 0) {
                btnKnowledge.isEnabled = false
                btnKnowledge.text = "暂无关联知识点"
            }

            // 加载图片
            if (!question.imageUrl.isNullOrEmpty()) {
                ivQuestionImage.visibility = View.VISIBLE
                Glide.with(this)
                    .load(question.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(ivQuestionImage)
            } else {
                ivQuestionImage.visibility = View.GONE
            }
        } else {
            tvQuestionContent.text = "题目不存在"
            tvCorrectAnswer.text = "-"
            btnKnowledge.isEnabled = false
            btnKnowledge.text = "题目不存在"
        }

        // 显示学生答案和教师留言
        val studentAnswer = intent.getStringExtra("student_answer") ?: ""
        val teacherMsg = intent.getStringExtra("teacher_msg") ?: ""

        tvStudentAnswer.text = if (studentAnswer.isNotEmpty()) studentAnswer else "无"

        // 根据status判断结果
        val status = intent.getIntExtra("status", 2) // 默认错误
        when (status) {
            1 -> {
                tvAnswerResult.text = "[正确]"
                tvAnswerResult.setTextColor(getColor(android.R.color.holo_green_dark))
                tvStudentAnswer.setTextColor(getColor(android.R.color.holo_green_dark))
            }
            2 -> {
                tvAnswerResult.text = "[错误]"
                tvAnswerResult.setTextColor(getColor(android.R.color.holo_red_dark))
                tvStudentAnswer.setTextColor(getColor(android.R.color.holo_red_dark))
            }
            0 -> {
                tvAnswerResult.text = "[待批改]"
                tvAnswerResult.setTextColor(getColor(android.R.color.holo_orange_dark))
            }
        }

        // 显示教师留言
        if (teacherMsg.isNotEmpty()) {
            layoutTeacherMsg.visibility = View.VISIBLE
            tvTeacherMsg.text = teacherMsg
        } else {
            layoutTeacherMsg.visibility = View.GONE
        }

        dbHelper.close()
    }

    private fun getSectionName(sectionId: Int): String {
        // 根据sectionId获取节名称
        val section = knowledgeDbHelper.getSectionById(sectionId)
        return section?.name ?: "第 ${sectionId} 节"
    }

    override fun onDestroy() {
        super.onDestroy()
        knowledgeDbHelper.close()
    }
}

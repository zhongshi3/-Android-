package com.example.myapplication

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.net.HttpURLConnection
import java.net.URL


class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvSection: TextView
    private lateinit var tvQuestionContent: TextView
    private lateinit var ivQuestionImage: ImageView
    private lateinit var knowledgeGraphHelper: KnowledgeGraphDBHelper
    private lateinit var etStudentAnswer: EditText
    private lateinit var btnSubmit: Button
    private lateinit var layoutSubmitButton: LinearLayout
    private lateinit var layoutResultStatus: LinearLayout
    private lateinit var tvResultStatus: TextView
    private lateinit var cardResult: View
    private lateinit var tvStandardAnswer: TextView
    private lateinit var tvTeacherComment: TextView
    private lateinit var tvTeacherCommentLabel: TextView

    private lateinit var dbHelper: QuestionDBHelper
    private var questionNumber: Int = 0
    private var currentQuestion: QuestionEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question_detail)

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
        etStudentAnswer = findViewById(R.id.et_student_answer)
        btnSubmit = findViewById(R.id.btn_submit)
        layoutSubmitButton = findViewById(R.id.layout_submit_button)
        layoutResultStatus = findViewById(R.id.layout_result_status)
        tvResultStatus = findViewById(R.id.tv_result_status)
        cardResult = findViewById(R.id.card_result)
        tvStandardAnswer = findViewById(R.id.tv_standard_answer)
        tvTeacherComment = findViewById(R.id.tv_teacher_comment)
        tvTeacherCommentLabel = findViewById(R.id.tv_teacher_comment_label)
    }

    private fun loadQuestion() {
        dbHelper = QuestionDBHelper(this)
        knowledgeGraphHelper = KnowledgeGraphDBHelper(this)
        currentQuestion = dbHelper.getQuestionByNumber(questionNumber)

        // 安全检查：确保视图已初始化
        if (!::tvSection.isInitialized || !::tvQuestionContent.isInitialized) {
            Toast.makeText(this, "视图初始化失败，请重启应用", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentQuestion?.let { question ->
            // 获取节名
            val sectionName = knowledgeGraphHelper.getSectionById(question.sectionId)?.name ?: "未知章节"
            tvSection.text = "题号：${question.questionNumber}   所处章节：$sectionName"
            tvQuestionContent.text = question.content
            
            // 加载题目图片（如果有的话）
            val imageUrl = question.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                loadQuestionImage(imageUrl)
            } else {
                ivQuestionImage.visibility = View.GONE
            }
            
            // 显示标准答案（无论何种状态都显示）
            tvStandardAnswer.text = question.answer

            // 查询云端答案
            queryCloudAnswer(question.questionNumber)
        }
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
    
    private fun queryCloudAnswer(questionNum: Int) {
        // 获取当前用户ID
        val userId = getCurrentUserId()
        if (userId <= 0) {
            // 未登录，不允许答题
            Toast.makeText(this, "请先登录后再答题", Toast.LENGTH_SHORT).show()
            showNoAnswerState(null)
            return
        }
        
        CloudApiHelper.getStudentAnswer(userId, questionNum, object : CloudApiHelper.GetStudentAnswerCallback {
            override fun onResult(success: Boolean, message: String, data: CloudApiHelper.StudentAnswer?) {
                runOnUiThread {
                    if (success) {
                        if (data != null) {
                            // 有答案记录
                            showAnswerState(data)
                        } else {
                            // 未查询到答案
                            showNoAnswerState(null)
                        }
                    } else {
                        // 查询失败，显示空状态
                        Toast.makeText(this@QuestionDetailActivity, "查询答案失败: $message", Toast.LENGTH_SHORT).show()
                        showNoAnswerState(null)
                    }
                }
            }
        })
    }
    
    private fun getCurrentUserId(): Long {
        // 从SharedPreferences获取当前登录用户ID
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return prefs.getLong("user_id", -1)
    }
    
    /**
     * 显示已批改状态的答案
     */
    private fun showAnswerState(answer: CloudApiHelper.StudentAnswer) {
        when (answer.status) {
            CloudApiHelper.AnswerStatus.CORRECT -> {
                // 已批改正确
                etStudentAnswer.setText(answer.studentAnswer)
                etStudentAnswer.isEnabled = false  // 不可修改
                
                // 隐藏提交按钮，显示正确状态
                layoutSubmitButton.visibility = View.GONE
                layoutResultStatus.visibility = View.VISIBLE
                tvResultStatus.text = "正确"
                tvResultStatus.setTextColor(ContextCompat.getColor(this, R.color.green))
                
                // 显示标准答案卡片
                cardResult.visibility = View.VISIBLE
                tvStandardAnswer.text = currentQuestion?.answer ?: ""
                
                // 显示教师留言
                if (!answer.teacherMsg.isNullOrEmpty()) {
                    tvTeacherCommentLabel.visibility = View.VISIBLE
                    tvTeacherComment.visibility = View.VISIBLE
                    tvTeacherComment.text = answer.teacherMsg
                } else {
                    tvTeacherCommentLabel.visibility = View.GONE
                    tvTeacherComment.visibility = View.GONE
                }
            }
            CloudApiHelper.AnswerStatus.WRONG -> {
                // 已批改错误
                etStudentAnswer.setText(answer.studentAnswer)
                etStudentAnswer.isEnabled = false  // 不可修改
                
                // 隐藏提交按钮，显示错误状态
                layoutSubmitButton.visibility = View.GONE
                layoutResultStatus.visibility = View.VISIBLE
                tvResultStatus.text = "错误"
                tvResultStatus.setTextColor(ContextCompat.getColor(this, R.color.red))
                
                // 显示标准答案卡片
                cardResult.visibility = View.VISIBLE
                tvStandardAnswer.text = currentQuestion?.answer ?: ""
                
                // 显示教师留言
                if (!answer.teacherMsg.isNullOrEmpty()) {
                    tvTeacherCommentLabel.visibility = View.VISIBLE
                    tvTeacherComment.visibility = View.VISIBLE
                    tvTeacherComment.text = answer.teacherMsg
                } else {
                    tvTeacherCommentLabel.visibility = View.GONE
                    tvTeacherComment.visibility = View.GONE
                }
            }
            CloudApiHelper.AnswerStatus.NOT_CHECKED -> {
                // 未批改状态
                showNoAnswerState(answer)
            }
        }
    }
    
    /**
     * 显示未提交或未批改状态的答案
     */
    private fun showNoAnswerState(existingAnswer: CloudApiHelper.StudentAnswer?) {
        // 允许编辑答案
        etStudentAnswer.isEnabled = true
        
        // 如果有已提交的答案，显示它
        if (existingAnswer != null) {
            etStudentAnswer.setText(existingAnswer.studentAnswer)
        } else {
            etStudentAnswer.setText("")
        }
        
        // 显示提交按钮
        layoutSubmitButton.visibility = View.VISIBLE
        layoutResultStatus.visibility = View.GONE
        
        // 隐藏结果卡片
        cardResult.visibility = View.GONE
        
        // 更新按钮状态
        updateSubmitButtonState()
    }
    
    private fun updateSubmitButtonState() {
        val answer = etStudentAnswer.text.toString().trim()
        btnSubmit.isEnabled = answer.isNotEmpty()
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSubmit.setOnClickListener {
            submitAnswer()
        }

        // 实时监听输入变化，更新按钮状态
        etStudentAnswer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSubmitButtonState()
            }
        })
    }
    
    private fun submitAnswer() {
        val answer = etStudentAnswer.text.toString().trim()
        if (answer.isEmpty()) {
            Toast.makeText(this, "请输入答案", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userId = getCurrentUserId()
        if (userId <= 0) {
            Toast.makeText(this, "请先登录后再答题", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 禁用按钮，防止重复提交
        btnSubmit.isEnabled = false
        btnSubmit.text = "提交中..."
        
        CloudApiHelper.submitAnswer(userId, questionNumber, answer, object : CloudApiHelper.SubmitAnswerCallback {
            override fun onResult(success: Boolean, message: String, data: CloudApiHelper.StudentAnswer?) {
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this@QuestionDetailActivity, message, Toast.LENGTH_SHORT).show()
                        
                        // 刷新显示状态（应该显示未批改状态）
                        if (data != null) {
                            showNoAnswerState(data)
                        } else {
                            showNoAnswerState(null)
                        }
                        
                        // 重新查询以更新状态
                        queryCloudAnswer(questionNumber)
                    } else {
                        Toast.makeText(this@QuestionDetailActivity, message, Toast.LENGTH_SHORT).show()
                        btnSubmit.isEnabled = true
                        btnSubmit.text = "提交答案"
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
        knowledgeGraphHelper.close()
    }
}

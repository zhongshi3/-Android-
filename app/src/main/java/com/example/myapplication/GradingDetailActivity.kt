package com.example.myapplication

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class GradingDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var tvQuestionContent: TextView
    private lateinit var ivQuestionImage: ImageView
    private lateinit var tvCorrectAnswer: TextView
    private lateinit var spinnerStudents: Spinner
    private lateinit var tvStudentAnswer: TextView
    private lateinit var tvAnswerStatus: TextView
    private lateinit var etTeacherMsg: android.widget.EditText
    private lateinit var btnCorrect: Button
    private lateinit var btnWrong: Button
    private lateinit var btnSubmit: Button
    private lateinit var tvHint: TextView

    private var questionId: Int = 0
    private var currentUserId: Long = -1
    private var studentIds: LongArray = longArrayOf()

    // 学生列表
    private var studentsList: MutableList<CloudApiHelper.StudentInfo> = mutableListOf()
    private var selectedStudentIndex: Int = -1

    // 当前批改状态
    private var isCorrectSelected: Boolean? = null  // null = 未选择, true = 正确, false = 错误
    private var isAlreadyGraded: Boolean = false     // 是否已批改过

    // 当前选中的答案ID
    private var currentAnswerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_grading_detail)

        // 获取参数
        questionId = intent.getIntExtra("question_id", 0)
        val questionNumber = intent.getIntExtra("question_number", questionId)
        studentIds = intent.getLongArrayExtra("student_ids") ?: longArrayOf()

        currentUserId = getCurrentUserId()

        initViews()
        setupListeners()
        loadQuestionData()
        loadStudentsData()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.grading_detail)) { v, insets ->
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
        tvCorrectAnswer = findViewById(R.id.tv_correct_answer)
        spinnerStudents = findViewById(R.id.spinner_students)
        tvStudentAnswer = findViewById(R.id.tv_student_answer)
        tvAnswerStatus = findViewById(R.id.tv_answer_status)
        etTeacherMsg = findViewById(R.id.et_teacher_msg)
        btnCorrect = findViewById(R.id.btn_correct)
        btnWrong = findViewById(R.id.btn_wrong)
        btnSubmit = findViewById(R.id.btn_submit)
        tvHint = findViewById(R.id.tv_hint)

        tvTitle.text = "批改第 ${questionId} 题"
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        spinnerStudents.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStudentIndex = position
                if (position >= 0 && position < studentsList.size) {
                    loadStudentAnswer(studentsList[position])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedStudentIndex = -1
            }
        }

        btnCorrect.setOnClickListener {
            isCorrectSelected = true
            updateGradeButtons()
        }

        btnWrong.setOnClickListener {
            isCorrectSelected = false
            updateGradeButtons()
        }

        btnSubmit.setOnClickListener {
            submitGrade()
        }
    }

    private fun updateGradeButtons() {
        val correctColor: Int
        val wrongColor: Int

        if (isCorrectSelected == true) {
            correctColor = getColor(android.R.color.holo_green_dark)
            wrongColor = getColor(android.R.color.darker_gray)
        } else if (isCorrectSelected == false) {
            correctColor = getColor(android.R.color.darker_gray)
            wrongColor = getColor(android.R.color.holo_red_dark)
        } else {
            correctColor = getColor(android.R.color.darker_gray)
            wrongColor = getColor(android.R.color.darker_gray)
        }

        btnCorrect.backgroundTintList = ColorStateList.valueOf(correctColor)
        btnWrong.backgroundTintList = ColorStateList.valueOf(wrongColor)
        btnSubmit.isEnabled = isCorrectSelected != null
    }

    private fun loadQuestionData() {
        // 从本地数据库获取题目
        val dbHelper = QuestionDBHelper(this)
        val question = dbHelper.getQuestionByNumber(questionId)

        if (question != null) {
            tvQuestionContent.text = question.content
            tvCorrectAnswer.text = question.answer

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
        }

        dbHelper.close()
    }

    private fun loadStudentsData() {
        if (studentIds.isEmpty()) {
            Toast.makeText(this, "暂无待批改学生", Toast.LENGTH_SHORT).show()
            return
        }

        // 获取学生信息
        CloudApiHelper.getStudentsInfo(studentIds.toList(), object : CloudApiHelper.GetStudentsInfoCallback {
            override fun onResult(success: Boolean, message: String, data: List<CloudApiHelper.StudentInfo>?) {
                runOnUiThread {
                    if (success && data != null) {
                        studentsList.clear()
                        studentsList.addAll(data)
                        setupStudentSpinner()
                    } else {
                        Toast.makeText(this@GradingDetailActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun setupStudentSpinner() {
        val studentNames = studentsList.map { it.username }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, studentNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStudents.adapter = adapter

        if (studentsList.isNotEmpty()) {
            // 自动选中第一个学生
            spinnerStudents.setSelection(0)
        }
    }

    private fun loadStudentAnswer(student: CloudApiHelper.StudentInfo) {
        // 重置状态
        isCorrectSelected = null
        isAlreadyGraded = false
        currentAnswerId = null
        updateGradeButtons()
        btnSubmit.text = "确认上传"
        btnSubmit.isEnabled = false

        // 获取该学生的答案
        CloudApiHelper.getStudentAnswer(student.userId, questionId, object : CloudApiHelper.GetStudentAnswerCallback {
            override fun onResult(success: Boolean, message: String, data: CloudApiHelper.StudentAnswer?) {
                runOnUiThread {
                    if (success && data != null) {
                        tvStudentAnswer.text = data.studentAnswer

                        // 检查是否已批改
                        when (data.status) {
                            0 -> {
                                tvAnswerStatus.text = "[未批改]"
                                tvAnswerStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                                etTeacherMsg.setText(data.teacherMsg ?: "")
                            }
                            1 -> {
                                tvAnswerStatus.text = "[已批改-正确]"
                                tvAnswerStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                                etTeacherMsg.setText(data.teacherMsg ?: "")
                                // 已批改，自动填充状态
                                isCorrectSelected = true
                                isAlreadyGraded = true
                                updateGradeButtons()
                                btnSubmit.text = "重新上传"
                                btnSubmit.isEnabled = true
                            }
                            2 -> {
                                tvAnswerStatus.text = "[已批改-错误]"
                                tvAnswerStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                                etTeacherMsg.setText(data.teacherMsg ?: "")
                                // 已批改，自动填充状态
                                isCorrectSelected = false
                                isAlreadyGraded = true
                                updateGradeButtons()
                                btnSubmit.text = "重新上传"
                                btnSubmit.isEnabled = true
                            }
                        }
                    } else {
                        tvStudentAnswer.text = "未找到答案"
                        tvAnswerStatus.text = ""
                    }
                }
            }
        })
    }

    private fun submitGrade() {
        if (isCorrectSelected == null) {
            Toast.makeText(this, "请先选择批改结果", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedStudentIndex < 0 || selectedStudentIndex >= studentsList.size) {
            Toast.makeText(this, "请先选择学生", Toast.LENGTH_SHORT).show()
            return
        }

        val student = studentsList[selectedStudentIndex]
        val status = if (isCorrectSelected == true) CloudApiHelper.AnswerStatus.CORRECT else CloudApiHelper.AnswerStatus.WRONG
        val teacherMsg = etTeacherMsg.text.toString().trim()

        // 上传批改结果
        CloudApiHelper.submitGrade(
            currentUserId,
            student.userId,
            questionId,
            status,
            teacherMsg,
            object : CloudApiHelper.SubmitGradeCallback {
                override fun onResult(success: Boolean, message: String) {
                    runOnUiThread {
                        if (success) {
                            showSuccessDialog()
                        } else {
                            Toast.makeText(this@GradingDetailActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    private fun showSuccessDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_grading_success, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btn_continue).setOnClickListener {
            dialog.dismiss()
            gotoNextUngraded()
        }

        dialogView.findViewById<Button>(R.id.btn_finish).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun gotoNextUngraded() {
        // 查找下一个未批改的学生
        var foundNext = false
        for (i in studentsList.indices) {
            if (i > selectedStudentIndex) {
                // 检查该学生是否未批改
                val student = studentsList[i]
                CloudApiHelper.getStudentAnswer(student.userId, questionId, object : CloudApiHelper.GetStudentAnswerCallback {
                    override fun onResult(success: Boolean, message: String, data: CloudApiHelper.StudentAnswer?) {
                        runOnUiThread {
                            if (success && data != null && data.status == 0) {
                                // 找到未批改的学生
                                spinnerStudents.setSelection(i)
                                foundNext = true
                            }
                        }
                    }
                })
                if (foundNext) break
            }
        }

        if (!foundNext) {
            // 没有找到下一个未批改的学生
            Toast.makeText(this, "该题目已全部批改完成！", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun getCurrentUserId(): Long {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return prefs.getLong("user_id", -1)
    }
}

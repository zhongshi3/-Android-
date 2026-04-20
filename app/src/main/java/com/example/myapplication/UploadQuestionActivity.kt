package com.example.myapplication

import android.content.Intent
import android.net.Uri
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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import java.io.File

class UploadQuestionActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var etContent: android.widget.EditText
    private lateinit var etAnswer: android.widget.EditText
    private lateinit var btnSelectImage: Button
    private lateinit var tvImageName: TextView
    private lateinit var ivPreview: ImageView
    private lateinit var spinnerPart: Spinner
    private lateinit var spinnerChapter: Spinner
    private lateinit var spinnerSection: Spinner
    private lateinit var tvSelectedSection: TextView
    private lateinit var btnUpload: Button

    private lateinit var knowledgeGraphHelper: KnowledgeGraphDBHelper
    
    private var selectedImageUri: Uri? = null
    private var selectedPartId: Int = -1
    private var selectedChapterId: Int = -1
    private var selectedSectionId: Int = -1

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            tvImageName.text = "已选择图片"
            ivPreview.visibility = View.VISIBLE
            Glide.with(this)
                .load(it)
                .into(ivPreview)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_upload_question)

        knowledgeGraphHelper = KnowledgeGraphDBHelper(this)

        initViews()
        setupSpinners()
        setupListeners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        etContent = findViewById(R.id.et_content)
        etAnswer = findViewById(R.id.et_answer)
        btnSelectImage = findViewById(R.id.btn_select_image)
        tvImageName = findViewById(R.id.tv_image_name)
        ivPreview = findViewById(R.id.iv_preview)
        spinnerPart = findViewById(R.id.spinner_part)
        spinnerChapter = findViewById(R.id.spinner_chapter)
        spinnerSection = findViewById(R.id.spinner_section)
        tvSelectedSection = findViewById(R.id.tv_selected_section)
        btnUpload = findViewById(R.id.btn_upload)
    }

    private fun setupSpinners() {
        // 加载部分列表
        val parts = knowledgeGraphHelper.getAllParts()
        val partNames = mutableListOf("请选择部分")
        partNames.addAll(parts.map { it.name })
        
        val partAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, partNames)
        partAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPart.adapter = partAdapter
        
        spinnerPart.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    selectedPartId = -1
                    clearChapterSpinner()
                    clearSectionSpinner()
                    return
                }
                val part = parts[position - 1]
                selectedPartId = part.id
                loadChapters(part.id)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadChapters(partId: Int) {
        val chapters = knowledgeGraphHelper.getChaptersByPartId(partId)
        val chapterNames = mutableListOf("请选择章")
        chapterNames.addAll(chapters.map { "${it.number}. ${it.name}" })
        
        val chapterAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, chapterNames)
        chapterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerChapter.adapter = chapterAdapter
        
        spinnerChapter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    selectedChapterId = -1
                    clearSectionSpinner()
                    return
                }
                val chapter = chapters[position - 1]
                selectedChapterId = chapter.id
                loadSections(chapter.id)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        clearSectionSpinner()
    }

    private fun loadSections(chapterId: Int) {
        val sections = knowledgeGraphHelper.getSectionsByChapterId(chapterId)
        val sectionNames = mutableListOf("请选择节")
        sectionNames.addAll(sections.map { "${it.number}. ${it.name}" })
        
        val sectionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sectionNames)
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSection.adapter = sectionAdapter
        
        spinnerSection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    selectedSectionId = -1
                    tvSelectedSection.visibility = View.GONE
                    return
                }
                val section = sections[position - 1]
                selectedSectionId = section.id
                tvSelectedSection.visibility = View.VISIBLE
                tvSelectedSection.text = "已选择：${section.number}. ${section.name}"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun clearChapterSpinner() {
        val emptyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("请先选择部分"))
        emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerChapter.adapter = emptyAdapter
        selectedChapterId = -1
    }

    private fun clearSectionSpinner() {
        val emptyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("请先选择章"))
        emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSection.adapter = emptyAdapter
        selectedSectionId = -1
        tvSelectedSection.visibility = View.GONE
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSelectImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnUpload.setOnClickListener {
            // 验证输入
            val content = etContent.text.toString().trim()
            val answer = etAnswer.text.toString().trim()

            if (content.isEmpty()) {
                Toast.makeText(this, "请输入题目内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (answer.isEmpty()) {
                Toast.makeText(this, "请输入答案", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedSectionId <= 0) {
                Toast.makeText(this, "请选择题目所属章节", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 显示确认对话框
            showConfirmDialog(content, answer)
        }
    }

    private fun showConfirmDialog(content: String, answer: String) {
        val section = knowledgeGraphHelper.getSectionById(selectedSectionId)
        val sectionName = section?.let { "${it.number}. ${it.name}" } ?: "未知"
        
        AlertDialog.Builder(this)
            .setTitle("确认上传")
            .setMessage("""请检查题目信息：
                
题目内容：${content.take(50)}${if (content.length > 50) "..." else ""}

答案：${answer.take(30)}${if (answer.length > 30) "..." else ""}

所属章节：$sectionName

图片：${if (selectedImageUri != null) "已选择" else "无"}

确认上传吗？""")
            .setPositiveButton("确认上传") { _, _ ->
                uploadQuestion(content, answer)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun uploadQuestion(content: String, answer: String) {
        val userId = getCurrentUserId()
        if (userId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        // 禁用按钮，防止重复提交
        btnUpload.isEnabled = false
        btnUpload.text = "上传中..."

        // 如果有图片，先上传图片
        if (selectedImageUri != null) {
            uploadImageWithCloudStorage { imageUrl ->
                if (imageUrl != null) {
                    // 图片上传成功，继续上传题目
                    doUploadQuestion(userId, content, answer, imageUrl)
                } else {
                    // 图片上传失败，但仍继续上传题目（无图片）
                    runOnUiThread {
                        Toast.makeText(this@UploadQuestionActivity, "图片上传失败，将不上传图片", Toast.LENGTH_SHORT).show()
                    }
                    doUploadQuestion(userId, content, answer, null)
                }
            }
        } else {
            // 没有图片，直接上传题目
            doUploadQuestion(userId, content, answer, null)
        }
    }

    /**
     * 将图片上传到云存储
     */
    private fun uploadImageWithCloudStorage(callback: (String?) -> Unit) {
        val uri = selectedImageUri ?: return callback(null)

        // 生成文件名
        val fileName = "question_image_${System.currentTimeMillis()}.jpg"

        // 将Uri转换为文件并转为Base64
        val file = getFileFromUri(uri)
        if (file == null || !file.exists()) {
            callback(null)
            return
        }

        try {
            val bytes = file.readBytes()
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            val imageData = "data:image/jpeg;base64,$base64"

            // 通过云函数上传图片
            CloudApiHelper.uploadImageToCloud(fileName, imageData, object : CloudApiHelper.UploadImageCallback {
                override fun onResult(success: Boolean, message: String, imageUrl: String?) {
                    runOnUiThread {
                        if (success && !imageUrl.isNullOrEmpty()) {
                            callback(imageUrl)
                        } else {
                            Toast.makeText(this@UploadQuestionActivity, "图片上传失败: $message", Toast.LENGTH_SHORT).show()
                            callback(null)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            callback(null)
        }
    }

    /**
     * 将Uri转换为File
     */
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 执行题目上传
     */
    private fun doUploadQuestion(userId: Long, content: String, answer: String, imageUrl: String?) {
        CloudApiHelper.uploadQuestion(
            userId = userId,
            content = content,
            answer = answer,
            sectionId = selectedSectionId,
            imageUrl = imageUrl,
            callback = object : CloudApiHelper.UploadQuestionCallback {
                override fun onResult(success: Boolean, message: String, data: CloudApiHelper.UploadQuestionData?) {
                    runOnUiThread {
                        btnUpload.isEnabled = true
                        btnUpload.text = "确认上传"

                        if (success) {
                            Toast.makeText(this@UploadQuestionActivity, "题目上传成功！", Toast.LENGTH_SHORT).show()
                            // 跳转到题目管理触发同步
                            val intent = Intent(this@UploadQuestionActivity, QuestionManagementActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@UploadQuestionActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    private fun getCurrentUserId(): Long {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return prefs.getLong("user_id", -1)
    }

    override fun onDestroy() {
        super.onDestroy()
        knowledgeGraphHelper.close()
    }
}

package com.example.myapplication

import android.app.ProgressDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 学生注册页面
 * 上半部分：单独注册（账号输入、班级选择、注册按钮）
 * 下半部分：批量注册（两个整数输入、班级选择、注册按钮）
 * 密码默认和账号一致
 */
class StudentRegisterActivity : AppCompatActivity() {
    
    // 上半部分：单独注册
    private lateinit var etSingleUsername: EditText
    private lateinit var spinnerSingleClass: Spinner
    private lateinit var btnSingleRegister: Button
    
    // 下半部分：批量注册
    private lateinit var etBatchStart: EditText
    private lateinit var etBatchEnd: EditText
    private lateinit var tvBatchRange: TextView
    private lateinit var spinnerBatchClass: Spinner
    private lateinit var btnBatchRegister: Button
    
    private lateinit var btnBack: ImageButton
    
    private lateinit var userManager: UserManager
    private lateinit var classDBHelper: ClassDBHelper
    
    private var teacherId: Long = 0
    private var classList = mutableListOf<ClassDBHelper.ClassInfo>()
    private var classMap = mutableMapOf<String, Long>() // 班级名称 -> 班级ID
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supportActionBar?.hide()
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_student_register)
        
        // 初始化
        classDBHelper = ClassDBHelper(this)
        
        // 获取当前教师ID
        val currentUser = UserManager.getCurrentUser()
        if (currentUser != null && currentUser.role == 1) {
            teacherId = currentUser.userId
        } else {
            Toast.makeText(this, "请以教师身份登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // 初始化视图
        initViews()
        
        // 设置状态栏边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.student_register)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
        
        // 加载班级数据
        loadClasses()
        
        // 设置监听器
        setupListeners()
    }
    
    /**
     * 初始化视图
     */
    private fun initViews() {
        // 上半部分：单独注册
        etSingleUsername = findViewById(R.id.et_single_username)
        spinnerSingleClass = findViewById(R.id.spinner_single_class)
        btnSingleRegister = findViewById(R.id.btn_single_register)
        
        // 下半部分：批量注册
        etBatchStart = findViewById(R.id.et_batch_start)
        etBatchEnd = findViewById(R.id.et_batch_end)
        tvBatchRange = findViewById(R.id.tv_batch_range)
        spinnerBatchClass = findViewById(R.id.spinner_batch_class)
        btnBatchRegister = findViewById(R.id.btn_batch_register)
        
        btnBack = findViewById(R.id.btn_back)
        
        // 设置批量注册输入框文本监听
        setupBatchInputListeners()
    }
    
    /**
     * 设置批量输入监听器
     */
    private fun setupBatchInputListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                updateBatchRangeInfo()
            }
        }
        
        etBatchStart.addTextChangedListener(textWatcher)
        etBatchEnd.addTextChangedListener(textWatcher)
    }
    
    /**
     * 更新批量注册范围信息
     */
    private fun updateBatchRangeInfo() {
        val startText = etBatchStart.text.toString()
        val endText = etBatchEnd.text.toString()
        
        if (startText.isEmpty() || endText.isEmpty()) {
            tvBatchRange.text = "请输入起始和结束数字"
            return
        }
        
        try {
            val start = startText.toInt()
            val end = endText.toInt()
            
            // 计算差值
            val diff = kotlin.math.abs(end - start)
            
            if (diff > 50) {
                tvBatchRange.text = "差值超过50，请重新输入"
                tvBatchRange.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            } else {
                val count = kotlin.math.abs(end - start) + 1
                tvBatchRange.text = "将生成 $count 个账号"
                tvBatchRange.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            }
            
        } catch (e: NumberFormatException) {
            tvBatchRange.text = "请输入有效整数"
            tvBatchRange.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        }
    }
    
    /**
     * 加载班级数据
     */
    private fun loadClasses() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // 从本地数据库获取班级
                val localClasses = classDBHelper.getClassesByTeacher(teacherId)
                
                // 尝试从云端获取班级
                val cloudResult = CloudApiHelper.getClassesByTeacher(teacherId)
                val cloudClasses = if (cloudResult.success) cloudResult.data else emptyList()
                
                // 合并班级列表（优先本地）
                classList.clear()
                classMap.clear()
                
                if (localClasses.isNotEmpty()) {
                    localClasses.forEach { classInfo ->
                        classList.add(classInfo)
                        classMap[classInfo.className] = classInfo.classId
                    }
                } else if (cloudClasses.isNotEmpty()) {
                    // 只有云端有数据时，添加到本地数据库
                    cloudClasses.forEach { cloudClass ->
                        // 这里简化处理，实际可能需要更复杂的同步逻辑
                        val classInfo = ClassDBHelper.ClassInfo(
                            classId = cloudClass.classId,
                            className = cloudClass.className,
                            teacherId = teacherId,
                            createTime = System.currentTimeMillis(),
                            studentCount = 0
                        )
                        classList.add(classInfo)
                        classMap[cloudClass.className] = cloudClass.classId
                    }
                }
                
                // 更新UI
                withContext(Dispatchers.Main) {
                    updateClassSpinners()
                    
                    if (classList.isEmpty()) {
                        Toast.makeText(this@StudentRegisterActivity, "请先创建班级", Toast.LENGTH_SHORT).show()
                        btnSingleRegister.isEnabled = false
                        btnBatchRegister.isEnabled = false
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StudentRegisterActivity, "加载班级失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * 更新班级下拉列表
     */
    private fun updateClassSpinners() {
        // 获取班级名称列表
        val classNameList = classList.map { it.className }
        
        // 设置适配器
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, classNameList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        spinnerSingleClass.adapter = adapter
        spinnerBatchClass.adapter = adapter
        
        // 设置默认选择第一个
        if (classNameList.isNotEmpty()) {
            spinnerSingleClass.setSelection(0)
            spinnerBatchClass.setSelection(0)
        }
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 单独注册按钮
        btnSingleRegister.setOnClickListener {
            registerSingleStudent()
        }
        
        // 批量注册按钮
        btnBatchRegister.setOnClickListener {
            registerBatchStudents()
        }
        
        // 返回按钮
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    /**
     * 单独注册学生
     */
    private fun registerSingleStudent() {
        // 获取输入
        val username = etSingleUsername.text.toString().trim()
        val selectedClass = spinnerSingleClass.selectedItem as? String
        
        // 验证输入
        if (username.isEmpty()) {
            etSingleUsername.error = "请输入学生账号"
            return
        }
        
        if (selectedClass == null) {
            Toast.makeText(this, "请选择班级", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 获取班级ID
        val classId = classMap[selectedClass]
        if (classId == null) {
            Toast.makeText(this, "班级ID获取失败", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 验证用户名格式
        val usernameValidation = UserValidationUtils.validateUsername(username)
        if (!usernameValidation.isValid) {
            etSingleUsername.error = usernameValidation.message
            return
        }
        
        // 密码默认和账号一致
        val password = username
        
        // 禁用按钮防止重复点击
        btnSingleRegister.isEnabled = false
        btnSingleRegister.text = "注册中..."
        
        // 调用注册逻辑
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // 调用云函数注册
                val registerResult = CloudApiHelper.registerStudent(username, password, classId)
                
                withContext(Dispatchers.Main) {
                    btnSingleRegister.isEnabled = true
                    btnSingleRegister.text = "注册"
                    
                    if (registerResult.success) {
                        Toast.makeText(this@StudentRegisterActivity, "注册成功: $username", Toast.LENGTH_SHORT).show()
                        
                        // 清空输入
                        etSingleUsername.text.clear()
                        
        // 可选：添加到本地数据库
        val studentId = registerResult.data?.userId ?: 0
        if (studentId > 0) {
            classDBHelper.addStudentToClass(studentId, classId)
        }
                        
                    } else {
                        Toast.makeText(this@StudentRegisterActivity, "注册失败: ${registerResult.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSingleRegister.isEnabled = true
                    btnSingleRegister.text = "注册"
                    Toast.makeText(this@StudentRegisterActivity, "注册异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * 批量注册学生
     */
    private fun registerBatchStudents() {
        // 获取输入
        val startText = etBatchStart.text.toString().trim()
        val endText = etBatchEnd.text.toString().trim()
        val selectedClass = spinnerBatchClass.selectedItem as? String
        
        // 验证输入
        if (startText.isEmpty() || endText.isEmpty()) {
            Toast.makeText(this, "请输入起始和结束数字", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedClass == null) {
            Toast.makeText(this, "请选择班级", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 获取班级ID
        val classId = classMap[selectedClass]
        if (classId == null) {
            Toast.makeText(this, "班级ID获取失败", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val start = startText.toInt()
            val end = endText.toInt()
            
            // 验证差值不超过50
            val diff = kotlin.math.abs(end - start)
            if (diff > 50) {
                Toast.makeText(this, "差值不能超过50", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 确定起始和结束
            val actualStart = kotlin.math.min(start, end)
            val actualEnd = kotlin.math.max(start, end)
            val count = actualEnd - actualStart + 1
            
            // 确认对话框
            AlertDialog.Builder(this)
                .setTitle("确认批量注册")
                .setMessage("将注册 $count 个学生账号，账号范围: $actualStart 到 $actualEnd，班级: $selectedClass")
                .setPositiveButton("确认注册") { _, _ ->
                    performBatchRegistration(actualStart, actualEnd, classId, selectedClass)
                }
                .setNegativeButton("取消", null)
                .show()
                
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "请输入有效整数", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 执行批量注册
     */
    private fun performBatchRegistration(start: Int, end: Int, classId: Long, className: String) {
        // 禁用按钮
        btnBatchRegister.isEnabled = false
        btnBatchRegister.text = "批量注册中..."
        
        // 进度对话框
        val progressDialog = ProgressDialog(this).apply {
            setTitle("批量注册")
            setMessage("正在注册学生账号...")
            setCancelable(false)
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            max = end - start + 1
            show()
        }
        
        var successCount = 0
        var failedCount = 0
        
        GlobalScope.launch(Dispatchers.IO) {
            try {
                for (i in start..end) {
                    val username = i.toString()
                    val password = username // 密码和账号一致
                    
                    try {
                        // 调用云函数注册
                        val registerResult = CloudApiHelper.registerStudent(username, password, classId)
                        
                        if (registerResult.success) {
                            successCount++
                            
                            // 添加到本地数据库
                            val studentId = registerResult.data?.userId ?: 0
                            if (studentId > 0) {
                                classDBHelper.addStudentToClass(studentId, classId)
                            }
                            
                        } else {
                            failedCount++
                            Log.w("BatchRegistration", "账号 $username 注册失败: ${registerResult.message}")
                        }
                        
                    } catch (e: Exception) {
                        failedCount++
                        Log.e("BatchRegistration", "账号 $username 注册异常: ${e.message}")
                    }
                    
                    // 更新进度
                    withContext(Dispatchers.Main) {
                        progressDialog.progress = i - start + 1
                    }
                    
                    // 延迟一下，避免请求过快
                    kotlinx.coroutines.delay(200)
                }
                
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    btnBatchRegister.isEnabled = true
                    btnBatchRegister.text = "批量注册"
                    
                    // 显示结果
                    val message = "批量注册完成\n" +
                                 "成功: $successCount 个\n" +
                                 "失败: $failedCount 个"
                    
                    AlertDialog.Builder(this@StudentRegisterActivity)
                        .setTitle("批量注册结果")
                        .setMessage(message)
                        .setPositiveButton("确定", null)
                        .show()
                    
                    // 清空输入
                    etBatchStart.text.clear()
                    etBatchEnd.text.clear()
                    tvBatchRange.text = "请输入起始和结束数字"
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    btnBatchRegister.isEnabled = true
                    btnBatchRegister.text = "批量注册"
                    Toast.makeText(this@StudentRegisterActivity, "批量注册异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        classDBHelper.close()
    }
}
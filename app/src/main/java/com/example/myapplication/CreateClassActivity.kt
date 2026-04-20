package com.example.myapplication

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 新建班级页面
 */
class CreateClassActivity : AppCompatActivity() {
    
    private lateinit var btnBack: ImageButton
    private lateinit var tvTeacherInfo: TextView
    private lateinit var etClassName: EditText
    private lateinit var btnCreate: Button
    
    private var teacherId: Long = 0
    private var existingClasses: List<CloudApiHelper.ClassInfo> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supportActionBar?.hide()
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_class)
        
        // 检查用户角色
        checkUserRole()
        
        // 初始化视图
        initViews()
        
        // 设置状态栏边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.create_class)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // 设置按钮点击事件
        setButtonClickListeners()
    }
    
    override fun onResume() {
        super.onResume()
        // 每次进入页面时刷新班级列表
        loadExistingClasses()
    }
    
    /**
     * 检查用户角色
     */
    private fun checkUserRole() {
        val currentUser = UserManager.getCurrentUser()
        
        if (currentUser == null || currentUser.role != 1) {
            Toast.makeText(this, "权限错误", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            teacherId = currentUser.userId
        }
    }
    
    /**
     * 初始化视图
     */
    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvTeacherInfo = findViewById(R.id.tv_teacher_info)
        etClassName = findViewById(R.id.et_class_name)
        btnCreate = findViewById(R.id.btn_create)
        
        // 设置教师信息
        val currentUser = UserManager.getCurrentUser()
        if (currentUser != null) {
            tvTeacherInfo.text = "教师: ${currentUser.username}"
        }
    }
    
    /**
     * 加载已存在的班级列表
     */
    private fun loadExistingClasses() {
        CloudApiHelper.getClasses(teacherId, object : CloudApiHelper.GetClassesCallback {
            override fun onResult(success: Boolean, message: String, classes: List<CloudApiHelper.ClassInfo>) {
                if (success) {
                    existingClasses = classes
                } else {
                    existingClasses = emptyList()
                }
            }
        })
    }
    
    /**
     * 设置按钮点击事件
     */
    private fun setButtonClickListeners() {
        // 创建按钮
        btnCreate.setOnClickListener {
            createClass()
        }
        
        // 返回按钮
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    /**
     * 创建班级
     */
    private fun createClass() {
        val className = etClassName.text.toString().trim()
        
        // 验证输入
        if (className.isEmpty()) {
            etClassName.error = "请输入班级名称"
            return
        }
        
        if (className.length < 2) {
            etClassName.error = "班级名称至少2个字符"
            return
        }
        
        if (className.length > 20) {
            etClassName.error = "班级名称不能超过20个字符"
            return
        }
        
        // 检查班级名称是否重复
        val isDuplicate = existingClasses.any { it.className == className }
        if (isDuplicate) {
            etClassName.error = "该班级名称已存在"
            return
        }
        
        // 显示进度对话框
        val progressDialog = ProgressDialog(this).apply {
            setTitle("正在创建班级")
            setMessage("请稍候...")
            setCancelable(false)
            show()
        }
        
        // 调用云函数创建班级
        CloudApiHelper.createClass(teacherId, className, object : CloudApiHelper.CreateClassCallback {
            override fun onResult(success: Boolean, message: String, classInfo: CloudApiHelper.ClassInfo?) {
                runOnUiThread {
                    progressDialog.dismiss()
                    
                    if (success) {
                        AlertDialog.Builder(this@CreateClassActivity)
                            .setTitle("创建成功")
                            .setMessage("班级 '$className' 创建成功！")
                            .setPositiveButton("确定") { _, _ ->
                                // 清空输入框
                                etClassName.text.clear()
                                // 刷新班级列表
                                loadExistingClasses()
                            }
                            .show()
                    } else {
                        Toast.makeText(this@CreateClassActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}

package com.example.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import java.io.File
import java.io.FileOutputStream

/**
 * 教师资源管理页面
 * 显示教师上传的资源列表，支持上传、下载、删除资源
 */
class ResourceManagementActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var rvResources: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnUpload: Button

    private lateinit var adapter: ResourceAdapter
    private val resourceList = mutableListOf<ResourceAdapter.ResourceInfo>()

    private var teacherId: Long = 0
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""

    companion object {
        private const val REQUEST_FILE_PICK = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_resource_management)

        // 获取教师ID
        teacherId = UserManager.getCurrentUser()?.userId ?: 0
        if (teacherId == 0L) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        loadResources()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        rvResources = findViewById(R.id.rv_resources)
        tvEmpty = findViewById(R.id.tv_empty)
        btnUpload = findViewById(R.id.btn_upload)

        adapter = ResourceAdapter(
            resources = resourceList,
            isTeacherMode = true,
            onDownloadClick = { resource -> downloadResource(resource) },
            onDeleteClick = { resource -> confirmDeleteResource(resource) }
        )
        rvResources.layoutManager = LinearLayoutManager(this)
        rvResources.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnUpload.setOnClickListener {
            openFilePicker()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.resource_management)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadResources() {
        CloudApiHelper.getTeacherResources(teacherId, object : CloudApiHelper.GetResourcesCallback {
            override fun onResult(success: Boolean, message: String, resources: List<CloudApiHelper.ResourceInfo>?) {
                runOnUiThread {
                    if (success && resources != null) {
                        resourceList.clear()
                        resources.forEach { r ->
                            resourceList.add(ResourceAdapter.ResourceInfo(
                                resId = r.resId,
                                resName = r.resName,
                                url = r.url,
                                teacherId = r.teacherId,
                                createTime = r.createTime
                            ))
                        }
                        adapter.notifyDataSetChanged()
                        updateEmptyState()
                    } else {
                        Toast.makeText(this@ResourceManagementActivity, "加载资源失败: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun updateEmptyState() {
        if (resourceList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvResources.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvResources.visibility = View.VISIBLE
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "image/jpeg",
                "image/png",
                "image/gif",
                "video/mp4",
                "video/quicktime"
            ))
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "选择文件"), REQUEST_FILE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE_PICK && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedFileUri = uri
                selectedFileName = getFileNameFromUri(uri)
                showUploadDialog()
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else "未知文件"
            } ?: "未知文件"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "未知文件"
        }
    }

    private fun showUploadDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "输入资源名称"
            setText(selectedFileName.substringBeforeLast("."))
        }

        AlertDialog.Builder(this)
            .setTitle("上传资源")
            .setMessage("文件名: $selectedFileName")
            .setView(editText)
            .setPositiveButton("上传") { _, _ ->
                val resName = editText.text.toString().trim()
                if (resName.isNotEmpty()) {
                    uploadResource(resName)
                } else {
                    Toast.makeText(this, "请输入资源名称", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun uploadResource(resName: String) {
        val uri = selectedFileUri ?: return

        val progressDialog = ProgressDialog(this).apply {
            setTitle("上传中")
            setMessage("请稍候...")
            setCancelable(false)
            show()
        }

        // 将文件转为Base64
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

                CloudApiHelper.uploadResource(
                    teacherId = teacherId,
                    resName = resName,
                    fileName = selectedFileName,
                    base64Data = base64,
                    callback = object : CloudApiHelper.UploadResourceCallback {
                        override fun onResult(success: Boolean, message: String, resource: CloudApiHelper.ResourceInfo?) {
                            runOnUiThread {
                                progressDialog.dismiss()
                                if (success) {
                                    Toast.makeText(this@ResourceManagementActivity, "上传成功", Toast.LENGTH_SHORT).show()
                                    loadResources()
                                } else {
                                    Toast.makeText(this@ResourceManagementActivity, "上传失败: $message", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        } catch (e: Exception) {
            progressDialog.dismiss()
            Toast.makeText(this, "读取文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadResource(resource: ResourceAdapter.ResourceInfo) {
        try {
            // 直接用浏览器打开URL下载
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resource.url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开下载链接", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteResource(resource: ResourceAdapter.ResourceInfo) {
        AlertDialog.Builder(this)
            .setTitle("删除资源")
            .setMessage("确定要删除「${resource.resName}」吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteResource(resource)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteResource(resource: ResourceAdapter.ResourceInfo) {
        val progressDialog = ProgressDialog(this).apply {
            setTitle("删除中")
            setMessage("请稍候...")
            setCancelable(false)
            show()
        }

        CloudApiHelper.deleteResource(resource.resId, teacherId, object : CloudApiHelper.DeleteResourceCallback {
            override fun onResult(success: Boolean, message: String) {
                runOnUiThread {
                    progressDialog.dismiss()
                    if (success) {
                        Toast.makeText(this@ResourceManagementActivity, "删除成功", Toast.LENGTH_SHORT).show()
                        loadResources()
                    } else {
                        Toast.makeText(this@ResourceManagementActivity, "删除失败: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}

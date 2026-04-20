package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * 学生课堂资源页面
 * 显示教师上传的资源列表，学生可以下载查看
 */
class ClassResourcesActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var rvResources: RecyclerView
    private lateinit var tvEmpty: TextView

    private lateinit var adapter: ResourceAdapter
    private val resourceList = mutableListOf<ResourceAdapter.ResourceInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_class_resources)

        initViews()
        setupListeners()
        loadResources()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        rvResources = findViewById(R.id.rv_resources)
        tvEmpty = findViewById(R.id.tv_empty)

        adapter = ResourceAdapter(
            resources = resourceList,
            isTeacherMode = false,  // 学生模式，不显示删除按钮
            onDownloadClick = { resource -> downloadResource(resource) }
        )
        rvResources.layoutManager = LinearLayoutManager(this)
        rvResources.adapter = adapter

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.class_resources)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
    }

    private fun loadResources() {
        tvEmpty.visibility = View.GONE

        CloudApiHelper.getAllResources(object : CloudApiHelper.GetResourcesCallback {
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
                        Toast.makeText(this@ClassResourcesActivity, "加载资源失败: $message", Toast.LENGTH_SHORT).show()
                        updateEmptyState()
                    }
                }
            }
        })
    }

    private fun updateEmptyState() {
        if (resourceList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "暂无资源"
            rvResources.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvResources.visibility = View.VISIBLE
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
}

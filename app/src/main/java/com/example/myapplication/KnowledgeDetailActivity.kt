package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * 知识点详情页面
 * 1. 显示知识点名称、类型、编号、所属章节
 * 2. 使用BFS算法查找最多7个前置知识点
 * 3. 调用DeepSeek API获取知识点说明
 */
class KnowledgeDetailActivity : AppCompatActivity() {

    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailType: TextView
    private lateinit var tvDetailChapter: TextView
    private lateinit var layoutPrerequisites: LinearLayout
    private lateinit var tvDetailDescription: TextView
    private lateinit var progressDescription: ProgressBar
    private lateinit var btnMarkLearned: Button

    private lateinit var dbHelper: KnowledgeGraphDBHelper
    
    private var currentNodeId: Int = 0
    private var currentNodeType: Int = 3
    
    private val client = OkHttpClient()
    private val deepSeekKey = "sk-20f5c7fbbb1845c0876595e8a14c4f0b"
    private val deepSeekUrl = "https://api.deepseek.com/v1/chat/completions"

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_knowledge_detail)

        dbHelper = KnowledgeGraphDBHelper(this)
        initViews()
        loadNodeDetail()
    }

    private fun initViews() {
        tvDetailName = findViewById(R.id.tv_detail_name)
        tvDetailType = findViewById(R.id.tv_detail_type)
        tvDetailChapter = findViewById(R.id.tv_detail_chapter)
        layoutPrerequisites = findViewById(R.id.layout_prerequisites)
        tvDetailDescription = findViewById(R.id.tv_detail_description)
        progressDescription = findViewById(R.id.progress_description)
        btnMarkLearned = findViewById(R.id.btn_mark_learned)

        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun loadNodeDetail() {
        currentNodeId = intent.getIntExtra("nodeId", 0)
        val nodeName = intent.getStringExtra("nodeName") ?: ""
        currentNodeType = intent.getIntExtra("nodeType", 3)

        // 显示基本信息
        tvDetailName.text = nodeName
        
        val typeText = when(currentNodeType) {
            0 -> "部分"
            1 -> "章"
            2 -> "节"
            else -> "知识点"
        }
        tvDetailType.text = typeText

        // 获取节点详情
        val currentNode = when(currentNodeType) {
            0 -> null
            1 -> dbHelper.getChapterById(currentNodeId)?.let { NodeInfo(it.id, it.name, it.number, 1) }
            2 -> dbHelper.getSectionById(currentNodeId)?.let { NodeInfo(it.id, it.name, it.number, 2) }
            3 -> dbHelper.getKnowledgePointById(currentNodeId)?.let { NodeInfo(it.id, it.name, it.number, 3) }
            else -> null
        }
        
        // 获取所属章节
        val chapterInfo = getChapterInfo(currentNodeId, currentNodeType)
        tvDetailChapter.text = chapterInfo

        // 如果是知识点，获取前置知识点并调用API
        if (currentNodeType == 3) {
            val prerequisites = getPrerequisitesBFS(currentNodeId, 7)
            displayPrerequisites(prerequisites)
            fetchKnowledgeDescription(nodeName, currentNode, prerequisites)
            updateLearnedButton()
        } else {
            tvDetailDescription.text = "非知识点，无详细说明"
            btnMarkLearned.visibility = View.GONE
        }
    }

    /**
     * 更新已学习按钮状态
     */
    private fun updateLearnedButton() {
        val isLearned = dbHelper.getKnowledgePointFlag(currentNodeId) == 1
        if (isLearned) {
            btnMarkLearned.text = "✓ 已学习"
            btnMarkLearned.setBackgroundColor(0xFF81C784.toInt())
        } else {
            btnMarkLearned.text = "标记为已学习"
            btnMarkLearned.setBackgroundColor(0xFF4CAF50.toInt())
        }
        
        btnMarkLearned.setOnClickListener {
            val newState = dbHelper.getKnowledgePointFlag(currentNodeId) != 1
            dbHelper.updateKnowledgePointFlag(currentNodeId, newState)
            updateLearnedButton()
            Toast.makeText(this, if (newState) "已标记为已学习" else "已取消标记", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 获取节点所属的章、节信息
     */
    private fun getChapterInfo(nodeId: Int, nodeType: Int): String {
        return when(nodeType) {
            3 -> { // 知识点
                val location = dbHelper.getKnowledgePointLocation(nodeId)
                if (location != null) {
                    "${location.second} > ${location.first}"
                } else {
                    "无所属章节"
                }
            }
            2 -> { // 节
                val section = dbHelper.getSectionById(nodeId)
                if (section != null) {
                    val chapter = dbHelper.getChapterById(section.chapterId)
                    if (chapter != null) {
                        "${chapter.name} > ${section.name}"
                    } else {
                        section.name
                    }
                } else {
                    "无所属章节"
                }
            }
            1 -> { // 章
                val chapter = dbHelper.getChapterById(nodeId)
                if (chapter != null) {
                    val part = dbHelper.getPartById(chapter.partId)
                    if (part != null) {
                        "${part.name} > ${chapter.name}"
                    } else {
                        chapter.name
                    }
                } else {
                    "无所属章节"
                }
            }
            else -> "无所属章节"
        }
    }

    /**
     * 使用BFS算法查找前置知识点
     * @param nodeId 目标知识点ID
     * @param maxCount 最大返回数量
     */
    private fun getPrerequisitesBFS(nodeId: Int, maxCount: Int): List<KnowledgeGraphDBHelper.KnowledgePoint> {
        val result = mutableListOf<KnowledgeGraphDBHelper.KnowledgePoint>()
        val visited = mutableSetOf<Int>()
        val queue = ArrayDeque<Int>()
        
        // 获取当前知识点的直接前置知识点
        val directPrerequisites = dbHelper.getPrerequisitesByKnowledgePointId(nodeId)
        directPrerequisites.forEach { queue.add(it.id) }
        
        // BFS遍历前置关系
        while (queue.isNotEmpty() && result.size < maxCount) {
            val currentId = queue.removeFirst()
            if (currentId in visited) continue
            visited.add(currentId)
            
            val point = dbHelper.getKnowledgePointById(currentId)
            if (point != null) {
                result.add(point)
                
                // 添加当前知识点的前置知识点到队列
                val prerequisites = dbHelper.getPrerequisitesByKnowledgePointId(currentId)
                prerequisites.forEach {
                    if (it.id !in visited) queue.add(it.id)
                }
            }
        }
        
        return result
    }

    /**
     * 显示前置知识点列表
     */
    private fun displayPrerequisites(prerequisites: List<KnowledgeGraphDBHelper.KnowledgePoint>) {
        layoutPrerequisites.removeAllViews()
        
        if (prerequisites.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "无前置知识点"
                setTextColor(resources.getColor(android.R.color.darker_gray, theme))
                textSize = 14f
            }
            layoutPrerequisites.addView(emptyView)
            return
        }

        prerequisites.forEach { point ->
            val itemView = TextView(this).apply {
                text = "${point.number}. ${point.name}"
                setTextColor(resources.getColor(android.R.color.black, theme))
                textSize = 14f
                setPadding(0, 8, 0, 8)
            }
            layoutPrerequisites.addView(itemView)
        }
    }

    /**
     * 调用DeepSeek API获取知识点说明
     */
    private fun fetchKnowledgeDescription(nodeName: String, currentNode: NodeInfo?, prerequisites: List<KnowledgeGraphDBHelper.KnowledgePoint>) {
        progressDescription.visibility = View.VISIBLE
        tvDetailDescription.text = "正在获取知识点说明..."

        scope.launch {
            try {
                val prompt = buildPrompt(nodeName, currentNode, prerequisites)
                val result = withContext(Dispatchers.IO) {
                    callDeepSeek(prompt)
                }

                progressDescription.visibility = View.GONE
                if (result != null) {
                    tvDetailDescription.text = result
                } else {
                    tvDetailDescription.text = "获取说明失败，请重试"
                }
            } catch (e: Exception) {
                progressDescription.visibility = View.GONE
                tvDetailDescription.text = "获取说明失败: ${e.message}"
            }
        }
    }

    /**
     * 获取章、节名称信息
     */
    private fun getChapterAndSectionNames(nodeId: Int): Pair<String, String> {
        val location = dbHelper.getKnowledgePointLocation(nodeId)
        return if (location != null) {
            // location: Pair<章节名称, 章名称>
            Pair(location.second, location.first)
        } else {
            Pair("", "")
        }
    }

    /**
     * 构建提示词
     */
    private fun buildPrompt(nodeName: String, currentNode: NodeInfo?, prerequisites: List<KnowledgeGraphDBHelper.KnowledgePoint>): String {
        val (chapterName, sectionName) = if (currentNode != null) {
            getChapterAndSectionNames(currentNode.id)
        } else {
            Pair("", "")
        }
        
        // 构建前置知识点名称列表
        val prerequisiteNames = if (prerequisites.isEmpty()) {
            "无"
        } else {
            prerequisites.joinToString("、") { it.name }
        }
        
        return """
            请你解释离散数学中$chapterName，$sectionName 下的知识点：$nodeName。
            要求：
            1. 尝试简洁，200字以内。
            2. 介绍知识点的定义、性质，如果知识点名称带有"定义"或"性质"则不用解释其他内容。
            3. 之前已经学习过$prerequisiteNames，不用再解释。
            4. 输出格式为“[知识点名称]的定义是：xxxxxx，(换行)性质有1.xxx，2.xxx”
            5.注意适当换行以保持美观
        """.trimIndent()
    }

    private fun callDeepSeek(prompt: String): String? {
        val json = JSONObject().apply {
            put("model", "deepseek-chat")
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", 300)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(deepSeekUrl)
            .addHeader("Authorization", "Bearer $deepSeekKey")
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseJson = JSONObject(response.body?.string() ?: "")
                val choices = responseJson.getJSONArray("choices")
                if (choices.length() > 0) {
                    choices.getJSONObject(0).getJSONObject("message").getString("content")
                } else null
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    /**
     * 节点信息数据类
     */
    data class NodeInfo(
        val id: Int,
        val name: String,
        val number: String?,
        val level: Int
    )
}
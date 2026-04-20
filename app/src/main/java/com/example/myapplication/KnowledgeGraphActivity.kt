package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

/**
 * 知识图谱可视化页面
 * 
 * 数据库结构：
 * - parts: 部分表
 * - chapters: 章表 (关联part_id)
 * - sections: 节表 (关联chapter_id)
 * - knowledge_points: 知识点表 (关联section_id)
 * - prerequisite_relations: 前置关系表
 */
class KnowledgeGraphActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var detailPanel: LinearLayout
    private lateinit var tvNodeName: TextView
    private lateinit var tvNodeType: TextView
    private lateinit var tvNodeDesc: TextView
    private lateinit var tvPrerequisites: TextView
    private lateinit var layoutChildren: LinearLayout
    private lateinit var tvChildrenTitle: TextView
    private lateinit var layoutChildrenList: LinearLayout
    private lateinit var btnRelated: Button

    private lateinit var dbHelper: KnowledgeGraphDBHelper

    // 当前选中的节点信息
    private var currentNodeId: Int = 0
    private var currentNodeType: Int = 0 // 0=部分, 1=章, 2=节, 3=知识点

    // 展开的节点记录: type -> set of ids
    private val expandedParts = mutableSetOf<Int>(1, 2, 3)  // 默认展开所有部分
    private val expandedChapters = mutableSetOf<Int>()
    private val expandedSections = mutableSetOf<Int>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_knowledge_graph)

        dbHelper = KnowledgeGraphDBHelper(this)
        initViews()
        setupWebView()
    }

    private fun initViews() {
        webView = findViewById(R.id.webview_knowledge_graph)
        progressBar = findViewById(R.id.progress_loading)
        detailPanel = findViewById(R.id.panel_node_detail)
        tvNodeName = findViewById(R.id.tv_node_name)
        tvNodeType = findViewById(R.id.tv_node_type)
        tvNodeDesc = findViewById(R.id.tv_node_description)
        tvPrerequisites = findViewById(R.id.tv_prerequisites)
        layoutChildren = findViewById(R.id.layout_children)
        tvChildrenTitle = findViewById(R.id.tv_children_title)
        layoutChildrenList = findViewById(R.id.layout_children_list)
        btnRelated = findViewById(R.id.btn_related)

        // 返回按钮
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        // 查看知识点按钮
        btnRelated.setOnClickListener {
            val intent = Intent(this, KnowledgeDetailActivity::class.java)
            intent.putExtra("nodeId", currentNodeId)
            intent.putExtra("nodeName", tvNodeName.text.toString())
            intent.putExtra("nodeType", currentNodeType)
            startActivity(intent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE

        webView.webChromeClient = WebChromeClient()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                loadKnowledgeGraphData()
            }
        }

        webView.addJavascriptInterface(WebAppInterface(), "Android")
        webView.loadUrl("file:///android_asset/knowledge_graph.html")
    }

    /**
     * 加载知识图谱数据
     * 根据展开的节点动态加载下级内容
     */
    private fun loadKnowledgeGraphData() {
        Thread {
            val nodes = mutableListOf<GraphNode>()
            val links = mutableListOf<GraphLink>()

            // 1. 加载所有部分
            val parts = dbHelper.getAllParts()
            parts.forEach { part ->
                nodes.add(GraphNode(
                    id = "p_${part.id}",
                    name = part.name,
                    category = 0,
                    rawId = part.id,
                    type = 0
                ))

                // 如果部分被展开，加载其下的章
                if (part.id in expandedParts) {
                    val chapters = dbHelper.getChaptersByPartId(part.id)
                    chapters.forEach { chapter ->
                        nodes.add(GraphNode(
                            id = "c_${chapter.id}",
                            name = chapter.name,
                            category = 1,
                            rawId = chapter.id,
                            type = 1
                        ))
                        links.add(GraphLink("p_${part.id}", "c_${chapter.id}"))

                        // 如果章被展开，加载其下的节
                        if (chapter.id in expandedChapters) {
                            val sections = dbHelper.getSectionsByChapterId(chapter.id)
                            sections.forEach { section ->
                                nodes.add(GraphNode(
                                    id = "s_${section.id}",
                                    name = section.name,
                                    category = 2,
                                    rawId = section.id,
                                    type = 2
                                ))
                                links.add(GraphLink("c_${chapter.id}", "s_${section.id}"))

                                // 如果节被展开，加载其下的知识点
                                if (section.id in expandedSections) {
                                    val points = dbHelper.getKnowledgePointsBySectionId(section.id)
                                    points.forEach { point ->
                                        nodes.add(GraphNode(
                                            id = "k_${point.id}",
                                            name = point.name,
                                            category = 3,
                                            rawId = point.id,
                                            type = 3,
                                            flag = point.flag
                                        ))
                                        links.add(GraphLink("s_${section.id}", "k_${point.id}"))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val graphData = buildGraphJson(nodes, links)
            
            android.util.Log.d("KnowledgeGraph", "Loading data: ${nodes.size} nodes, ${links.size} links")
            android.util.Log.d("KnowledgeGraph", "Data: $graphData")

            runOnUiThread {
                webView.evaluateJavascript("initKnowledgeGraph($graphData);", null)
                progressBar.visibility = View.GONE
            }
        }.start()
    }

    /**
     * 处理节点点击展开/收起
     */
    private fun toggleNode(nodeId: String, nodeType: Int) {
        val rawId = nodeId.substringAfter("_").toInt()
        
        when (nodeType) {
            0 -> { // 部分
                if (rawId in expandedParts) {
                    expandedParts.remove(rawId)
                    // 收起时同时收起所有子节点
                    val chapters = dbHelper.getChaptersByPartId(rawId)
                    chapters.forEach { chapter ->
                        expandedChapters.remove(chapter.id)
                        val sections = dbHelper.getSectionsByChapterId(chapter.id)
                        sections.forEach { section ->
                            expandedSections.remove(section.id)
                        }
                    }
                } else {
                    expandedParts.add(rawId)
                }
            }
            1 -> { // 章
                if (rawId in expandedChapters) {
                    expandedChapters.remove(rawId)
                    // 收起时同时收起所有子节点
                    val sections = dbHelper.getSectionsByChapterId(rawId)
                    sections.forEach { section ->
                        expandedSections.remove(section.id)
                    }
                } else {
                    expandedChapters.add(rawId)
                }
            }
            2 -> { // 节
                if (rawId in expandedSections) {
                    expandedSections.remove(rawId)
                } else {
                    expandedSections.add(rawId)
                }
            }
        }
        
        loadKnowledgeGraphData()
    }

    private fun buildGraphJson(nodes: List<GraphNode>, links: List<GraphLink>): String {
        val nodesArray = JSONArray()
        
        nodes.forEach { node ->
            val symbolSize = when(node.category) {
                0 -> 50    // 部分
                1 -> 40    // 章
                2 -> 30    // 节
                else -> 20  // 知识点
            }
            
            val color = when(node.category) {
                0 -> "#1976D2"  // 部分 - 蓝色
                1 -> "#D32F2F"  // 章 - 红色
                2 -> "#F57C00"  // 节 - 橙色
                else -> if (node.flag == 1) "#4CAF50" else "#FFC107" // 知识点 - 已学习绿色，未学习黄色
            }
            
            nodesArray.put(JSONObject().apply {
                put("id", node.id)
                put("name", node.name)
                put("category", node.category)
                put("rawId", node.rawId)
                put("type", node.type)
                put("symbolSize", symbolSize)
                put("itemStyle", JSONObject().put("color", color))
                put("label", JSONObject().apply {
                    put("show", true)
                    put("fontSize", when(node.category) {
                        0 -> 18
                        1 -> 16
                        2 -> 14
                        else -> 12
                    })
                    put("fontWeight", if (node.category <= 1) "bold" else "normal")
                })
            })
        }

        val linksArray = JSONArray()
        links.forEach { link ->
            linksArray.put(JSONObject().apply {
                put("source", link.source)
                put("target", link.target)
                put("lineStyle", JSONObject().apply {
                    put("color", "#BDBDBD")
                    put("width", 1.5)
                    put("curveness", 0.2)
                })
            })
        }

        val categoriesArray = JSONArray().apply {
            put(JSONObject().put("name", "部分"))
            put(JSONObject().put("name", "章"))
            put(JSONObject().put("name", "节"))
            put(JSONObject().put("name", "知识点"))
        }

        return JSONObject().apply {
            put("nodes", nodesArray)
            put("links", linksArray)
            put("categories", categoriesArray)
        }.toString()
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun onNodeClick(nodeId: String, nodeName: String, nodeType: String) {
            runOnUiThread { 
                val type = nodeType.toIntOrNull() ?: 0
                val rawId = nodeId.substringAfter("_").toInt()
                
                // 部分、章、节可以点击展开/收起
                if (type < 3) {
                    toggleNode(nodeId, type)
                }
                
                showNodeDetail(rawId, nodeName, type) 
            }
        }
    }

    private fun showNodeDetail(nodeId: Int, name: String, type: Int) {
        detailPanel.visibility = View.VISIBLE

        currentNodeId = nodeId
        currentNodeType = type

        tvNodeName.text = name
        
        val typeText = when(type) {
            0 -> "部分"
            1 -> "章"
            2 -> "节"
            else -> "知识点"
        }
        tvNodeType.text = typeText
        
        if (type == 3) {
            // 知识点：显示查看知识点按钮
            btnRelated.visibility = View.VISIBLE
            layoutChildren.visibility = View.GONE
            tvPrerequisites.visibility = View.GONE
            
            tvNodeDesc.text = "${name}。"
        } else {
            // 部分/章/节：显示下级内容列表
            btnRelated.visibility = View.GONE
            tvPrerequisites.visibility = View.GONE
            
            tvNodeDesc.text = when(type) {
                0 -> "点击展开查看下属章节"
                1 -> "点击展开查看下属节"
                2 -> "点击展开查看下属知识点"
                else -> ""
            }
            
            showChildrenNodes(nodeId, type)
        }
    }
    
    private fun showChildrenNodes(parentId: Int, parentType: Int) {
        val children = when(parentType) {
            0 -> dbHelper.getChaptersByPartId(parentId)
                .map { ChildNode(it.id, it.name, 1) }
            1 -> dbHelper.getSectionsByChapterId(parentId)
                .map { ChildNode(it.id, it.name, 2) }
            2 -> dbHelper.getKnowledgePointsBySectionId(parentId)
                .map { ChildNode(it.id, it.name, 3) }
            else -> emptyList()
        }
        
        if (children.isEmpty()) {
            layoutChildren.visibility = View.GONE
            return
        }
        
        layoutChildren.visibility = View.VISIBLE
        layoutChildrenList.removeAllViews()
        
        val titleText = when(parentType) {
            0 -> "包含章节"
            1 -> "包含节"
            2 -> "包含知识点"
            else -> "下级内容"
        }
        tvChildrenTitle.text = titleText
        
        children.forEach { child ->
            val childView = TextView(this).apply {
                text = "• ${child.name}"
                textSize = 15f
                setTextColor(resources.getColor(android.R.color.black, null))
                setPadding(16, 12, 16, 12)
                background = resources.getDrawable(android.R.drawable.list_selector_background, null)
                setOnClickListener {
                    showNodeDetail(child.id, child.name, child.type)
                }
            }
            layoutChildrenList.addView(childView)
        }
    }

    // 数据类
    data class GraphNode(
        val id: String,
        val name: String,
        val category: Int,
        val rawId: Int,
        val type: Int,
        val flag: Int = 0
    )
    
    data class GraphLink(
        val source: String,
        val target: String
    )
    
    data class ChildNode(
        val id: Int,
        val name: String,
        val type: Int
    )
}

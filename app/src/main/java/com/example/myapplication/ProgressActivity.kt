package com.example.myapplication

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.CloudApiHelper.StudentAnswer

class ProgressActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var progressKnowledge: ProgressBar
    private lateinit var progressQuestion: ProgressBar
    private lateinit var tvKnowledgeProgress: TextView
    private lateinit var tvQuestionProgress: TextView
    private lateinit var rvChapterStats: RecyclerView

    private lateinit var knowledgeDBHelper: KnowledgeGraphDBHelper
    private lateinit var questionDBHelper: QuestionDBHelper
    private lateinit var answerCacheHelper: AnswerCacheHelper
    
    private var userId: Long = -1
    private var answerStatusMap: Map<Int, StudentAnswer> = emptyMap()

    // 状态常量（与云端保持一致）
    companion object {
        const val STATUS_NOT_CHECKED = 0  // 未批改
        const val STATUS_CORRECT = 1      // 正确
        const val STATUS_WRONG = 2        // 错误
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_progress)

        knowledgeDBHelper = KnowledgeGraphDBHelper(this)
        questionDBHelper = QuestionDBHelper(this)
        answerCacheHelper = AnswerCacheHelper(this)
        userId = getCurrentUserId()
        
        initViews()
        syncAndLoadData()
    }
    
    private fun getCurrentUserId(): Long {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return prefs.getLong("user_id", -1)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        progressKnowledge = findViewById(R.id.progress_knowledge)
        progressQuestion = findViewById(R.id.progress_question)
        tvKnowledgeProgress = findViewById(R.id.tv_knowledge_progress)
        tvQuestionProgress = findViewById(R.id.tv_question_progress)
        rvChapterStats = findViewById(R.id.rv_chapter_stats)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.progress_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnBack.setOnClickListener {
            finish()
        }

        rvChapterStats.layoutManager = LinearLayoutManager(this)
    }

    private fun syncAndLoadData() {
        if (userId <= 0) {
            loadDataFromCache()
            return
        }

        // 从云端获取学生的所有答题记录
        CloudApiHelper.getStudentAllAnswers(userId, object : CloudApiHelper.GetStudentAllAnswersCallback {
            override fun onResult(success: Boolean, message: String, data: Map<Int, CloudApiHelper.StudentAnswer>?) {
                runOnUiThread {
                    if (success && data != null) {
                        // 保存到本地缓存
                        answerCacheHelper.saveAnswers(userId, data)
                        answerStatusMap = data
                    } else {
                        // 加载本地缓存
                        answerStatusMap = answerCacheHelper.getAnswers(userId)
                    }
                    loadDataFromCache()
                }
            }
        })
    }

    private fun loadDataFromCache() {
        // 如果本地缓存也没有，使用本地缓存
        if (answerStatusMap.isEmpty()) {
            answerStatusMap = answerCacheHelper.getAnswers(userId)
        }

        // 计算总体进度
        calculateOverallProgress()

        // 获取章节统计数据
        val chapterStatsList = getChapterStats()

        // 设置RecyclerView
        rvChapterStats.adapter = ChapterStatsAdapter(chapterStatsList)
    }

    private fun calculateOverallProgress() {
        // 总体知识点进度
        val allKnowledgePoints = knowledgeDBHelper.getAllKnowledgePoints()
        val totalKnowledge = allKnowledgePoints.size
        val learnedKnowledge = allKnowledgePoints.count { it.flag == 1 }
        val knowledgePercent = if (totalKnowledge > 0) (learnedKnowledge * 100) / totalKnowledge else 0

        progressKnowledge.progress = knowledgePercent
        tvKnowledgeProgress.text = "${knowledgePercent}%"

        // 总体习题进度（从云端缓存读取答题状态）
        val allQuestions = questionDBHelper.getAllValidQuestions()
        val totalQuestions = allQuestions.size
        // 题目有云端答题记录就算已完成（无论对错）
        val completedQuestions = allQuestions.count { question ->
            val answer = answerStatusMap[question.questionNumber]
            answer != null
        }
        val questionPercent = if (totalQuestions > 0) (completedQuestions * 100) / totalQuestions else 0

        progressQuestion.progress = questionPercent
        tvQuestionProgress.text = "${questionPercent}%"
    }

    private fun getChapterStats(): List<ChapterStats> {
        val statsList = mutableListOf<ChapterStats>()

        // 获取所有章节（按章分组）
        val chapters = knowledgeDBHelper.getChaptersByPartId(1) + 
                      knowledgeDBHelper.getChaptersByPartId(2) + 
                      knowledgeDBHelper.getChaptersByPartId(3) + 
                      knowledgeDBHelper.getChaptersByPartId(4)

        for (chapter in chapters) {
            // 获取该章下的所有节
            val sections = knowledgeDBHelper.getSectionsByChapterId(chapter.id)
            
            var totalKnowledge = 0
            var learnedKnowledge = 0
            var totalQuestions = 0
            var completedQuestions = 0

            for (section in sections) {
                // 该节的知识点统计
                val knowledgePoints = knowledgeDBHelper.getKnowledgePointsBySectionId(section.id)
                totalKnowledge += knowledgePoints.size
                learnedKnowledge += knowledgePoints.count { it.flag == 1 }

                // 该节的习题统计（从云端缓存读取答题状态）
                val questions = questionDBHelper.getAllValidQuestions().filter { it.sectionId == section.id }
                totalQuestions += questions.size
                // 题目有云端答题记录就算已完成
                completedQuestions += questions.count { question ->
                    val answer = answerStatusMap[question.questionNumber]
                    answer != null
                }
            }

            val chapterName = "第${chapter.number}章 ${chapter.name}"
            statsList.add(ChapterStats(
                chapterId = chapter.id,
                chapterName = chapterName,
                totalKnowledge = totalKnowledge,
                learnedKnowledge = learnedKnowledge,
                totalQuestions = totalQuestions,
                completedQuestions = completedQuestions
            ))
        }

        return statsList
    }
    
    override fun onDestroy() {
        super.onDestroy()
        knowledgeDBHelper.close()
        questionDBHelper.close()
        answerCacheHelper.close()
    }
}

// 章节统计数据类
data class ChapterStats(
    val chapterId: Int,
    val chapterName: String,
    val totalKnowledge: Int,
    val learnedKnowledge: Int,
    val totalQuestions: Int,
    val completedQuestions: Int
)

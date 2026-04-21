package com.example.myapplication

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class QuestionSyncHelper(private val context: Context) {

    companion object {
        private const val TAG = "QuestionSyncHelper"
        private const val TIMEOUT_SECONDS = 30L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    // 使用与CloudApiHelper相同的URL
    private val cloudFunctionUrl = CloudApiHelper.REGISTER_FUNCTION_URL

    // 同步题目数据（支持分页循环请求）
    suspend fun syncQuestions(dbHelper: QuestionDBHelper): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                // 获取本地最大题号
                val localMaxNumber = dbHelper.getLocalMaxQuestionNumber()
                Log.d(TAG, "本地最大题号: $localMaxNumber")

                // 分页循环请求，累积所有数据
                val allNewQuestions = mutableListOf<QuestionEntity>()
                val allDeletedQuestionNumbers = mutableSetOf<Int>()
                var currentPage = 1
                val pageSize = 100
                var hasMore = true
                var totalInserted = 0
                var totalDeleted = 0

                Log.d(TAG, "开始分页同步习题...")

                while (hasMore) {
                    Log.d(TAG, "请求第 $currentPage 页，每页 $pageSize 条")

                    // 构建请求数据
                    val requestData = JSONObject().apply {
                        put("action", "syncQuestions")
                        put("maxQuestionNumber", localMaxNumber)
                        put("page", currentPage)
                        put("pageSize", pageSize)
                    }

                    // 发送请求到云函数
                    val requestBody = requestData.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url(cloudFunctionUrl)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful || responseBody == null) {
                        Log.e(TAG, "云函数请求失败: ${response.code}")
                        return@withContext SyncResult(
                            success = false,
                            message = "网络请求失败: ${response.code}",
                            newQuestionsCount = totalInserted,
                            deletedQuestionsCount = totalDeleted
                        )
                    }

                    // 解析响应
                    val jsonResponse = JSONObject(responseBody)
                    val code = jsonResponse.optInt("code", -1)
                    val msg = jsonResponse.optString("msg", "未知错误")
                    val data = jsonResponse.optJSONObject("data")

                    if (code != 1 || data == null) {
                        Log.e(TAG, "云函数业务失败: code=$code, msg=$msg")
                        return@withContext SyncResult(
                            success = false,
                            message = msg,
                            newQuestionsCount = totalInserted,
                            deletedQuestionsCount = totalDeleted
                        )
                    }

                    // 解析新增题目
                    val newQuestionsArray = data.optJSONArray("newQuestions")
                    if (newQuestionsArray != null) {
                        for (i in 0 until newQuestionsArray.length()) {
                            val questionObj = newQuestionsArray.getJSONObject(i)
                            val question = QuestionEntity(
                                questionNumber = questionObj.optInt("qId", 0),
                                content = questionObj.optString("content", ""),
                                answer = questionObj.optString("answer", ""),
                                sectionId = questionObj.optInt("sectionId", 0),
                                imageUrl = questionObj.optString("imageUrl", null)
                            )
                            if (question.questionNumber > 0) {
                                allNewQuestions.add(question)
                            }
                        }
                    }

                    // 解析删除题目（只在第一页获取）
                    if (currentPage == 1) {
                        val deletedQuestionsArray = data.optJSONArray("deletedQuestionNumbers")
                        if (deletedQuestionsArray != null) {
                            for (i in 0 until deletedQuestionsArray.length()) {
                                val questionNumber = deletedQuestionsArray.optInt(i, 0)
                                if (questionNumber > 0) {
                                    allDeletedQuestionNumbers.add(questionNumber)
                                }
                            }
                        }
                    }

                    // 检查是否还有更多数据
                    hasMore = data.optBoolean("hasMore", false)
                    Log.d(TAG, "第 $currentPage 页获取 ${newQuestionsArray?.length() ?: 0} 条，hasMore=$hasMore")

                    currentPage++
                }

                Log.d(TAG, "分页同步完成，共获取 ${allNewQuestions.size} 条新增题目，${allDeletedQuestionNumbers.size} 条删除标记")

                // 执行数据库操作
                dbHelper.apply {
                    // 批量插入或更新新题目
                    for (question in allNewQuestions) {
                        val success = insertOrUpdateQuestion(
                            questionNumber = question.questionNumber,
                            content = question.content,
                            answer = question.answer,
                            sectionId = question.sectionId,
                            imageUrl = question.imageUrl
                        )
                        if (success) totalInserted++
                    }

                    // 批量标记删除题目（使用isDeleted标记）
                    if (allDeletedQuestionNumbers.isNotEmpty()) {
                        totalDeleted = markQuestionsAsDeleted(allDeletedQuestionNumbers.toList())
                    }
                }

                Log.d(TAG, "数据库操作完成: 插入/更新${totalInserted}题, 标记删除${totalDeleted}题")

                return@withContext SyncResult(
                    success = true,
                    message = "同步成功: 新增${totalInserted}题, 删除${totalDeleted}题",
                    newQuestionsCount = totalInserted,
                    deletedQuestionsCount = totalDeleted
                )

            } catch (e: Exception) {
                Log.e(TAG, "同步失败", e)
                return@withContext SyncResult(
                    success = false,
                    message = "同步失败: ${e.message}",
                    newQuestionsCount = 0,
                    deletedQuestionsCount = 0
                )
            }
        }
    }

    // 异步执行同步（不阻塞UI）
    fun syncQuestionsAsync(
        dbHelper: QuestionDBHelper,
        onStart: () -> Unit = {},
        onSuccess: (SyncResult) -> Unit = {},
        onError: (SyncResult) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) { onStart() }
            
            val result = syncQuestions(dbHelper)
            
            withContext(Dispatchers.Main) {
                if (result.success) {
                    onSuccess(result)
                } else {
                    onError(result)
                }
            }
        }
    }

    // 检查是否需要同步（现在总是返回true，因为不再依赖本地JSON数据）
    fun shouldCheckSync(dbHelper: QuestionDBHelper): Boolean {
        return true // 总是检查同步，因为数据完全依赖云端
    }
}

// 同步结果数据类
data class SyncResult(
    val success: Boolean,
    val message: String,
    val newQuestionsCount: Int,
    val deletedQuestionsCount: Int
)
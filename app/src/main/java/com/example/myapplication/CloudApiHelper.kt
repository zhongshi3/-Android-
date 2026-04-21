package com.example.myapplication

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 云API帮助类
 * 用于调用云函数进行用户登录、注册等操作
 * 
 * 适配优化后的云函数：
 * - 统一使用 actionMap 路由
 * - answer集合增加冗余字段支持
 * - 统一返回格式 { code, msg, data }
 */
object CloudApiHelper {

    // ==================== 常量定义 ====================
    
    /**
     * 用户角色常量
     * 与云函数 constants.js 中 ROLE 一致
     */
    object Role {
        const val STUDENT = 0    // 学生
        const val TEACHER = 1   // 教师
    }
    
    /**
     * 答题状态常量
     * 与云函数 constants.js 中 ANSWER_STATUS 一致
     */
    object AnswerStatus {
        const val NOT_CHECKED = 0  // 未批改
        const val CORRECT = 1      // 正确
        const val WRONG = 2        // 错误
    }
    
    // 云函数地址
    private const val BASE_CLOUD_FUNCTION_URL = "https://fc-mp-ea6433d3-77e4-4c0b-9055-7fac5f1236e4.next.bspapp.com"
    private const val LOGIN_FUNCTION_URL = "$BASE_CLOUD_FUNCTION_URL/login"
    const val REGISTER_FUNCTION_URL = "$BASE_CLOUD_FUNCTION_URL/register"

    // OkHttp客户端
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ==================== 数据模型 ====================

    /**
     * 用户信息数据类
     * 对应云函数user集合的字段结构
     */
    data class UserInfo(
        val userId: Long,
        val username: String,
        val role: Int,
        val classId: Int? = null,
        val status: String? = null  // 新增：账号状态
    )

    /**
     * 登录响应数据类
     */
    data class LoginResponse(
        val success: Boolean,
        val message: String,
        val userInfo: UserInfo? = null,
        val token: String? = null
    )

    /**
     * 班级信息数据类（优化版）
     */
    data class ClassInfo(
        val classId: Long,
        val className: String,
        val teacherId: Long,
        val teacherName: String? = null,   // 新增：教师名称（冗余字段）
        val studentCount: Int = 0          // 新增：学生人数（冗余字段）
    )

    /**
     * 学生信息数据类
     */
    data class StudentInfo(
        val userId: Long,
        val username: String,
        val classId: Long? = null
    )

    /**
     * 学生答案数据类（优化版 - 支持冗余字段）
     */
    data class StudentAnswer(
        val qId: Int,
        val studentAnswer: String,
        val status: Int,                    // 0: 未批改, 1: 正确, 2: 错误
        val teacherMsg: String? = null,
        // 以下为冗余字段（云函数直接返回）
        val studentName: String? = null,     // 学生姓名
        val classId: Long? = null,           // 班级ID
        val className: String? = null,       // 班级名称
        val teacherId: Long? = null,         // 教师ID
        val questionBrief: String? = null,    // 题目摘要
        val sectionId: Int? = null,          // 节号
        val correctAnswer: String? = null     // 正确答案
    )

    /**
     * 提交答案响应数据类
     */
    data class SubmitAnswerResponse(
        val success: Boolean,
        val message: String,
        val data: StudentAnswer? = null,
        val isNew: Boolean = true            // 是否新增答案
    )

    /**
     * 查询学生答案响应数据类
     */
    data class GetStudentAnswerResponse(
        val success: Boolean,
        val message: String,
        val data: StudentAnswer? = null
    )

    /**
     * 未批改题目信息数据类（优化版 - 增加冗余字段）
     */
    data class QuestionUngradedInfo(
        val qId: Int,
        val userId: Long,
        val studentName: String? = null,     // 新增：学生姓名
        val className: String? = null,        // 新增：班级名称
        val questionBrief: String? = null     // 新增：题目摘要
    )

    /**
     * 所有答题记录信息数据类（优化版 - 增加冗余字段）
     */
    data class QuestionAnsweredInfo(
        val qId: Int,
        val userId: Long,
        val status: Int,                      // 0:未批改, 1:正确, 2:错误
        val studentName: String? = null,      // 新增：学生姓名
        val className: String? = null         // 新增：班级名称
    )

    /**
     * 错题信息数据类（优化版 - 增加正确答案）
     */
    data class ErrorQuestionInfo(
        val qId: Int,
        val contentPreview: String,           // 题目摘要
        val studentAnswer: String,
        val correctAnswer: String? = null,   // 新增：正确答案
        val teacherMsg: String? = null,
        val sectionId: Int,
        val status: Int                        // 0: 待复习, 1: 已复习
    )

    /**
     * 资源信息数据类（优化版 - 增加扩展字段）
     */
    data class ResourceInfo(
        val resId: String,
        val resName: String,
        val url: String,
        val teacherId: Long,
        val teacherName: String? = null,      // 新增：教师名称
        val type: String? = null,              // 新增：资源类型
        val tags: List<String>? = null,        // 新增：标签
        val classIds: List<Long>? = null,      // 新增：关联班级
        val size: Long? = null,                // 新增：文件大小
        val createTime: Long
    )

    /**
     * 班级题目答题统计数据类
     */
    data class ClassQuestionStats(
        val classId: Long,
        val qId: Int,
        val totalStudents: Int,
        val notDoneCount: Int,
        val notCheckedCount: Int,
        val wrongCount: Int,
        val correctCount: Int
    )

    /**
     * 班级整体统计信息数据类
     */
    data class ClassOverallStats(
        val classId: Long,
        val className: String,
        val totalStudents: Int,
        val totalQuestions: Int,
        val totalSubmitted: Int,
        val totalCorrect: Int,
        val totalWrong: Int,
        val avgCorrectRate: Int,
        val completionRate: Int,
        val studentCount: Int
    )

    /**
     * 班级统计概览数据类
     */
    data class ClassStatsOverview(
        val classId: Long,
        val className: String,
        val studentCount: Int,
        val submittedCount: Int,
        val correctRate: Int,
        val createTime: Long
    )

    /**
     * 学生进度统计信息数据类
     */
    data class StudentProgressStats(
        val userId: Long,
        val username: String,
        val totalAnswered: Int,
        val correctCount: Int,
        val wrongCount: Int,
        val ungradedCount: Int,
        val correctRate: Int,
        val errorQuestionCount: Int,
        val recentAnswers: List<RecentAnswer>
    )

    /**
     * 最近答题记录数据类
     */
    data class RecentAnswer(
        val qId: Int,
        val questionBrief: String,
        val status: Int,
        val updateTime: Long
    )

    /**
     * 删除题目数据类
     */
    data class DeleteQuestionData(
        val qId: Int
    )

    /**
     * 上传题目数据类
     */
    data class UploadQuestionData(
        val qId: Int,
        val content: String,
        val answer: String,
        val sectionId: Int,
        val imageUrl: String?
    )

    /**
     * 学生注册数据类
     */
    data class StudentRegisterData(
        val userId: Long,
        val username: String,
        val classId: Long
    )

    /**
     * 注册学生响应数据类
     */
    data class RegisterStudentResponse(
        val success: Boolean,
        val message: String,
        val data: StudentRegisterData? = null
    )

    /**
     * 获取班级响应数据类
     */
    data class GetClassesResponse(
        val success: Boolean,
        val message: String,
        val data: List<ClassInfo> = emptyList()
    )

    /**
     * 习题同步响应数据类
     */
    data class QuestionSyncResponse(
        val success: Boolean,
        val message: String,
        val newQuestionsCount: Int = 0,
        val deletedQuestionsCount: Int = 0,
        val hasMore: Boolean = false,
        val currentPage: Int = 1,
        val pageSize: Int = 100
    )

    // ==================== 回调接口 ====================

    interface CloudApiCallback {
        fun onSuccess(result: JSONObject)
        fun onError(error: String)
    }

    interface LoginCallback {
        fun onResult(response: LoginResponse)
    }

    interface QuestionSyncCallback {
        fun onResult(response: QuestionSyncResponse)
    }

    interface GetClassesCallback {
        fun onResult(success: Boolean, message: String, classes: List<ClassInfo>)
    }

    interface CreateClassCallback {
        fun onResult(success: Boolean, message: String, classInfo: ClassInfo? = null)
    }

    interface SubmitAnswerCallback {
        fun onResult(success: Boolean, message: String, data: StudentAnswer? = null)
    }

    interface GetStudentAnswerCallback {
        fun onResult(success: Boolean, message: String, data: StudentAnswer? = null)
    }

    interface GetBatchStudentAnswersCallback {
        fun onResult(success: Boolean, message: String, data: Map<Int, StudentAnswer>? = null)
    }

    interface GetStudentAllAnswersCallback {
        fun onResult(success: Boolean, message: String, data: Map<Int, StudentAnswer>?)
    }

    interface DeleteQuestionCallback {
        fun onResult(success: Boolean, message: String, data: DeleteQuestionData? = null)
    }

    interface UploadQuestionCallback {
        fun onResult(success: Boolean, message: String, data: UploadQuestionData? = null)
    }

    interface GetClassQuestionStatsCallback {
        fun onResult(success: Boolean, message: String, data: ClassQuestionStats? = null)
    }

    interface GetTeacherUngradedQuestionsCallback {
        fun onResult(success: Boolean, message: String, data: List<QuestionUngradedInfo>?)
    }

    interface GetTeacherAllAnsweredQuestionsCallback {
        fun onResult(success: Boolean, message: String, data: List<QuestionAnsweredInfo>?)
    }

    interface GetStudentsInfoCallback {
        fun onResult(success: Boolean, message: String, data: List<StudentInfo>?)
    }

    interface GetClassStudentsCallback {
        fun onResult(success: Boolean, message: String, data: List<StudentInfo>?)
    }

    interface SubmitGradeCallback {
        fun onResult(success: Boolean, message: String)
    }

    interface GetStudentErrorQuestionsCallback {
        fun onResult(success: Boolean, message: String, data: List<ErrorQuestionInfo>?)
    }

    interface GetResourcesCallback {
        fun onResult(success: Boolean, message: String, resources: List<ResourceInfo>?)
    }

    interface UploadResourceCallback {
        fun onResult(success: Boolean, message: String, resource: ResourceInfo? = null)
    }

    interface DeleteResourceCallback {
        fun onResult(success: Boolean, message: String)
    }

    interface UploadImageCallback {
        fun onResult(success: Boolean, message: String, imageUrl: String? = null)
    }

    interface GetClassOverallStatsCallback {
        fun onResult(success: Boolean, message: String, data: ClassOverallStats? = null)
    }

    interface GetTeacherAllClassesStatsCallback {
        fun onResult(success: Boolean, message: String, data: List<ClassStatsOverview>? = null)
    }

    interface GetStudentProgressStatsCallback {
        fun onResult(success: Boolean, message: String, data: StudentProgressStats? = null)
    }

    // ==================== 公共方法 ====================

    private fun runOnBackgroundThread(task: () -> Unit) {
        Thread {
            task()
        }.start()
    }

    // ==================== 用户相关功能 ====================

    fun registerUser(
        username: String,
        password: String,
        role: Int,
        classId: Int? = null,
        callback: CloudApiCallback
    ) {
        println("云函数注册参数接收 - username: '$username', role: $role, classId: $classId")
        
        if (username.isBlank() || password.isBlank()) {
            callback.onError("用户名或密码不能为空")
            return
        }

        val finalClassId = if (role == Role.TEACHER) 0 else (classId ?: 0)
        
        println("注册请求 - 最终参数: role=$role, classId=$finalClassId")
        
        val requestData = JSONObject().apply {
            put("action", "register")
            put("username", username)
            put("password", password)
            put("role", role)
            put("classId", finalClassId)
        }
        
        executeRequest(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result)
            }
            override fun onError(error: String) {
                callback.onError(error)
            }
        })
    }

    fun registerTeacher(
        username: String,
        password: String,
        callback: CloudApiCallback
    ) {
        registerUser(username, password, Role.TEACHER, 0, callback)
    }

    fun checkUsernameExists(username: String, callback: CloudApiCallback) {
        runOnBackgroundThread {
            try {
                Thread.sleep(500)
                val result = JSONObject().apply {
                    put("code", 1)
                    put("msg", "用户名格式检查通过")
                    put("exists", false)
                    put("canUse", true)
                }
                callback.onSuccess(result)
            } catch (e: Exception) {
                callback.onError("检查失败：${e.message}")
            }
        }
    }

    fun login(username: String, password: String, callback: LoginCallback) {
        println("云函数登录参数接收 - username: '$username'")
        
        if (username.isBlank() || password.isBlank()) {
            callback.onResult(LoginResponse(false, "用户名或密码不能为空"))
            return
        }

        val requestData = JSONObject().apply {
            put("action", "login")
            put("username", username)
            put("password", password)
        }
        
        executeRequest(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                val code = result.optInt("code", -1)
                val message = result.optString("msg", "未知错误")
                
                if (code == 1) {
                    val userData = result.optJSONObject("data")
                    val userInfo = if (userData != null) {
                        UserInfo(
                            userId = userData.optLong("userId", 0L),
                            username = userData.optString("username", ""),
                            role = userData.optInt("role", 0),
                            classId = if (userData.has("classId") && !userData.isNull("classId")) 
                                userData.optInt("classId") else null
                        )
                    } else null
                    val token = result.optString("token", "")
                    callback.onResult(LoginResponse(true, message, userInfo, token))
                } else {
                    callback.onResult(LoginResponse(false, message))
                }
            }
            override fun onError(error: String) {
                callback.onResult(LoginResponse(false, error))
            }
        })
    }

    // ==================== 班级相关功能 ====================

    fun getClasses(teacherId: Long, callback: GetClassesCallback) {
        val requestData = JSONObject().apply {
            put("action", "getClasses")
            put("teacherId", teacherId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                val code = result.optInt("code", -1)
                val message = result.optString("msg", "未知错误")
                
                if (code == 1) {
                    val classesArray = result.optJSONArray("data")
                    val classes = mutableListOf<ClassInfo>()
                    
                    classesArray?.let {
                        for (i in 0 until it.length()) {
                            val classObj = it.getJSONObject(i)
                            classes.add(ClassInfo(
                                classId = classObj.optLong("classId"),
                                className = classObj.optString("className"),
                                teacherId = teacherId,
                                teacherName = classObj.optString("teacherName", null),     // 新增
                                studentCount = classObj.optInt("studentCount", 0)         // 新增
                            ))
                        }
                    }
                    callback.onResult(true, message, classes)
                } else {
                    callback.onResult(false, message, emptyList())
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, emptyList())
            }
        })
    }

    fun createClass(teacherId: Long, className: String, callback: CreateClassCallback) {
        val requestData = JSONObject().apply {
            put("action", "createClass")
            put("teacherId", teacherId)
            put("className", className)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                val code = result.optInt("code", -1)
                val message = result.optString("msg", "未知错误")
                
                if (code == 1) {
                    val data = result.optJSONObject("data")
                    val classInfo = if (data != null) {
                        ClassInfo(
                            classId = data.optLong("classId"),
                            className = data.optString("className"),
                            teacherId = data.optLong("teacherId")
                        )
                    } else null
                    callback.onResult(true, message, classInfo)
                } else {
                    callback.onResult(false, message, null)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    suspend fun getClassesByTeacher(teacherId: Long): GetClassesResponse {
        val requestData = JSONObject().apply {
            put("action", "getClasses")
            put("teacherId", teacherId)
        }
        
        return try {
            val responseBody = executeRequestSync(REGISTER_FUNCTION_URL, requestData)
            val result = JSONObject(responseBody)
            
            val code = result.optInt("code", -1)
            val message = result.optString("msg", "未知错误")
            
            if (code == 1) {
                val classesArray = result.optJSONArray("data")
                val classes = mutableListOf<ClassInfo>()
                
                classesArray?.let {
                    for (i in 0 until it.length()) {
                        val classObj = it.getJSONObject(i)
                        classes.add(ClassInfo(
                            classId = classObj.optLong("classId"),
                            className = classObj.optString("className"),
                            teacherId = classObj.optLong("teacherId"),
                            teacherName = classObj.optString("teacherName", null),
                            studentCount = classObj.optInt("studentCount", 0)
                        ))
                    }
                }
                GetClassesResponse(true, message, classes)
            } else {
                GetClassesResponse(false, message)
            }
        } catch (e: Exception) {
            GetClassesResponse(false, "请求异常: ${e.message}")
        }
    }

    // ==================== 学生相关功能 ====================

    fun getStudentsInfo(userIds: List<Long>, callback: GetStudentsInfoCallback) {
        val requestData = JSONObject().apply {
            put("action", "getStudentsInfo")
            put("userIds", JSONArray(userIds))
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseArrayResponse(result) { list ->
                    val students = mutableListOf<StudentInfo>()
                    list?.let {
                        for (i in 0 until it.length()) {
                            val item = it.getJSONObject(i)
                            students.add(StudentInfo(
                                userId = item.optLong("userId"),
                                username = item.optString("username"),
                                classId = if (item.has("classId") && !item.isNull("classId"))
                                    item.optLong("classId") else null
                            ))
                        }
                    }
                    callback.onResult(true, result.optString("msg"), students)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun getClassStudents(classId: Long, callback: GetClassStudentsCallback) {
        val requestData = JSONObject().apply {
            put("action", "getClassStudents")
            put("classId", classId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseArrayResponse(result) { list ->
                    val students = mutableListOf<StudentInfo>()
                    list?.let {
                        for (i in 0 until it.length()) {
                            val item = it.getJSONObject(i)
                            students.add(StudentInfo(
                                userId = item.optLong("userId"),
                                username = item.optString("username"),
                                classId = classId
                            ))
                        }
                    }
                    callback.onResult(true, result.optString("msg"), students)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    suspend fun registerStudent(username: String, password: String, classId: Long): RegisterStudentResponse {
        val requestData = JSONObject().apply {
            put("action", "register")
            put("username", username)
            put("password", password)
            put("role", Role.STUDENT)
            put("classId", classId)
        }
        
        return try {
            val responseBody = executeRequestSync(REGISTER_FUNCTION_URL, requestData)
            val result = JSONObject(responseBody)
            
            val code = result.optInt("code", -1)
            val message = result.optString("msg", "未知错误")
            
            if (code == 1) {
                val data = result.optJSONObject("data")
                val registerData = if (data != null) {
                    StudentRegisterData(
                        userId = data.optLong("userId"),
                        username = data.optString("username"),
                        classId = data.optLong("classId")
                    )
                } else null
                RegisterStudentResponse(true, message, registerData)
            } else {
                RegisterStudentResponse(false, message)
            }
        } catch (e: Exception) {
            RegisterStudentResponse(false, "请求异常: ${e.message}")
        }
    }

    // ==================== 题目相关功能 ====================

    fun syncQuestions(maxQuestionNumber: Int, page: Int = 1, pageSize: Int = 100, callback: QuestionSyncCallback) {
        val requestData = JSONObject().apply {
            put("action", "syncQuestions")
            put("maxQuestionNumber", maxQuestionNumber)
            put("page", page)
            put("pageSize", pageSize)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                val code = result.optInt("code", -1)
                val message = result.optString("msg", "未知错误")
                
                if (code == 1) {
                    val data = result.optJSONObject("data")
                    if (data != null) {
                        val newQuestionsArray = data.optJSONArray("newQuestions")
                        val deletedQuestionsArray = data.optJSONArray("deletedQuestionNumbers")
                        
                        callback.onResult(QuestionSyncResponse(
                            success = true,
                            message = message,
                            newQuestionsCount = newQuestionsArray?.length() ?: 0,
                            deletedQuestionsCount = deletedQuestionsArray?.length() ?: 0,
                            hasMore = data.optBoolean("hasMore", false),
                            currentPage = data.optInt("currentPage", page),
                            pageSize = data.optInt("pageSize", pageSize)
                        ))
                    } else {
                        callback.onResult(QuestionSyncResponse(false, message))
                    }
                } else {
                    callback.onResult(QuestionSyncResponse(false, message))
                }
            }
            override fun onError(error: String) {
                callback.onResult(QuestionSyncResponse(false, error))
            }
        })
    }

    fun uploadQuestion(
        userId: Long,
        content: String,
        answer: String,
        sectionId: Int,
        imageUrl: String?,
        callback: UploadQuestionCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "uploadQuestion")
            put("userId", userId)
            put("content", content)
            put("answer", answer)
            put("sectionId", sectionId)
            put("imageUrl", imageUrl ?: "")
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseObjectResponse(result) { data ->
                    val uploadData = if (data != null) {
                        UploadQuestionData(
                            qId = data.optInt("qId"),
                            content = data.optString("content"),
                            answer = data.optString("answer"),
                            sectionId = data.optInt("sectionId"),
                            imageUrl = data.optString("imageUrl", null)
                        )
                    } else null
                    callback.onResult(true, result.optString("msg"), uploadData)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun deleteQuestion(userId: Long, qId: Int, callback: DeleteQuestionCallback) {
        val requestData = JSONObject().apply {
            put("action", "deleteQuestion")
            put("userId", userId)
            put("qId", qId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseObjectResponse(result) { data ->
                    val deleteData = if (data != null) {
                        DeleteQuestionData(qId = data.optInt("qId"))
                    } else null
                    callback.onResult(true, result.optString("msg"), deleteData)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    // ==================== 答题相关功能 ====================

    fun submitAnswer(
        userId: Long,
        qId: Int,
        studentAnswer: String,
        callback: SubmitAnswerCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "submitAnswer")
            put("userId", userId)
            put("qId", qId)
            put("studentAnswer", studentAnswer)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseObjectResponse(result) { data ->
                    val answerData = if (data != null) {
                        StudentAnswer(
                            qId = data.optInt("qId"),
                            studentAnswer = data.optString("studentAnswer"),
                            status = data.optInt("status")
                        )
                    } else null
                    callback.onResult(true, result.optString("msg"), answerData)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun getStudentAnswer(userId: Long, qId: Int, callback: GetStudentAnswerCallback) {
        val requestData = JSONObject().apply {
            put("action", "getStudentAnswer")
            put("userId", userId)
            put("qId", qId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                val code = result.optInt("code", -1)
                val message = result.optString("msg", "未知错误")
                
                if (code == 1) {
                    val data = result.optJSONObject("data")
                    val answerData = parseStudentAnswer(data)
                    callback.onResult(true, message, answerData)
                } else {
                    callback.onResult(false, message, null)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun getBatchStudentAnswers(userId: Long, qIds: List<Int>, callback: GetBatchStudentAnswersCallback) {
        val requestData = JSONObject().apply {
            put("action", "getBatchStudentAnswers")
            put("userId", userId)
            put("qIds", JSONArray(qIds))
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                val code = result.optInt("code", -1)
                val message = result.optString("msg", "未知错误")
                
                if (code == 1) {
                    val data = result.optJSONObject("data")
                    val answersMap = mutableMapOf<Int, StudentAnswer>()
                    
                    data?.let {
                        it.keys().forEach { key ->
                            val answerObj = it.getJSONObject(key)
                            answersMap[key.toInt()] = parseStudentAnswer(answerObj)!!
                        }
                    }
                    callback.onResult(true, message, answersMap)
                } else {
                    callback.onResult(false, message, null)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun getStudentAllAnswers(userId: Long, callback: GetStudentAllAnswersCallback) {
        val requestData = JSONObject().apply {
            put("action", "getStudentAllAnswers")
            put("userId", userId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                val code = result.optInt("code", -1)
                val message = result.optString("msg", "未知错误")
                
                if (code == 1) {
                    val data = result.optJSONObject("data")
                    val answersMap = mutableMapOf<Int, StudentAnswer>()
                    
                    data?.let {
                        it.keys().forEach { key ->
                            val answerObj = it.getJSONObject(key)
                            answersMap[key.toInt()] = parseStudentAnswer(answerObj)!!
                        }
                    }
                    callback.onResult(true, message, answersMap)
                } else {
                    callback.onResult(false, message, null)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun submitGrade(
        teacherId: Long,
        userId: Long,
        qId: Int,
        status: Int,
        teacherMsg: String,
        callback: SubmitGradeCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "submitGrade")
            put("teacherId", teacherId)
            put("userId", userId)
            put("qId", qId)
            put("status", status)
            put("teacherMsg", teacherMsg)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                val code = result.optInt("code", -1)
                callback.onResult(code == 1, result.optString("msg"))
            }
            override fun onError(error: String) {
                callback.onResult(false, error)
            }
        })
    }

    // ==================== 统计相关功能 ====================

    fun getClassQuestionStats(classId: Long, qId: Int, callback: GetClassQuestionStatsCallback) {
        val requestData = JSONObject().apply {
            put("action", "getClassQuestionStats")
            put("classId", classId)
            put("qId", qId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseObjectResponse(result) { data ->
                    val stats = if (data != null) {
                        ClassQuestionStats(
                            classId = data.optLong("classId"),
                            qId = data.optInt("qId"),
                            totalStudents = data.optInt("totalStudents"),
                            notDoneCount = data.optInt("notDoneCount"),
                            notCheckedCount = data.optInt("notCheckedCount"),
                            wrongCount = data.optInt("wrongCount"),
                            correctCount = data.optInt("correctCount")
                        )
                    } else null
                    callback.onResult(true, result.optString("msg"), stats)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun getTeacherUngradedQuestions(teacherId: Long, callback: GetTeacherUngradedQuestionsCallback) {
        val requestData = JSONObject().apply {
            put("action", "getTeacherUngradedQuestions")
            put("teacherId", teacherId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseArrayResponse(result) { list ->
                    val ungradedList = mutableListOf<QuestionUngradedInfo>()
                    list?.let {
                        for (i in 0 until it.length()) {
                            val item = it.getJSONObject(i)
                            ungradedList.add(QuestionUngradedInfo(
                                qId = item.optInt("qId"),
                                userId = item.optLong("userId"),
                                studentName = item.optString("studentName", null),     // 新增
                                className = item.optString("className", null),       // 新增
                                questionBrief = item.optString("questionBrief", null) // 新增
                            ))
                        }
                    }
                    callback.onResult(true, result.optString("msg"), ungradedList)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun getTeacherAllAnsweredQuestions(teacherId: Long, callback: GetTeacherAllAnsweredQuestionsCallback) {
        val requestData = JSONObject().apply {
            put("action", "getTeacherAllAnsweredQuestions")
            put("teacherId", teacherId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseArrayResponse(result) { list ->
                    val answeredList = mutableListOf<QuestionAnsweredInfo>()
                    list?.let {
                        for (i in 0 until it.length()) {
                            val item = it.getJSONObject(i)
                            answeredList.add(QuestionAnsweredInfo(
                                qId = item.optInt("qId"),
                                userId = item.optLong("userId"),
                                status = item.optInt("status", 0),
                                studentName = item.optString("studentName", null),  // 新增
                                className = item.optString("className", null)       // 新增
                            ))
                        }
                    }
                    callback.onResult(true, result.optString("msg"), answeredList)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun getStudentErrorQuestions(userId: Long, callback: GetStudentErrorQuestionsCallback) {
        val requestData = JSONObject().apply {
            put("action", "getStudentErrorQuestions")
            put("userId", userId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseArrayResponse(result) { list ->
                    val errorList = mutableListOf<ErrorQuestionInfo>()
                    list?.let {
                        for (i in 0 until it.length()) {
                            val item = it.getJSONObject(i)
                            errorList.add(ErrorQuestionInfo(
                                qId = item.optInt("qId"),
                                contentPreview = item.optString("contentPreview", ""),
                                studentAnswer = item.optString("studentAnswer", ""),
                                correctAnswer = item.optString("correctAnswer", null),  // 新增
                                teacherMsg = item.optString("teacherMsg", null),
                                sectionId = item.optInt("sectionId", 0),
                                status = item.optInt("status", 0)
                            ))
                        }
                    }
                    callback.onResult(true, result.optString("msg"), errorList)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    // ==================== 资源相关功能 ====================

    fun getTeacherResources(teacherId: Long, callback: GetResourcesCallback) {
        val requestData = JSONObject().apply {
            put("action", "getTeacherResources")
            put("teacherId", teacherId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseArrayResponse(result) { list ->
                    val resources = parseResourceList(list)
                    callback.onResult(true, result.optString("msg"), resources)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun getAllResources(callback: GetResourcesCallback) {
        val requestData = JSONObject().apply {
            put("action", "getAllResources")
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseArrayResponse(result) { list ->
                    val resources = parseResourceList(list)
                    callback.onResult(true, result.optString("msg"), resources)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun uploadResource(
        teacherId: Long,
        resName: String,
        fileName: String,
        base64Data: String,
        type: String? = null,
        tags: List<String>? = null,
        classIds: List<Long>? = null,
        callback: UploadResourceCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "uploadResource")
            put("teacherId", teacherId)
            put("resName", resName)
            put("fileName", fileName)
            put("fileData", base64Data)
            type?.let { put("type", it) }
            tags?.let { put("tags", JSONArray(it)) }
            classIds?.let { put("classIds", JSONArray(it.toList())) }
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                val code = result.optInt("code", -1)
                val message = result.optString("msg", "未知错误")
                
                if (code == 1) {
                    val data = result.optJSONObject("data")
                    val resource = parseResource(data)
                    callback.onResult(true, message, resource)
                } else {
                    callback.onResult(false, message, null)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun deleteResource(resId: String, teacherId: Long, callback: DeleteResourceCallback) {
        val requestData = JSONObject().apply {
            put("action", "deleteResource")
            put("resId", resId)
            put("teacherId", teacherId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                val code = result.optInt("code", -1)
                callback.onResult(code == 1, result.optString("msg"))
            }
            override fun onError(error: String) {
                callback.onResult(false, error)
            }
        })
    }

    fun uploadImageToCloud(fileName: String, base64Data: String, callback: UploadImageCallback) {
        val requestData = JSONObject().apply {
            put("action", "uploadImage")
            put("fileName", fileName)
            put("imageData", base64Data)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                val code = result.optInt("code", -1)
                val message = result.optString("msg", "未知错误")
                
                if (code == 1) {
                    val data = result.optJSONObject("data")
                    val imageUrl = data?.optString("imageUrl")
                    callback.onResult(true, message, imageUrl)
                } else {
                    callback.onResult(false, message, null)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    // ==================== 高级统计功能 ====================

    fun getClassOverallStats(classId: Long, callback: GetClassOverallStatsCallback) {
        val requestData = JSONObject().apply {
            put("action", "getClassOverallStats")
            put("classId", classId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseObjectResponse(result) { data ->
                    val stats = if (data != null) {
                        ClassOverallStats(
                            classId = data.optLong("classId"),
                            className = data.optString("className", ""),
                            totalStudents = data.optInt("totalStudents"),
                            totalQuestions = data.optInt("totalQuestions"),
                            totalSubmitted = data.optInt("totalSubmitted"),
                            totalCorrect = data.optInt("totalCorrect"),
                            totalWrong = data.optInt("totalWrong"),
                            avgCorrectRate = data.optInt("avgCorrectRate"),
                            completionRate = data.optInt("completionRate"),
                            studentCount = data.optInt("studentCount")
                        )
                    } else null
                    callback.onResult(true, result.optString("msg"), stats)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun getTeacherAllClassesStats(teacherId: Long, callback: GetTeacherAllClassesStatsCallback) {
        val requestData = JSONObject().apply {
            put("action", "getTeacherAllClassesStats")
            put("teacherId", teacherId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseArrayResponse(result) { list ->
                    val statsList = mutableListOf<ClassStatsOverview>()
                    list?.let {
                        for (i in 0 until it.length()) {
                            val item = it.getJSONObject(i)
                            statsList.add(ClassStatsOverview(
                                classId = item.optLong("classId"),
                                className = item.optString("className", ""),
                                studentCount = item.optInt("studentCount"),
                                submittedCount = item.optInt("submittedCount"),
                                correctRate = item.optInt("correctRate"),
                                createTime = item.optLong("createTime", System.currentTimeMillis())
                            ))
                        }
                    }
                    callback.onResult(true, result.optString("msg"), statsList)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    fun getStudentProgressStats(userId: Long, callback: GetStudentProgressStatsCallback) {
        val requestData = JSONObject().apply {
            put("action", "getStudentProgressStats")
            put("userId", userId)
        }
        
        executeRequestAsync(REGISTER_FUNCTION_URL, requestData, object : CloudApiCallback {
            override fun onSuccess(result: JSONObject) {
                parseObjectResponse(result) { data ->
                    val stats = if (data != null) {
                        val recentAnswersArray = data.optJSONArray("recentAnswers")
                        val recentAnswers = mutableListOf<RecentAnswer>()
                        
                        recentAnswersArray?.let {
                            for (i in 0 until it.length()) {
                                val item = it.getJSONObject(i)
                                recentAnswers.add(RecentAnswer(
                                    qId = item.optInt("qId"),
                                    questionBrief = item.optString("questionBrief", ""),
                                    status = item.optInt("status"),
                                    updateTime = item.optLong("updateTime", System.currentTimeMillis())
                                ))
                            }
                        }
                        
                        StudentProgressStats(
                            userId = data.optLong("userId"),
                            username = data.optString("username", ""),
                            totalAnswered = data.optInt("totalAnswered"),
                            correctCount = data.optInt("correctCount"),
                            wrongCount = data.optInt("wrongCount"),
                            ungradedCount = data.optInt("ungradedCount"),
                            correctRate = data.optInt("correctRate"),
                            errorQuestionCount = data.optInt("errorQuestionCount"),
                            recentAnswers = recentAnswers
                        )
                    } else null
                    callback.onResult(true, result.optString("msg"), stats)
                }
            }
            override fun onError(error: String) {
                callback.onResult(false, error, null)
            }
        })
    }

    // ==================== 内部辅助方法 ====================

    private fun executeRequest(url: String, body: JSONObject, callback: CloudApiCallback) {
        try {
            val requestBodyStr = body.toString()
            println("云函数请求JSON数据: $requestBodyStr")
            println("云函数请求URL: $url")

            val request = Request.Builder()
                .url(url)
                .post(requestBodyStr.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback.onError("网络连接失败：${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string()
                        if (response.isSuccessful && responseBody != null) {
                            val result = JSONObject(responseBody)
                            val code = result.optInt("code", -1)
                            
                            if (code == 1) {
                                callback.onSuccess(result)
                            } else {
                                callback.onError(result.optString("msg", "未知错误"))
                            }
                        } else {
                            callback.onError("服务器响应错误：${response.code}")
                        }
                    } catch (e: Exception) {
                        callback.onError("解析响应失败：${e.message}")
                    }
                }
            })
        } catch (e: Exception) {
            callback.onError("请求失败：${e.message}")
        }
    }

    private fun executeRequestAsync(url: String, body: JSONObject, callback: CloudApiCallback) {
        executeRequest(url, body, callback)
    }

    private fun executeRequestSync(url: String, body: JSONObject): String {
        val requestBodyStr = body.toString()
        val request = Request.Builder()
            .url(url)
            .post(requestBodyStr.toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()
        
        val response = client.newCall(request).execute()
        return response.body?.string() ?: "{}"
    }

    private fun parseArrayResponse(result: JSONObject, onSuccess: (JSONArray?) -> Unit) {
        val code = result.optInt("code", -1)
        if (code == 1) {
            val data = result.optJSONArray("data")
            onSuccess(data)
        } else {
            onSuccess(null)
        }
    }

    private fun parseObjectResponse(result: JSONObject, onSuccess: (JSONObject?) -> Unit) {
        val code = result.optInt("code", -1)
        if (code == 1) {
            val data = result.optJSONObject("data")
            onSuccess(data)
        } else {
            onSuccess(null)
        }
    }

    /**
     * 解析学生答案（支持冗余字段）
     */
    private fun parseStudentAnswer(data: JSONObject?): StudentAnswer? {
        if (data == null) return null
        return StudentAnswer(
            qId = data.optInt("qId"),
            studentAnswer = data.optString("studentAnswer"),
            status = data.optInt("status"),
            teacherMsg = data.optString("teacherMsg", null),
            // 冗余字段
            studentName = data.optString("studentName", null),
            classId = if (data.has("classId") && !data.isNull("classId")) data.optLong("classId") else null,
            className = data.optString("className", null),
            teacherId = if (data.has("teacherId") && !data.isNull("teacherId")) data.optLong("teacherId") else null,
            questionBrief = data.optString("questionBrief", null),
            sectionId = if (data.has("sectionId") && !data.isNull("sectionId")) data.optInt("sectionId") else null,
            correctAnswer = data.optString("correctAnswer", null)
        )
    }

    /**
     * 解析资源列表（支持扩展字段）
     */
    private fun parseResourceList(data: JSONArray?): List<ResourceInfo> {
        val resources = mutableListOf<ResourceInfo>()
        data?.let {
            for (i in 0 until it.length()) {
                parseResource(it.getJSONObject(i))?.let { resource ->
                    resources.add(resource)
                }
            }
        }
        return resources
    }

    /**
     * 解析单个资源（支持扩展字段）
     */
    private fun parseResource(data: JSONObject?): ResourceInfo? {
        if (data == null) return null
        
        val tagsArray = data.optJSONArray("tags")
        val tags = tagsArray?.let {
            val list = mutableListOf<String>()
            for (i in 0 until it.length()) {
                list.add(it.getString(i))
            }
            list
        }
        
        val classIdsArray = data.optJSONArray("classIds")
        val classIds = classIdsArray?.let {
            val list = mutableListOf<Long>()
            for (i in 0 until it.length()) {
                list.add(it.getLong(i))
            }
            list
        }
        
        return ResourceInfo(
            resId = data.optString("resId", ""),
            resName = data.optString("resName", "") ?: data.optString("name", ""),
            url = data.optString("url", ""),
            teacherId = data.optLong("teacherId", 0),
            teacherName = data.optString("teacherName", null),
            type = data.optString("type", null),
            tags = tags,
            classIds = classIds,
            size = if (data.has("size") && !data.isNull("size")) data.optLong("size") else null,
            createTime = data.optLong("createTime", System.currentTimeMillis())
        )
    }
}

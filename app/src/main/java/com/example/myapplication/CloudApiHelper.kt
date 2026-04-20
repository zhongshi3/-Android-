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
 */
object CloudApiHelper {
    
    /**
     * 用户信息数据类
     * 对应云函数user集合的字段结构
     * 注意：云端user表中userId是Number类型（整数）
     */
    data class UserInfo(
        val userId: Long,  // 云端user表中userId是Number类型（整数）
        val username: String,
        val role: Int, // 0:学生, 1:教师
        val classId: Int? = null,
        val createTime: Long = System.currentTimeMillis(),
        val updateTime: Long = System.currentTimeMillis()
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
    // 云函数地址 - 独立配置注册和登录路径
    private const val BASE_CLOUD_FUNCTION_URL = "https://fc-mp-ea6433d3-77e4-4c0b-9055-7fac5f1236e4.next.bspapp.com"
    private const val LOGIN_FUNCTION_URL = "$BASE_CLOUD_FUNCTION_URL/login"
    const val REGISTER_FUNCTION_URL = "$BASE_CLOUD_FUNCTION_URL/register"

    // OkHttp客户端
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     *
     */
    fun registerUser(
        username: String,
        password: String,
        role: Int, // 0:学生, 1:教师
        classId: Int? = null, // 学生需要班级ID，教师为0
        callback: CloudApiCallback
    ) {
        // 添加详细的参数检查日志
        println("云函数注册参数接收 - username: '$username', role: $role, classId: $classId")
        
        // 检查基本参数
        if (username.isBlank() || password.isBlank()) {
            callback.onError("用户名或密码不能为空")
            return
        }

        // 构建请求数据，根据云函数register/index.js的要求
        // 云函数要求：教师(classId必须为0)，学生(classId必须大于0)
        val finalClassId = if (role == 1) {
            // 教师：班级必须为0
            0
        } else {
            // 学生：必须提供有效的班级ID
            // 注意：云函数会验证班级是否存在
            classId ?: 0
        }
        
        println("注册请求 - 最终参数: role=$role, classId=$finalClassId")
        
        val requestData = JSONObject().apply {
            put("action", "register")  // 标识为注册操作
            put("username", username)
            put("password", password)
            put("role", role)
            put("classId", finalClassId)
        }
        
        val requestBodyStr = requestData.toString()
        println("云函数注册请求JSON数据: $requestBodyStr")
        println("云函数注册请求URL: $REGISTER_FUNCTION_URL")

        // 创建请求 - 使用独立的注册云函数URL
        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)  // 使用独立的注册URL
            .post(requestBodyStr.toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")  // 添加Accept头
            .build()
        
        println("云函数注册请求头: ${request.headers}")

        // 异步执行请求
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
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            callback.onSuccess(result)
                        } else {
                            callback.onError("注册失败：$message")
                        }
                    } else {
                        callback.onError("服务器响应错误：${response.code}")
                    }
                } catch (e: Exception) {
                    callback.onError("解析响应失败：${e.message}")
                }
            }
        })
    }
    
    /**
     * 注册教师用户（便捷方法）
     */
    fun registerTeacher(
        username: String,
        password: String,
        callback: CloudApiCallback
    ) {
        // 调用通用注册方法，role=1表示教师，classId=0
        registerUser(username, password, 1, 0, callback)
    }

    /**
     * 检查用户名是否已存在
     * 注意：云函数没有专门的检查接口，此方法为模拟实现
     * 实际用户名检查在注册时由云函数统一处理
     * 
     * 警告：此方法返回的结果仅供参考，实际用户名可用性以云函数注册时的检查为准
     */
    fun checkUsernameExists(
        username: String,
        callback: CloudApiCallback
    ) {
        // 由于云函数没有专门的用户名检查接口，这里模拟一个成功响应
        // 实际用户名重复检查会在注册时由云函数统一处理
        runOnBackgroundThread {
            try {
                // 模拟网络延迟
                Thread.sleep(500)
                
                // 模拟云函数格式的响应
                // 注意：云函数实际检查在注册时进行，这里只是UI友好的模拟
                val result = JSONObject().apply {
                    put("code", 1)
                    put("msg", "用户名格式检查通过")
                    put("exists", false) // 假设用户名可用，实际由注册时云函数检查
                    put("canUse", true)  // 前端UI可以使用
                    put("note", "实际用户名可用性以注册时云函数检查为准")
                }
                
                callback.onSuccess(result)
            } catch (e: Exception) {
                callback.onError("检查失败：${e.message}")
            }
        }
    }
    
    /**
     * 在后台线程执行任务
     */
    private fun runOnBackgroundThread(task: () -> Unit) {
        Thread {
            task()
        }.start()
    }

    /**
     * 用户登录
     */
    fun login(
        username: String,
        password: String,
        callback: LoginCallback
    ) {
        // 添加更详细的参数检查日志
        println("云函数登录参数接收 - username: '$username' (长度: ${username.length}), password: '***' (长度: ${password.length})")
        
        // 检查参数是否真的非空（云函数会检查空字符串）
        if (username.isBlank() || password.isBlank()) {
            println("警告：登录参数包含空值或空白字符")
            callback.onResult(LoginResponse(false, "用户名或密码不能为空"))
            return
        }

        // 构建请求数据 - 发送到统一的register云函数，添加action参数
        val requestData = JSONObject().apply {
            put("action", "login")  // 标识为登录操作
            put("username", username)
            put("password", password)
        }
        
        val requestBodyStr = requestData.toString()
        println("云函数登录请求JSON数据: $requestBodyStr")
        println("云函数登录请求URL: $REGISTER_FUNCTION_URL (使用统一的register云函数)")

        // 创建请求 - 使用register云函数URL，通过action参数区分登录/注册
        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)  // 使用register云函数URL
            .post(requestBodyStr.toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")  // 添加Accept头
            .build()
        
        println("云函数登录请求头: ${request.headers}")
        println("重要：登录请求已重定向到register云函数，使用action='login'参数")

        // 异步执行请求
        println("开始发送云函数登录请求...")
        println("请求URL详细信息: ${request.url}")
        println("请求完整URL: ${request.url.toString()}")
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("云函数登录请求失败: ${e.message}")
                callback.onResult(LoginResponse(false, "网络连接失败：${e.message}"))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    // 详细记录响应信息，便于调试
                    val requestUrl = call.request().url.toString()
                    
                    println("云函数登录请求完成")
                    println("请求URL: $requestUrl")
                    println("响应状态码: ${response.code}")
                    println("响应消息: ${response.message}")
                    println("响应体长度: ${responseBody?.length ?: 0}")
                    
                    if (response.isSuccessful) {
                        if (responseBody != null) {
                            // 记录详细的响应信息
                            println("云函数请求成功 - URL: $requestUrl, Code: ${response.code}, Body: $responseBody")
                            
                            val result = JSONObject(responseBody)
                            
                            // 检查响应格式
                            if (result.has("code")) {
                                // 标准响应格式：包含code字段
                                val code = result.optInt("code", -1)
                                val message = result.optString("msg", "未知错误")
                                
                                println("标准响应格式 - Code: $code, Message: $message")
                                
                                if (code == 1) {
                                    // 登录成功，解析用户信息
                                    val userData = result.optJSONObject("data")
                                    val userInfo = if (userData != null) {
                                        UserInfo(
                                            userId = userData.optLong("userId", 0L),  // 使用Long类型匹配云端Number
                                            username = userData.optString("username", ""),
                                            role = userData.optInt("role", 0),
                                            classId = if (userData.has("classId") && !userData.isNull("classId")) 
                                                userData.optInt("classId") else null
                                            // createTime和updateTime使用默认值
                                        )
                                    } else {
                                        null
                                    }

                                    val token = result.optString("token", "")
                                    callback.onResult(LoginResponse(true, message, userInfo, token))
                                } else {
                                    println("云函数业务逻辑失败 - Code: $code, Message: $message")
                                    callback.onResult(LoginResponse(false, message))
                                }
                            } else if (result.has("path") && result.has("httpMethod")) {
                                // 网关响应格式：包含path和httpMethod字段
                                println("收到网关响应，云函数可能未正确执行或返回")
                                println("响应类型：网关请求日志")
                                println("请求路径：${result.optString("path", "")}")
                                println("HTTP方法：${result.optString("httpMethod", "")}")
                                
                                // 检查body字段是否包含实际响应
                                val bodyStr = result.optString("body", "")
                                if (bodyStr.isNotEmpty() && bodyStr.startsWith("{")) {
                                    try {
                                        val bodyJson = JSONObject(bodyStr)
                                        if (bodyJson.has("code")) {
                                            // body中包含标准响应
                                            val code = bodyJson.optInt("code", -1)
                                            val message = bodyJson.optString("msg", "未知错误")
                                            println("在body中找到云函数响应 - Code: $code, Message: $message")
                                            callback.onResult(LoginResponse(code == 1, message))
                                        } else {
                                            // body中是请求数据或其他内容
                                            println("body中是请求数据或其他内容: $bodyStr")
                                            callback.onResult(LoginResponse(false, "云函数返回网关调试信息，请检查云函数部署"))
                                        }
                                    } catch (e: Exception) {
                                        println("解析body失败: ${e.message}")
                                        callback.onResult(LoginResponse(false, "云函数响应格式异常"))
                                    }
                                } else {
                                    callback.onResult(LoginResponse(false, "云函数返回网关请求信息，请检查云函数代码和部署"))
                                }
                            } else {
                                // 未知响应格式
                                println("未知响应格式: $responseBody")
                                callback.onResult(LoginResponse(false, "云函数返回未知格式响应"))
                            }
                        } else {
                            println("云函数响应体为空 - URL: $requestUrl, Code: ${response.code}")
                            callback.onResult(LoginResponse(false, "服务器返回空响应"))
                        }
                    } else {
                        println("云函数HTTP请求失败 - URL: $requestUrl, Code: ${response.code}, Message: ${response.message}")
                        callback.onResult(LoginResponse(false, "服务器响应错误 ${response.code}: ${response.message}"))
                    }
                } catch (e: Exception) {
                    println("云函数响应解析异常: ${e.message}")
                    callback.onResult(LoginResponse(false, "解析响应失败：${e.message}"))
                }
            }
        })
    }

    /**
     * 通用回调接口
     */
    interface CloudApiCallback {
        fun onSuccess(result: JSONObject)
        fun onError(error: String)
    }
    
    /**
     * 登录专用回调接口
     */
    interface LoginCallback {
        fun onResult(response: LoginResponse)
    }
    
    /**
     * 习题同步响应数据类
     */
    data class QuestionSyncResponse(
        val success: Boolean,
        val message: String,
        val newQuestionsCount: Int = 0,
        val deletedQuestionsCount: Int = 0
    )
    
    /**
     * 习题同步回调接口
     */
    interface QuestionSyncCallback {
        fun onResult(response: QuestionSyncResponse)
    }
    
    /**
     * 同步习题数据
     */
    fun syncQuestions(
        maxQuestionNumber: Int,
        callback: QuestionSyncCallback
    ) {
        // 构建请求数据
        val requestData = JSONObject().apply {
            put("action", "syncQuestions")
            put("maxQuestionNumber", maxQuestionNumber)
        }
        
        val requestBodyStr = requestData.toString()
        println("习题同步请求JSON数据: $requestBodyStr")
        println("习题同步请求URL: $REGISTER_FUNCTION_URL")
        
        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestBodyStr.toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()
        
        println("开始发送习题同步请求...")
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("习题同步请求失败: ${e.message}")
                callback.onResult(QuestionSyncResponse(false, "网络连接失败：${e.message}"))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    println("习题同步响应状态码: ${response.code}")
                    println("习题同步响应体: $responseBody")
                    
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        
                        if (result.has("code")) {
                            val code = result.optInt("code", -1)
                            val message = result.optString("msg", "未知错误")
                            val data = result.optJSONObject("data")
                            
                            if (code == 1 && data != null) {
                                val newQuestionsArray = data.optJSONArray("newQuestions")
                                val deletedQuestionsArray = data.optJSONArray("deletedQuestionNumbers")
                                
                                val newQuestionsCount = newQuestionsArray?.length() ?: 0
                                val deletedQuestionsCount = deletedQuestionsArray?.length() ?: 0
                                
                                println("同步成功: 新增${newQuestionsCount}题, 删除${deletedQuestionsCount}题")
                                callback.onResult(QuestionSyncResponse(
                                    true,
                                    message,
                                    newQuestionsCount,
                                    deletedQuestionsCount
                                ))
                            } else {
                                println("习题同步业务失败: code=$code, msg=$message")
                                callback.onResult(QuestionSyncResponse(false, message))
                            }
                        } else {
                            println("习题同步响应格式错误")
                            callback.onResult(QuestionSyncResponse(false, "响应格式错误"))
                        }
                    } else {
                        println("习题同步HTTP请求失败: ${response.code}")
                        callback.onResult(QuestionSyncResponse(false, "服务器响应错误 ${response.code}"))
                    }
                } catch (e: Exception) {
                    println("习题同步响应解析异常: ${e.message}")
                    callback.onResult(QuestionSyncResponse(false, "解析响应失败：${e.message}"))
                }
            }
        })
    }
    
    // ==================== 班级相关功能 ====================
    
    /**
     * 班级信息数据类
     */
    data class ClassInfo(
        val classId: Long,
        val className: String,
        val teacherId: Long,
        val createTime: Long = System.currentTimeMillis(),
        val studentCount: Int = 0
    )
    
    /**
     * 创建班级回调
     */
    interface CreateClassCallback {
        fun onResult(success: Boolean, message: String, classInfo: ClassInfo? = null)
    }
    
    /**
     * 获取班级回调
     */
    interface GetClassesCallback {
        fun onResult(success: Boolean, message: String, classes: List<ClassInfo>)
    }
    
    /**
     * 获取教师的所有班级（异步）
     */
    fun getClasses(teacherId: Long, callback: GetClassesCallback) {
        val requestData = JSONObject().apply {
            put("action", "getClasses")
            put("teacherId", teacherId)
        }
        
        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}", emptyList())
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")
                        
                        if (code == 1) {
                            val classesArray = result.optJSONArray("data")
                            val classes = mutableListOf<ClassInfo>()
                            
                            if (classesArray != null) {
                                for (i in 0 until classesArray.length()) {
                                    val classObj = classesArray.getJSONObject(i)
                                    classes.add(ClassInfo(
                                        classId = classObj.optLong("classId"),
                                        className = classObj.optString("className"),
                                        teacherId = teacherId
                                    ))
                                }
                            }
                            callback.onResult(true, message, classes)
                        } else {
                            callback.onResult(false, message, emptyList())
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}", emptyList())
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}", emptyList())
                }
            }
        })
    }
    
    /**
     * 创建新班级（异步）
     */
    fun createClass(teacherId: Long, className: String, callback: CreateClassCallback) {
        val requestData = JSONObject().apply {
            put("action", "createClass")
            put("teacherId", teacherId)
            put("className", className)
        }
        
        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
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
                            callback.onResult(false, message)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}")
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}")
                }
            }
        })
    }
    
    /**
     * 获取班级响应数据类
     */
    data class GetClassesResponse(
        val success: Boolean,
        val message: String,
        val data: List<ClassInfo> = emptyList()
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
     * 学生注册数据类
     */
    data class StudentRegisterData(
        val userId: Long,
        val username: String,
        val classId: Long
    )
    
    /**
     * 学生答案数据类
     */
    data class StudentAnswer(
        val qId: Int,  // 题号
        val studentAnswer: String,
        val status: Int,  // 0: 未批改, 1: 正确, 2: 错误
        val teacherMsg: String? = null  // 教师留言
    )
    
    /**
     * 提交答案响应数据类
     */
    data class SubmitAnswerResponse(
        val success: Boolean,
        val message: String,
        val data: StudentAnswer? = null
    )
    
    /**
     * 查询学生答案响应数据类
     */
    data class GetStudentAnswerResponse(
        val success: Boolean,
        val message: String,
        val data: StudentAnswer? = null  // null表示未查询到
    )
    
    /**
     * 提交学生答案（异步）
     */
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
        
        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")
                        
                        if (code == 1) {
                            val data = result.optJSONObject("data")
                            val answerData = if (data != null) {
                                StudentAnswer(
                                    qId = data.optInt("qId"),
                                    studentAnswer = data.optString("studentAnswer"),
                                    status = data.optInt("status")
                                )
                            } else null
                            callback.onResult(true, message, answerData)
                        } else {
                            callback.onResult(false, message)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}")
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}")
                }
            }
        })
    }
    
    /**
     * 提交答案回调接口
     */
    interface SubmitAnswerCallback {
        fun onResult(success: Boolean, message: String, data: StudentAnswer? = null)
    }
    
    /**
     * 查询学生答案（异步）
     */
    fun getStudentAnswer(
        userId: Long,
        qId: Int,
        callback: GetStudentAnswerCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "getStudentAnswer")
            put("userId", userId)
            put("qId", qId)
        }
        
        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")
                        
                        if (code == 1) {
                            val data = result.optJSONObject("data")
                            val answerData = if (data != null) {
                                StudentAnswer(
                                    qId = data.optInt("qId"),
                                    studentAnswer = data.optString("studentAnswer"),
                                    status = data.optInt("status"),
                                    teacherMsg = if (data.has("teacherMsg") && !data.isNull("teacherMsg")) 
                                        data.optString("teacherMsg") else null
                                )
                            } else null
                            callback.onResult(true, message, answerData)
                        } else {
                            callback.onResult(false, message, null)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}", null)
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}", null)
                }
            }
        })
    }
    
    /**
     * 查询学生答案回调接口
     */
    interface GetStudentAnswerCallback {
        fun onResult(success: Boolean, message: String, data: StudentAnswer? = null)
    }
    
    /**
     * 批量查询学生答案回调接口
     */
    interface GetBatchStudentAnswersCallback {
        fun onResult(success: Boolean, message: String, data: Map<Int, StudentAnswer>? = null)
    }
    
    /**
     * 批量查询学生答案（异步）
     * @param userId 用户ID
     * @param qIds 题目ID列表
     * @param callback 回调
     */
    fun getBatchStudentAnswers(
        userId: Long,
        qIds: List<Int>,
        callback: GetBatchStudentAnswersCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "getBatchStudentAnswers")
            put("userId", userId)
            put("qIds", JSONArray(qIds))
        }
        
        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")
                        
                        if (code == 1) {
                            val data = result.optJSONObject("data")
                            val answersMap = mutableMapOf<Int, StudentAnswer>()
                            
                            data?.let {
                                it.keys().forEach { key ->
                                    val answerObj = it.getJSONObject(key)
                                    answersMap[key.toInt()] = StudentAnswer(
                                        qId = answerObj.optInt("qId"),
                                        studentAnswer = answerObj.optString("studentAnswer"),
                                        status = answerObj.optInt("status"),
                                        teacherMsg = if (answerObj.has("teacherMsg") && !answerObj.isNull("teacherMsg"))
                                            answerObj.optString("teacherMsg") else null
                                    )
                                }
                            }
                            callback.onResult(true, message, answersMap)
                        } else {
                            callback.onResult(false, message, null)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}", null)
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}", null)
                }
            }
        })
    }

    /**
     * 获取学生所有答题记录回调接口
     */
    interface GetStudentAllAnswersCallback {
        fun onResult(success: Boolean, message: String, data: Map<Int, StudentAnswer>?)
    }

    /**
     * 获取指定学生的所有答题记录
     * @param userId 学生用户ID
     * @param callback 回调
     */
    fun getStudentAllAnswers(
        userId: Long,
        callback: GetStudentAllAnswersCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "getStudentAllAnswers")
            put("userId", userId)
        }

        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}", null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            val data = result.optJSONObject("data")
                            val answersMap = mutableMapOf<Int, StudentAnswer>()

                            data?.let {
                                it.keys().forEach { key ->
                                    val answerObj = it.getJSONObject(key)
                                    answersMap[key.toInt()] = StudentAnswer(
                                        qId = answerObj.optInt("qId"),
                                        studentAnswer = answerObj.optString("studentAnswer"),
                                        status = answerObj.optInt("status"),
                                        teacherMsg = if (answerObj.has("teacherMsg") && !answerObj.isNull("teacherMsg"))
                                            answerObj.optString("teacherMsg") else null
                                    )
                                }
                            }
                            callback.onResult(true, message, answersMap)
                        } else {
                            callback.onResult(false, message, null)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}", null)
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}", null)
                }
            }
        })
    }
    
    /**
     * 删除题目响应数据类
     */
    data class DeleteQuestionResponse(
        val success: Boolean,
        val message: String,
        val data: DeleteQuestionData? = null
    )
    
    /**
     * 删除题目数据类
     */
    data class DeleteQuestionData(
        val qId: Int
    )
    
    /**
     * 上传题目响应数据类
     */
    data class UploadQuestionResponse(
        val success: Boolean,
        val message: String,
        val data: UploadQuestionData? = null
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
     * 删除题目（异步）
     */
    fun deleteQuestion(
        userId: Long,
        qId: Int,
        callback: DeleteQuestionCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "deleteQuestion")
            put("userId", userId)
            put("qId", qId)
        }
        
        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")
                        
                        if (code == 1) {
                            val data = result.optJSONObject("data")
                            val deleteData = if (data != null) {
                                DeleteQuestionData(
                                    qId = data.optInt("qId")
                                )
                            } else null
                            callback.onResult(true, message, deleteData)
                        } else {
                            callback.onResult(false, message)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}")
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}")
                }
            }
        })
    }
    
    /**
     * 删除题目回调接口
     */
    interface DeleteQuestionCallback {
        fun onResult(success: Boolean, message: String, data: DeleteQuestionData? = null)
    }
    
    /**
     * 上传题目（异步）
     */
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
        
        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")
                        
                        if (code == 1) {
                            val data = result.optJSONObject("data")
                            val uploadData = if (data != null) {
                                UploadQuestionData(
                                    qId = data.optInt("qId"),
                                    content = data.optString("content"),
                                    answer = data.optString("answer"),
                                    sectionId = data.optInt("sectionId"),
                                    imageUrl = if (data.has("imageUrl") && !data.isNull("imageUrl")) 
                                        data.optString("imageUrl") else null
                                )
                            } else null
                            callback.onResult(true, message, uploadData)
                        } else {
                            callback.onResult(false, message)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}")
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}")
                }
            }
        })
    }
    
    /**
     * 上传题目回调接口
     */
    interface UploadQuestionCallback {
        fun onResult(success: Boolean, message: String, data: UploadQuestionData? = null)
    }
    
    /**
     * 同步获取教师管理的班级
     * @param teacherId 教师ID
     * @return GetClassesResponse
     */
    suspend fun getClassesByTeacher(teacherId: Long): GetClassesResponse {
        return try {
            // 构建请求数据
            val requestData = JSONObject().apply {
                put("action", "getClasses")
                put("teacherId", teacherId)
            }
            
            val requestBodyStr = requestData.toString()
            println("获取班级请求JSON数据: $requestBodyStr")
            println("获取班级请求URL: $REGISTER_FUNCTION_URL")
            
            val request = Request.Builder()
                .url(REGISTER_FUNCTION_URL)
                .post(requestBodyStr.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()
            
            println("开始发送获取班级请求...")
            
            // 同步执行请求
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            println("获取班级响应状态码: ${response.code}")
            println("获取班级响应体: $responseBody")
            
            if (response.isSuccessful && responseBody != null) {
                val result = JSONObject(responseBody)
                
                if (result.has("code")) {
                    val code = result.optInt("code", -1)
                    val message = result.optString("msg", "未知错误")
                    
                    if (code == 1) {
                        // data 直接是数组，不是对象
                        val classesArray = result.optJSONArray("data")
                        val classes = mutableListOf<ClassInfo>()
                        
                        if (classesArray != null) {
                            for (i in 0 until classesArray.length()) {
                                val classObj = classesArray.getJSONObject(i)
                                val classInfo = ClassInfo(
                                    classId = classObj.optLong("classId"),
                                    className = classObj.optString("className"),
                                    teacherId = classObj.optLong("teacherId"),
                                    studentCount = classObj.optInt("studentCount", 0)
                                )
                                classes.add(classInfo)
                            }
                        }
                        
                        println("获取班级成功: ${classes.size}个班级")
                        return GetClassesResponse(true, message, classes)
                    } else {
                        println("获取班级业务失败: code=$code, msg=$message")
                        return GetClassesResponse(false, message)
                    }
                } else {
                    println("获取班级响应格式错误")
                    return GetClassesResponse(false, "响应格式错误")
                }
            } else {
                println("获取班级HTTP请求失败: ${response.code}")
                return GetClassesResponse(false, "服务器响应错误 ${response.code}")
            }
            
        } catch (e: Exception) {
            println("获取班级异常: ${e.message}")
            println("获取班级异常类型: ${e.javaClass.name}")
            e.printStackTrace()
            val errorMsg = e.message ?: "未知错误 (${e.javaClass.simpleName})"
            return GetClassesResponse(false, "请求异常: $errorMsg")
        }
    }
    
    /**
     * 同步注册学生
     * @param username 用户名
     * @param password 密码
     * @param classId 班级ID
     * @return RegisterStudentResponse
     */
    suspend fun registerStudent(username: String, password: String, classId: Long): RegisterStudentResponse {
        return try {
            // 构建请求数据
            val requestData = JSONObject().apply {
                put("action", "register")
                put("username", username)
                put("password", password)
                put("role", 0)  // 学生角色
                put("classId", classId)
            }
            
            val requestBodyStr = requestData.toString()
            println("注册学生请求JSON数据: $requestBodyStr")
            println("注册学生请求URL: $REGISTER_FUNCTION_URL")
            
            val request = Request.Builder()
                .url(REGISTER_FUNCTION_URL)
                .post(requestBodyStr.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()
            
            println("开始发送注册学生请求...")
            
            // 同步执行请求
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            println("注册学生响应状态码: ${response.code}")
            println("注册学生响应体: $responseBody")
            
            if (response.isSuccessful && responseBody != null) {
                val result = JSONObject(responseBody)
                
                if (result.has("code")) {
                    val code = result.optInt("code", -1)
                    val message = result.optString("msg", "未知错误")
                    val data = result.optJSONObject("data")
                    
                    if (code == 1 && data != null) {
                        val registerData = StudentRegisterData(
                            userId = data.optLong("userId"),
                            username = data.optString("username"),
                            classId = data.optLong("classId")
                        )
                        
                        println("注册学生成功: ${registerData.username}")
                        return RegisterStudentResponse(true, message, registerData)
                    } else {
                        println("注册学生业务失败: code=$code, msg=$message")
                        return RegisterStudentResponse(false, message)
                    }
                } else {
                    println("注册学生响应格式错误")
                    return RegisterStudentResponse(false, "响应格式错误")
                }
            } else {
                println("注册学生HTTP请求失败: ${response.code}")
                return RegisterStudentResponse(false, "服务器响应错误 ${response.code}")
            }
            
        } catch (e: Exception) {
            println("注册学生异常: ${e.message}")
            return RegisterStudentResponse(false, "请求异常: ${e.message}")
        }
    }

    /**
     * 上传图片到云存储（通过云函数，使用 Base64 方式）
     */
    fun uploadImageToCloud(
        fileName: String,
        base64Data: String,
        callback: UploadImageCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "uploadImage")
            put("fileName", fileName)
            put("imageData", base64Data)
        }

        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            val data = result.optJSONObject("data")
                            val imageUrl = data?.optString("imageUrl") ?: ""
                            callback.onResult(true, message, imageUrl)
                        } else {
                            callback.onResult(false, message)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}")
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}")
                }
            }
        })
    }

    /**
     * 上传图片回调接口
     */
    interface UploadImageCallback {
        fun onResult(success: Boolean, message: String, imageUrl: String? = null)
    }

    /**
     * 班级题目答题统计数据类
     */
    data class ClassQuestionStats(
        val classId: Long,
        val qId: Int,
        val totalStudents: Int,      // 班级总人数
        val notDoneCount: Int,       // 未完成人数
        val notCheckedCount: Int,    // 未批改人数
        val wrongCount: Int,         // 错误人数
        val correctCount: Int        // 正确人数
    )

    /**
     * 获取班级答题统计回调接口
     */
    interface GetClassQuestionStatsCallback {
        fun onResult(success: Boolean, message: String, data: ClassQuestionStats? = null)
    }

    /**
     * 获取班级某道题的答题统计
     * @param classId 班级ID
     * @param qId 题目ID
     * @param callback 回调
     */
    fun getClassQuestionStats(
        classId: Long,
        qId: Int,
        callback: GetClassQuestionStatsCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "getClassQuestionStats")
            put("classId", classId)
            put("qId", qId)
        }

        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            val data = result.optJSONObject("data")
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
                            callback.onResult(true, message, stats)
                        } else {
                            callback.onResult(false, message)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}")
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}")
                }
            }
        })
    }

    // ==================== 习题批改相关功能 ====================

    /**
     * 学生信息数据类
     */
    data class StudentInfo(
        val userId: Long,
        val username: String,
        val classId: Long? = null
    )

    /**
     * 未批改题目信息数据类
     */
    data class QuestionUngradedInfo(
        val qId: Int,
        val userId: Long
    )

    /**
     * 获取教师管理班级学生未批改题目回调接口
     */
    interface GetTeacherUngradedQuestionsCallback {
        fun onResult(success: Boolean, message: String, data: List<QuestionUngradedInfo>?)
    }

    /**
     * 获取教师管理班级学生的未批改题目列表
     * @param teacherId 教师ID
     * @param callback 回调
     */
    fun getTeacherUngradedQuestions(
        teacherId: Long,
        callback: GetTeacherUngradedQuestionsCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "getTeacherUngradedQuestions")
            put("teacherId", teacherId)
        }

        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}", null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            val dataArray = result.optJSONArray("data")
                            val list = mutableListOf<QuestionUngradedInfo>()

                            dataArray?.let {
                                for (i in 0 until it.length()) {
                                    val item = it.getJSONObject(i)
                                    list.add(QuestionUngradedInfo(
                                        qId = item.optInt("qId"),
                                        userId = item.optLong("userId")
                                    ))
                                }
                            }
                            callback.onResult(true, message, list)
                        } else {
                            callback.onResult(false, message, null)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}", null)
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}", null)
                }
            }
        })
    }

    /**
     * 所有答题记录信息数据类
     */
    data class QuestionAnsweredInfo(
        val qId: Int,
        val userId: Long,
        val status: Int  // 0:未批改, 1:正确, 2:错误
    )

    /**
     * 获取教师管理班级学生所有答题记录回调接口
     */
    interface GetTeacherAllAnsweredQuestionsCallback {
        fun onResult(success: Boolean, message: String, data: List<QuestionAnsweredInfo>?)
    }

    /**
     * 获取教师管理班级学生的所有有答题记录的题目列表
     * @param teacherId 教师ID
     * @param callback 回调
     */
    fun getTeacherAllAnsweredQuestions(
        teacherId: Long,
        callback: GetTeacherAllAnsweredQuestionsCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "getTeacherAllAnsweredQuestions")
            put("teacherId", teacherId)
        }

        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}", null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            val dataArray = result.optJSONArray("data")
                            val list = mutableListOf<QuestionAnsweredInfo>()

                            dataArray?.let {
                                for (i in 0 until it.length()) {
                                    val item = it.getJSONObject(i)
                                    list.add(QuestionAnsweredInfo(
                                        qId = item.optInt("qId"),
                                        userId = item.optLong("userId"),
                                        status = item.optInt("status", 0)
                                    ))
                                }
                            }
                            callback.onResult(true, message, list)
                        } else {
                            callback.onResult(false, message, null)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}", null)
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}", null)
                }
            }
        })
    }

    /**
     * 获取学生信息回调接口
     */
    interface GetStudentsInfoCallback {
        fun onResult(success: Boolean, message: String, data: List<StudentInfo>?)
    }

    /**
     * 获取多个学生的信息
     * @param userIds 用户ID列表
     * @param callback 回调
     */
    fun getStudentsInfo(
        userIds: List<Long>,
        callback: GetStudentsInfoCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "getStudentsInfo")
            put("userIds", JSONArray(userIds))
        }

        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}", null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            val dataArray = result.optJSONArray("data")
                            val list = mutableListOf<StudentInfo>()

                            dataArray?.let {
                                for (i in 0 until it.length()) {
                                    val item = it.getJSONObject(i)
                                    list.add(StudentInfo(
                                        userId = item.optLong("userId"),
                                        username = item.optString("username"),
                                        classId = if (item.has("classId") && !item.isNull("classId"))
                                            item.optLong("classId") else null
                                    ))
                                }
                            }
                            callback.onResult(true, message, list)
                        } else {
                            callback.onResult(false, message, null)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}", null)
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}", null)
                }
            }
        })
    }

    /**
     * 获取班级学生回调接口
     */
    interface GetClassStudentsCallback {
        fun onResult(success: Boolean, message: String, data: List<StudentInfo>?)
    }

    /**
     * 获取指定班级的所有学生
     * @param classId 班级ID
     * @param callback 回调
     */
    fun getClassStudents(
        classId: Long,
        callback: GetClassStudentsCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "getClassStudents")
            put("classId", classId)
        }

        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}", null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            val dataArray = result.optJSONArray("data")
                            val list = mutableListOf<StudentInfo>()

                            dataArray?.let {
                                for (i in 0 until it.length()) {
                                    val item = it.getJSONObject(i)
                                    list.add(StudentInfo(
                                        userId = item.optLong("userId"),
                                        username = item.optString("username"),
                                        classId = classId
                                    ))
                                }
                            }
                            callback.onResult(true, message, list)
                        } else {
                            callback.onResult(false, message, null)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}", null)
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}", null)
                }
            }
        })
    }

    /**
     * 提交批改回调接口
     */
    interface SubmitGradeCallback {
        fun onResult(success: Boolean, message: String)
    }

    /**
     * 提交批改结果
     * @param teacherId 教师ID
     * @param userId 学生ID
     * @param qId 题目ID
     * @param status 批改状态 (1=正确, 2=错误)
     * @param teacherMsg 教师留言
     * @param callback 回调
     */
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

        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            callback.onResult(true, message)
                        } else {
                            callback.onResult(false, message)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}")
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}")
                }
            }
        })
    }

    // ==================== 错题本相关功能 ====================

    /**
     * 错题信息数据类
     */
    data class ErrorQuestionInfo(
        val qId: Int,
        val contentPreview: String,
        val studentAnswer: String,
        val teacherMsg: String?,
        val sectionId: Int,
        val status: Int  // 0: 待复习, 1: 已复习
    )

    /**
     * 获取学生错题回调接口
     */
    interface GetStudentErrorQuestionsCallback {
        fun onResult(success: Boolean, message: String, data: List<ErrorQuestionInfo>?)
    }

    /**
     * 获取学生的错题列表（从已批改为错误的答题记录中获取）
     * @param userId 学生用户ID
     * @param callback 回调
     */
    fun getStudentErrorQuestions(
        userId: Long,
        callback: GetStudentErrorQuestionsCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "getStudentErrorQuestions")
            put("userId", userId)
        }

        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}", null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            val dataArray = result.optJSONArray("data")
                            val list = mutableListOf<ErrorQuestionInfo>()

                            dataArray?.let {
                                for (i in 0 until it.length()) {
                                    val item = it.getJSONObject(i)
                                    list.add(ErrorQuestionInfo(
                                        qId = item.optInt("qId"),
                                        contentPreview = item.optString("contentPreview", ""),
                                        studentAnswer = item.optString("studentAnswer", ""),
                                        teacherMsg = if (item.has("teacherMsg") && !item.isNull("teacherMsg"))
                                            item.optString("teacherMsg") else null,
                                        sectionId = item.optInt("sectionId", 0),
                                        status = item.optInt("status", 0)
                                    ))
                                }
                            }
                            callback.onResult(true, message, list)
                        } else {
                            callback.onResult(false, message, null)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}", null)
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}", null)
                }
            }
        })
    }

    // ==================== 资源管理相关功能 ====================

    /**
     * 资源信息数据类
     */
    data class ResourceInfo(
        val resId: String,
        val resName: String,
        val url: String,
        val teacherId: Long,
        val createTime: Long
    )

    /**
     * 获取资源列表回调接口
     */
    interface GetResourcesCallback {
        fun onResult(success: Boolean, message: String, resources: List<ResourceInfo>?)
    }

    /**
     * 获取教师上传的资源列表
     * @param teacherId 教师ID
     * @param callback 回调
     */
    fun getTeacherResources(teacherId: Long, callback: GetResourcesCallback) {
        val requestData = JSONObject().apply {
            put("action", "getTeacherResources")
            put("teacherId", teacherId)
        }

        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}", null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            val dataArray = result.optJSONArray("data")
                            val list = mutableListOf<ResourceInfo>()

                            dataArray?.let {
                                for (i in 0 until it.length()) {
                                    val item = it.getJSONObject(i)
                                    list.add(ResourceInfo(
                                        resId = item.optString("resId", ""),
                                        resName = item.optString("resName", ""),
                                        url = item.optString("url", ""),
                                        teacherId = item.optLong("teacherId", 0),
                                        createTime = item.optLong("createTime", System.currentTimeMillis())
                                    ))
                                }
                            }
                            callback.onResult(true, message, list)
                        } else {
                            callback.onResult(false, message, null)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}", null)
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}", null)
                }
            }
        })
    }

    /**
     * 获取所有资源列表（学生用）
     * @param callback 回调
     */
    fun getAllResources(callback: GetResourcesCallback) {
        val requestData = JSONObject().apply {
            put("action", "getAllResources")
        }

        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}", null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            val dataArray = result.optJSONArray("data")
                            val list = mutableListOf<ResourceInfo>()

                            dataArray?.let {
                                for (i in 0 until it.length()) {
                                    val item = it.getJSONObject(i)
                                    list.add(ResourceInfo(
                                        resId = item.optString("resId", ""),
                                        resName = item.optString("resName", ""),
                                        url = item.optString("url", ""),
                                        teacherId = item.optLong("teacherId", 0),
                                        createTime = item.optLong("createTime", System.currentTimeMillis())
                                    ))
                                }
                            }
                            callback.onResult(true, message, list)
                        } else {
                            callback.onResult(false, message, null)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}", null)
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}", null)
                }
            }
        })
    }

    /**
     * 上传资源回调接口
     */
    interface UploadResourceCallback {
        fun onResult(success: Boolean, message: String, resource: ResourceInfo? = null)
    }

    /**
     * 上传资源到云存储并保存到数据库
     * @param teacherId 教师ID
     * @param resName 资源名称
     * @param fileName 文件名
     * @param base64Data 文件的Base64数据
     * @param callback 回调
     */
    fun uploadResource(
        teacherId: Long,
        resName: String,
        fileName: String,
        base64Data: String,
        callback: UploadResourceCallback
    ) {
        val requestData = JSONObject().apply {
            put("action", "uploadResource")
            put("teacherId", teacherId)
            put("resName", resName)
            put("fileName", fileName)
            put("fileData", base64Data)
        }

        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}", null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            val data = result.optJSONObject("data")
                            if (data != null) {
                                val resource = ResourceInfo(
                                    resId = data.optString("resId", ""),
                                    resName = data.optString("resName", ""),
                                    url = data.optString("url", ""),
                                    teacherId = data.optLong("teacherId", 0),
                                    createTime = data.optLong("createTime", System.currentTimeMillis())
                                )
                                callback.onResult(true, message, resource)
                            } else {
                                callback.onResult(true, message, null)
                            }
                        } else {
                            callback.onResult(false, message, null)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}", null)
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}", null)
                }
            }
        })
    }

    /**
     * 删除资源回调接口
     */
    interface DeleteResourceCallback {
        fun onResult(success: Boolean, message: String)
    }

    /**
     * 删除资源
     * @param resId 资源ID
     * @param teacherId 教师ID（用于验证）
     * @param callback 回调
     */
    fun deleteResource(resId: String, teacherId: Long, callback: DeleteResourceCallback) {
        val requestData = JSONObject().apply {
            put("action", "deleteResource")
            put("resId", resId)
            put("teacherId", teacherId)
        }

        val request = Request.Builder()
            .url(REGISTER_FUNCTION_URL)
            .post(requestData.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onResult(false, "网络连接失败：${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val result = JSONObject(responseBody)
                        val code = result.optInt("code", -1)
                        val message = result.optString("msg", "未知错误")

                        if (code == 1) {
                            callback.onResult(true, message)
                        } else {
                            callback.onResult(false, message)
                        }
                    } else {
                        callback.onResult(false, "服务器响应错误 ${response.code}")
                    }
                } catch (e: Exception) {
                    callback.onResult(false, "解析响应失败：${e.message}")
                }
            }
        })
    }
}
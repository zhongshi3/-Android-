'use strict';

console.log('=== 开始导入依赖 ===');

// ==================== 公共工具模块 ====================

// 统一请求解析
function parseEvent(event) {
    if (!event) return {}
    if (!event.body) return event
    return typeof event.body === 'string' ? JSON.parse(event.body) : event.body
}

// 统一返回格式
const ok = (msg, data = null) => ({ code: 1, msg, data })
const fail = (msg, code = 0, data = null) => ({ code, msg, data })

// 统一时间处理
function now() {
    return new Date()
}

// 角色常量
const ROLE = {
    STUDENT: 0,
    TEACHER: 1
}

// 状态常量
const ANSWER_STATUS = {
    NOT_CHECKED: 0,
    CORRECT: 1,
    WRONG: 2
}

// ==================== 数据库连接模块 ====================

let db;
try {
    db = uniCloud.database();
    console.log('uniCloud.database导入成功');
} catch (error) {
    console.error('uniCloud.database导入失败:', error);
}

let bcrypt;
try {
    bcrypt = require('bcryptjs');
    console.log('bcryptjs导入成功，版本:', bcrypt.version || '未知');
} catch (error) {
    console.error('bcryptjs导入失败:', error.message);
    console.error('错误堆栈:', error.stack);
    // 如果bcryptjs导入失败，使用简单加密（仅用于测试）
    bcrypt = {
        hashSync: (password, saltRounds) => {
            console.warn('使用模拟bcrypt，生产环境不安全！');
            return password;
        },
        compareSync: (password, hashedPassword) => {
            console.warn('使用模拟bcrypt.compareSync，生产环境不安全！');
            return password === hashedPassword;
        }
    };
}

// ==================== 控制器层 ====================

// 用户控制器
const userController = {
    register: handleRegister,
    login: handleLogin,
    getStudentsInfo: handleGetStudentsInfo,
    getClassStudents: handleGetClassStudents,
    changePassword: handleChangePassword
}

// 班级控制器
const classController = {
    getClasses: handleGetClasses,
    createClass: handleCreateClass
}

// 题目控制器
const questionController = {
    syncQuestions: handleQuestionSync,
    uploadQuestion: handleUploadQuestion,
    deleteQuestion: handleDeleteQuestion
}

// 答题控制器
const answerController = {
    submitAnswer: handleSubmitAnswer,
    getStudentAnswer: handleGetStudentAnswer,
    getBatchStudentAnswers: handleGetBatchStudentAnswers,
    getStudentAllAnswers: handleGetStudentAllAnswers,
    submitGrade: handleSubmitGrade,
    getClassQuestionStats: handleGetClassQuestionStats,
    getTeacherUngradedQuestions: handleGetTeacherUngradedQuestions,
    getTeacherAllAnsweredQuestions: handleGetTeacherAllAnsweredQuestions,
    getStudentErrorQuestions: handleGetStudentErrorQuestions
}

// 资源控制器
const resourceController = {
    getTeacherResources: handleGetTeacherResources,
    getAllResources: handleGetAllResources,
    uploadResource: handleUploadResource,
    deleteResource: handleDeleteResource
}

// 文件控制器
const fileController = {
    getUploadParams: handleGetUploadParams,
    uploadImage: handleUploadImage
}

// action路由表
const actionMap = {
    register: userController.register,
    login: userController.login,
    getStudentsInfo: userController.getStudentsInfo,
    getClassStudents: userController.getClassStudents,
    changePassword: userController.changePassword,
    getClasses: classController.getClasses,
    createClass: classController.createClass,
    syncQuestions: questionController.syncQuestions,
    uploadQuestion: questionController.uploadQuestion,
    deleteQuestion: questionController.deleteQuestion,
    submitAnswer: answerController.submitAnswer,
    getStudentAnswer: answerController.getStudentAnswer,
    getBatchStudentAnswers: answerController.getBatchStudentAnswers,
    getStudentAllAnswers: answerController.getStudentAllAnswers,
    submitGrade: answerController.submitGrade,
    getClassQuestionStats: answerController.getClassQuestionStats,
    getTeacherUngradedQuestions: answerController.getTeacherUngradedQuestions,
    getTeacherAllAnsweredQuestions: answerController.getTeacherAllAnsweredQuestions,
    getStudentErrorQuestions: answerController.getStudentErrorQuestions,
    getTeacherResources: resourceController.getTeacherResources,
    getAllResources: resourceController.getAllResources,
    uploadResource: resourceController.uploadResource,
    deleteResource: resourceController.deleteResource,
    getUploadParams: fileController.getUploadParams,
    uploadImage: fileController.uploadImage
}

// 云函数入口函数 - 单入口 + action分发
exports.main = async (event, context) => {
    console.log('=== 用户管理云函数开始执行 ===');
    console.log('完整event对象:', JSON.stringify(event, null, 2));
    
    try {
        const params = parseEvent(event)
        const action = params.action || 'register'
        
        console.log('云函数请求参数:', { action });
        
        const handler = actionMap[action]
        if (!handler) {
            return fail('未知操作类型，支持: ' + Object.keys(actionMap).join(', '))
        }
        
        return await handler(params, db, bcrypt)
        
    } catch (error) {
        console.error('用户管理云函数错误:', error);
        console.error('错误堆栈:', error.stack);
        
        return {
            code: -1,
            msg: '服务器内部错误: ' + error.message,
            data: null
        };
    }
}

// ==================== 用户服务层 ====================

// 修改密码处理函数
async function handleChangePassword(params, db, bcrypt) {
    console.log('=== 处理修改密码请求 ===');

    if (!db) return fail('数据库连接失败', -3);

    const { userId, username, newPassword } = params;

    // 参数校验
    if (!userId || !username || !newPassword) {
        return fail('用户ID、用户名和新密码不能为空');
    }

    // 密码长度校验（与注册登录一致）
    if (newPassword.length < 6 || newPassword.length > 20) {
        return fail('密码长度必须在6-20位之间');
    }

    try {
        const userCollection = db.collection('user');

        // 查询用户是否存在
        const queryResult = await userCollection.where({ userId, username }).get();

        if (queryResult.data.length === 0) {
            return fail('用户不存在');
        }

        const user = queryResult.data[0];

        // 验证新密码是否与原密码相同（前端已验证，此处再次确认）
        const isSameAsOld = bcrypt.compareSync(newPassword, user.passwordHash);
        if (isSameAsOld) {
            return fail('新密码不能与原密码相同');
        }

        // 生成新密码的哈希
        const newPasswordHash = bcrypt.hashSync(newPassword, 10);

        // 更新密码
        await userCollection.doc(user._id).update({
            passwordHash: newPasswordHash,
            updateTime: now()
        });

        console.log('密码修改成功！');
        return ok('密码修改成功');

    } catch (error) {
        console.error('修改密码处理错误:', error);
        return fail('数据库操作失败: ' + error.message, -2);
    }
}

// 登录处理函数
async function handleLogin(params, db, bcrypt) {
    console.log('=== 处理登录请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { username, password } = params
    
    // 参数校验
    if (!username || !password) {
        return fail('用户名和密码不能为空')
    }
    
    // 用户名格式校验
    const usernameRegex = /^[a-zA-Z0-9_]{3,18}$/
    if (!usernameRegex.test(username)) {
        return fail('用户名格式错误')
    }
    
    // 密码长度校验
    if (password.length < 6 || password.length > 20) {
        return fail('密码长度错误')
    }
    
    try {
        const userCollection = db.collection('user')
        const queryResult = await userCollection.where({ username }).get()
        
        if (queryResult.data.length === 0) {
            return fail('用户名或密码错误')
        }
        
        const user = queryResult.data[0]
        
        // 验证密码
        const isPasswordValid = bcrypt.compareSync(password, user.passwordHash)
        if (!isPasswordValid) {
            return fail('用户名或密码错误')
        }
        
        console.log('密码验证成功！')
        
        return ok('登录成功', {
            userId: user.userId,
            username: user.username,
            role: user.role,
            classId: user.classId
        })
        
    } catch (error) {
        console.error('登录处理错误:', error);
        return fail('数据库操作失败: ' + error.message, -2)
    }
}

// 注册处理函数
async function handleRegister(params, db, bcrypt) {
    console.log('=== 处理注册请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { username, password, role, classId } = params
    
    // 参数校验
    if (!username || !password || role === undefined || classId === undefined) {
        return fail('用户名、密码、用户类型和班级不能为空')
    }
    
    // 用户名格式校验
    const usernameRegex = /^[a-zA-Z0-9_]{3,18}$/
    if (!usernameRegex.test(username)) {
        return fail('用户名格式错误：必须是3-18位的英文字母、数字或下划线')
    }
    
    // 密码长度校验
    if (password.length < 6 || password.length > 20) {
        return fail('密码长度必须在6-20位之间')
    }
    
    // 用户类型校验
    if (role !== ROLE.STUDENT && role !== ROLE.TEACHER) {
        return fail('用户类型错误：0-学生，1-教师')
    }
    
    // 教师必须班级为0
    if (role === ROLE.TEACHER && classId !== 0) {
        return fail('教师用户的班级必须为0')
    }
    
    // 学生班级必须大于0
    if (role === ROLE.STUDENT && classId <= 0) {
        return fail('学生必须选择有效的班级（班级ID需大于0）')
    }
    
    try {
        // 检查用户名是否已存在
        const userCollection = db.collection('user')
        const existResult = await userCollection.where({ username }).get()
        
        if (existResult.data.length > 0) {
            return fail('用户名已存在，请更换')
        }
        
        // 密码加密
        console.log('开始密码加密')
        const saltRounds = 10
        const hashedPassword = bcrypt.hashSync(password, saltRounds)
        
        // 生成用户ID
        const userId = Date.now()
        
        // 构建用户数据
        const userData = {
            userId: userId,
            username: username,
            passwordHash: hashedPassword,  // 使用passwordHash字段名
            role: role,
            classId: classId,
            status: 'active',  // 新增状态字段
            createTime: now(),
            updateTime: now()
        }
        
        console.log('准备插入的用户数据:', JSON.stringify(userData, null, 2))
        
        // 插入数据库
        const addResult = await userCollection.add(userData)
        console.log('用户插入成功，文档ID:', addResult.id)
        
        return ok('注册成功', {
            userId: userId,
            username: username,
            role: role,
            classId: classId
        })
        
    } catch (error) {
        console.error('数据库插入失败:', error.message)
        return fail('数据库操作失败', -2)
    }
}

// 获取学生信息
async function handleGetStudentsInfo(params, db) {
    console.log('=== 处理获取学生信息请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { userIds } = params
    
    if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
        return fail('用户ID列表不能为空')
    }
    
    try {
        const userCollection = db.collection('user')
        const usersResult = await userCollection.where({
            userId: db.command.in(userIds.map(id => parseInt(id))),
            role: ROLE.STUDENT
        }).get()
        
        const students = usersResult.data || []
        const resultData = students.map(student => ({
            userId: student.userId,
            username: student.username,
            classId: student.classId
        }))
        
        return ok('获取成功', resultData)
        
    } catch (error) {
        console.error('获取学生信息错误:', error)
        return fail('获取失败: ' + error.message, -2)
    }
}

// 获取班级所有学生
async function handleGetClassStudents(params, db) {
    console.log('=== 处理获取班级学生请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { classId } = params
    
    if (!classId) return fail('班级ID不能为空')
    
    try {
        const userCollection = db.collection('user')
        const studentsResult = await userCollection.where({
            classId: parseInt(classId),
            role: ROLE.STUDENT
        }).get()
        
        const students = studentsResult.data || []
        const resultData = students.map(student => ({
            userId: student.userId,
            username: student.username,
            classId: student.classId
        }))
        
        return ok('获取成功', resultData)
        
    } catch (error) {
        console.error('获取班级学生错误:', error)
        return fail('获取失败: ' + error.message, -2)
    }
}

// ==================== 班级服务层 ====================

// 获取班级信息
async function handleGetClasses(params, db) {
    console.log('=== 处理获取班级信息请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { teacherId } = params
    
    if (!teacherId) return fail('教师ID不能为空')
    
    try {
        const classCollection = db.collection('class')
        const queryResult = await classCollection.where({
            teacherId: parseInt(teacherId)
        }).get()
        
        // 增强返回数据，包含studentCount
        const classes = queryResult.data.map(c => ({
            classId: c.classId,
            className: c.className,
            studentCount: c.studentCount || 0
        }))
        
        return ok('获取班级成功', classes)
        
    } catch (error) {
        console.error('获取班级处理错误:', error)
        return fail('数据库操作失败: ' + error.message, -2)
    }
}

// 新建班级
async function handleCreateClass(params, db) {
    console.log('=== 处理新建班级请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { teacherId, className } = params
    
    if (!teacherId) return fail('教师ID不能为空')
    if (!className || className.trim() === '') return fail('班级名称不能为空')
    
    try {
        // 自动生成班级ID
        const classId = parseInt(Date.now() % 1000000)
        
        // 获取教师名称（如果存在）
        let teacherName = ''
        try {
            const userCollection = db.collection('user')
            const teacherResult = await userCollection.where({
                userId: parseInt(teacherId),
                role: ROLE.TEACHER
            }).get()
            if (teacherResult.data.length > 0) {
                teacherName = teacherResult.data[0].username
            }
        } catch (e) {
            console.warn('获取教师名称失败:', e.message)
        }
        
        // 构建班级数据
        const classData = {
            classId: classId,
            className: className.trim(),
            teacherId: parseInt(teacherId),
            teacherName: teacherName,  // 新增教师名称
            studentCount: 0,  // 新增学生数量
            createTime: now(),
            updateTime: now()
        }
        
        console.log('准备插入的班级数据:', JSON.stringify(classData, null, 2))
        
        const classCollection = db.collection('class')
        const addResult = await classCollection.add(classData)
        console.log('班级插入成功，文档ID:', addResult.id)
        
        return ok('班级创建成功', {
            classId: classId,
            className: className.trim(),
            teacherId: parseInt(teacherId)
        })
        
    } catch (error) {
        console.error('新建班级处理错误:', error)
        return fail('数据库操作失败: ' + error.message, -2)
    }
}

// ==================== 题目服务层 ====================

// 习题同步
async function handleQuestionSync(params, db) {
    console.log('=== 处理习题同步请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { maxQuestionNumber = 0, page = 1, pageSize = 100 } = params
    
    console.log('同步请求参数: maxQuestionNumber =', maxQuestionNumber)
    console.log('分页参数: page =', page, ', pageSize =', pageSize)
    
    try {
        const questionCollection = db.collection('question')
        
        // 并行查询新增/修改题目和删除题目
        const [newQuestionsResult, deletedQuestionsResult] = await Promise.all([
            questionCollection.where({
                qId: db.command.gt(maxQuestionNumber),
                isDeleted: db.command.neq(true)  // 过滤已删除题目
            }).orderBy('qId', 'asc').skip((page - 1) * pageSize).limit(pageSize).get(),
            
            questionCollection.where({
                isDeleted: true
            }).limit(pageSize).get()
        ])
        
        const newQuestions = newQuestionsResult.data || []
        const deletedQuestions = deletedQuestionsResult.data || []
        const hasMore = newQuestions.length === pageSize
        
        // 转换数据格式
        const formattedNewQuestions = newQuestions.map(q => ({
            qId: q.qId,
            content: q.content || '',
            answer: q.answer || '',
            sectionId: q.sectionId || 0,
            imageUrl: q.imageUrl || ''
        }))
        
        const deletedQuestionNumbers = deletedQuestions.map(q => q.qId)
        
        return ok('同步成功', {
            newQuestions: formattedNewQuestions,
            deletedQuestionNumbers: deletedQuestionNumbers,
            hasMore: hasMore,
            currentPage: page,
            pageSize: pageSize
        })
        
    } catch (error) {
        console.error('习题同步处理错误:', error)
        return fail('数据库操作失败: ' + error.message, -2)
    }
}

// 上传题目
async function handleUploadQuestion(params, db) {
    console.log('=== 处理上传题目请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { userId, content, answer, sectionId, imageUrl = '' } = params
    
    if (!userId) return fail('用户ID不能为空')
    if (!content || content.trim() === '') return fail('题目内容不能为空')
    if (!answer || answer.trim() === '') return fail('答案不能为空')
    if (sectionId === undefined || sectionId === null) return fail('节号不能为空')
    
    try {
        // 验证用户类型是否为教师
        const userCollection = db.collection('user')
        const userResult = await userCollection.where({ userId: parseInt(userId) }).get()
        
        if (userResult.data.length === 0) return fail('用户不存在')
        
        const user = userResult.data[0]
        if (user.role !== ROLE.TEACHER) return fail('只有教师才能上传题目')
        
        // 查询当前最大题号
        const questionCollection = db.collection('question')
        const maxQResult = await questionCollection.orderBy('qId', 'desc').limit(1).get()
        
        let newQId = 1
        if (maxQResult.data.length > 0) {
            newQId = (maxQResult.data[0].qId || 0) + 1
        }
        
        // 构建题目数据
        const questionData = {
            qId: newQId,
            content: content.trim(),
            answer: answer.trim(),
            sectionId: parseInt(sectionId),
            imageUrl: imageUrl,
            isDeleted: false,  // 新增删除标记
            deletedAt: null,
            deletedBy: null,
            createTime: now(),
            updateTime: now()
        }
        
        const addResult = await questionCollection.add(questionData)
        console.log('题目插入成功，文档ID:', addResult.id)
        
        return ok('题目上传成功', {
            qId: newQId,
            content: content.trim(),
            answer: answer.trim(),
            sectionId: parseInt(sectionId),
            imageUrl: imageUrl
        })
        
    } catch (error) {
        console.error('上传题目处理错误:', error)
        return fail('数据库操作失败: ' + error.message, -2)
    }
}

// 删除题目（改用isDeleted标记）
async function handleDeleteQuestion(params, db) {
    console.log('=== 处理删除题目请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { userId, qId } = params
    
    if (!userId) return fail('用户ID不能为空')
    if (qId === undefined || qId === null) return fail('题号不能为空')
    
    try {
        // 验证用户类型是否为教师
        const userCollection = db.collection('user')
        const userResult = await userCollection.where({ userId: parseInt(userId) }).get()
        
        if (userResult.data.length === 0) return fail('用户不存在')
        
        const user = userResult.data[0]
        if (user.role !== ROLE.TEACHER) return fail('只有教师才能删除题目')
        
        // 查询题目是否存在
        const questionCollection = db.collection('question')
        const questionResult = await questionCollection.where({ qId: parseInt(qId) }).get()
        
        if (questionResult.data.length === 0) return fail('题目不存在')
        
        // 使用isDeleted标记删除
        const questionDoc = questionResult.data[0]
        await questionCollection.doc(questionDoc._id).update({
            isDeleted: true,
            deletedAt: now(),
            deletedBy: parseInt(userId),
            updateTime: now()
        })
        
        console.log('题目删除成功，qId:', qId)
        
        return ok('题目删除成功', { qId: parseInt(qId) })
        
    } catch (error) {
        console.error('删除题目处理错误:', error)
        return fail('数据库操作失败: ' + error.message, -2)
    }
}

// ==================== 答题服务层（核心优化） ====================

// 提交学生答案（增强版 - 补充冗余字段）
async function handleSubmitAnswer(params, db) {
    console.log('=== 处理提交学生答案请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { userId, qId, studentAnswer } = params
    
    if (!userId) return fail('用户ID不能为空')
    if (qId === undefined || qId === null) return fail('题号不能为空')
    if (!studentAnswer || studentAnswer.trim() === '') return fail('答案不能为空')
    
    try {
        const answerCollection = db.collection('answer')
        const questionCollection = db.collection('question')
        const userCollection = db.collection('user')
        const classCollection = db.collection('class')
        
        const parsedUserId = parseInt(userId)
        const parsedQId = parseInt(qId)
        
        // 查询现有答案
        const existingResult = await answerCollection.where({
            userId: parsedUserId,
            qId: parsedQId
        }).get()
        
        const currentTime = now()
        
        // 如果已批改，不允许修改
        if (existingResult.data.length > 0) {
            const existingAnswer = existingResult.data[0]
            if (existingAnswer.status === ANSWER_STATUS.CORRECT || existingAnswer.status === ANSWER_STATUS.WRONG) {
                return fail('答案已被批改，无法修改')
            }
            
            // 更新答案
            await answerCollection.doc(existingAnswer._id).update({
                studentAnswer: studentAnswer.trim(),
                updateTime: currentTime
            })
            
            return ok('答案更新成功', {
                qId: parsedQId,
                studentAnswer: studentAnswer.trim(),
                status: ANSWER_STATUS.NOT_CHECKED,
                isNew: false
            })
        }
        
        // 补充冗余字段 - 优化answer集合
        let enrichedData = {
            userId: parsedUserId,
            qId: parsedQId,
            studentAnswer: studentAnswer.trim(),
            status: ANSWER_STATUS.NOT_CHECKED,
            createTime: currentTime,
            updateTime: currentTime
        }
        
        // 查询学生信息
        try {
            const userResult = await userCollection.where({ userId: parsedUserId }).get()
            if (userResult.data.length > 0) {
                const student = userResult.data[0]
                enrichedData.studentName = student.username
                enrichedData.classId = student.classId
                
                // 查询班级信息
                if (student.classId) {
                    const classResult = await classCollection.where({ classId: student.classId }).get()
                    if (classResult.data.length > 0) {
                        enrichedData.className = classResult.data[0].className
                        enrichedData.teacherId = classResult.data[0].teacherId
                    }
                }
            }
        } catch (e) {
            console.warn('补充学生/班级信息失败:', e.message)
        }
        
        // 查询题目信息
        try {
            const questionResult = await questionCollection.where({ qId: parsedQId }).get()
            if (questionResult.data.length > 0) {
                const question = questionResult.data[0]
                enrichedData.questionBrief = question.content ? question.content.substring(0, 50) : ''
                enrichedData.sectionId = question.sectionId
                enrichedData.correctAnswer = question.answer
            }
        } catch (e) {
            console.warn('补充题目信息失败:', e.message)
        }
        
        console.log('准备插入的答案数据:', JSON.stringify(enrichedData, null, 2))
        
        const addResult = await answerCollection.add(enrichedData)
        console.log('答案插入成功，文档ID:', addResult.id)
        
        return ok('答案提交成功', {
            qId: parsedQId,
            studentAnswer: studentAnswer.trim(),
            status: ANSWER_STATUS.NOT_CHECKED,
            isNew: true
        })
        
    } catch (error) {
        console.error('提交答案处理错误:', error)
        return fail('数据库操作失败: ' + error.message, -2)
    }
}

// 查询学生答案
async function handleGetStudentAnswer(params, db) {
    console.log('=== 处理查询学生答案请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { userId, qId } = params
    
    if (!userId) return fail('用户ID不能为空')
    if (qId === undefined || qId === null) return fail('题号不能为空')
    
    try {
        const answerCollection = db.collection('answer')
        const queryResult = await answerCollection.where({
            userId: parseInt(userId),
            qId: parseInt(qId)
        }).get()
        
        if (queryResult.data.length === 0) {
            return ok('未查询到答案', null)
        }
        
        const answerData = queryResult.data[0]
        
        return ok('查询成功', {
            qId: answerData.qId,
            studentAnswer: answerData.studentAnswer,
            status: answerData.status,
            teacherMsg: answerData.teacherMsg || null,
            updateTime: answerData.updateTime
        })
        
    } catch (error) {
        console.error('查询答案处理错误:', error)
        return fail('数据库操作失败: ' + error.message, -2)
    }
}

// 批量查询学生答案
async function handleGetBatchStudentAnswers(params, db) {
    console.log('=== 处理批量查询学生答案请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { userId, qIds } = params
    
    if (!userId) return fail('用户ID不能为空')
    if (!Array.isArray(qIds) || qIds.length === 0) {
        return ok('无题目ID', [])
    }
    
    try {
        const answerCollection = db.collection('answer')
        const queryResult = await answerCollection.where({
            userId: parseInt(userId),
            qId: db.command.in(qIds.map(id => parseInt(id)))
        }).get()
        
        const answersMap = {}
        queryResult.data.forEach(answerData => {
            answersMap[answerData.qId] = {
                qId: answerData.qId,
                studentAnswer: answerData.studentAnswer,
                status: answerData.status,
                teacherMsg: answerData.teacherMsg || null,
                updateTime: answerData.updateTime
            }
        })
        
        return ok('查询成功', answersMap)
        
    } catch (error) {
        console.error('批量查询答案处理错误:', error)
        return fail('数据库操作失败: ' + error.message, -2)
    }
}

// 获取学生所有答题记录
async function handleGetStudentAllAnswers(params, db) {
    console.log('=== 处理获取学生所有答题记录请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { userId } = params
    
    if (!userId) return fail('用户ID不能为空')
    
    try {
        const answerCollection = db.collection('answer')
        const queryResult = await answerCollection.where({
            userId: parseInt(userId)
        }).get()
        
        const answersMap = {}
        queryResult.data.forEach(answerData => {
            answersMap[answerData.qId] = {
                qId: answerData.qId,
                studentAnswer: answerData.studentAnswer,
                status: answerData.status,
                teacherMsg: answerData.teacherMsg || null,
                updateTime: answerData.updateTime
            }
        })
        
        return ok('查询成功', answersMap)
        
    } catch (error) {
        console.error('获取学生所有答题记录处理错误:', error)
        return fail('数据库操作失败: ' + error.message, -2)
    }
}

// 提交批改结果
async function handleSubmitGrade(params, db) {
    console.log('=== 处理提交批改结果请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { teacherId, userId, qId, status, teacherMsg = '' } = params
    
    if (!teacherId) return fail('教师ID不能为空')
    if (!userId) return fail('学生ID不能为空')
    if (qId === undefined || qId === null) return fail('题号不能为空')
    if (status === undefined || status === null || (status !== 1 && status !== 2)) {
        return fail('批改状态错误：1为正确，2为错误')
    }
    
    try {
        const answerCollection = db.collection('answer')
        const parsedTeacherId = parseInt(teacherId)
        const parsedUserId = parseInt(userId)
        const parsedQId = parseInt(qId)
        const parsedStatus = parseInt(status)
        const currentTime = now()
        
        // 查询该学生的答案记录
        const existingResult = await answerCollection.where({
            userId: parsedUserId,
            qId: parsedQId
        }).get()
        
        if (existingResult.data.length > 0) {
            // 更新批改状态
            const answerDoc = existingResult.data[0]
            await answerCollection.doc(answerDoc._id).update({
                status: parsedStatus,
                teacherMsg: teacherMsg,
                updateTime: currentTime
            })
            
            console.log('批改更新成功，文档ID:', answerDoc._id)
            
            return ok('批改更新成功', {
                userId: parsedUserId,
                qId: parsedQId,
                status: parsedStatus,
                teacherMsg: teacherMsg
            })
        } else {
            // 创建新记录
            await answerCollection.add({
                userId: parsedUserId,
                qId: parsedQId,
                studentAnswer: '',
                status: parsedStatus,
                teacherMsg: teacherMsg,
                createTime: currentTime,
                updateTime: currentTime
            })
            
            console.log('批改记录创建成功')
            
            return ok('批改提交成功', {
                userId: parsedUserId,
                qId: parsedQId,
                status: parsedStatus,
                teacherMsg: teacherMsg
            })
        }
        
    } catch (error) {
        console.error('提交批改错误:', error)
        return fail('提交失败: ' + error.message, -2)
    }
}

// 获取班级题目答题统计（优化版 - 直接查询answer）
async function handleGetClassQuestionStats(params, db) {
    console.log('=== 处理获取班级题目答题统计请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { classId, qId } = params
    
    if (!classId) return fail('班级ID不能为空')
    if (qId === undefined || qId === null) return fail('题号不能为空')
    
    try {
        const parsedClassId = parseInt(classId)
        const parsedQId = parseInt(qId)
        
        // 直接从answer查询该班级的答题情况
        const answerCollection = db.collection('answer')
        const answersResult = await answerCollection.where({
            classId: parsedClassId,
            qId: parsedQId
        }).get()
        
        const classAnswers = answersResult.data || []
        
        // 从class表获取班级总人数
        let totalStudents = 0
        try {
            const classCollection = db.collection('class')
            const classResult = await classCollection.where({ classId: parsedClassId }).get()
            if (classResult.data.length > 0) {
                totalStudents = classResult.data[0].studentCount || 0
            }
        } catch (e) {
            // 如果没有studentCount，从user表统计
            const userCollection = db.collection('user')
            const studentsResult = await userCollection.where({
                classId: parsedClassId,
                role: ROLE.STUDENT
            }).get()
            totalStudents = studentsResult.data.length
        }
        
        console.log('班级总人数:', totalStudents)
        
        // 统计各状态人数
        let notCheckedCount = 0
        let wrongCount = 0
        let correctCount = 0
        
        classAnswers.forEach(answer => {
            switch (answer.status) {
                case ANSWER_STATUS.NOT_CHECKED:
                    notCheckedCount++
                    break
                case ANSWER_STATUS.CORRECT:
                    correctCount++
                    break
                case ANSWER_STATUS.WRONG:
                    wrongCount++
                    break
            }
        })
        
        const submittedCount = notCheckedCount + wrongCount + correctCount
        const notDoneCount = totalStudents - submittedCount
        
        console.log('答题统计结果:')
        console.log('- 总人数:', totalStudents)
        console.log('- 已提交:', submittedCount)
        console.log('- 未完成:', notDoneCount)
        console.log('- 未批改:', notCheckedCount)
        console.log('- 错误:', wrongCount)
        console.log('- 正确:', correctCount)
        
        return ok('获取统计成功', {
            classId: parsedClassId,
            qId: parsedQId,
            totalStudents: totalStudents,
            notDoneCount: notDoneCount,
            notCheckedCount: notCheckedCount,
            wrongCount: wrongCount,
            correctCount: correctCount
        })
        
    } catch (error) {
        console.error('获取班级答题统计错误:', error)
        return fail('获取统计失败: ' + error.message, -2)
    }
}

// 获取教师管理班级学生的未批改题目（优化版 - 直接查询answer）
async function handleGetTeacherUngradedQuestions(params, db) {
    console.log('=== 处理获取教师管理班级学生未批改题目请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { teacherId } = params
    
    if (!teacherId) return fail('教师ID不能为空')
    
    try {
        const parsedTeacherId = parseInt(teacherId)
        
        // 直接从answer表查询该教师的未批改答案
        const answerCollection = db.collection('answer')
        const ungradedResult = await answerCollection.where({
            teacherId: parsedTeacherId,
            status: ANSWER_STATUS.NOT_CHECKED
        }).get()
        
        const ungradedAnswers = ungradedResult.data || []
        console.log('未批改答案数量:', ungradedAnswers.length)
        
        // 构建返回数据（利用冗余字段）
        const resultData = ungradedAnswers.map(answer => ({
            qId: answer.qId,
            userId: answer.userId,
            studentName: answer.studentName || '',
            className: answer.className || '',
            questionBrief: answer.questionBrief || ''
        }))
        
        return ok('获取成功', resultData)
        
    } catch (error) {
        console.error('获取未批改题目错误:', error)
        return fail('获取失败: ' + error.message, -2)
    }
}

// 获取教师管理班级学生所有有答题记录的题目（优化版）
async function handleGetTeacherAllAnsweredQuestions(params, db) {
    console.log('=== 处理获取教师管理班级学生所有有答题记录题目请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { teacherId } = params
    
    if (!teacherId) return fail('教师ID不能为空')
    
    try {
        const parsedTeacherId = parseInt(teacherId)
        
        // 直接从answer表查询
        const answerCollection = db.collection('answer')
        const allAnswersResult = await answerCollection.where({
            teacherId: parsedTeacherId
        }).get()
        
        const allAnswers = allAnswersResult.data || []
        console.log('所有答题记录数量:', allAnswers.length)
        
        // 构建返回数据
        const resultData = allAnswers.map(answer => ({
            qId: answer.qId,
            userId: answer.userId,
            studentName: answer.studentName || '',
            className: answer.className || '',
            status: answer.status
        }))
        
        return ok('获取成功', resultData)
        
    } catch (error) {
        console.error('获取所有答题记录题目错误:', error)
        return fail('获取失败: ' + error.message, -2)
    }
}

// 获取学生错题列表（优化版 - 利用冗余字段）
async function handleGetStudentErrorQuestions(params, db) {
    console.log('=== 处理获取学生错题列表请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { userId } = params
    
    if (!userId) return fail('学生ID不能为空')
    
    try {
        const parsedUserId = parseInt(userId)
        
        // 直接从answer表查询错误状态
        const answerCollection = db.collection('answer')
        const answersResult = await answerCollection.where({
            userId: parsedUserId,
            status: ANSWER_STATUS.WRONG
        }).get()
        
        const errorAnswers = answersResult.data || []
        console.log('错误答题记录数量:', errorAnswers.length)
        
        if (errorAnswers.length === 0) {
            return ok('暂无错题', [])
        }
        
        // 利用冗余字段构建返回数据（无需二次查询question表）
        const resultData = errorAnswers.map(answer => ({
            qId: answer.qId,
            contentPreview: answer.questionBrief || '',
            studentAnswer: answer.studentAnswer || '',
            correctAnswer: answer.correctAnswer || '',
            teacherMsg: answer.teacherMsg || null,
            sectionId: answer.sectionId || 0,
            status: 0
        }))
        
        resultData.sort((a, b) => a.qId - b.qId)
        
        return ok('获取成功', resultData)
        
    } catch (error) {
        console.error('获取学生错题错误:', error)
        return fail('获取失败: ' + error.message, -2)
    }
}

// ==================== 资源服务层 ====================

// 获取教师上传的资源列表
async function handleGetTeacherResources(params, db) {
    console.log('=== 处理获取教师资源列表请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { teacherId } = params
    
    if (!teacherId) return fail('教师ID不能为空')
    
    try {
        const resourceCollection = db.collection('resource')
        const result = await resourceCollection.where({
            teacherId: parseInt(teacherId)
        }).orderBy('createTime', 'desc').get()
        
        const resources = result.data || []
        
        // 增强返回数据
        const resultData = resources.map(r => ({
            resId: r._id,
            resName: r.name || r.resName || '',
            url: r.url,
            teacherId: r.teacherId,
            teacherName: r.teacherName || '',
            type: r.type || '',
            tags: r.tags || [],
            size: r.size || 0,
            createTime: r.createTime
        }))
        
        return ok('获取成功', resultData)
        
    } catch (error) {
        console.error('获取教师资源错误:', error)
        return fail('获取失败: ' + error.message, -2)
    }
}

// 获取所有资源列表
async function handleGetAllResources(params, db) {
    console.log('=== 处理获取所有资源列表请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    try {
        const resourceCollection = db.collection('resource')
        const result = await resourceCollection.orderBy('createTime', 'desc').get()
        
        const resources = result.data || []
        
        const resultData = resources.map(r => ({
            resId: r._id,
            resName: r.name || r.resName || '',
            url: r.url,
            teacherId: r.teacherId,
            teacherName: r.teacherName || '',
            type: r.type || '',
            tags: r.tags || [],
            size: r.size || 0,
            createTime: r.createTime
        }))
        
        return ok('获取成功', resultData)
        
    } catch (error) {
        console.error('获取所有资源错误:', error)
        return fail('获取失败: ' + error.message, -2)
    }
}

// 上传资源
async function handleUploadResource(params) {
    console.log('=== 处理上传资源请求 ===');
    
    const { teacherId, resName, fileName, fileData, type, tags, classIds } = params
    
    if (!teacherId) return fail('教师ID不能为空')
    if (!resName) return fail('资源名称不能为空')
    if (!fileName) return fail('文件名不能为空')
    if (!fileData) return fail('文件数据不能为空')
    
    try {
        // 生成云存储路径
        const timestamp = Date.now()
        const cloudPath = `resources/${timestamp}_${fileName}`
        
        // 将 Base64 转为 Buffer
        let buffer
        if (fileData.startsWith('data:')) {
            const base64Data = fileData.split(',')[1]
            buffer = Buffer.from(base64Data, 'base64')
        } else {
            buffer = Buffer.from(fileData, 'base64')
        }
        
        console.log('文件数据大小:', buffer.length, 'bytes')
        
        // 上传到云存储
        const uploadResult = await uniCloud.uploadFile({
            cloudPath: cloudPath,
            fileContent: buffer
        })
        
        console.log('文件上传成功, fileID:', uploadResult.fileID)
        
        // 获取教师名称
        let teacherName = ''
        try {
            const userCollection = db.collection('user')
            const teacherResult = await userCollection.where({
                userId: parseInt(teacherId),
                role: ROLE.TEACHER
            }).get()
            if (teacherResult.data.length > 0) {
                teacherName = teacherResult.data[0].username
            }
        } catch (e) {
            console.warn('获取教师名称失败:', e.message)
        }
        
        // 保存到数据库
        if (db) {
            const resourceCollection = db.collection('resource')
            const resourceData = {
                name: resName,
                url: uploadResult.fileID,
                teacherId: parseInt(teacherId),
                teacherName: teacherName,
                type: type || '',
                tags: tags || [],
                classIds: classIds || [],
                size: buffer.length,
                createTime: now(),
                updateTime: now()
            }
            
            const addResult = await resourceCollection.add(resourceData)
            console.log('资源保存成功, _id:', addResult.id)
            
            return ok('上传成功', {
                resId: addResult.id,
                resName: resName,
                url: uploadResult.fileID,
                teacherId: parseInt(teacherId),
                createTime: now()
            })
        } else {
            return ok('上传成功（未保存到数据库）', {
                resId: '',
                resName: resName,
                url: uploadResult.fileID,
                teacherId: parseInt(teacherId),
                createTime: now()
            })
        }
        
    } catch (error) {
        console.error('上传资源处理错误:', error)
        return fail('上传失败: ' + error.message, -2)
    }
}

// 删除资源
async function handleDeleteResource(params, db) {
    console.log('=== 处理删除资源请求 ===');
    
    if (!db) return fail('数据库连接失败', -3)
    
    const { resId, teacherId } = params
    
    if (!resId) return fail('资源ID不能为空')
    if (!teacherId) return fail('教师ID不能为空')
    
    try {
        const resourceCollection = db.collection('resource')
        const existingResult = await resourceCollection.doc(resId).get()
        
        if (!existingResult.data || existingResult.data.length === 0) {
            return fail('资源不存在')
        }
        
        const resource = existingResult.data[0]
        
        if (resource.teacherId !== parseInt(teacherId)) {
            return fail('无权删除该资源')
        }
        
        // 删除云存储文件
        if (resource.url) {
            try {
                await uniCloud.deleteFile({
                    fileList: [resource.url]
                })
                console.log('云存储文件删除成功')
            } catch (e) {
                console.warn('云存储文件删除失败:', e.message)
            }
        }
        
        // 删除数据库记录
        await resourceCollection.doc(resId).remove()
        console.log('资源删除成功')
        
        return ok('删除成功', null)
        
    } catch (error) {
        console.error('删除资源错误:', error)
        return fail('删除失败: ' + error.message, -2)
    }
}

// ==================== 文件服务层 ====================

// 获取图片上传参数
async function handleGetUploadParams(params) {
    console.log('=== 处理获取上传参数请求 ===');
    
    const { fileName } = params
    
    const timestamp = Date.now()
    const cloudPath = `question-images/${timestamp}_${fileName || 'image.jpg'}`
    
    return ok('获取上传参数成功', {
        cloudPath: cloudPath,
        fileType: 'image'
    })
}

// 上传图片到云存储
async function handleUploadImage(params) {
    console.log('=== 处理上传图片请求 ===');
    
    const { fileName, imageData } = params
    
    if (!fileName) return fail('文件名不能为空')
    if (!imageData) return fail('图片数据不能为空')
    
    try {
        const timestamp = Date.now()
        const cloudPath = `question-images/${timestamp}_${fileName}`
        
        let buffer
        if (imageData.startsWith('data:image')) {
            const base64Data = imageData.split(',')[1]
            buffer = Buffer.from(base64Data, 'base64')
        } else {
            buffer = Buffer.from(imageData, 'base64')
        }
        
        console.log('图片数据大小:', buffer.length, 'bytes')
        
        const uploadResult = await uniCloud.uploadFile({
            cloudPath: cloudPath,
            fileContent: buffer
        })
        
        console.log('图片上传成功, fileID:', uploadResult.fileID)
        
        return ok('图片上传成功', {
            imageUrl: uploadResult.fileID,
            cloudPath: cloudPath
        })
        
    } catch (error) {
        console.error('上传图片处理错误:', error)
        return fail('上传图片失败: ' + error.message, -2)
    }
}

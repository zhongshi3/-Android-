'use strict';

console.log('=== 开始导入依赖 ===');

// 引入uniCloud数据库和bcrypt加密
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
            return password; // 模拟：直接返回密码（不安全！）
        },
        compareSync: (password, hashedPassword) => {
            console.warn('使用模拟bcrypt.compareSync，生产环境不安全！');
            return password === hashedPassword; // 模拟：直接比较字符串（不安全！）
        }
    };
}

// 云函数入口函数 - 统一用户管理云函数（支持注册和登录）
exports.main = async (event, context) => {
    console.log('=== 用户管理云函数开始执行 ===');
    console.log('完整event对象:', JSON.stringify(event, null, 2));
    
    try {
        // 1. 获取参数 - 从event.body中解析JSON
        let action, username, password, role, classId;
        
        console.log('检查event.body是否存在:', !!event.body);
        console.log('event.body类型:', typeof event.body);
        
        if (event.body) {
            // 如果event中有body字段，说明是HTTP触发
            console.log('开始解析event.body');
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            console.log('解析后的body:', JSON.stringify(body, null, 2));
            
            action = body.action || 'register'; // 默认操作为注册
            username = body.username;
            password = body.password;
            role = body.role;
            classId = body.classId;
        } else {
            // 直接调用的情况
            console.log('使用直接调用参数');
            action = event.action || 'register';
            username = event.username;
            password = event.password;
            role = event.role;
            classId = event.classId;
        }
        
        console.log('云函数请求参数:', { action, username, password, role, classId });
        
        // 根据action参数路由到不同的处理逻辑
        if (action === 'login') {
            return await handleLogin(username, password, db, bcrypt);
        } else if (action === 'register') {
            return await handleRegister(username, password, role, classId, db, bcrypt);
        } else if (action === 'syncQuestions') {
            return await handleQuestionSync(event, db);
        } else if (action === 'getClasses') {
            return await handleGetClasses(event, db);
        } else if (action === 'createClass') {
            return await handleCreateClass(event, db);
        } else if (action === 'submitAnswer') {
            return await handleSubmitAnswer(event, db);
        } else if (action === 'getStudentAnswer') {
            return await handleGetStudentAnswer(event, db);
        } else if (action === 'getBatchStudentAnswers') {
            return await handleGetBatchStudentAnswers(event, db);
        } else if (action === 'deleteQuestion') {
            return await handleDeleteQuestion(event, db);
        } else if (action === 'uploadQuestion') {
            return await handleUploadQuestion(event, db);
        } else if (action === 'getUploadParams') {
            return await handleGetUploadParams(event);
        } else if (action === 'uploadImage') {
            return await handleUploadImage(event);
        } else if (action === 'getClassQuestionStats') {
            return await handleGetClassQuestionStats(event, db);
        } else if (action === 'getTeacherUngradedQuestions') {
            return await handleGetTeacherUngradedQuestions(event, db);
        } else if (action === 'getStudentsInfo') {
            return await handleGetStudentsInfo(event, db);
        } else if (action === 'submitGrade') {
            return await handleSubmitGrade(event, db);
        } else if (action === 'getStudentErrorQuestions') {
            return await handleGetStudentErrorQuestions(event, db);
        } else if (action === 'getTeacherAllAnsweredQuestions') {
            return await handleGetTeacherAllAnsweredQuestions(event, db);
        } else if (action === 'getStudentAllAnswers') {
            return await handleGetStudentAllAnswers(event, db);
        } else if (action === 'getClassStudents') {
            return await handleGetClassStudents(event, db);
        } else if (action === 'getTeacherResources') {
            return await handleGetTeacherResources(event, db);
        } else if (action === 'getAllResources') {
            return await handleGetAllResources(event, db);
        } else if (action === 'uploadResource') {
            return await handleUploadResource(event);
        } else if (action === 'deleteResource') {
            return await handleDeleteResource(event, db);
        } else {
            return {
                code: 0,
                msg: '未知的操作类型，支持: register, login, syncQuestions, getClasses, createClass, submitAnswer, getStudentAnswer, deleteQuestion, uploadQuestion, getClassQuestionStats, getTeacherUngradedQuestions, getStudentsInfo, submitGrade, getStudentErrorQuestions, getClassStudents, getTeacherResources, getAllResources, uploadResource, deleteResource',
                data: null
            };
        }
        
    } catch (error) {
        console.error('用户管理云函数错误:', error);
        console.error('错误堆栈:', error.stack);
        
        return {
            code: -1,
            msg: '服务器内部错误: ' + error.message,
            data: null
        };
    }
};

// 登录处理函数
async function handleLogin(username, password, db, bcrypt) {
    console.log('=== 处理登录请求 ===');
    
    // 检查db是否已初始化
    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }
    
    // 参数校验
    if (!username || !password) {
        console.error('参数校验失败: 用户名或密码为空');
        return {
            code: 0,
            msg: '用户名和密码不能为空',
            data: null
        };
    }
    
    // 用户名格式校验
    const usernameRegex = /^[a-zA-Z0-9_]{3,18}$/;
    if (!usernameRegex.test(username)) {
        console.error('用户名格式错误:', username);
        return {
            code: 0,
            msg: '用户名格式错误',
            data: null
        };
    }
    
    // 密码长度校验
    if (password.length < 6 || password.length > 20) {
        console.error('密码长度错误:', password.length);
        return {
            code: 0,
            msg: '密码长度错误',
            data: null
        };
    }
    
    try {
        // 查询用户是否存在
        const userCollection = db.collection('user');
        console.log('查询用户:', username);
        
        const queryResult = await userCollection.where({
            username: username
        }).get();
        
        console.log('查询结果数据长度:', queryResult.data.length);
        
        if (queryResult.data.length === 0) {
            console.error('用户不存在:', username);
            return {
                code: 0,
                msg: '用户名或密码错误',
                data: null
            };
        }
        
        const user = queryResult.data[0];
        console.log('找到用户:', {
            userId: user.userId,
            username: user.username,
            role: user.role,
            classId: user.classId,
            hasPassword: !!user.password,
            passwordLength: user.password ? user.password.length : 0
        });
        
        // 验证密码
        console.log('开始验证密码');
        const hashedPassword = user.password;
        
        // 检查bcrypt.compareSync是否可用
        if (typeof bcrypt.compareSync !== 'function') {
            console.error('bcrypt.compareSync不可用，使用简单验证');
            const isPasswordValid = (password === hashedPassword);
            console.log('简单密码验证结果:', isPasswordValid);
            
            if (!isPasswordValid) {
                return {
                    code: 0,
                    msg: '用户名或密码错误',
                    data: null
                };
            }
        } else {
            const isPasswordValid = bcrypt.compareSync(password, hashedPassword);
            console.log('bcrypt密码验证结果:', isPasswordValid);
            
            if (!isPasswordValid) {
                return {
                    code: 0,
                    msg: '用户名或密码错误',
                    data: null
                };
            }
        }
        
        console.log('密码验证成功！');
        
        // 构建登录成功响应
        return {
            code: 1,
            msg: '登录成功',
            data: {
                userId: user.userId,
                username: user.username,
                role: user.role,
                classId: user.classId
            }
        };
        
    } catch (error) {
        console.error('登录处理错误:', error);
        console.error('错误堆栈:', error.stack);
        
        return {
            code: -2,
            msg: '数据库操作失败: ' + error.message,
            data: null
        };
    }
}

// 习题同步处理函数
async function handleQuestionSync(event, db) {
    console.log('=== 处理习题同步请求 ===');
    
    // 检查db是否已初始化
    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }
    
    try {
        // 获取参数 - 从event.body中解析JSON
        let maxQuestionNumber;
        
        if (event.body) {
            // 如果event中有body字段，说明是HTTP触发
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            maxQuestionNumber = body.maxQuestionNumber || 0;
        } else {
            // 直接调用的情况
            maxQuestionNumber = event.maxQuestionNumber || 0;
        }
        
        console.log('同步请求参数: maxQuestionNumber =', maxQuestionNumber);
        
        // 获取分页参数
        let page = 1;
        let pageSize = 100;
        
        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            page = body.page || 1;
            pageSize = body.pageSize || 100;
        }
        
        console.log('分页参数: page =', page, ', pageSize =', pageSize);
        
        // 查询数据库
        const questionCollection = db.collection('question');
        
        // 1. 获取所有题号大于maxQuestionNumber的题目（新增和修改的题目）
        const newQuestionsQuery = questionCollection.where({
            qId: db.command.gt(maxQuestionNumber)
        }).orderBy('qId', 'asc').skip((page - 1) * pageSize).limit(pageSize).get();
        
        // 2. 获取所有节号为0的题目（被删除的题目）
        const deletedQuestionsQuery = questionCollection.where({
            sectionId: 0
        }).limit(pageSize).get();
        
        // 并行查询
        const [newQuestionsResult, deletedQuestionsResult] = await Promise.all([
            newQuestionsQuery,
            deletedQuestionsQuery
        ]);
        
        const newQuestions = newQuestionsResult.data || [];
        const deletedQuestions = deletedQuestionsResult.data || [];
        
        // 判断是否还有更多数据
        const hasMore = newQuestions.length === pageSize;
        
        console.log('查询结果:');
        console.log('- 新增/修改题目数量:', newQuestions.length);
        console.log('- 删除题目数量:', deletedQuestions.length);
        console.log('- 是否还有更多:', hasMore);
        
        // 转换数据格式，适配本地数据库
        const formattedNewQuestions = newQuestions.map(q => ({
            qId: q.qId,
            content: q.content || '',
            answer: q.answer || '',
            sectionId: q.sectionId || 0,
            imageUrl: q.imageUrl || ''
        }));
        
        const deletedQuestionNumbers = deletedQuestions.map(q => q.qId);
        
        // 返回结果
        return {
            code: 1,
            msg: '同步成功',
            data: {
                newQuestions: formattedNewQuestions,
                deletedQuestionNumbers: deletedQuestionNumbers,
                hasMore: hasMore,
                currentPage: page,
                pageSize: pageSize
            }
        };
        
    } catch (error) {
        console.error('习题同步处理错误:', error);
        console.error('错误堆栈:', error.stack);
        
        return {
            code: -2,
            msg: '数据库操作失败: ' + error.message,
            data: null
        };
    }
}

// 获取班级信息处理函数
async function handleGetClasses(event, db) {
    console.log('=== 处理获取班级信息请求 ===');
    
    // 检查db是否已初始化
    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }
    
    try {
        // 获取参数 - 从event.body中解析JSON
        let teacherId;
        
        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            teacherId = body.teacherId;
        } else {
            teacherId = event.teacherId;
        }
        
        console.log('获取班级请求参数: teacherId =', teacherId);
        
        // 参数校验
        if (!teacherId) {
            return {
                code: 0,
                msg: '教师ID不能为空',
                data: null
            };
        }
        
        // 查询班级表，获取该教师创建的所有班级
        const classCollection = db.collection('class');
        const queryResult = await classCollection.where({
            teacherId: parseInt(teacherId)
        }).get();
        
        console.log('查询到的班级数量:', queryResult.data.length);
        
        // 提取班级编号和名称
        const classes = queryResult.data.map(c => ({
            classId: c.classId,
            className: c.className
        }));
        
        return {
            code: 1,
            msg: '获取班级成功',
            data: classes
        };
        
    } catch (error) {
        console.error('获取班级处理错误:', error);
        console.error('错误堆栈:', error.stack);
        
        return {
            code: -2,
            msg: '数据库操作失败: ' + error.message,
            data: null
        };
    }
}

// 新建班级处理函数
async function handleCreateClass(event, db) {
    console.log('=== 处理新建班级请求 ===');
    
    // 检查db是否已初始化
    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }
    
    try {
        // 获取参数 - 从event.body中解析JSON
        let teacherId, className;
        
        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            teacherId = body.teacherId;
            className = body.className;
        } else {
            teacherId = event.teacherId;
            className = event.className;
        }
        
        console.log('新建班级请求参数: teacherId =', teacherId, ', className =', className);
        
        // 参数校验
        if (!teacherId) {
            return {
                code: 0,
                msg: '教师ID不能为空',
                data: null
            };
        }
        
        if (!className || className.trim() === '') {
            return {
                code: 0,
                msg: '班级名称不能为空',
                data: null
            };
        }
        
        // 自动生成班级ID（使用时间戳后6位）
        const classId = parseInt(Date.now() % 1000000);
        console.log('生成的班级ID:', classId);
        
        // 构建班级数据
        const classData = {
            classId: classId,
            className: className.trim(),
            teacherId: parseInt(teacherId),
            createTime: Date.now(),
            updateTime: Date.now()
        };
        
        console.log('准备插入的班级数据:', JSON.stringify(classData, null, 2));
        
        // 插入班级表
        const classCollection = db.collection('class');
        const addResult = await classCollection.add(classData);
        console.log('班级插入成功，文档ID:', addResult.id);
        
        return {
            code: 1,
            msg: '班级创建成功',
            data: {
                classId: classId,
                className: className.trim(),
                teacherId: parseInt(teacherId)
            }
        };
        
    } catch (error) {
        console.error('新建班级处理错误:', error);
        console.error('错误堆栈:', error.stack);
        
        return {
            code: -2,
            msg: '数据库操作失败: ' + error.message,
            data: null
        };
    }
}

// 提交学生答案处理函数
async function handleSubmitAnswer(event, db) {
    console.log('=== 处理提交学生答案请求 ===');
    
    // 检查db是否已初始化
    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }
    
    try {
        // 获取参数 - 从event.body中解析JSON
        let userId, qId, studentAnswer;
        
        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            userId = body.userId;
            qId = body.qId;
            studentAnswer = body.studentAnswer;
        } else {
            userId = event.userId;
            qId = event.qId;
            studentAnswer = event.studentAnswer;
        }
        
        console.log('提交答案请求参数: userId =', userId, ', qId =', qId);
        
        // 参数校验
        if (!userId) {
            return {
                code: 0,
                msg: '用户ID不能为空',
                data: null
            };
        }
        
        if (qId === undefined || qId === null) {
            return {
                code: 0,
                msg: '题号不能为空',
                data: null
            };
        }
        
        if (!studentAnswer || studentAnswer.trim() === '') {
            return {
                code: 0,
                msg: '答案不能为空',
                data: null
            };
        }
        
        // 查询该学生是否已经提交过本题答案
        const answerCollection = db.collection('answer');
        const existingResult = await answerCollection.where({
            userId: parseInt(userId),
            qId: parseInt(qId)
        }).get();
        
        console.log('查询现有答案结果:', existingResult.data.length);
        
        // 获取当前时间和题号
        const now = Date.now();
        const questionId = parseInt(qId);
        
        // 检查是否已经提交过且已被批改（非0状态）
        if (existingResult.data.length > 0) {
            const existingAnswer = existingResult.data[0];
            const currentStatus = existingAnswer.status;
            
            // 如果已批改（状态为1正确或2错误），不允许修改
            if (currentStatus === 1 || currentStatus === 2) {
                console.log('答案已被批改，不允许修改');
                return {
                    code: 0,
                    msg: '答案已被批改，无法修改',
                    data: null
                };
            }
            
            // 未批改（状态为0），更新答案
            const docId = existingAnswer._id;
            await answerCollection.doc(docId).update({
                studentAnswer: studentAnswer.trim(),
                updateTime: now
            });
            
            console.log('更新已有答案成功，文档ID:', docId);
            
            return {
                code: 1,
                msg: '答案更新成功',
                data: {
                    qId: questionId,
                    studentAnswer: studentAnswer.trim(),
                    status: 0,
                    isNew: false
                }
            };
        }
        
        // 没有提交过，插入新答案
        const newAnswerData = {
            userId: parseInt(userId),
            qId: questionId,
            studentAnswer: studentAnswer.trim(),
            status: 0,  // 0: 未批改
            createTime: now,
            updateTime: now
        };
        
        console.log('准备插入的答案数据:', JSON.stringify(newAnswerData, null, 2));
        
        const addResult = await answerCollection.add(newAnswerData);
        console.log('答案插入成功，文档ID:', addResult.id);
        
        return {
            code: 1,
            msg: '答案提交成功',
            data: {
                qId: questionId,
                studentAnswer: studentAnswer.trim(),
                status: 0,
                isNew: true
            }
        };
        
    } catch (error) {
        console.error('提交答案处理错误:', error);
        console.error('错误堆栈:', error.stack);
        
        return {
            code: -2,
            msg: '数据库操作失败: ' + error.message,
            data: null
        };
    }
}

// 查询学生答案处理函数
async function handleGetStudentAnswer(event, db) {
    console.log('=== 处理查询学生答案请求 ===');
    
    // 检查db是否已初始化
    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }
    
    try {
        // 获取参数 - 从event.body中解析JSON
        let userId, qId;
        
        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            userId = body.userId;
            qId = body.qId;
        } else {
            userId = event.userId;
            qId = event.qId;
        }
        
        console.log('查询答案请求参数: userId =', userId, ', qId =', qId);
        
        // 参数校验
        if (!userId) {
            return {
                code: 0,
                msg: '用户ID不能为空',
                data: null
            };
        }
        
        if (qId === undefined || qId === null) {
            return {
                code: 0,
                msg: '题号不能为空',
                data: null
            };
        }
        
        // 查询学生答案
        const answerCollection = db.collection('answer');
        const queryResult = await answerCollection.where({
            userId: parseInt(userId),
            qId: parseInt(qId)
        }).get();
        
        console.log('查询结果数量:', queryResult.data.length);
        
        if (queryResult.data.length === 0) {
            return {
                code: 1,
                msg: '未查询到答案',
                data: null
            };
        }
        
        const answerData = queryResult.data[0];
        
        // 返回答案信息
        return {
            code: 1,
            msg: '查询成功',
            data: {
                qId: answerData.qId,
                studentAnswer: answerData.studentAnswer,
                status: answerData.status,  // 0: 未批改, 1: 正确, 2: 错误
                teacherMsg: answerData.teacherMsg || null,
                updateTime: answerData.updateTime
            }
        };
        
    } catch (error) {
        console.error('查询答案处理错误:', error);
        console.error('错误堆栈:', error.stack);
        
        return {
            code: -2,
            msg: '数据库操作失败: ' + error.message,
            data: null
        };
    }
}

// 批量查询学生答案处理函数
async function handleGetBatchStudentAnswers(event, db) {
    console.log('=== 处理批量查询学生答案请求 ===');
    
    // 检查db是否已初始化
    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }
    
    try {
        // 获取参数 - 从event.body中解析JSON
        let userId, qIds;
        
        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            userId = body.userId;
            qIds = body.qIds;
        } else {
            userId = event.userId;
            qIds = event.qIds;
        }
        
        console.log('批量查询答案请求参数: userId =', userId, ', qIds =', qIds);
        
        // 参数校验
        if (!userId) {
            return {
                code: 0,
                msg: '用户ID不能为空',
                data: null
            };
        }
        
        if (!Array.isArray(qIds) || qIds.length === 0) {
            return {
                code: 1,
                msg: '无题目ID',
                data: []
            };
        }
        
        // 批量查询学生答案
        const answerCollection = db.collection('answer');
        const queryResult = await answerCollection.where({
            userId: parseInt(userId),
            qId: db.command.in(qIds.map(id => parseInt(id)))
        }).get();
        
        console.log('批量查询结果数量:', queryResult.data.length);
        
        // 转换结果为Map格式
        const answersMap = {};
        queryResult.data.forEach(answerData => {
            answersMap[answerData.qId] = {
                qId: answerData.qId,
                studentAnswer: answerData.studentAnswer,
                status: answerData.status,  // 0: 未批改, 1: 正确, 2: 错误
                teacherMsg: answerData.teacherMsg || null,
                updateTime: answerData.updateTime
            };
        });
        
        return {
            code: 1,
            msg: '查询成功',
            data: answersMap
        };
        
    } catch (error) {
        console.error('批量查询答案处理错误:', error);
        console.error('错误堆栈:', error.stack);
        
        return {
            code: -2,
            msg: '数据库操作失败: ' + error.message,
            data: null
        };
    }
}

// 获取学生所有答题记录处理函数
async function handleGetStudentAllAnswers(event, db) {
    console.log('=== 处理获取学生所有答题记录请求 ===');
    
    // 检查db是否已初始化
    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }
    
    try {
        // 获取参数 - 从event.body中解析JSON
        let userId;
        
        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            userId = body.userId;
        } else {
            userId = event.userId;
        }
        
        console.log('获取学生所有答题记录请求参数: userId =', userId);
        
        // 参数校验
        if (!userId) {
            return {
                code: 0,
                msg: '用户ID不能为空',
                data: null
            };
        }
        
        // 查询该学生的所有答题记录
        const answerCollection = db.collection('answer');
        const queryResult = await answerCollection.where({
            userId: parseInt(userId)
        }).get();
        
        console.log('查询结果数量:', queryResult.data.length);
        
        // 转换结果为Map格式
        const answersMap = {};
        queryResult.data.forEach(answerData => {
            answersMap[answerData.qId] = {
                qId: answerData.qId,
                studentAnswer: answerData.studentAnswer,
                status: answerData.status,  // 0: 未批改, 1: 正确, 2: 错误
                teacherMsg: answerData.teacherMsg || null,
                updateTime: answerData.updateTime
            };
        });
        
        return {
            code: 1,
            msg: '查询成功',
            data: answersMap
        };
        
    } catch (error) {
        console.error('获取学生所有答题记录处理错误:', error);
        console.error('错误堆栈:', error.stack);
        
        return {
            code: -2,
            msg: '数据库操作失败: ' + error.message,
            data: null
        };
    }
}

// 注册处理函数
async function handleRegister(username, password, role, classId, db, bcrypt) {
    console.log('=== 处理注册请求 ===');
    
    // 检查db是否已初始化
    if (!db) {
        console.error('数据库连接未初始化，db为:', db);
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }
    
    // 2. 参数校验
    // 2.1 非空校验
    if (!username || !password || role === undefined || classId === undefined) {
        return {
            code: 0,
            msg: '用户名、密码、用户类型和班级不能为空',
            data: null
        };
    }
    
    // 2.2 用户名格式校验（3-18位英文字母、数字、下划线）
    const usernameRegex = /^[a-zA-Z0-9_]{3,18}$/;
    if (!usernameRegex.test(username)) {
        return {
            code: 0,
            msg: '用户名格式错误：必须是3-18位的英文字母、数字或下划线',
            data: null
        };
    }
    
    // 2.3 密码长度校验
    if (password.length < 6 || password.length > 20) {
        return {
            code: 0,
            msg: '密码长度必须在6-20位之间',
            data: null
        };
    }
    
    // 2.4 用户类型校验
    if (role !== 0 && role !== 1) {
        return {
            code: 0,
            msg: '用户类型错误：0-学生，1-教师',
            data: null
        };
    }
    
    // 2.5 班级校验
    // 教师必须班级为0
    if (role === 1 && classId !== 0) {
        return {
            code: 0,
            msg: '教师用户的班级必须为0',
            data: null
        };
    }
    
    // 学生班级必须大于0（如果需要班级存在性验证，可在此添加）
    if (role === 0 && classId <= 0) {
        return {
            code: 0,
            msg: '学生必须选择有效的班级（班级ID需大于0）',
            data: null
        };
    }
    
    try {
        // 3. 检查用户名是否已存在
        const userCollection = db.collection('user');
        const existResult = await userCollection.where({
            username: username
        }).get();
        
        if (existResult.data.length > 0) {
            return {
                code: 0,
                msg: '用户名已存在，请更换',
                data: null
            };
        }
        
        // 4. 密码加密
        console.log('开始密码加密');
        const saltRounds = 10;
        const hashedPassword = bcrypt.hashSync(password, saltRounds);
        console.log('密码加密完成');
        
        // 5. 生成用户ID（使用时间戳）
        const userId = Date.now().toString();
        console.log('生成的用户ID:', userId);
        
        // 6. 构建用户数据
        const userData = {
            userId: parseInt(userId),    // 转为整数类型
            username: username,
            password: hashedPassword,
            role: role,           // 0:学生，1:教师
            classId: classId,     // 班级ID
            createTime: Date.now(),
            updateTime: Date.now()
        };
        
        console.log('准备插入的用户数据:', JSON.stringify(userData, null, 2));
        
        // 7. 插入数据库
        console.log('开始插入数据库...');
        const addResult = await userCollection.add(userData);
        console.log('用户插入成功，文档ID:', addResult.id);
        
        // 8. 返回成功响应
        console.log('返回成功响应');
        return {
            code: 1,
            msg: '注册成功',
            data: {
                userId: userId,
                username: username,
                role: role,
                classId: classId
            }
        };
        
    } catch (dbError) {
        console.error('数据库插入失败:', dbError.message);
        console.error('数据库错误堆栈:', dbError.stack);
        
        return {
            code: -2,
            msg: '数据库操作失败',
            data: null
        };
    }
}

// 删除题目处理函数
async function handleDeleteQuestion(event, db) {
    console.log('=== 处理删除题目请求 ===');
    
    // 检查db是否已初始化
    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }
    
    try {
        // 获取参数 - 从event.body中解析JSON
        let userId, qId;
        
        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            userId = body.userId;
            qId = body.qId;
        } else {
            userId = event.userId;
            qId = event.qId;
        }
        
        console.log('删除题目请求参数: userId =', userId, ', qId =', qId);
        
        // 参数校验
        if (!userId) {
            return {
                code: 0,
                msg: '用户ID不能为空',
                data: null
            };
        }
        
        if (qId === undefined || qId === null) {
            return {
                code: 0,
                msg: '题号不能为空',
                data: null
            };
        }
        
        // 验证用户类型是否为教师
        const userCollection = db.collection('user');
        const userResult = await userCollection.where({
            userId: parseInt(userId)
        }).get();
        
        if (userResult.data.length === 0) {
            return {
                code: 0,
                msg: '用户不存在',
                data: null
            };
        }
        
        const user = userResult.data[0];
        if (user.role !== 1) {
            return {
                code: 0,
                msg: '只有教师才能删除题目',
                data: null
            };
        }
        
        // 查询题目是否存在
        const questionCollection = db.collection('question');
        const questionResult = await questionCollection.where({
            qId: parseInt(qId)
        }).get();
        
        if (questionResult.data.length === 0) {
            return {
                code: 0,
                msg: '题目不存在',
                data: null
            };
        }
        
        // 将题目节号改为0（标记为删除）
        const questionDoc = questionResult.data[0];
        const docId = questionDoc._id;
        
        await questionCollection.doc(docId).update({
            sectionId: 0,
            updateTime: Date.now()
        });
        
        console.log('题目删除成功，qId:', qId);
        
        return {
            code: 1,
            msg: '题目删除成功',
            data: {
                qId: parseInt(qId)
            }
        };
        
    } catch (error) {
        console.error('删除题目处理错误:', error);
        console.error('错误堆栈:', error.stack);
        
        return {
            code: -2,
            msg: '数据库操作失败: ' + error.message,
            data: null
        };
    }
}

// 上传题目处理函数
async function handleUploadQuestion(event, db) {
    console.log('=== 处理上传题目请求 ===');
    
    // 检查db是否已初始化
    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }
    
    try {
        // 获取参数 - 从event.body中解析JSON
        let userId, content, answer, sectionId, imageUrl;
        
        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            userId = body.userId;
            content = body.content;
            answer = body.answer;
            sectionId = body.sectionId;
            imageUrl = body.imageUrl || '';
        } else {
            userId = event.userId;
            content = event.content;
            answer = event.answer;
            sectionId = event.sectionId;
            imageUrl = event.imageUrl || '';
        }
        
        console.log('上传题目请求参数: userId =', userId, ', sectionId =', sectionId);
        
        // 参数校验
        if (!userId) {
            return {
                code: 0,
                msg: '用户ID不能为空',
                data: null
            };
        }
        
        if (!content || content.trim() === '') {
            return {
                code: 0,
                msg: '题目内容不能为空',
                data: null
            };
        }
        
        if (!answer || answer.trim() === '') {
            return {
                code: 0,
                msg: '答案不能为空',
                data: null
            };
        }
        
        if (sectionId === undefined || sectionId === null) {
            return {
                code: 0,
                msg: '节号不能为空',
                data: null
            };
        }
        
        // 验证用户类型是否为教师
        const userCollection = db.collection('user');
        const userResult = await userCollection.where({
            userId: parseInt(userId)
        }).get();
        
        if (userResult.data.length === 0) {
            return {
                code: 0,
                msg: '用户不存在',
                data: null
            };
        }
        
        const user = userResult.data[0];
        if (user.role !== 1) {
            return {
                code: 0,
                msg: '只有教师才能上传题目',
                data: null
            };
        }
        
        // 查询当前最大题号
        const questionCollection = db.collection('question');
        const maxQResult = await questionCollection.orderBy('qId', 'desc').limit(1).get();
        
        let newQId = 1;
        if (maxQResult.data.length > 0) {
            newQId = (maxQResult.data[0].qId || 0) + 1;
        }
        
        console.log('新题号:', newQId);
        
        // 构建题目数据
        const questionData = {
            qId: newQId,
            content: content.trim(),
            answer: answer.trim(),
            sectionId: parseInt(sectionId),
            imageUrl: imageUrl || '',
            createTime: Date.now(),
            updateTime: Date.now()
        };
        
        console.log('准备插入的题目数据:', JSON.stringify(questionData, null, 2));
        
        // 插入题目
        const addResult = await questionCollection.add(questionData);
        console.log('题目插入成功，文档ID:', addResult.id);
        
        return {
            code: 1,
            msg: '题目上传成功',
            data: {
                qId: newQId,
                content: content.trim(),
                answer: answer.trim(),
                sectionId: parseInt(sectionId),
                imageUrl: imageUrl || ''
            }
        };
        
    } catch (error) {
        console.error('上传题目处理错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '数据库操作失败: ' + error.message,
            data: null
        };
    }
}

// 获取图片上传参数处理函数
async function handleGetUploadParams(event) {
    console.log('=== 处理获取上传参数请求 ===');

    try {
        // 获取参数
        let fileName;

        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            fileName = body.fileName;
        } else {
            fileName = event.fileName;
        }

        console.log('获取上传参数: fileName =', fileName);

        // 生成云存储路径
        const timestamp = Date.now();
        const cloudPath = `question-images/${timestamp}_${fileName || 'image.jpg'}`;

        // 返回上传参数（用于前端直传）
        return {
            code: 1,
            msg: '获取上传参数成功',
            data: {
                cloudPath: cloudPath,
                fileType: 'image'
            }
        };

    } catch (error) {
        console.error('获取上传参数错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '获取上传参数失败: ' + error.message,
            data: null
        };
    }
}

// 上传图片到云存储处理函数
async function handleUploadImage(event, context) {
    console.log('=== 处理上传图片请求 ===');

    // 检查 db 是否已初始化
    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }

    try {
        // 获取参数
        let fileName, imageData;

        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            fileName = body.fileName;
            imageData = body.imageData;
        } else {
            fileName = event.fileName;
            imageData = event.imageData;
        }

        console.log('上传图片请求参数: fileName =', fileName);

        // 参数校验
        if (!fileName) {
            return {
                code: 0,
                msg: '文件名不能为空',
                data: null
            };
        }

        if (!imageData) {
            return {
                code: 0,
                msg: '图片数据不能为空',
                data: null
            };
        }

        // 生成云存储路径
        const timestamp = Date.now();
        const cloudPath = `question-images/${timestamp}_${fileName}`;

        // 将 Base64 转为 Buffer
        let buffer;
        if (imageData.startsWith('data:image')) {
            // 处理 data:image/jpeg;base64,xxx 格式
            const base64Data = imageData.split(',')[1];
            buffer = Buffer.from(base64Data, 'base64');
        } else {
            buffer = Buffer.from(imageData, 'base64');
        }

        console.log('图片数据大小:', buffer.length, 'bytes');

        // 上传到云存储
        const uploadResult = await uniCloud.uploadFile({
            cloudPath: cloudPath,
            fileContent: buffer
        });

        console.log('图片上传成功, fileID:', uploadResult.fileID);

        // 构建可访问的 URL
        const imageUrl = uploadResult.fileID;

        return {
            code: 1,
            msg: '图片上传成功',
            data: {
                imageUrl: imageUrl,
                cloudPath: cloudPath
            }
        };

    } catch (error) {
        console.error('上传图片处理错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '上传图片失败: ' + error.message,
            data: null
        };
    }
}

// 获取班级题目答题统计处理函数
async function handleGetClassQuestionStats(event, db) {
    console.log('=== 处理获取班级题目答题统计请求 ===');

    // 检查db是否已初始化
    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }

    try {
        // 获取参数 - 从event.body中解析JSON
        let classId, qId;

        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            classId = body.classId;
            qId = body.qId;
        } else {
            classId = event.classId;
            qId = event.qId;
        }

        console.log('获取班级答题统计请求参数: classId =', classId, ', qId =', qId);

        // 参数校验
        if (!classId) {
            return {
                code: 0,
                msg: '班级ID不能为空',
                data: null
            };
        }

        if (qId === undefined || qId === null) {
            return {
                code: 0,
                msg: '题号不能为空',
                data: null
            };
        }

        const parsedClassId = parseInt(classId);
        const parsedQId = parseInt(qId);

        // 1. 获取班级总人数
        const userCollection = db.collection('user');
        const classStudentsResult = await userCollection.where({
            classId: parsedClassId,
            role: 0  // 学生角色
        }).get();

        const classStudents = classStudentsResult.data || [];
        const totalStudents = classStudents.length;
        console.log('班级总人数:', totalStudents);

        if (totalStudents === 0) {
            return {
                code: 1,
                msg: '班级暂无学生',
                data: {
                    classId: parsedClassId,
                    qId: parsedQId,
                    totalStudents: 0,
                    notDoneCount: 0,
                    notCheckedCount: 0,
                    wrongCount: 0,
                    correctCount: 0
                }
            };
        }

        // 获取班级所有学生的userId列表
        const studentUserIds = classStudents.map(s => s.userId);
        console.log('班级学生userId列表:', studentUserIds);

        // 2. 查询该班级所有学生对该题的答题情况
        const answerCollection = db.collection('answer');
        const answersResult = await answerCollection.where({
            qId: parsedQId
        }).get();

        const allAnswers = answersResult.data || [];

        // 过滤出属于该班级的学生的答案
        const classAnswers = allAnswers.filter(answer => studentUserIds.includes(answer.userId));

        console.log('该班级学生答题记录数:', classAnswers.length);

        // 统计各状态人数
        let notCheckedCount = 0;
        let wrongCount = 0;
        let correctCount = 0;

        classAnswers.forEach(answer => {
            switch (answer.status) {
                case 0:
                    notCheckedCount++;
                    break;
                case 1:
                    correctCount++;
                    break;
                case 2:
                    wrongCount++;
                    break;
            }
        });

        // 计算未完成人数 = 班级总人数 - 已提交人数
        const submittedCount = notCheckedCount + wrongCount + correctCount;
        const notDoneCount = totalStudents - submittedCount;

        console.log('答题统计结果:');
        console.log('- 总人数:', totalStudents);
        console.log('- 已提交:', submittedCount);
        console.log('- 未完成:', notDoneCount);
        console.log('- 未批改:', notCheckedCount);
        console.log('- 错误:', wrongCount);
        console.log('- 正确:', correctCount);

        return {
            code: 1,
            msg: '获取统计成功',
            data: {
                classId: parsedClassId,
                qId: parsedQId,
                totalStudents: totalStudents,
                notDoneCount: notDoneCount,
                notCheckedCount: notCheckedCount,
                wrongCount: wrongCount,
                correctCount: correctCount
            }
        };

    } catch (error) {
        console.error('获取班级答题统计错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '获取统计失败: ' + error.message,
            data: null
        };
    }
}

// 获取教师管理班级学生的未批改题目处理函数
async function handleGetTeacherUngradedQuestions(event, db) {
    console.log('=== 处理获取教师管理班级学生未批改题目请求 ===');

    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }

    try {
        let teacherId;

        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            teacherId = body.teacherId;
        } else {
            teacherId = event.teacherId;
        }

        console.log('获取未批改题目请求参数: teacherId =', teacherId);

        if (!teacherId) {
            return {
                code: 0,
                msg: '教师ID不能为空',
                data: null
            };
        }

        const parsedTeacherId = parseInt(teacherId);

        // 1. 获取该教师管理的所有班级
        const classCollection = db.collection('class');
        const classesResult = await classCollection.where({
            teacherId: parsedTeacherId
        }).get();

        const classes = classesResult.data || [];
        console.log('教师管理的班级数量:', classes.length);

        if (classes.length === 0) {
            return {
                code: 1,
                msg: '暂无班级',
                data: []
            };
        }

        // 提取班级ID列表
        const classIds = classes.map(c => c.classId);
        console.log('班级ID列表:', classIds);

        // 2. 获取这些班级的所有学生
        const userCollection = db.collection('user');
        const studentsResult = await userCollection.where({
            role: 0,  // 学生角色
            classId: db.command.in(classIds)
        }).get();

        const students = studentsResult.data || [];
        console.log('班级学生数量:', students.length);

        if (students.length === 0) {
            return {
                code: 1,
                msg: '班级暂无学生',
                data: []
            };
        }

        // 提取学生userId列表
        const studentUserIds = students.map(s => s.userId);
        console.log('学生userId列表:', studentUserIds);

        // 3. 查询所有学生的未批改答案（status = 0）
        const answerCollection = db.collection('answer');
        const ungradedResult = await answerCollection.where({
            userId: db.command.in(studentUserIds),
            status: 0  // 未批改
        }).get();

        const ungradedAnswers = ungradedResult.data || [];
        console.log('未批改答案数量:', ungradedAnswers.length);

        // 4. 提取qId列表
        const qIds = [...new Set(ungradedAnswers.map(a => a.qId))];
        console.log('涉及题目数量:', qIds.length);

        // 5. 构建返回数据
        const resultData = ungradedAnswers.map(answer => ({
            qId: answer.qId,
            userId: answer.userId
        }));

        return {
            code: 1,
            msg: '获取成功',
            data: resultData
        };

    } catch (error) {
        console.error('获取未批改题目错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '获取失败: ' + error.message,
            data: null
        };
    }
}

// 获取教师管理班级学生所有有答题记录的题目（包括已批改和未批改）
async function handleGetTeacherAllAnsweredQuestions(event, db) {
    console.log('=== 处理获取教师管理班级学生所有有答题记录题目请求 ===');

    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }

    try {
        let teacherId;

        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            teacherId = body.teacherId;
        } else {
            teacherId = event.teacherId;
        }

        console.log('获取所有答题记录题目请求参数: teacherId =', teacherId);

        if (!teacherId) {
            return {
                code: 0,
                msg: '教师ID不能为空',
                data: null
            };
        }

        const parsedTeacherId = parseInt(teacherId);

        // 1. 获取该教师管理的所有班级
        const classCollection = db.collection('class');
        const classesResult = await classCollection.where({
            teacherId: parsedTeacherId
        }).get();

        const classes = classesResult.data || [];
        console.log('教师管理的班级数量:', classes.length);

        if (classes.length === 0) {
            return {
                code: 1,
                msg: '暂无班级',
                data: []
            };
        }

        // 提取班级ID列表
        const classIds = classes.map(c => c.classId);
        console.log('班级ID列表:', classIds);

        // 2. 获取这些班级的所有学生
        const userCollection = db.collection('user');
        const studentsResult = await userCollection.where({
            role: 0,  // 学生角色
            classId: db.command.in(classIds)
        }).get();

        const students = studentsResult.data || [];
        console.log('班级学生数量:', students.length);

        if (students.length === 0) {
            return {
                code: 1,
                msg: '班级暂无学生',
                data: []
            };
        }

        // 提取学生userId列表
        const studentUserIds = students.map(s => s.userId);
        console.log('学生userId列表:', studentUserIds);

        // 3. 查询所有学生的所有答题记录（不区分status）
        const answerCollection = db.collection('answer');
        const allAnswersResult = await answerCollection.where({
            userId: db.command.in(studentUserIds)
        }).get();

        const allAnswers = allAnswersResult.data || [];
        console.log('所有答题记录数量:', allAnswers.length);

        // 4. 构建返回数据
        const resultData = allAnswers.map(answer => ({
            qId: answer.qId,
            userId: answer.userId,
            status: answer.status  // 0:未批改, 1:正确, 2:错误
        }));

        return {
            code: 1,
            msg: '获取成功',
            data: resultData
        };

    } catch (error) {
        console.error('获取所有答题记录题目错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '获取失败: ' + error.message,
            data: null
        };
    }
}

// 获取学生信息处理函数
async function handleGetStudentsInfo(event, db) {
    console.log('=== 处理获取学生信息请求 ===');

    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }

    try {
        let userIds;

        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            userIds = body.userIds;
        } else {
            userIds = event.userIds;
        }

        console.log('获取学生信息请求参数: userIds =', userIds);

        if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
            return {
                code: 0,
                msg: '用户ID列表不能为空',
                data: null
            };
        }

        // 查询学生信息
        const userCollection = db.collection('user');
        const usersResult = await userCollection.where({
            userId: db.command.in(userIds.map(id => parseInt(id))),
            role: 0  // 学生角色
        }).get();

        const students = usersResult.data || [];
        console.log('查询到的学生数量:', students.length);

        // 构建返回数据
        const resultData = students.map(student => ({
            userId: student.userId,
            username: student.username,
            classId: student.classId
        }));

        return {
            code: 1,
            msg: '获取成功',
            data: resultData
        };

    } catch (error) {
        console.error('获取学生信息错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '获取失败: ' + error.message,
            data: null
        };
    }
}

// 获取班级所有学生处理函数
async function handleGetClassStudents(event, db) {
    console.log('=== 处理获取班级学生请求 ===');

    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }

    try {
        let classId;

        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            classId = body.classId;
        } else {
            classId = event.classId;
        }

        console.log('获取班级学生请求参数: classId =', classId);

        if (!classId) {
            return {
                code: 0,
                msg: '班级ID不能为空',
                data: null
            };
        }

        // 查询班级学生
        const userCollection = db.collection('user');
        const studentsResult = await userCollection.where({
            classId: parseInt(classId),
            role: 0  // 学生角色
        }).get();

        const students = studentsResult.data || [];
        console.log('查询到的班级学生数量:', students.length);

        // 构建返回数据
        const resultData = students.map(student => ({
            userId: student.userId,
            username: student.username,
            classId: student.classId
        }));

        return {
            code: 1,
            msg: '获取成功',
            data: resultData
        };

    } catch (error) {
        console.error('获取班级学生错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '获取失败: ' + error.message,
            data: null
        };
    }
}

// ==================== 资源管理相关函数 ====================

// 获取教师上传的资源列表
async function handleGetTeacherResources(event, db) {
    console.log('=== 处理获取教师资源列表请求 ===');

    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }

    try {
        let teacherId;

        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            teacherId = body.teacherId;
        } else {
            teacherId = event.teacherId;
        }

        console.log('获取教师资源请求参数: teacherId =', teacherId);

        if (!teacherId) {
            return {
                code: 0,
                msg: '教师ID不能为空',
                data: null
            };
        }

        // 查询该教师的资源
        const resourceCollection = db.collection('resource');
        const result = await resourceCollection.where({
            teacherId: parseInt(teacherId)
        }).orderBy('createTime', 'desc').get();

        const resources = result.data || [];
        console.log('查询到的资源数量:', resources.length);

        // 构建返回数据
        const resultData = resources.map(r => ({
            resId: r._id,
            resName: r.resName,
            url: r.url,
            teacherId: r.teacherId,
            createTime: r.createTime
        }));

        return {
            code: 1,
            msg: '获取成功',
            data: resultData
        };

    } catch (error) {
        console.error('获取教师资源错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '获取失败: ' + error.message,
            data: null
        };
    }
}

// 获取所有资源列表（学生用）
async function handleGetAllResources(event, db) {
    console.log('=== 处理获取所有资源列表请求 ===');

    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }

    try {
        // 查询所有资源
        const resourceCollection = db.collection('resource');
        const result = await resourceCollection.orderBy('createTime', 'desc').get();

        const resources = result.data || [];
        console.log('查询到的资源数量:', resources.length);

        // 构建返回数据
        const resultData = resources.map(r => ({
            resId: r._id,
            resName: r.resName,
            url: r.url,
            teacherId: r.teacherId,
            createTime: r.createTime
        }));

        return {
            code: 1,
            msg: '获取成功',
            data: resultData
        };

    } catch (error) {
        console.error('获取所有资源错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '获取失败: ' + error.message,
            data: null
        };
    }
}

// 上传资源到云存储并保存到数据库
async function handleUploadResource(event) {
    console.log('=== 处理上传资源请求 ===');

    try {
        let teacherId, resName, fileName, fileData;

        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            teacherId = body.teacherId;
            resName = body.resName;
            fileName = body.fileName;
            fileData = body.fileData;
        } else {
            teacherId = event.teacherId;
            resName = event.resName;
            fileName = event.fileName;
            fileData = event.fileData;
        }

        console.log('上传资源请求参数: teacherId =', teacherId, ', resName =', resName, ', fileName =', fileName);

        // 参数校验
        if (!teacherId) {
            return {
                code: 0,
                msg: '教师ID不能为空',
                data: null
            };
        }

        if (!resName) {
            return {
                code: 0,
                msg: '资源名称不能为空',
                data: null
            };
        }

        if (!fileName) {
            return {
                code: 0,
                msg: '文件名不能为空',
                data: null
            };
        }

        if (!fileData) {
            return {
                code: 0,
                msg: '文件数据不能为空',
                data: null
            };
        }

        // 生成云存储路径
        const timestamp = Date.now();
        const cloudPath = `resources/${timestamp}_${fileName}`;

        // 将 Base64 转为 Buffer
        let buffer;
        if (fileData.startsWith('data:')) {
            // 处理 data:image/jpeg;base64,xxx 格式
            const base64Data = fileData.split(',')[1];
            buffer = Buffer.from(base64Data, 'base64');
        } else {
            buffer = Buffer.from(fileData, 'base64');
        }

        console.log('文件数据大小:', buffer.length, 'bytes');

        // 上传到云存储
        const uploadResult = await uniCloud.uploadFile({
            cloudPath: cloudPath,
            fileContent: buffer
        });

        console.log('文件上传成功, fileID:', uploadResult.fileID);

        // 保存到数据库
        if (db) {
            const resourceCollection = db.collection('resource');
            const addResult = await resourceCollection.add({
                resName: resName,
                url: uploadResult.fileID,
                teacherId: parseInt(teacherId),
                createTime: Date.now(),
                updateTime: Date.now()
            });

            console.log('资源保存成功, _id:', addResult.id);

            return {
                code: 1,
                msg: '上传成功',
                data: {
                    resId: addResult.id,
                    resName: resName,
                    url: uploadResult.fileID,
                    teacherId: parseInt(teacherId),
                    createTime: Date.now()
                }
            };
        } else {
            return {
                code: 1,
                msg: '上传成功（未保存到数据库）',
                data: {
                    resId: '',
                    resName: resName,
                    url: uploadResult.fileID,
                    teacherId: parseInt(teacherId),
                    createTime: Date.now()
                }
            };
        }

    } catch (error) {
        console.error('上传资源处理错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '上传失败: ' + error.message,
            data: null
        };
    }
}

// 删除资源
async function handleDeleteResource(event, db) {
    console.log('=== 处理删除资源请求 ===');

    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }

    try {
        let resId, teacherId;

        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            resId = body.resId;
            teacherId = body.teacherId;
        } else {
            resId = event.resId;
            teacherId = event.teacherId;
        }

        console.log('删除资源请求参数: resId =', resId, ', teacherId =', teacherId);

        if (!resId) {
            return {
                code: 0,
                msg: '资源ID不能为空',
                data: null
            };
        }

        if (!teacherId) {
            return {
                code: 0,
                msg: '教师ID不能为空',
                data: null
            };
        }

        // 查询资源，确保是该教师的资源
        const resourceCollection = db.collection('resource');
        const existingResult = await resourceCollection.doc(resId).get();

        if (!existingResult.data || existingResult.data.length === 0) {
            return {
                code: 0,
                msg: '资源不存在',
                data: null
            };
        }

        const resource = existingResult.data[0];

        if (resource.teacherId !== parseInt(teacherId)) {
            return {
                code: 0,
                msg: '无权删除该资源',
                data: null
            };
        }

        // 删除云存储文件
        if (resource.url) {
            try {
                await uniCloud.deleteFile({
                    fileList: [resource.url]
                });
                console.log('云存储文件删除成功');
            } catch (e) {
                console.warn('云存储文件删除失败:', e.message);
            }
        }

        // 删除数据库记录
        await resourceCollection.doc(resId).remove();
        console.log('资源删除成功');

        return {
            code: 1,
            msg: '删除成功',
            data: null
        };

    } catch (error) {
        console.error('删除资源错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '删除失败: ' + error.message,
            data: null
        };
    }
}

// 提交批改结果处理函数
async function handleSubmitGrade(event, db) {
    console.log('=== 处理提交批改结果请求 ===');

    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }

    try {
        let teacherId, userId, qId, status, teacherMsg;

        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            teacherId = body.teacherId;
            userId = body.userId;
            qId = body.qId;
            status = body.status;
            teacherMsg = body.teacherMsg || '';
        } else {
            teacherId = event.teacherId;
            userId = event.userId;
            qId = event.qId;
            status = event.status;
            teacherMsg = event.teacherMsg || '';
        }

        console.log('提交批改请求参数: teacherId =', teacherId, ', userId =', userId, ', qId =', qId, ', status =', status);

        // 参数校验
        if (!teacherId) {
            return {
                code: 0,
                msg: '教师ID不能为空',
                data: null
            };
        }

        if (!userId) {
            return {
                code: 0,
                msg: '学生ID不能为空',
                data: null
            };
        }

        if (qId === undefined || qId === null) {
            return {
                code: 0,
                msg: '题号不能为空',
                data: null
            };
        }

        if (status === undefined || status === null || (status !== 1 && status !== 2)) {
            return {
                code: 0,
                msg: '批改状态错误：1为正确，2为错误',
                data: null
            };
        }

        const parsedTeacherId = parseInt(teacherId);
        const parsedUserId = parseInt(userId);
        const parsedQId = parseInt(qId);
        const parsedStatus = parseInt(status);

        // 查询该学生的答案记录
        const answerCollection = db.collection('answer');
        const existingResult = await answerCollection.where({
            userId: parsedUserId,
            qId: parsedQId
        }).get();

        const now = Date.now();

        if (existingResult.data.length > 0) {
            // 已有答案记录，更新批改状态
            const answerDoc = existingResult.data[0];
            const docId = answerDoc._id;

            await answerCollection.doc(docId).update({
                status: parsedStatus,
                teacherMsg: teacherMsg,
                updateTime: now
            });

            console.log('批改更新成功，文档ID:', docId);

            return {
                code: 1,
                msg: '批改更新成功',
                data: {
                    userId: parsedUserId,
                    qId: parsedQId,
                    status: parsedStatus,
                    teacherMsg: teacherMsg
                }
            };
        } else {
            // 没有答案记录，创建新记录（这种情况一般不会发生，但做兼容处理）
            await answerCollection.add({
                userId: parsedUserId,
                qId: parsedQId,
                studentAnswer: '',
                status: parsedStatus,
                teacherMsg: teacherMsg,
                createTime: now,
                updateTime: now
            });

            console.log('批改记录创建成功');

            return {
                code: 1,
                msg: '批改提交成功',
                data: {
                    userId: parsedUserId,
                    qId: parsedQId,
                    status: parsedStatus,
                    teacherMsg: teacherMsg
                }
            };
        }

    } catch (error) {
        console.error('提交批改错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '提交失败: ' + error.message,
            data: null
        };
    }
}

// 获取学生错题列表处理函数
async function handleGetStudentErrorQuestions(event, db) {
    console.log('=== 处理获取学生错题列表请求 ===');

    if (!db) {
        console.error('数据库连接未初始化');
        return {
            code: -3,
            msg: '数据库连接失败',
            data: null
        };
    }

    try {
        let userId;

        if (event.body) {
            const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
            userId = body.userId;
        } else {
            userId = event.userId;
        }

        console.log('获取学生错题请求参数: userId =', userId);

        if (!userId) {
            return {
                code: 0,
                msg: '学生ID不能为空',
                data: null
            };
        }

        const parsedUserId = parseInt(userId);

        // 1. 查询该学生的所有答题记录（状态为2错误）
        const answerCollection = db.collection('answer');
        const answersResult = await answerCollection.where({
            userId: parsedUserId,
            status: 2  // 错误
        }).get();

        const errorAnswers = answersResult.data || [];
        console.log('错误答题记录数量:', errorAnswers.length);

        if (errorAnswers.length === 0) {
            return {
                code: 1,
                msg: '暂无错题',
                data: []
            };
        }

        // 2. 获取对应的题目信息
        const questionCollection = db.collection('question');
        const qIds = errorAnswers.map(a => a.qId);
        const questionsResult = await questionCollection.where({
            qId: db.command.in(qIds),
            sectionId: db.command.neq(0)  // 过滤已删除的题目
        }).get();

        const questionsMap = {};
        questionsResult.data.forEach(q => {
            questionsMap[q.qId] = q;
        });

        // 3. 构建返回数据
        const resultData = [];
        errorAnswers.forEach(answer => {
            const question = questionsMap[answer.qId];
            if (question) {
                resultData.push({
                    qId: answer.qId,
                    contentPreview: question.content ? question.content.substring(0, 50) + '...' : '',
                    studentAnswer: answer.studentAnswer || '',
                    teacherMsg: answer.teacherMsg || null,
                    sectionId: question.sectionId || 0,
                    status: 0  // 待复习状态
                });
            }
        });

        // 按题号排序
        resultData.sort((a, b) => a.qId - b.qId);

        console.log('返回错题数量:', resultData.length);

        return {
            code: 1,
            msg: '获取成功',
            data: resultData
        };

    } catch (error) {
        console.error('获取学生错题错误:', error);
        console.error('错误堆栈:', error.stack);

        return {
            code: -2,
            msg: '获取失败: ' + error.message,
            data: null
        };
    }
}
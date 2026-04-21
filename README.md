# 离散数学学习助手 - 项目文档

## 项目概述

离散数学学习助手是一个基于Android平台的在线教育应用，专为离散数学课程设计。系统采用双角色架构，支持教师和学生两种用户角色，提供完整的习题管理、答题、批改和统计分析功能。

### 核心功能特点
- **双角色系统**：教师端和学生端功能分离
- **云同步**：题目、答案、批改状态实时同步
- **本地缓存**：离线答题和状态管理
- **统计分析**：班级和个人学习进度统计
- **错题管理**：自动收集和复习错题

## 技术架构

### 前端技术栈
- **开发语言**：Kotlin
- **UI框架**：Android Jetpack (Activity, RecyclerView等)
- **数据库**：SQLite (本地缓存)
- **网络通信**：HTTP/REST API

### 后端技术栈
- **云服务**：uniCloud (腾讯云)
- **数据库**：云数据库 (MongoDB兼容)
- **云函数**：Node.js

## 数据库设计

### 云数据库表结构

#### 1. 用户表 (user)
```javascript
{
  userId: Number,           // 用户编号（唯一）
  username: String,         // 用户名（唯一）
  passwordHash: String,     // 密码哈希值（bcrypt加密）
  role: Number,             // 0学生 1教师
  classId: Number,          // 班级编号（学生有效，教师为0）
  status: String,           // 账号状态：active/disabled
  createTime: Date,         // 创建时间
  updateTime: Date          // 更新时间
}
```

#### 2. 班级表 (class)
```javascript
{
  classId: Number,          // 班级编号（唯一）
  className: String,       // 班级名
  teacherId: Number,        // 教师编号
  teacherName: String,     // 教师名称（冗余字段）
  studentCount: Number,     // 学生人数（冗余字段）
  createTime: Date,        // 创建时间
  updateTime: Date         // 更新时间
}
```

#### 3. 题目表 (question)
```javascript
{
  qId: Number,              // 题号（唯一）
  content: String,         // 题目文本
  answer: String,          // 标准答案
  sectionId: Number,       // 节号
  imageUrl: String,        // 图片URL
  isDeleted: Boolean,      // 删除标记
  deletedAt: Date,         // 删除时间
  deletedBy: Number,       // 删除操作者ID
  createTime: Date,        // 创建时间
  updateTime: Date         // 更新时间
}
```

#### 4. 学生答题表 (answer) - 核心优化
```javascript
{
  qId: Number,              // 题号
  questionBrief: String,    // 题目摘要（冗余字段）
  sectionId: Number,        // 题目所属节（冗余字段）
  correctAnswer: String,    // 正确答案（冗余字段）

  userId: Number,           // 学生用户编号
  studentName: String,     // 学生姓名（冗余字段）
  classId: Number,         // 班级编号（冗余字段）
  className: String,       // 班级名称（冗余字段）

  teacherId: Number,        // 教师编号（冗余字段）

  studentAnswer: String,   // 学生答案
  status: Number,           // 0未批改 1正确 2错误
  teacherMsg: String,       // 教师留言

  createTime: Date,        // 提交时间
  updateTime: Date         // 批改/更新时间
}
```

#### 5. 课堂资源表 (resource)
```javascript
{
  resId: String,            // 资源编号
  name: String,             // 资源名称
  url: String,              // 资源URL（云存储路径）
  teacherId: Number,        // 教师编号
  teacherName: String,      // 教师名称（冗余字段）
  type: String,            // 资源类型：pptx/pdf/docx/video等
  tags: Array,             // 标签数组
  classIds: Array,         // 关联班级ID数组
  size: Number,            // 文件大小（字节）
  createTime: Date,        // 上传时间
  updateTime: Date         // 更新时间
}
```

### 数据库索引设计

```javascript
// user集合
db.user.createIndex({ userId: 1 }, { unique: true })
db.user.createIndex({ username: 1 }, { unique: true })
db.user.createIndex({ classId: 1, role: 1 })

// class集合
db.class.createIndex({ classId: 1 }, { unique: true })
db.class.createIndex({ teacherId: 1 })

// question集合
db.question.createIndex({ qId: 1 }, { unique: true })
db.question.createIndex({ sectionId: 1, isDeleted: 1 })

// answer集合（最关键）
db.answer.createIndex({ userId: 1, qId: 1 }, { unique: true })
db.answer.createIndex({ teacherId: 1, status: 1 })
db.answer.createIndex({ classId: 1, qId: 1 })
db.answer.createIndex({ userId: 1, status: 1 })
db.answer.createIndex({ qId: 1, status: 1 })

// resource集合
db.resource.createIndex({ teacherId: 1, createTime: -1 })
db.resource.createIndex({ classIds: 1, createTime: -1 })
```

### 状态码常量

```javascript
// 用户角色
ROLE.STUDENT = 0
ROLE.TEACHER = 1

// 答题状态
ANSWER_STATUS.NOT_CHECKED = 0   // 未批改
ANSWER_STATUS.CORRECT = 1      // 正确
ANSWER_STATUS.WRONG = 2        // 错误

// 账号状态
STATUS.ACTIVE = 'active'
STATUS.DISABLED = 'disabled'
```

### 本地数据库 (SQLite)
- **表名**：questions
- **字段**：id, question_number, content, answer, section_id, image_url, in_error_book
- **特殊规则**：section_id=0 表示题目已被删除

## 功能模块分析

### 1. 用户认证系统

#### 登录流程
1. **输入验证**：用户名3-18位字母数字下划线，密码6-20位
2. **云函数调用**：调用云函数进行身份验证
3. **角色路由**：根据用户角色跳转到不同主页面
4. **缓存管理**：切换用户时清理旧用户缓存

#### 注册功能
- **教师注册**：直接注册为教师，班级ID为0
- **学生注册**：需选择有效班级

### 2. 学生端功能模块

#### 主页面 (StudentMainActivity)
- 知识库入口
- 习题库入口  
- 错题本入口
- 用户信息管理

#### 习题库 (QuestionBankActivity)
- **题目同步**：自动从云端同步最新题目
- **答题状态**：显示未开始/未批改/正确/错误统计
- **本地缓存**：答题状态本地存储，支持离线使用
- **答题功能**：点击题目进入答题详情页

#### 答题详情 (QuestionDetailActivity)
- 题目内容展示（支持图片）
- 答案输入和提交
- 答题状态实时更新

#### 错题本 (ErrorBookNewActivity)
- 自动收集错误题目
- 错题复习功能
- 错题统计和分析

### 3. 教师端功能模块

#### 主页面 (TeacherMainActivity)
- 班级管理
- 题目管理
- 资源管理
- 作业批改

#### 班级管理 (ClassManagementActivity)
- **班级统计**：查看班级答题统计详情
- **学生注册**：生成学生注册信息
- **新建班级**：创建新的班级

#### 题目管理 (QuestionManagementActivity)
- **题目列表**：查看所有题目
- **题目上传**：上传新题目（支持图片）
- **题目删除**：软删除题目（节号设为0）
- **题目同步**：与云端数据同步

#### 作业批改 (HomeworkGradingActivity)
- **未批改列表**：查看所有待批改题目
- **批量批改**：支持批量批改功能
- **评分统计**：班级整体评分统计

#### 资源管理 (ResourceManagementActivity)
- **资源上传**：上传教学资源
- **资源管理**：查看和删除资源
- **资源共享**：学生可查看教师上传的资源

## 云函数功能分析

### 优化后的云函数架构

基于性能优化建议，云函数已重构为模块化架构，采用路由表+控制器模式，显著提升性能和可维护性。

#### 模块化目录结构
```
user-center/
├── index.js                    # 主入口文件（路由分发）
├── common/                     # 公共工具模块
│   ├── parseEvent.js          # 事件解析器
│   ├── response.js            # 统一响应格式
│   ├── constants.js           # 常量定义
│   ├── time.js                # 时间工具
│   ├── validator.js           # 参数验证器
│   └── auth.js                # 权限验证
├── controllers/               # 控制器模块
│   ├── userController.js      # 用户管理控制器
│   ├── classController.js     # 班级管理控制器
│   ├── questionController.js  # 题目管理控制器
│   ├── answerController.js    # 答题管理控制器（已优化）
│   ├── resourceController.js  # 资源管理控制器
│   └── statisticsController.js # 统计控制器（新增）
└── routes/                    # 路由配置
    └── routeTable.js          # 路由表
```

### 优化特点

#### 1. 代码结构优化
- **路由表模式**：统一的路由分发，避免重复代码
- **模块化设计**：功能按模块分离，便于维护
- **统一错误处理**：标准化的错误响应格式

#### 2. 数据库优化
- **冗余字段增强**：在answer集合中添加studentName、className、questionBrief等冗余字段
- **减少联表查询**：高频统计接口直接查询answer集合，避免多表关联
- **批量操作优化**：支持批量查询和批量更新

#### 3. 性能提升
- **高频统计重构**：重写班级统计、教师统计等高频接口
- **缓存策略**：合理利用数据库索引和查询优化
- **并发处理**：使用Promise.all进行并行查询

### 主要云函数操作

#### 用户管理相关
- **register**：用户注册（支持教师/学生）
- **login**：用户登录（统一验证规则）
- **getClasses**：获取班级列表
- **createClass**：创建班级

#### 题目管理相关
- **syncQuestions**：题目同步（支持分页和增量更新）
- **uploadQuestion**：上传题目（支持图片）
- **deleteQuestion**：软删除题目（sectionId=0）
- **getUploadParams**：获取上传参数

#### 答题管理相关（已优化）
- **submitAnswer**：提交答案（包含冗余字段填充）
- **getStudentAnswer**：查询单个答案
- **getBatchStudentAnswers**：批量查询答案
- **getStudentAllAnswers**：获取学生所有答案
- **submitGrade**：提交批改结果

#### 统计管理相关（新增优化）
- **getClassQuestionStats**：班级题目答题统计（直接查询answer集合）
- **getTeacherUngradedQuestions**：获取未批改题目（利用teacherId冗余字段）
- **getTeacherAllAnsweredQuestions**：获取所有答题记录
- **getStudentErrorQuestions**：获取学生错题列表
- **getClassOverallStats**：班级整体统计（新增）
- **getTeacherAllClassesStats**：教师所有班级统计概览（新增）
- **getStudentProgressStats**：学生个人学习进度统计（新增）

#### 资源管理相关
- **uploadResource**：上传课堂资源
- **getTeacherResources**：获取教师资源
- **getAllPublicResources**：获取所有公开资源
- **deleteResource**：删除资源

## 项目文件结构

```
app/src/main/java/com/example/myapplication/
├── Activity文件
│   ├── MainActivity.kt                 # 登录页面
│   ├── StudentMainActivity.kt          # 学生主页面
│   ├── TeacherMainActivity.kt          # 教师主页面
│   ├── QuestionBankActivity.kt         # 习题库
│   ├── QuestionManagementActivity.kt   # 题目管理
│   ├── ClassManagementActivity.kt      # 班级管理
│   ├── HomeworkGradingActivity.kt      # 作业批改
│   └── ...其他Activity文件
├── 数据库相关
│   ├── QuestionDBHelper.kt             # 本地数据库管理
│   ├── AnswerCacheHelper.kt            # 答题缓存
│   └── KnowledgeGraphDBHelper.kt       # 知识图谱数据库
├── 云服务相关
│   ├── CloudApiHelper.kt               # 云API调用封装
│   ├── QuestionSyncHelper.kt           # 题目同步管理
│   └── UserManager.kt                  # 用户管理
├── 工具类
│   ├── UserValidationUtils.kt          # 用户验证工具
│   └── ...其他工具类
└── 适配器
    ├── QuestionAdapter.kt              # 题目列表适配器
    └── ...其他适配器
```

## 核心业务逻辑

### 1. 题目同步机制
- **增量同步**：基于最大题号进行增量更新
- **删除标记**：sectionId=0表示题目删除
- **分页处理**：支持大数据量分页同步
- **错误处理**：网络异常时的降级处理

### 2. 答题状态管理
- **本地缓存**：答题状态在本地SQLite缓存
- **云端同步**：定期与云端同步答题状态
- **状态映射**：维护题目编号到答题状态的映射关系

### 3. 权限控制
- **角色验证**：根据用户角色显示不同功能
- **操作权限**：只有教师可以进行题目管理、批改等操作
- **数据隔离**：教师只能管理自己创建的班级

### 4. 错误处理机制
- **网络异常**：优雅降级，使用本地缓存数据
- **数据验证**：前后端统一的验证规则
- **用户反馈**：友好的错误提示信息

## 部署和运行

### 环境要求
- Android Studio Arctic Fox 或更高版本
- Android SDK 30 或更高版本
- Kotlin 1.5+

### 构建步骤
1. 克隆项目到本地
2. 使用Android Studio打开项目
3. 配置云函数环境（需要uniCloud账号）
4. 构建并运行应用

### 云服务配置
1. 在uniCloud控制台创建云服务
2. 配置云函数和数据库
3. 更新应用中的云服务配置信息

## 优化成果总结

### 云函数架构优化
基于性能优化建议，已完成对云函数的全面重构，实现了以下改进：

#### 1. 代码结构优化
- **路由表模式**：统一的路由分发，避免重复代码
- **模块化设计**：功能按模块分离，便于维护
- **统一错误处理**：标准化的错误响应格式

#### 2. 数据库性能提升
- **冗余字段增强**：在answer集合中添加studentName、className、questionBrief等冗余字段
- **减少联表查询**：高频统计接口直接查询answer集合，避免多表关联
- **批量操作优化**：支持批量查询和批量更新

#### 3. 高频接口性能优化
- **班级统计重构**：直接查询answer集合，减少数据库查询次数
- **教师统计优化**：利用teacherId冗余字段，避免复杂的班级-学生关联查询
- **学生进度统计**：高效获取个人学习数据，支持实时分析

### 前端适配优化
已完成前端代码的适配更新，包括：

#### 1. 新增统计助手类
- **StatisticsHelper**：封装优化后的统计接口
- **格式化显示**：统一的数据展示格式
- **智能分析**：提供班级活跃度评分、学生学习评级等分析功能

#### 2. 增强用户体验
- **实时统计**：优化后的统计接口响应更快
- **详细分析**：提供更丰富的学习数据分析
- **性能提升**：减少网络请求次数，提升应用响应速度

### 技术特色
1. **前后端分离**：清晰的架构分层
2. **离线优先**：完善的离线使用能力
3. **实时同步**：高效的云端数据同步
4. **模块化设计**：易于维护和扩展
5. **性能优化**：基于冗余字段的高效查询

### 用户体验特色
1. **双角色设计**：满足不同用户需求
2. **智能统计**：详细的学习数据分析
3. **错题管理**：个性化的学习辅助
4. **资源丰富**：支持多种教学资源
5. **性能提升**：更快的统计加载和数据分析

## 未来扩展方向

### 功能扩展
1. **知识图谱**：构建知识点关联关系
2. **智能推荐**：基于学习数据的题目推荐
3. **讨论区**：师生互动交流平台
4. **移动批改**：移动端便捷的批改体验

### 技术优化
1. **性能优化**：大数据量下的性能提升
2. **安全性增强**：数据加密和权限控制
3. **多平台支持**：Web端和iOS端支持
4. **国际化**：多语言支持

---

*最后更新：2026年4月21日*
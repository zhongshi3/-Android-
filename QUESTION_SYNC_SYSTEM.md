# 习题同步系统实现文档

## 概述
实现了完整的习题同步系统，支持新增图片题目、标记删除题目、云端同步等功能。

## 主要功能

### 1. 数据库结构升级
- **版本**: 从 v5 升级到 v6
- **新增字段**: `image_url` (图片URL)、`is_deleted` (删除标记)
- **删除标记**: 使用 `is_deleted = true` 表示题目已被删除
- **常量定义**: `QuestionDBHelper.COLUMN_IS_DELETED = "is_deleted"`

### 2. 题目实体类更新
```kotlin
data class QuestionEntity(
    // ... 原有字段
    val imageUrl: String? = null,  // 图片URL字段
    val isDeleted: Boolean = false  // 删除标记（使用isDeleted）
)
```

### 3. 新增数据库操作方法

#### 获取有效题目（排除已删除题目）
```kotlin
fun getAllValidQuestions(): List<QuestionEntity>  // isDeleted = 0
fun getErrorBookQuestions(): List<QuestionEntity>  // 错题本专用
```

#### 同步相关方法
```kotlin
fun getLocalMaxQuestionNumber(): Int  // 获取本地最大题号
fun markQuestionsAsDeleted(): Int  // 批量标记删除（使用isDeleted）
fun insertOrUpdateQuestion(): Boolean  // 插入或更新题目
```

### 4. 界面逻辑更新

#### 习题库 (QuestionBankActivity)
- 使用 `getAllValidQuestions()` 只显示有效题目
- 进入时自动检查并同步习题更新
- 同步成功后自动刷新界面

#### 错题本 (ErrorBookActivity)
- 使用 `getErrorBookQuestions()` 只显示有效错题
- 自动过滤已删除题目

#### 学习进度 (ProgressActivity)
- 使用 `getAllValidQuestions()` 统计有效题目
- 章节统计只计算有效题目进度

### 5. 云函数扩展

#### 新增同步功能
- **路由**: `action = "syncQuestions"`
- **参数**: `maxQuestionNumber` (本地最大题号)
- **响应**:
  - `newQuestions`: 题号大于本地最大题号的所有题目（过滤isDeleted）
  - `deletedQuestionNumbers`: 所有isDeleted=true的题号

#### 云函数实现 (`register/index.js`)
```javascript
async function handleQuestionSync(event, db) {
    // 1. 获取参数
    // 2. 查询新增题目 (qId > maxQuestionNumber, isDeleted != true)
    // 3. 查询删除题目 (isDeleted = true)
    // 4. 返回结果
}
```

### 6. 本地同步工具

#### QuestionSyncHelper
- **功能**: 处理云端通信和本地数据库更新
- **异步支持**: 使用协程实现异步同步
- **结果回调**: 支持成功/失败回调

#### 同步流程
1. 获取本地最大题号
2. 发送同步请求到云函数
3. 解析响应数据
4. 批量插入/更新新题目
5. 批量标记删除题目（使用isDeleted标记）
6. 刷新界面显示

### 7. CloudApiHelper 扩展
- **新增**: `QuestionSyncResponse` 数据类
- **新增**: `QuestionSyncCallback` 回调接口
- **新增**: `syncQuestions()` 同步方法

## 使用方法

### 1. 进入习题库自动同步
- 用户进入习题库时自动检查更新
- 如果有更新，显示提示信息
- 同步完成后自动刷新列表

### 2. 新增图片题目
- 云数据库 `question` 表新增 `imageUrl` 字段
- 本地数据库同步新增 `image_url` 列
- 支持题目附带图片

### 3. 题目删除机制
- 云端将题目 `isDeleted` 设置为 `true` 表示删除
- 本地同步时自动过滤 isDeleted=1 的题目
- 本地数据库使用 `is_deleted` 列标记删除

### 4. 数据同步规则
- **新增题目**: 题号 > 本地最大题号
- **更新题目**: 云端修改已有题目的内容
- **删除题目**: 云端将 isDeleted 设置为 true
- **同步频率**: 每次进入习题库时检查

## 部署说明

### 1. 云函数更新
1. 将修改后的 `register/index.js` 上传到 uniCloud
2. 确保云数据库 `question` 表包含 `isDeleted` 字段
3. 测试同步功能是否正常工作

### 2. 本地应用更新
1. 安装新版应用（数据库版本升级到v6）
2. 首次运行会自动升级数据库结构
3. 原有题目数据会保留，新增 `is_deleted` 列

### 3. 网络配置
- 修改 `QuestionSyncHelper.cloudFunctionUrl` 为实际云函数地址
- 确保网络连接正常
- 配置适当的超时时间

## 错误处理

### 1. 网络错误
- 显示友好提示信息
- 不影响本地功能使用
- 下次进入时重试

### 2. 数据格式错误
- 验证响应数据格式
- 跳过无效数据项
- 记录错误日志

### 3. 数据库错误
- 使用事务确保数据一致性
- 错误时回滚操作
- 保留原有数据

## 性能优化

### 1. 批量操作
- 使用 SQLite 事务批量插入
- 减少数据库操作次数
- 提高同步效率

### 2. 异步处理
- 使用协程避免阻塞UI
- 后台执行同步任务
- 结果回调到主线程

### 3. 数据过滤
- 本地只查询有效题目（isDeleted = 0）
- 减少内存占用
- 提高列表渲染性能

## 测试建议

### 1. 功能测试
- 新增图片题目同步
- 题目删除标记同步（使用isDeleted）
- 本地最大题号计算
- 界面刷新逻辑

### 2. 网络测试
- 正常网络环境同步
- 弱网环境同步
- 断网后恢复同步

### 3. 兼容性测试
- 旧版本升级测试
- 数据库迁移测试
- 不同设备测试

## 注意事项

1. **isDeleted = true** 的题目不会被显示在任何界面
2. 同步过程会保留用户已做的答案和批改状态
3. 图片URL需要确保网络可访问
4. 云函数地址需要正确配置
5. 首次同步可能需要较长时间（取决于题目数量）
6. 不再复用节号（sectionId）作为删除标记
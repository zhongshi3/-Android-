# 云函数参数传递问题诊断

## 问题描述
输入了用户名和密码，但云函数返回"账号和密码不能为空"错误。

## 日志分析
从之前的日志可以看到：
```
云函数请求成功 - URL: https://fc-mp-ea6433d3-77e4-4c0b-9055-7fac5f1236e4.next.bspapp.com/login, Code: 200, Body: {"code":0,"msg":"账号和密码不能为空"}
```

这表明：
1. ✅ HTTP请求成功（200）
2. ✅ 云函数执行了
3. ❌ 云函数认为参数为空

## 可能的原因

### 1. 请求JSON格式问题
云函数期望的格式可能不正确：
- 直接参数：`{"username": "value", "password": "value"}`
- 包含body字段：`{"body": "{\"username\":\"value\",\"password\":\"value\"}"}`

### 2. 参数值为空字符串
云函数检查 `if (!username || !password)`，空字符串 `""` 会被认为是"空"。

### 3. 字符编码或空白字符
输入可能包含不可见字符或编码问题。

### 4. uniCloud请求解析问题
uniCloud可能需要特定的请求头或格式。

## 已实施的调试措施

### 1. 添加详细日志
在 `CloudApiHelper.kt` 中添加了：
- 参数接收日志（显示用户名长度和密码长度）
- JSON数据构建日志
- URL和请求头日志

### 2. 参数检查
添加了前端参数检查，防止空值传递。

### 3. 请求头优化
添加了 `Accept: application/json` 头。

## 下一步诊断步骤

### 1. 查看新的调试日志
运行应用并查看控制台输出，重点关注：
```
云函数登录参数接收 - username: '...' (长度: X), password: '***' (长度: Y)
云函数登录请求JSON数据: {...}
云函数登录请求URL: https://...
云函数登录请求头: {...}
```

### 2. 手动测试云函数
使用Postman或curl测试：
```bash
curl -X POST "https://fc-mp-ea6433d3-77e4-4c0b-9055-7fac5f1236e4.next.bspapp.com/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "testpass"}'
```

### 3. 检查云函数配置
- 验证云函数是否正确部署
- 检查uniCloud控制台的云函数日志
- 确认云函数入口函数 `exports.main` 的参数处理

## 备用解决方案

### 方案A：修改请求格式
如果当前格式不工作，尝试：
```kotlin
// 备用格式1：包含body字段
val requestData = JSONObject().apply {
    put("body", JSONObject().apply {
        put("username", username)
        put("password", password)
    }.toString())
}

// 备用格式2：URL编码格式
val formBody = FormBody.Builder()
    .add("username", username)
    .add("password", password)
    .build()
```

### 方案B：修改云函数代码
如果云函数可以修改，更新参数检查逻辑：
```javascript
// 更严格的空值检查
const username = event.username || event.body?.username || "";
const password = event.password || event.body?.password || "";
```

## 立即操作建议

1. **运行应用**并输入测试凭据
2. **查看Android Logcat**输出中的调试日志
3. **根据日志**判断问题根源
4. **调整代码**相应部分

## 预期输出示例

正常情况应该看到：
```
云函数登录参数接收 - username: 'admin' (长度: 5), password: '***' (长度: 8)
云函数登录请求JSON数据: {"username":"admin","password":"password123"}
云函数登录请求成功 - URL: ..., Code: 200, Body: {"code":1,"msg":"登录成功",...}
```

异常情况可能显示：
- 参数长度为0（输入为空）
- JSON格式不正确
- 请求头缺失
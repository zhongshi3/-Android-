# 统一Activity顶部栏风格

## 目标
确保所有Activity的顶部栏风格一致：
1. 隐藏系统ActionBar/TitleBar
2. 正确处理状态栏边距
3. 使用自定义标题栏（如果需要）
4. 避免顶部栏重叠问题

## 已修改的Activity

### 1. TeacherMainActivity (教师工作台)
**修改内容**:
- 代码：添加`supportActionBar?.hide()`
- 代码：改进状态栏边距处理
- 布局：移除`android:paddingTop="60dp"`
- 布局：添加`android:layout_marginTop="40dp"`到标题栏

### 2. StudentMainActivity (学生主页)
**修改内容**:
- 代码：添加`supportActionBar?.hide()`
- 代码：改进状态栏边距处理
- 布局：移除`android:paddingTop="60dp"`
- 布局：添加`android:layout_marginTop="40dp"`到标题栏

### 3. MainActivity (登录页面)
**修改内容**:
- 代码：添加`supportActionBar?.hide()`
- 布局：已有`android:layout_marginTop="80dp"`，无需修改

### 4. TeacherRegisterActivity (教师注册页面)
**修改内容**:
- 代码：添加`supportActionBar?.hide()`

## 统一的修复模式

### 代码修复模式（每个Activity）
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 隐藏ActionBar/TitleBar
    supportActionBar?.hide()
    
    enableEdgeToEdge()
    setContentView(R.layout.activity_xxx)
    
    // ... 其他初始化代码
    
    // 状态栏边距处理
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container)) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(
            systemBars.left, 
            systemBars.top,  // 使用状态栏高度作为顶部padding
            systemBars.right, 
            systemBars.bottom
        )
        insets
    }
}
```

### 布局修复模式
1. **移除硬编码的顶部padding**：不要使用`android:paddingTop`
2. **使用状态栏边距**：通过代码动态设置
3. **自定义标题栏**：如果需要标题，使用自定义TextView
4. **顶部边距**：为标题栏添加适当的`layout_marginTop`

## 主题配置修改
**修改前**:
```xml
<style name="Base.Theme.MyApplication" parent="Theme.AppCompat.Light.DarkActionBar">
```

**修改后**:
```xml
<style name="Base.Theme.MyApplication" parent="Theme.Material3.DayNight.NoActionBar">
```

## 优势
1. **一致性**：所有Activity都有相同的顶部栏风格
2. **现代化**：使用Material 3无ActionBar主题
3. **兼容性**：正确处理不同设备的状态栏高度
4. **灵活性**：可以自由设计自定义标题栏

## 其他需要检查的Activity
根据项目结构，以下Activity可能也需要类似修复：
- `KnowledgeBaseActivity` (知识库)
- `QuestionBankActivity` (习题库)
- `ErrorBookActivity` (错题本)
- `UserProfileActivity` (用户信息)
- 其他功能页面

## 验证方法
1. 重新编译并运行应用
2. 测试每个Activity：
   - 检查系统状态栏是否正常显示
   - 检查是否有重复的标题显示
   - 检查顶部内容是否与状态栏重叠
   - 检查布局边距是否合适

## 注意事项
1. Material 3主题可能需要相应的依赖支持
2. 某些页面可能需要保留ActionBar（如设置页面）
3. 夜间主题已经配置为无ActionBar
4. 如果使用自定义主题颜色，需要相应调整
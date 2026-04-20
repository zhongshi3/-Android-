package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * 班级数据库管理类
 * 管理班级信息和学生班级关系
 */
class ClassDBHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    
    companion object {
        // 数据库信息
        private const val DB_NAME = "class_management.db"
        private const val DB_VERSION = 1
        
        // 班级表
        const val TABLE_CLASS = "class_table"
        const val COLUMN_CLASS_ID = "class_id"
        const val COLUMN_CLASS_NAME = "class_name"
        const val COLUMN_TEACHER_ID = "teacher_id"
        const val COLUMN_CREATE_TIME = "create_time"
        const val COLUMN_STUDENT_COUNT = "student_count"
        
        // 学生班级关系表
        const val TABLE_STUDENT_CLASS = "student_class_table"
        const val COLUMN_STUDENT_ID = "student_id"
        const val COLUMN_JOIN_TIME = "join_time"
        
        // SQL语句
        private const val CREATE_CLASS_TABLE = """
            CREATE TABLE $TABLE_CLASS (
                $COLUMN_CLASS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CLASS_NAME TEXT NOT NULL,
                $COLUMN_TEACHER_ID INTEGER NOT NULL,
                $COLUMN_CREATE_TIME INTEGER NOT NULL,
                $COLUMN_STUDENT_COUNT INTEGER DEFAULT 0
            )
        """
        
        private const val CREATE_STUDENT_CLASS_TABLE = """
            CREATE TABLE $TABLE_STUDENT_CLASS (
                $COLUMN_STUDENT_ID INTEGER NOT NULL,
                $COLUMN_CLASS_ID INTEGER NOT NULL,
                $COLUMN_JOIN_TIME INTEGER NOT NULL,
                PRIMARY KEY ($COLUMN_STUDENT_ID, $COLUMN_CLASS_ID)
            )
        """
        
        // 索引
        private const val CREATE_TEACHER_INDEX = """
            CREATE INDEX idx_teacher_id ON $TABLE_CLASS($COLUMN_TEACHER_ID)
        """
        
        private const val CREATE_STUDENT_INDEX = """
            CREATE INDEX idx_student_id ON $TABLE_STUDENT_CLASS($COLUMN_STUDENT_ID)
        """
        
        private const val CREATE_CLASS_INDEX = """
            CREATE INDEX idx_class_id ON $TABLE_STUDENT_CLASS($COLUMN_CLASS_ID)
        """
    }
    
    /**
     * 班级数据类
     */
    data class ClassInfo(
        val classId: Long,
        val className: String,
        val teacherId: Long,
        val createTime: Long,
        val studentCount: Int
    )
    
    /**
     * 学生班级关系数据类
     */
    data class StudentClass(
        val studentId: Long,
        val classId: Long,
        val joinTime: Long
    )
    
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_CLASS_TABLE)
        db.execSQL(CREATE_STUDENT_CLASS_TABLE)
        db.execSQL(CREATE_TEACHER_INDEX)
        db.execSQL(CREATE_STUDENT_INDEX)
        db.execSQL(CREATE_CLASS_INDEX)
        Log.d("ClassDBHelper", "数据库表创建完成")
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CLASS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STUDENT_CLASS")
        onCreate(db)
    }
    
    /**
     * 创建新班级
     * @param className 班级名称
     * @param teacherId 教师ID
     * @return 新创建的班级ID，-1表示失败
     */
    fun createClass(className: String, teacherId: Long): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CLASS_NAME, className)
            put(COLUMN_TEACHER_ID, teacherId)
            put(COLUMN_CREATE_TIME, System.currentTimeMillis())
            put(COLUMN_STUDENT_COUNT, 0)
        }
        
        val classId = db.insert(TABLE_CLASS, null, values)
        db.close()
        
        if (classId != -1L) {
            Log.d("ClassDBHelper", "创建班级成功: $className, ID: $classId")
        } else {
            Log.e("ClassDBHelper", "创建班级失败: $className")
        }
        
        return classId
    }
    
    /**
     * 删除班级
     * @param classId 班级ID
     * @return 是否成功
     */
    fun deleteClass(classId: Long): Boolean {
        val db = writableDatabase
        
        // 先删除学生班级关系
        db.delete(TABLE_STUDENT_CLASS, "$COLUMN_CLASS_ID = ?", arrayOf(classId.toString()))
        
        // 再删除班级
        val rowsAffected = db.delete(TABLE_CLASS, "$COLUMN_CLASS_ID = ?", arrayOf(classId.toString()))
        db.close()
        
        val success = rowsAffected > 0
        if (success) {
            Log.d("ClassDBHelper", "删除班级成功: $classId")
        } else {
            Log.e("ClassDBHelper", "删除班级失败: $classId")
        }
        
        return success
    }
    
    /**
     * 根据教师ID获取所有班级
     * @param teacherId 教师ID
     * @return 班级列表
     */
    fun getClassesByTeacher(teacherId: Long): List<ClassInfo> {
        val db = readableDatabase
        val classes = mutableListOf<ClassInfo>()
        
        val cursor = db.query(
            TABLE_CLASS,
            null,
            "$COLUMN_TEACHER_ID = ?",
            arrayOf(teacherId.toString()),
            null, null,
            "$COLUMN_CREATE_TIME DESC"
        )
        
        while (cursor.moveToNext()) {
            classes.add(cursorToClassInfo(cursor))
        }
        
        cursor.close()
        db.close()
        
        Log.d("ClassDBHelper", "获取教师($teacherId)的班级数量: ${classes.size}")
        return classes
    }
    
    /**
     * 根据班级ID获取班级信息
     * @param classId 班级ID
     * @return 班级信息，null表示不存在
     */
    fun getClassById(classId: Long): ClassInfo? {
        val db = readableDatabase
        
        val cursor = db.query(
            TABLE_CLASS,
            null,
            "$COLUMN_CLASS_ID = ?",
            arrayOf(classId.toString()),
            null, null, null
        )
        
        val classInfo = if (cursor.moveToFirst()) {
            cursorToClassInfo(cursor)
        } else {
            null
        }
        
        cursor.close()
        db.close()
        
        return classInfo
    }
    
    /**
     * 添加学生到班级
     * @param studentId 学生ID
     * @param classId 班级ID
     * @return 是否成功
     */
    fun addStudentToClass(studentId: Long, classId: Long): Boolean {
        val db = writableDatabase
        
        // 检查是否已存在
        val existsCursor = db.query(
            TABLE_STUDENT_CLASS,
            null,
            "$COLUMN_STUDENT_ID = ? AND $COLUMN_CLASS_ID = ?",
            arrayOf(studentId.toString(), classId.toString()),
            null, null, null
        )
        
        if (existsCursor.count > 0) {
            existsCursor.close()
            db.close()
            Log.w("ClassDBHelper", "学生($studentId)已存在于班级($classId)")
            return false
        }
        
        existsCursor.close()
        
        // 插入学生班级关系
        val values = ContentValues().apply {
            put(COLUMN_STUDENT_ID, studentId)
            put(COLUMN_CLASS_ID, classId)
            put(COLUMN_JOIN_TIME, System.currentTimeMillis())
        }
        
        val result = db.insert(TABLE_STUDENT_CLASS, null, values)
        
        // 更新班级学生数量
        if (result != -1L) {
            db.execSQL(
                "UPDATE $TABLE_CLASS SET $COLUMN_STUDENT_COUNT = $COLUMN_STUDENT_COUNT + 1 WHERE $COLUMN_CLASS_ID = ?",
                arrayOf(classId)
            )
        }
        
        db.close()
        
        val success = result != -1L
        if (success) {
            Log.d("ClassDBHelper", "添加学生($studentId)到班级($classId)成功")
        } else {
            Log.e("ClassDBHelper", "添加学生($studentId)到班级($classId)失败")
        }
        
        return success
    }
    
    /**
     * 从班级移除学生
     * @param studentId 学生ID
     * @param classId 班级ID
     * @return 是否成功
     */
    fun removeStudentFromClass(studentId: Long, classId: Long): Boolean {
        val db = writableDatabase
        
        // 删除学生班级关系
        val rowsAffected = db.delete(
            TABLE_STUDENT_CLASS,
            "$COLUMN_STUDENT_ID = ? AND $COLUMN_CLASS_ID = ?",
            arrayOf(studentId.toString(), classId.toString())
        )
        
        // 更新班级学生数量
        if (rowsAffected > 0) {
            db.execSQL(
                "UPDATE $TABLE_CLASS SET $COLUMN_STUDENT_COUNT = $COLUMN_STUDENT_COUNT - 1 WHERE $COLUMN_CLASS_ID = ? AND $COLUMN_STUDENT_COUNT > 0",
                arrayOf(classId)
            )
        }
        
        db.close()
        
        val success = rowsAffected > 0
        if (success) {
            Log.d("ClassDBHelper", "从班级($classId)移除学生($studentId)成功")
        } else {
            Log.e("ClassDBHelper", "从班级($classId)移除学生($studentId)失败")
        }
        
        return success
    }
    
    /**
     * 获取班级的学生列表
     * @param classId 班级ID
     * @return 学生ID列表
     */
    fun getStudentsInClass(classId: Long): List<Long> {
        val db = readableDatabase
        val students = mutableListOf<Long>()
        
        val cursor = db.query(
            TABLE_STUDENT_CLASS,
            arrayOf(COLUMN_STUDENT_ID),
            "$COLUMN_CLASS_ID = ?",
            arrayOf(classId.toString()),
            null, null,
            "$COLUMN_JOIN_TIME ASC"
        )
        
        while (cursor.moveToNext()) {
            val studentId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_ID))
            students.add(studentId)
        }
        
        cursor.close()
        db.close()
        
        Log.d("ClassDBHelper", "获取班级($classId)学生数量: ${students.size}")
        return students
    }
    
    /**
     * 获取学生所在的班级
     * @param studentId 学生ID
     * @return 班级ID列表
     */
    fun getClassesOfStudent(studentId: Long): List<Long> {
        val db = readableDatabase
        val classes = mutableListOf<Long>()
        
        val cursor = db.query(
            TABLE_STUDENT_CLASS,
            arrayOf(COLUMN_CLASS_ID),
            "$COLUMN_STUDENT_ID = ?",
            arrayOf(studentId.toString()),
            null, null, null
        )
        
        while (cursor.moveToNext()) {
            val classId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CLASS_ID))
            classes.add(classId)
        }
        
        cursor.close()
        db.close()
        
        return classes
    }
    
    /**
     * 获取班级统计信息
     * @param classId 班级ID
     * @return 包含班级信息和学生数量的Map
     */
    fun getClassStatistics(classId: Long): Map<String, Any?> {
        val classInfo = getClassById(classId)
        val students = getStudentsInClass(classId)
        
        return mapOf(
            "classInfo" to classInfo,
            "studentCount" to students.size,
            "studentIds" to students
        )
    }
    
    /**
     * 更新班级名称
     * @param classId 班级ID
     * @param newClassName 新班级名称
     * @return 是否成功
     */
    fun updateClassName(classId: Long, newClassName: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CLASS_NAME, newClassName)
        }
        
        val rowsAffected = db.update(
            TABLE_CLASS,
            values,
            "$COLUMN_CLASS_ID = ?",
            arrayOf(classId.toString())
        )
        
        db.close()
        
        val success = rowsAffected > 0
        if (success) {
            Log.d("ClassDBHelper", "更新班级($classId)名称为: $newClassName")
        } else {
            Log.e("ClassDBHelper", "更新班级($classId)名称失败")
        }
        
        return success
    }
    
    /**
     * 获取所有班级（管理员功能）
     * @return 所有班级列表
     */
    fun getAllClasses(): List<ClassInfo> {
        val db = readableDatabase
        val classes = mutableListOf<ClassInfo>()
        
        val cursor = db.query(
            TABLE_CLASS,
            null,
            null, null,
            null, null,
            "$COLUMN_CREATE_TIME DESC"
        )
        
        while (cursor.moveToNext()) {
            classes.add(cursorToClassInfo(cursor))
        }
        
        cursor.close()
        db.close()
        
        return classes
    }
    
    /**
     * 从Cursor转换为ClassInfo
     */
    private fun cursorToClassInfo(cursor: Cursor): ClassInfo {
        return ClassInfo(
            classId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CLASS_ID)),
            className = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_NAME)),
            teacherId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TEACHER_ID)),
            createTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATE_TIME)),
            studentCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_COUNT))
        )
    }
}
package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// 导入 CloudApiHelper 中的 StudentAnswer 数据类
import com.example.myapplication.CloudApiHelper.StudentAnswer

/**
 * 答题状态本地缓存
 * 用于缓存云端的答题状态，避免每次都请求云端
 */
class AnswerCacheHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "answer_cache.db"
        private const val DB_VERSION = 1
        const val TABLE_NAME = "answer_cache"

        // 列名常量
        const val COLUMN_ID = "id"
        const val COLUMN_Q_ID = "q_id"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_STUDENT_ANSWER = "student_answer"
        const val COLUMN_STATUS = "status"  // 0未批改/1正确/2错误
        const val COLUMN_TEACHER_MSG = "teacher_msg"
        const val COLUMN_UPDATE_TIME = "update_time"

        // 状态常量
        const val STATUS_NOT_CHECKED = 0  // 未批改
        const val STATUS_CORRECT = 1      // 正确
        const val STATUS_WRONG = 2        // 错误
    }

    private val createTableSql = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_Q_ID INTEGER NOT NULL,
            $COLUMN_USER_ID INTEGER NOT NULL,
            $COLUMN_STUDENT_ANSWER TEXT,
            $COLUMN_STATUS INTEGER DEFAULT 0,
            $COLUMN_TEACHER_MSG TEXT,
            $COLUMN_UPDATE_TIME INTEGER DEFAULT 0,
            UNIQUE($COLUMN_Q_ID, $COLUMN_USER_ID)
        )
    """.trimIndent()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(createTableSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 目前只有版本1，暂不处理升级
    }

    /**
     * 批量保存答题状态
     */
    fun saveAnswers(userId: Long, answers: Map<Int, StudentAnswer>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((qId, answer) in answers) {
                val values = ContentValues().apply {
                    put(COLUMN_Q_ID, qId)
                    put(COLUMN_USER_ID, userId)
                    put(COLUMN_STUDENT_ANSWER, answer.studentAnswer)
                    put(COLUMN_STATUS, answer.status)
                    put(COLUMN_TEACHER_MSG, answer.teacherMsg)
                    put(COLUMN_UPDATE_TIME, System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * 获取指定用户的答题状态缓存
     */
    fun getAnswers(userId: Long): Map<Int, StudentAnswer> {
        val answers = mutableMapOf<Int, StudentAnswer>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            "$COLUMN_Q_ID ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val qId = it.getInt(it.getColumnIndexOrThrow(COLUMN_Q_ID))
                val studentAnswer = it.getString(it.getColumnIndexOrThrow(COLUMN_STUDENT_ANSWER))
                val status = it.getInt(it.getColumnIndexOrThrow(COLUMN_STATUS))
                val teacherMsg = it.getString(it.getColumnIndexOrThrow(COLUMN_TEACHER_MSG))

                answers[qId] = StudentAnswer(
                    qId = qId,
                    studentAnswer = studentAnswer ?: "",
                    status = status,
                    teacherMsg = teacherMsg
                )
            }
        }
        return answers
    }

    /**
     * 获取单个题目的答题状态
     */
    fun getAnswer(userId: Long, qId: Int): StudentAnswer? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_USER_ID = ? AND $COLUMN_Q_ID = ?",
            arrayOf(userId.toString(), qId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                val studentAnswer = it.getString(it.getColumnIndexOrThrow(COLUMN_STUDENT_ANSWER))
                val status = it.getInt(it.getColumnIndexOrThrow(COLUMN_STATUS))
                val teacherMsg = it.getString(it.getColumnIndexOrThrow(COLUMN_TEACHER_MSG))

                return StudentAnswer(
                    qId = qId,
                    studentAnswer = studentAnswer ?: "",
                    status = status,
                    teacherMsg = teacherMsg
                )
            }
        }
        return null
    }

    /**
     * 清除指定用户的缓存
     */
    fun clearCache(userId: Long) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_USER_ID = ?", arrayOf(userId.toString()))
    }
    
    /**
     * 清空所有缓存
     */
    fun clearAll() {
        val db = writableDatabase
        db.delete(TABLE_NAME, null, null)
    }
}

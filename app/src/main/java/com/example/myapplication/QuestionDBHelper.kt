package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class QuestionDBHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "question_bank.db"
        private const val DB_VERSION = 6  // 版本升级：新增 is_deleted 字段
        const val TABLE_NAME = "questions"

        // 列名常量
        const val COLUMN_ID = "id"
        const val COLUMN_QUESTION_NUMBER = "question_number"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_ANSWER = "answer"
        const val COLUMN_SECTION_ID = "section_id"
        const val COLUMN_IMAGE_URL = "image_url"
        const val COLUMN_IN_ERROR_BOOK = "in_error_book"  // 是否在错题库中
        const val COLUMN_IS_DELETED = "is_deleted"  // 是否已删除
    }

    private val createTableSql = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_QUESTION_NUMBER INTEGER NOT NULL UNIQUE,
            $COLUMN_CONTENT TEXT NOT NULL,
            $COLUMN_ANSWER TEXT NOT NULL,
            $COLUMN_SECTION_ID INTEGER DEFAULT 0,
            $COLUMN_IMAGE_URL TEXT,
            $COLUMN_IN_ERROR_BOOK INTEGER DEFAULT 0,
            $COLUMN_IS_DELETED INTEGER DEFAULT 0
        );
    """.trimIndent()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(createTableSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        when (oldVersion) {
            1, 2, 3 -> {
                // 版本4：新增image_url列
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN image_url TEXT")
                // 版本5：删除学生答案、状态字段（重建表）
                db.execSQL("CREATE TEMPORARY TABLE backup_questions AS SELECT id, question_number, content, answer, section_id, image_url, in_error_book FROM $TABLE_NAME")
                db.execSQL("DROP TABLE $TABLE_NAME")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                        $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COLUMN_QUESTION_NUMBER INTEGER NOT NULL UNIQUE,
                        $COLUMN_CONTENT TEXT NOT NULL,
                        $COLUMN_ANSWER TEXT NOT NULL,
                        $COLUMN_SECTION_ID INTEGER DEFAULT 0,
                        $COLUMN_IMAGE_URL TEXT,
                        $COLUMN_IN_ERROR_BOOK INTEGER DEFAULT 0,
                        $COLUMN_IS_DELETED INTEGER DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO $TABLE_NAME (id, question_number, content, answer, section_id, image_url, in_error_book) SELECT id, question_number, content, answer, section_id, image_url, in_error_book FROM backup_questions")
                db.execSQL("DROP TABLE backup_questions")
            }
            4, 5 -> {
                // 版本6：新增 is_deleted 列
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_IS_DELETED INTEGER DEFAULT 0")
            }
        }
    }

    // 插入题目
    fun insertQuestion(
        questionNumber: Int,
        content: String,
        answer: String,
        sectionId: Int,
        imageUrl: String? = null
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_QUESTION_NUMBER, questionNumber)
            put(COLUMN_CONTENT, content)
            put(COLUMN_ANSWER, answer)
            put(COLUMN_SECTION_ID, sectionId)
            put(COLUMN_IMAGE_URL, imageUrl)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    // 批量插入题目
    fun insertQuestions(questions: List<QuestionEntity>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (question in questions) {
                val values = ContentValues().apply {
                    put(COLUMN_QUESTION_NUMBER, question.questionNumber)
                    put(COLUMN_CONTENT, question.content)
                    put(COLUMN_ANSWER, question.answer)
                    put(COLUMN_SECTION_ID, question.sectionId)
                    put(COLUMN_IMAGE_URL, question.imageUrl)
                }
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // 获取所有题目（包括已删除的）
    fun getAllQuestions(): List<QuestionEntity> {
        val questions = mutableListOf<QuestionEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_QUESTION_NUMBER ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                questions.add(cursorToQuestionEntity(it))
            }
        }
        return questions
    }

    // 根据题号获取题目
    fun getQuestionByNumber(questionNumber: Int): QuestionEntity? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_QUESTION_NUMBER = ?",
            arrayOf(questionNumber.toString()),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return cursorToQuestionEntity(it)
            }
        }
        return null
    }

    // 获取题目统计信息（只统计有效题目）
    fun getQuestionStats(): Map<String, Int> {
        val stats = mutableMapOf(
            "total" to 0,
            "errorBook" to 0
        )
        val db = readableDatabase
        
        // 统计总题数
        val totalCursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_NAME WHERE $COLUMN_IS_DELETED = 0",
            null
        )
        totalCursor.use {
            if (it.moveToFirst()) {
                stats["total"] = it.getInt(0)
            }
        }
        
        // 统计错题数量
        val errorCursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_NAME WHERE $COLUMN_IN_ERROR_BOOK = 1 AND $COLUMN_IS_DELETED = 0",
            null
        )
        errorCursor.use {
            if (it.moveToFirst()) {
                stats["errorBook"] = it.getInt(0)
            }
        }
        
        return stats
    }

    // 获取错题列表（过滤已删除的题目）
    fun getErrorBookQuestions(): List<QuestionEntity> {
        val questions = mutableListOf<QuestionEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_IN_ERROR_BOOK = ? AND $COLUMN_IS_DELETED = 0",
            arrayOf("1"),
            null,
            null,
            "$COLUMN_QUESTION_NUMBER ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                questions.add(cursorToQuestionEntity(it))
            }
        }
        return questions
    }

    // 添加到错题库
    fun addToErrorBook(questionNumber: Int): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IN_ERROR_BOOK, 1)
        }
        return db.update(TABLE_NAME, values, "$COLUMN_QUESTION_NUMBER = ?", arrayOf(questionNumber.toString()))
    }

    // 获取本地最大题号
    fun getLocalMaxQuestionNumber(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT MAX($COLUMN_QUESTION_NUMBER) FROM $TABLE_NAME WHERE $COLUMN_IS_DELETED = 0",
            null
        )
        var maxNumber = 0
        cursor.use {
            if (it.moveToFirst() && !it.isNull(0)) {
                maxNumber = it.getInt(0)
            }
        }
        return maxNumber
    }

    // 根据节号筛选获取所有有效题目（isDeleted为0）
    fun getAllValidQuestions(): List<QuestionEntity> {
        val questions = mutableListOf<QuestionEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_IS_DELETED = 0",
            null,
            null,
            null,
            "$COLUMN_QUESTION_NUMBER ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                questions.add(cursorToQuestionEntity(it))
            }
        }
        return questions
    }

    // 批量标记题目为已删除
    fun markQuestionsAsDeleted(questionNumbers: List<Int>): Int {
        if (questionNumbers.isEmpty()) return 0
        
        val db = writableDatabase
        val placeholders = questionNumbers.joinToString(",") { "?" }
        val selection = "$COLUMN_QUESTION_NUMBER IN ($placeholders)"
        val selectionArgs = questionNumbers.map { it.toString() }.toTypedArray()
        
        val values = ContentValues().apply {
            put(COLUMN_IS_DELETED, 1)
        }
        
        return db.update(TABLE_NAME, values, selection, selectionArgs)
    }

    // 插入或更新题目（用于同步）
    fun insertOrUpdateQuestion(
        questionNumber: Int,
        content: String,
        answer: String,
        sectionId: Int,
        imageUrl: String? = null
    ): Boolean {
        val db = writableDatabase
        val existingQuestion = getQuestionByNumber(questionNumber)
        
        return if (existingQuestion != null) {
            // 更新现有题目
            val values = ContentValues().apply {
                put(COLUMN_CONTENT, content)
                put(COLUMN_ANSWER, answer)
                put(COLUMN_SECTION_ID, sectionId)
                put(COLUMN_IMAGE_URL, imageUrl)
                // 保留原有的错题库标记
            }
            val rowsAffected = db.update(
                TABLE_NAME, 
                values, 
                "$COLUMN_QUESTION_NUMBER = ?", 
                arrayOf(questionNumber.toString())
            )
            rowsAffected > 0
        } else {
            // 插入新题目
            val values = ContentValues().apply {
                put(COLUMN_QUESTION_NUMBER, questionNumber)
                put(COLUMN_CONTENT, content)
                put(COLUMN_ANSWER, answer)
                put(COLUMN_SECTION_ID, sectionId)
                put(COLUMN_IMAGE_URL, imageUrl)
            }
            val result = db.insert(TABLE_NAME, null, values)
            result != -1L
        }
    }

    private fun cursorToQuestionEntity(cursor: android.database.Cursor): QuestionEntity {
        return QuestionEntity(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            questionNumber = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUESTION_NUMBER)),
            content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)),
            answer = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANSWER)),
            sectionId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SECTION_ID)),
            imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL)),
            inErrorBook = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IN_ERROR_BOOK)) == 1,
            isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DELETED)) == 1
        )
    }
}

// 数据实体类
data class QuestionEntity(
    val id: Int = 0,
    val questionNumber: Int,
    val content: String,
    val answer: String,
    val sectionId: Int = 0,
    val imageUrl: String? = null,
    val inErrorBook: Boolean = false,
    val isDeleted: Boolean = false  // 是否已删除（使用isDeleted标记）
)
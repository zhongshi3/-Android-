package com.example.myapplication

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 知识图谱数据库帮助类
 * 
 * 数据库结构：
 * - parts: 部分表 (id, name)
 * - chapters: 章表 (id, part_id, number, name)
 * - sections: 节表 (id, chapter_id, number, name)
 * - knowledge_points: 知识点表 (id, section_id, number, name)
 * - prerequisite_relations: 前置关系表 (id, prerequisite_id, target_id)
 */
class KnowledgeGraphDBHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "knowledge_graph_v2.db"
        private const val DB_VERSION = 3  // 版本3：添加图论部分
    }

    // ==================== 表结构定义 ====================
    
    // 部分表
    private val createPartsTable = """
        CREATE TABLE IF NOT EXISTS parts (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL
        );
    """.trimIndent()

    // 章表
    private val createChaptersTable = """
        CREATE TABLE IF NOT EXISTS chapters (
            id INTEGER PRIMARY KEY,
            part_id INTEGER NOT NULL,
            number TEXT NOT NULL,
            name TEXT NOT NULL,
            FOREIGN KEY (part_id) REFERENCES parts(id)
        );
    """.trimIndent()

    // 节表
    private val createSectionsTable = """
        CREATE TABLE IF NOT EXISTS sections (
            id INTEGER PRIMARY KEY,
            chapter_id INTEGER NOT NULL,
            number TEXT NOT NULL,
            name TEXT NOT NULL,
            FOREIGN KEY (chapter_id) REFERENCES chapters(id)
        );
    """.trimIndent()

    // 知识点表
    private val createKnowledgePointsTable = """
        CREATE TABLE IF NOT EXISTS knowledge_points (
            id INTEGER PRIMARY KEY,
            section_id INTEGER NOT NULL,
            number TEXT NOT NULL,
            name TEXT NOT NULL,
            flag INTEGER DEFAULT 0,
            FOREIGN KEY (section_id) REFERENCES sections(id)
        );
    """.trimIndent()

    // 前置关系表（知识点之间的前置关系）
    private val createPrerequisiteRelationsTable = """
        CREATE TABLE IF NOT EXISTS prerequisite_relations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            prerequisite_id INTEGER NOT NULL,
            target_id INTEGER NOT NULL,
            FOREIGN KEY (prerequisite_id) REFERENCES knowledge_points(id),
            FOREIGN KEY (target_id) REFERENCES knowledge_points(id)
        );
    """.trimIndent()

    // ==================== 数据模型 ====================
    
    data class Part(val id: Int, val name: String)
    data class Chapter(val id: Int, val partId: Int, val number: String, val name: String)
    data class Section(val id: Int, val chapterId: Int, val number: String, val name: String)
    data class KnowledgePoint(val id: Int, val sectionId: Int, val number: String, val name: String, val flag: Int = 0)
    data class PrerequisiteRelation(val id: Int, val prerequisiteId: Int, val targetId: Int)

    // ==================== 数据库生命周期 ====================
    
    override fun onCreate(db: SQLiteDatabase) {
        // 创建表
        db.execSQL(createPartsTable)
        db.execSQL(createChaptersTable)
        db.execSQL(createSectionsTable)
        db.execSQL(createKnowledgePointsTable)
        db.execSQL(createPrerequisiteRelationsTable)
        
        // 插入初始数据
        insertInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // 版本2：添加flag字段
            db.execSQL("ALTER TABLE knowledge_points ADD COLUMN flag INTEGER DEFAULT 0")
        }
        if (oldVersion < 3) {
            // 版本3：添加图论部分
            insertGraphTheoryData(db)
        }
    }
    
    /**
     * 插入图论部分数据（供onUpgrade和onCreate共用）
     */
    private fun insertGraphTheoryData(db: SQLiteDatabase) {
        // 部分表
        db.execSQL("INSERT INTO parts (id, name) VALUES (4, '图论')")
        
        // 章表
        db.execSQL("INSERT INTO chapters (id, part_id, number, name) VALUES (8, 4, '8', '图')")
        db.execSQL("INSERT INTO chapters (id, part_id, number, name) VALUES (9, 4, '9', '树')")
        db.execSQL("INSERT INTO chapters (id, part_id, number, name) VALUES (10, 4, '10', '平面图')")
        
        // 节表 (从id=34开始)
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (34, 8, '8.1', '图的定义')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (35, 8, '8.2', '图的相关概念')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (36, 8, '8.3', '图的连通性')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (37, 8, '8.4', '图的运算')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (38, 8, '8.5', '欧拉图与哈密顿图')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (39, 8, '8.6', '最短路径与旅行商问题')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (40, 9, '9.1', '树')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (41, 9, '9.2', '生成树')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (42, 9, '9.3', '根树')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (43, 10, '10.1', '平面图的基本概念')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (44, 10, '10.2', '欧拉公式')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (45, 10, '10.3', '平面图的判断')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (46, 10, '10.4', '对偶图')")
        
        // 知识点表 (从id=138开始)
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (138, 34, '138', '无序对')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (139, 34, '139', '有向图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (140, 34, '140', '无向图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (141, 35, '141', '图的基本概念')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (142, 35, '142', '基图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (143, 35, '143', '相邻与关联')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (144, 35, '144', '重数、简单图和多重图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (145, 35, '145', '度数')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (146, 35, '146', '握手定理')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (147, 35, '147', '图的表示方法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (148, 36, '148', '路径')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (149, 36, '149', '通路')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (150, 36, '150', '回路')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (151, 36, '151', '连通分支与割集')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (152, 36, '152', '连通图及其判别定理')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (153, 37, '153', '并图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (154, 37, '154', '差图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (155, 37, '155', '交图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (156, 37, '156', '环和')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (157, 38, '157', '欧拉图和半欧拉图的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (158, 38, '158', '欧拉图的充要条件')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (159, 38, '159', 'Fleury算法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (160, 38, '160', '哈密顿图和半哈密顿图的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (161, 38, '161', '哈密顿图的必要条件')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (162, 38, '162', '哈密顿图的充分条件')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (163, 39, '163', '带权图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (164, 39, '164', 'Dijkstra算法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (165, 39, '165', '旅行商问题')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (166, 40, '166', '树的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (167, 40, '167', '树的基本概念')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (168, 40, '168', '树的性质')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (169, 41, '169', '生成树的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (170, 41, '170', '破圈法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (171, 41, '171', '最小生成树')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (172, 41, '172', 'Kruskal算法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (173, 41, '173', 'Prim算法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (174, 42, '174', '根树')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (175, 42, '175', '有序树、正则树与完全树')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (176, 42, '176', 'Huffman算法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (177, 42, '177', '二叉树遍历法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (178, 43, '178', '平面图与平面嵌入')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (179, 43, '179', '平面图的性质')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (180, 43, '180', '极小非平面图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (181, 44, '181', '欧拉公式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (182, 44, '182', '欧拉公式的推论')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (183, 45, '183', '插入与消去2度顶点')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (184, 45, '184', '同胚')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (185, 45, '185', '平面图的两个充要条件')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (186, 46, '186', '对偶图的概念')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (187, 46, '187', '对偶图的性质')")
        
        // 前置关系表 - 图论部分
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (48, 138)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (138, 139)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (138, 140)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (139, 141)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (140, 141)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (141, 142)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (141, 143)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (143, 144)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (143, 145)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (145, 146)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (143, 147)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (146, 148)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (148, 149)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (148, 150)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (149, 151)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (151, 152)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (141, 153)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (141, 154)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (141, 155)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (141, 156)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (152, 157)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (157, 158)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (158, 159)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (152, 160)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (160, 161)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (160, 162)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (152, 163)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (163, 164)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (163, 165)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (152, 166)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (166, 167)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (167, 168)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (168, 169)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (169, 170)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (170, 171)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (171, 172)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (171, 173)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (168, 174)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (174, 175)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (175, 176)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (175, 177)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (152, 178)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (178, 179)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (179, 180)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (179, 181)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (181, 182)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (179, 183)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (183, 184)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (182, 185)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (184, 185)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (179, 186)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (182, 187)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (186, 187)")
    }

    // ==================== 初始数据 ====================
    
    private fun insertInitialData(db: SQLiteDatabase) {
        // ========== 部分表 ==========
        db.execSQL("INSERT INTO parts (id, name) VALUES (1, '数理逻辑')")
        db.execSQL("INSERT INTO parts (id, name) VALUES (2, '集合论')")
        db.execSQL("INSERT INTO parts (id, name) VALUES (3, '代数系统')")
        
        // ========== 章表 ==========
        // 数理逻辑
        db.execSQL("INSERT INTO chapters (id, part_id, number, name) VALUES (1, 1, '1', '命题逻辑')")
        db.execSQL("INSERT INTO chapters (id, part_id, number, name) VALUES (2, 1, '2', '谓词逻辑')")
        // 集合论
        db.execSQL("INSERT INTO chapters (id, part_id, number, name) VALUES (3, 2, '3', '集合')")
        db.execSQL("INSERT INTO chapters (id, part_id, number, name) VALUES (4, 2, '4', '二元关系')")
        db.execSQL("INSERT INTO chapters (id, part_id, number, name) VALUES (5, 2, '5', '函数')")
        // 代数系统
        db.execSQL("INSERT INTO chapters (id, part_id, number, name) VALUES (6, 3, '6', '代数系统')")
        db.execSQL("INSERT INTO chapters (id, part_id, number, name) VALUES (7, 3, '7', '格与布尔代数')")
        
        // ========== 节表 ==========
        // 1. 命题逻辑
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (1, 1, '1.1', '命题的符号化')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (2, 1, '1.2', '命题公式')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (3, 1, '1.3', '命题逻辑的等值演算')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (4, 1, '1.4', '范式')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (5, 1, '1.5', '联结词的完备集')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (6, 1, '1.6', '消解法')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (7, 1, '1.7', '命题逻辑的推理')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (8, 1, '1.8', '自然推理系统P')")
        // 2. 谓词逻辑
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (9, 2, '2.1', '一阶命题符号化')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (10, 2, '2.2', '谓词公式')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (11, 2, '2.3', '谓词逻辑的演算')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (12, 2, '2.4', '谓词逻辑的推理')")
        // 3. 集合
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (13, 3, '3.1', '集合的概念')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (14, 3, '3.2', '集合的运算')")
        // 4. 二元关系
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (15, 4, '4.1', '有序对与笛卡尔积')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (16, 4, '4.2', '二元关系')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (17, 4, '4.3', '关系的运算')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (18, 4, '4.4', '关系的性质')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (19, 4, '4.5', '关系的闭包')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (20, 4, '4.6', '等价关系与划分')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (21, 4, '4.7', '偏序关系')")
        // 5. 函数
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (22, 5, '5.1', '函数的概念')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (23, 5, '5.2', '集合的势')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (24, 5, '5.3', '集合的基数')")
        // 6. 代数系统
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (25, 6, '6.1', '二元运算')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (26, 6, '6.2', '代数系统')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (27, 6, '6.3', '群')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (28, 6, '6.4', '子群')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (29, 6, '6.5', '循环群与置换群')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (30, 6, '6.6', '环和域')")
        // 7. 格与布尔代数
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (31, 7, '7.1', '格')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (32, 7, '7.2', '特殊的格')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (33, 7, '7.3', '布尔代数')")
        
        // ========== 知识点表 ==========
        // 1.1 命题的符号化
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (1, 1, '1', '原子命题')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (2, 1, '2', '联结词')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (3, 1, '3', '复合命题')")
        // 1.2 命题公式
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (4, 2, '4', '真值表')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (5, 2, '5', '命题变项')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (6, 2, '6', '命题公式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (7, 2, '7', '公式的类型')")
        // 1.3 命题逻辑的等值演算
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (8, 3, '8', '等值式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (9, 3, '9', '等值式模式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (10, 3, '10', '置换规则')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (11, 3, '11', '等值演算')")
        // 1.4 范式
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (12, 4, '12', '简单析取式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (13, 4, '13', '简单合取式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (14, 4, '14', '析取范式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (15, 4, '15', '合取范式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (16, 4, '16', '主析取范式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (17, 4, '17', '主合取范式')")
        // 1.5 联结词的完备集
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (18, 5, '18', '真值函数')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (19, 5, '19', '联结词完备集')")
        // 1.6 消解法
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (20, 6, '20', '消解规则')")
        // 1.7 命题逻辑的推理
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (21, 7, '21', '推理')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (22, 7, '22', '推理的形式结构')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (23, 7, '23', '推理定律')")
        // 1.8 自然推理系统P
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (24, 8, '24', '推理规则')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (25, 8, '25', '自然推理系统P')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (26, 8, '26', '证明')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (27, 8, '27', '附加前提法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (28, 8, '28', '归谬法')")
        // 2.1 一阶命题符号化
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (29, 9, '29', '个体词')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (30, 9, '30', '谓词')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (31, 9, '31', '量词')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (32, 9, '32', '一阶命题符号化')")
        // 2.2 谓词公式
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (33, 10, '33', '一阶语言L')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (34, 10, '34', '原子公式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (35, 10, '35', '谓词公式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (36, 10, '36', '自由出现和约束出现')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (37, 10, '37', '闭式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (38, 10, '38', '解释')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (39, 10, '39', '公式的类型')")
        // 2.3 谓词逻辑的演算
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (40, 11, '40', '等值式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (41, 11, '41', '基本等值式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (42, 11, '42', '等值演算的三条规则')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (43, 11, '43', '前束范式')")
        // 2.4 谓词逻辑的推理
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (44, 12, '44', '推理')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (45, 12, '45', '三组推理定律')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (46, 12, '46', '四条推理规则')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (47, 12, '47', '自然推理系统N')")
        // 3.1 集合的概念
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (48, 13, '48', '集合和元素')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (49, 13, '49', '集合间的关系')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (50, 13, '50', '空集、幂集和全集')")
        // 3.2 集合的运算
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (51, 14, '51', '集合的四种基本运算')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (52, 14, '52', '集合运算的顺序')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (53, 14, '53', '文氏图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (54, 14, '54', '包含排斥原理')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (55, 14, '55', '集合恒等式')")
        // 4.1 有序对与笛卡尔积
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (56, 15, '56', '有序对')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (57, 15, '57', '笛卡尔积')")
        // 4.2 二元关系
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (58, 16, '58', '集合上的二元关系')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (59, 16, '59', '三种特殊的关系')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (60, 16, '60', '常用关系')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (61, 16, '61', '关系的三种表示方法')")
        // 4.3 关系的运算
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (62, 17, '62', '定义域')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (63, 17, '63', '值域')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (64, 17, '64', '逆')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (65, 17, '65', '右复合与左复合')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (66, 17, '66', '限制')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (67, 17, '67', '像')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (68, 17, '68', '基本运算的性质')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (69, 17, '69', '幂运算')")
        // 4.4 关系的性质
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (70, 18, '70', '自反性与反自反性')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (71, 18, '71', '对称性与反对称性')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (72, 18, '72', '传递性')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (73, 18, '73', '五种性质成立的充要条件')")
        // 4.5 关系的闭包
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (74, 19, '74', '闭包的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (75, 19, '75', '闭包的构造方法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (76, 19, '76', '闭包的性质')")
        // 4.6 等价关系与划分
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (77, 20, '77', '等价关系')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (78, 20, '78', '等价类')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (79, 20, '79', '商集')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (80, 20, '80', '划分')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (81, 20, '81', '划分与商集的关系')")
        // 4.7 偏序关系
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (82, 21, '82', '偏序关系')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (83, 21, '83', '可比与不可比')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (84, 21, '84', '全序关系')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (85, 21, '85', '偏序集')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (86, 21, '86', '覆盖')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (87, 21, '87', '偏序集的哈斯图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (88, 21, '88', '最大、最小、极大、极小元')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (89, 21, '89', '上界、下届、上确界、下确界')")
        // 5.1 函数的概念
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (90, 22, '90', '函数的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (91, 22, '91', '单射，满射和双射')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (92, 22, '92', '函数的复合')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (93, 22, '93', '反函数')")
        // 5.2 集合的势
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (94, 23, '94', '等势')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (95, 23, '95', '等势的性质')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (96, 23, '96', '康托定理')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (97, 23, '97', '优势')")
        // 5.3 集合的基数
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (98, 24, '98', '有穷集和无穷集')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (99, 24, '99', '基数')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (100, 24, '100', '可数集')")
        // 6.1 二元运算
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (101, 25, '101', '二元运算的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (102, 25, '102', '一元运算')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (103, 25, '103', '二元运算的六个律')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (104, 25, '104', '单位元和零元')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (105, 25, '105', '逆元')")
        // 6.2 代数系统
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (106, 26, '106', '代数系统的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (107, 26, '107', '同类型')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (108, 26, '108', '子代数')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (109, 26, '109', '积代数')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (110, 26, '110', '同态和同构')")
        // 6.3 群
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (111, 27, '111', '群和半群的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (112, 27, '112', '群的相关概念')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (113, 27, '113', '特殊群')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (114, 27, '114', '群的重要性质')")
        // 6.4 子群
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (115, 28, '115', '子群')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (116, 28, '116', '子群的三个判定定理')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (117, 28, '117', '生成子群与中心')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (118, 28, '118', '右陪集和代表元素')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (119, 28, '119', '右陪集的性质')")
        // 6.5 循环群与置换群
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (120, 29, '120', '循环群和生成元')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (121, 29, '121', '循环群的分类')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (122, 29, '122', '循环群生成元和子群求法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (123, 29, '123', '置换元和置换的乘法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (124, 29, '124', '置换的性质')")
        // 6.6 环和域
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (125, 30, '125', '环的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (126, 30, '126', '环的运算性质')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (127, 30, '127', '整环和域')")
        // 7.1 格
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (128, 31, '128', '格的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (129, 31, '129', '对偶原理')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (130, 31, '130', '格的性质')")
        // 7.2 特殊的格
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (131, 32, '131', '分配格')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (132, 32, '132', '有界格')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (133, 32, '133', '补元和有补格')")
        // 7.3 布尔代数
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (134, 33, '134', '布尔代数的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (135, 33, '135', '布尔代数的性质')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (136, 33, '136', '原子')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (137, 33, '137', '有限布尔代数的表示定理')")
        
        // ========== 前置关系表 ==========
        // 1.1 命题的符号化
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (1, 3)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (2, 3)")
        // 1.2 命题公式
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (5, 6)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (2, 6)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (6, 4)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (4, 7)")
        // 1.3 命题逻辑的等值演算
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (6, 8)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (7, 8)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (8, 9)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (6, 10)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (9, 11)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (10, 11)")
        // 1.4 范式
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (2, 12)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (5, 12)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (2, 13)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (5, 13)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (12, 14)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (11, 14)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (13, 15)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (11, 15)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (14, 16)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (4, 16)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (15, 17)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (4, 17)")
        // 1.5 联结词的完备集
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (5, 18)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (16, 18)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (17, 18)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (18, 19)")
        // 1.6 消解法
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (12, 20)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (17, 20)")
        // 1.7 命题逻辑的推理
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (7, 21)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (21, 22)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (16, 22)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (11, 22)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (22, 23)")
        // 1.8 自然推理系统P
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (21, 26)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (23, 24)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (24, 25)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (26, 25)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (25, 27)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (25, 28)")
        // 2.1 一阶命题符号化
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (29, 30)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (29, 31)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (30, 32)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (31, 32)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (3, 32)")
        // 2.2 谓词公式
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (32, 33)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (48, 33)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (33, 34)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (34, 35)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (35, 36)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (36, 37)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (35, 38)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (37, 39)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (38, 39)")
        // 2.3 谓词逻辑的演算
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (39, 40)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (40, 41)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (40, 42)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (42, 43)")
        // 2.4 谓词逻辑的推理
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (39, 44)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (44, 45)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (44, 46)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (46, 47)")
        // 3.1 集合的概念
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (48, 49)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (49, 50)")
        // 3.2 集合的运算
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (50, 51)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (51, 52)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (51, 53)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (51, 54)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (52, 55)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (11, 55)")
        // 4.1 有序对与笛卡尔积
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (50, 56)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (56, 57)")
        // 4.2 二元关系
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (56, 58)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (58, 59)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (58, 60)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (58, 61)")
        // 4.3 关系的运算
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (58, 62)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (58, 63)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (58, 64)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (58, 65)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (58, 66)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (58, 67)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (62, 68)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (63, 68)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (64, 68)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (65, 68)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (66, 68)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (67, 68)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (68, 69)")
        // 4.4 关系的性质
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (58, 70)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (58, 71)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (58, 72)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (70, 73)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (71, 73)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (72, 73)")
        // 4.5 关系的闭包
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (73, 74)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (74, 75)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (74, 76)")
        // 4.6 等价关系与划分
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (73, 77)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (77, 78)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (78, 79)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (78, 80)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (79, 81)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (80, 81)")
        // 4.7 偏序关系
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (73, 82)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (82, 83)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (83, 84)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (82, 85)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (85, 86)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (86, 87)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (87, 88)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (87, 89)")
        // 5.1 函数的概念
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (82, 90)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (90, 91)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (91, 92)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (91, 93)")
        // 5.2 集合的势
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (91, 94)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (94, 95)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (94, 96)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (94, 97)")
        // 5.3 集合的基数
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (94, 98)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (98, 99)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (99, 100)")
        // 6.1 二元运算
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (48, 101)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (48, 102)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (101, 103)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (103, 104)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (104, 105)")
        // 6.2 代数系统
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (101, 106)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (106, 107)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (106, 108)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (106, 109)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (91, 110)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (107, 110)")
        // 6.3 群
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (106, 111)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (111, 112)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (112, 113)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (113, 114)")
        // 6.4 子群
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (114, 115)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (115, 116)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (116, 117)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (117, 118)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (118, 119)")
        // 6.5 循环群与置换群
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (114, 120)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (120, 121)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (121, 122)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (116, 122)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (114, 123)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (123, 124)")
        // 6.6 环和域
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (112, 125)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (125, 126)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (126, 127)")
        // 7.1 格
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (82, 128)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (106, 128)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (128, 129)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (129, 130)")
        // 7.2 特殊的格
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (130, 131)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (130, 132)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (132, 133)")
        // 7.3 布尔代数
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (131, 134)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (133, 134)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (134, 135)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (135, 136)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (136, 137)")
        
        // ========== 四、图论 ==========
        // 部分表
        db.execSQL("INSERT INTO parts (id, name) VALUES (4, '图论')")
        
        // 章表
        db.execSQL("INSERT INTO chapters (id, part_id, number, name) VALUES (8, 4, '8', '图')")
        db.execSQL("INSERT INTO chapters (id, part_id, number, name) VALUES (9, 4, '9', '树')")
        db.execSQL("INSERT INTO chapters (id, part_id, number, name) VALUES (10, 4, '10', '平面图')")
        
        // 节表 (从id=34开始)
        // 8. 图
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (34, 8, '8.1', '图的定义')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (35, 8, '8.2', '图的相关概念')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (36, 8, '8.3', '图的连通性')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (37, 8, '8.4', '图的运算')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (38, 8, '8.5', '欧拉图与哈密顿图')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (39, 8, '8.6', '最短路径与旅行商问题')")
        // 9. 树
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (40, 9, '9.1', '树')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (41, 9, '9.2', '生成树')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (42, 9, '9.3', '根树')")
        // 10. 平面图
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (43, 10, '10.1', '平面图的基本概念')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (44, 10, '10.2', '欧拉公式')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (45, 10, '10.3', '平面图的判断')")
        db.execSQL("INSERT INTO sections (id, chapter_id, number, name) VALUES (46, 10, '10.4', '对偶图')")
        
        // 知识点表 (从id=138开始)
        // 8.1 图的定义
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (138, 34, '138', '无序对')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (139, 34, '139', '有向图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (140, 34, '140', '无向图')")
        // 8.2 图的相关概念
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (141, 35, '141', '图的基本概念')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (142, 35, '142', '基图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (143, 35, '143', '相邻与关联')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (144, 35, '144', '重数、简单图和多重图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (145, 35, '145', '度数')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (146, 35, '146', '握手定理')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (147, 35, '147', '图的表示方法')")
        // 8.3 图的连通性
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (148, 36, '148', '路径')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (149, 36, '149', '通路')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (150, 36, '150', '回路')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (151, 36, '151', '连通分支与割集')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (152, 36, '152', '连通图及其判别定理')")
        // 8.4 图的运算
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (153, 37, '153', '并图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (154, 37, '154', '差图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (155, 37, '155', '交图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (156, 37, '156', '环和')")
        // 8.5 欧拉图与哈密顿图
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (157, 38, '157', '欧拉图和半欧拉图的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (158, 38, '158', '欧拉图的充要条件')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (159, 38, '159', 'Fleury算法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (160, 38, '160', '哈密顿图和半哈密顿图的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (161, 38, '161', '哈密顿图的必要条件')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (162, 38, '162', '哈密顿图的充分条件')")
        // 8.6 最短路径与旅行商问题
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (163, 39, '163', '带权图')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (164, 39, '164', 'Dijkstra算法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (165, 39, '165', '旅行商问题')")
        // 9.1 树
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (166, 40, '166', '树的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (167, 40, '167', '树的基本概念')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (168, 40, '168', '树的性质')")
        // 9.2 生成树
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (169, 41, '169', '生成树的定义')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (170, 41, '170', '破圈法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (171, 41, '171', '最小生成树')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (172, 41, '172', 'Kruskal算法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (173, 41, '173', 'Prim算法')")
        // 9.3 根树
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (174, 42, '174', '根树')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (175, 42, '175', '有序树、正则树与完全树')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (176, 42, '176', 'Huffman算法')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (177, 42, '177', '二叉树遍历法')")
        // 10.1 平面图的基本概念
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (178, 43, '178', '平面图与平面嵌入')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (179, 43, '179', '平面图的性质')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (180, 43, '180', '极小非平面图')")
        // 10.2 欧拉公式
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (181, 44, '181', '欧拉公式')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (182, 44, '182', '欧拉公式的推论')")
        // 10.3 平面图的判断
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (183, 45, '183', '插入与消去2度顶点')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (184, 45, '184', '同胚')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (185, 45, '185', '平面图的两个充要条件')")
        // 10.4 对偶图
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (186, 46, '186', '对偶图的概念')")
        db.execSQL("INSERT INTO knowledge_points (id, section_id, number, name) VALUES (187, 46, '187', '对偶图的性质')")
        
        // 前置关系表 - 图论部分
        // 8.1 图的定义
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (48, 138)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (138, 139)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (138, 140)")
        // 8.2 图的相关概念
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (139, 141)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (140, 141)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (141, 142)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (141, 143)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (143, 144)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (143, 145)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (145, 146)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (143, 147)")
        // 8.3 图的连通性
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (146, 148)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (148, 149)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (148, 150)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (149, 151)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (151, 152)")
        // 8.4 图的运算
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (141, 153)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (141, 154)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (141, 155)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (141, 156)")
        // 8.5 欧拉图与哈密顿图
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (152, 157)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (157, 158)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (158, 159)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (152, 160)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (160, 161)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (160, 162)")
        // 8.6 最短路径与旅行商问题
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (152, 163)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (163, 164)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (163, 165)")
        // 9.1 树
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (152, 166)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (166, 167)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (167, 168)")
        // 9.2 生成树
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (168, 169)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (169, 170)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (170, 171)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (171, 172)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (171, 173)")
        // 9.3 根树
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (168, 174)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (174, 175)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (175, 176)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (175, 177)")
        // 10.1 平面图的基本概念
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (152, 178)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (178, 179)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (179, 180)")
        // 10.2 欧拉公式
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (179, 181)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (181, 182)")
        // 10.3 平面图的判断
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (179, 183)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (183, 184)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (182, 185)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (184, 185)")
        // 10.4 对偶图
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (179, 186)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (182, 187)")
        db.execSQL("INSERT INTO prerequisite_relations (prerequisite_id, target_id) VALUES (186, 187)")
    }

    // ==================== 查询方法 ====================
    
    /**
     * 获取所有部分
     */
    fun getAllParts(): List<Part> {
        val parts = mutableListOf<Part>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM parts ORDER BY id", null)
        
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                parts.add(Part(id, name))
            }
        }
        return parts
    }
    
    /**
     * 根据部分ID获取章
     */
    fun getChaptersByPartId(partId: Int): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM chapters WHERE part_id = ? ORDER BY id",
            arrayOf(partId.toString())
        )
        
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val pId = it.getInt(it.getColumnIndexOrThrow("part_id"))
                val number = it.getString(it.getColumnIndexOrThrow("number"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                chapters.add(Chapter(id, pId, number, name))
            }
        }
        return chapters
    }
    
    /**
     * 根据章ID获取节
     */
    fun getSectionsByChapterId(chapterId: Int): List<Section> {
        val sections = mutableListOf<Section>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM sections WHERE chapter_id = ? ORDER BY id",
            arrayOf(chapterId.toString())
        )
        
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val cId = it.getInt(it.getColumnIndexOrThrow("chapter_id"))
                val number = it.getString(it.getColumnIndexOrThrow("number"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                sections.add(Section(id, cId, number, name))
            }
        }
        return sections
    }
    
    /**
     * 根据节ID获取知识点
     */
    fun getKnowledgePointsBySectionId(sectionId: Int): List<KnowledgePoint> {
        val points = mutableListOf<KnowledgePoint>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM knowledge_points WHERE section_id = ? ORDER BY id",
            arrayOf(sectionId.toString())
        )
        
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val sId = it.getInt(it.getColumnIndexOrThrow("section_id"))
                val number = it.getString(it.getColumnIndexOrThrow("number"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val flag = it.getInt(it.getColumnIndexOrThrow("flag"))
                points.add(KnowledgePoint(id, sId, number, name, flag))
            }
        }
        return points
    }
    
    /**
     * 获取知识点的前置知识点
     */
    fun getPrerequisitesByKnowledgePointId(kpId: Int): List<KnowledgePoint> {
        val points = mutableListOf<KnowledgePoint>()
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT kp.* FROM knowledge_points kp
            INNER JOIN prerequisite_relations pr ON kp.id = pr.prerequisite_id
            WHERE pr.target_id = ?
            ORDER BY kp.id
        """.trimIndent(), arrayOf(kpId.toString()))
        
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val sId = it.getInt(it.getColumnIndexOrThrow("section_id"))
                val number = it.getString(it.getColumnIndexOrThrow("number"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val flag = it.getInt(it.getColumnIndexOrThrow("flag"))
                points.add(KnowledgePoint(id, sId, number, name, flag))
            }
        }
        return points
    }
    
    /**
     * 根据ID获取部分
     */
    fun getPartById(id: Int): Part? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM parts WHERE id = ?", arrayOf(id.toString()))
        
        cursor.use {
            if (it.moveToFirst()) {
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                return Part(id, name)
            }
        }
        return null
    }
    
    /**
     * 根据ID获取章
     */
    fun getChapterById(id: Int): Chapter? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM chapters WHERE id = ?", arrayOf(id.toString()))
        
        cursor.use {
            if (it.moveToFirst()) {
                val partId = it.getInt(it.getColumnIndexOrThrow("part_id"))
                val number = it.getString(it.getColumnIndexOrThrow("number"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                return Chapter(id, partId, number, name)
            }
        }
        return null
    }
    
    /**
     * 根据ID获取节
     */
    fun getSectionById(id: Int): Section? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM sections WHERE id = ?", arrayOf(id.toString()))
        
        cursor.use {
            if (it.moveToFirst()) {
                val chapterId = it.getInt(it.getColumnIndexOrThrow("chapter_id"))
                val number = it.getString(it.getColumnIndexOrThrow("number"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                return Section(id, chapterId, number, name)
            }
        }
        return null
    }
    
    /**
     * 根据ID获取知识点
     */
    fun getKnowledgePointById(id: Int): KnowledgePoint? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM knowledge_points WHERE id = ?", arrayOf(id.toString()))
        
        cursor.use {
            if (it.moveToFirst()) {
                val sectionId = it.getInt(it.getColumnIndexOrThrow("section_id"))
                val number = it.getString(it.getColumnIndexOrThrow("number"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val flag = it.getInt(it.getColumnIndexOrThrow("flag"))
                return KnowledgePoint(id, sectionId, number, name, flag)
            }
        }
        return null
    }

    /**
     * 获取知识点所属的节和章信息
     * 返回: Pair<节名称, 章名称>
     */
    fun getKnowledgePointLocation(kpId: Int): Pair<String, String>? {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT s.name as section_name, c.name as chapter_name
            FROM knowledge_points kp
            INNER JOIN sections s ON kp.section_id = s.id
            INNER JOIN chapters c ON s.chapter_id = c.id
            WHERE kp.id = ?
        """.trimIndent(), arrayOf(kpId.toString()))
        
        cursor.use {
            if (it.moveToFirst()) {
                val sectionName = it.getString(it.getColumnIndexOrThrow("section_name"))
                val chapterName = it.getString(it.getColumnIndexOrThrow("chapter_name"))
                return Pair(sectionName, chapterName)
            }
        }
        return null
    }

    /**
     * 获取所有知识点（用于兼容旧代码）
     */
    fun getAllKnowledgePoints(): List<KnowledgePoint> {
        val points = mutableListOf<KnowledgePoint>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM knowledge_points ORDER BY id", null)
        
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val sectionId = it.getInt(it.getColumnIndexOrThrow("section_id"))
                val number = it.getString(it.getColumnIndexOrThrow("number"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val flag = it.getInt(it.getColumnIndexOrThrow("flag"))
                points.add(KnowledgePoint(id, sectionId, number, name, flag))
            }
        }
        return points
    }

    /**
     * 更新知识点学习状态
     * @param id 知识点ID
     * @param learned 是否已学习 (1=已学习, 0=未学习)
     */
    fun updateKnowledgePointFlag(id: Int, learned: Boolean) {
        val db = writableDatabase
        db.execSQL("UPDATE knowledge_points SET flag = ? WHERE id = ?", arrayOf(if (learned) 1 else 0, id))
    }

    /**
     * 获取知识点学习状态
     */
    fun getKnowledgePointFlag(id: Int): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT flag FROM knowledge_points WHERE id = ?", arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(it.getColumnIndexOrThrow("flag"))
            }
        }
        return 0
    }
}

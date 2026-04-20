const { Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType } = require('docx');
const fs = require('fs');

// 创建文档 - 按用户要求设置字体和字号
const doc = new Document({
    styles: {
        default: {
            document: {
                run: { font: "SimSun", size: 24 }  // 宋体小四 12pt
            }
        },
        paragraphStyles: [
            {
                id: "Heading1",
                name: "Heading 1",
                basedOn: "Normal",
                next: "Normal",
                quickFormat: true,
                // 四号 = 14pt = 28 half-points, 黑体, 数字用Times New Roman
                run: { font: "Times New Roman", size: 28, bold: true, fontCs: "Times New Roman" },
                paragraph: { spacing: { before: 120, after: 120 }, alignment: AlignmentType.LEFT, outlineLevel: 0 }
            },
            {
                id: "Heading2",
                name: "Heading 2",
                basedOn: "Normal",
                next: "Normal",
                quickFormat: true,
                // 小四 = 12pt = 24 half-points
                run: { font: "Times New Roman", size: 24, bold: true, fontCs: "Times New Roman" },
                paragraph: { spacing: { before: 60, after: 60 }, alignment: AlignmentType.LEFT, outlineLevel: 1 }
            },
            {
                id: "Heading3",
                name: "Heading 3",
                basedOn: "Normal",
                next: "Normal",
                quickFormat: true,
                // 五号 = 10.5pt = 21 half-points
                run: { font: "Times New Roman", size: 21, fontCs: "Times New Roman" },
                paragraph: { spacing: { before: 40, after: 40 }, alignment: AlignmentType.LEFT, outlineLevel: 2 }
            }
        ]
    },
    sections: [{
        properties: {
            page: {
                size: { width: 11906, height: 16838 },  // A4
                margin: { top: 2838, right: 2838, bottom: 2838, left: 2838 }
            }
        },
        children: [
            // 目录标题
            new Paragraph({
                children: [new TextRun({ text: "目  录", font: "SimHei", size: 36, bold: true })],
                alignment: AlignmentType.CENTER,
                spacing: { before: 400, after: 600 }
            }),

            // ========== 第一章 绪论 ==========
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [
                    new TextRun({ text: "第一章 ", font: "SimHei", size: 28, bold: true }),
                    new TextRun({ text: "绪论", font: "SimHei", size: 28, bold: true })
                ],
                spacing: { before: 200, after: 100 }
            }),
            // 1.1
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "1.1 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "研究背景与意义", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "1.1.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "教育信息化发展现状", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "1.1.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "移动学习应用趋势", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "1.1.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "研究目的与意义", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            // 1.2
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "1.2 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "国内外研究现状", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "1.2.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "国外在线教育平台发展", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "1.2.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "国内智慧教育应用研究", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "1.2.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "现有答题系统存在的问题", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "1.3 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "主要研究内容", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "1.4 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "论文结构安排", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),

            // ========== 第二章 系统相关技术介绍 ==========
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [
                    new TextRun({ text: "第二章 ", font: "SimHei", size: 28, bold: true }),
                    new TextRun({ text: "系统相关技术介绍", font: "SimHei", size: 28, bold: true })
                ],
                spacing: { before: 200, after: 100 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "2.1 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "开发技术概述", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "2.1.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "Android开发技术", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "2.1.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "Kotlin语言特性", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "2.2 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "云开发技术", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "2.2.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "UniCloud云数据库", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "2.2.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "云函数技术", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "2.2.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "前端HBuilderX开发工具", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "2.3 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "本地数据存储技术", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "2.3.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "SQLite数据库", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "2.3.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "SharedPreferences配置存储", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "2.3.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "JSON数据交换格式", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "2.4 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "系统架构模式", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "2.4.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "MVVM架构设计", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "2.4.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "分层架构设计", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),

            // ========== 第三章 系统需求分析 ==========
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [
                    new TextRun({ text: "第三章 ", font: "SimHei", size: 28, bold: true }),
                    new TextRun({ text: "系统需求分析", font: "SimHei", size: 28, bold: true })
                ],
                spacing: { before: 200, after: 100 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "3.1 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "系统可行性分析", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.1.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "技术可行性", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.1.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "经济可行性", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.1.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "操作可行性", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "3.2 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "用户角色分析", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.2.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "学生用户需求", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.2.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "教师用户需求", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.2.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "系统管理员需求", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "3.3 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "功能需求分析", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.3.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "用户注册登录模块", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.3.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "班级管理模块", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.3.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "题目管理模块", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.3.4 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "答题考试模块", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.3.5 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "作业批改模块", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.3.6 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "知识图谱模块", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "3.4 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "非功能需求分析", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.4.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "性能需求", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.4.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "安全需求", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.4.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "可靠性需求", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.4.4 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "易用性需求", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "3.5 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "数据流分析", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.5.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "顶层数据流图", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.5.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "0层数据流图", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.5.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "数据字典", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "3.6 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "用例分析", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.6.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "学生用例图", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "3.6.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "教师用例图", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),

            // ========== 第四章 系统设计 ==========
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [
                    new TextRun({ text: "第四章 ", font: "SimHei", size: 28, bold: true }),
                    new TextRun({ text: "系统设计", font: "SimHei", size: 28, bold: true })
                ],
                spacing: { before: 200, after: 100 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "4.1 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "系统总体设计", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.1.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "系统功能结构图", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.1.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "系统模块划分", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.1.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "系统流程图", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "4.2 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "数据库设计", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.2.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "数据库概念设计（E-R图）", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.2.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "数据库逻辑设计", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.2.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "数据库物理设计", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.2.4 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "云数据库与本地数据库同步策略", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "4.3 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "云函数设计", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.3.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "云函数架构", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.3.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "注册云函数设计", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.3.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "数据操作云函数设计", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "4.4 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "界面设计", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.4.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "界面设计原则", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.4.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "登录注册界面设计", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.4.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "学生主界面设计", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "4.4.4 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "教师管理界面设计", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),

            // ========== 第五章 系统实现 ==========
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [
                    new TextRun({ text: "第五章 ", font: "SimHei", size: 28, bold: true }),
                    new TextRun({ text: "系统实现", font: "SimHei", size: 28, bold: true })
                ],
                spacing: { before: 200, after: 100 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "5.1 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "开发环境配置", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.1.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "硬件环境", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.1.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "软件环境", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.1.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "开发工具配置", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "5.2 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "用户认证模块实现", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.2.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "注册功能实现", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.2.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "登录验证实现", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.2.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "权限控制实现", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "5.3 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "班级管理模块实现", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.3.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "创建班级功能", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.3.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "班级信息管理", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.3.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "学生加入班级", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "5.4 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "题目管理模块实现", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.4.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "题目录入功能", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.4.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "题目分类管理", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.4.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "题目删除与恢复", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "5.5 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "答题考试模块实现", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.5.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "题目展示实现", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.5.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "答案提交处理", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.5.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "离线缓存机制", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "5.6 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "作业批改模块实现", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.6.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "待批改作业列表", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.6.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "在线批改功能", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.6.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "批改结果反馈", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "5.7 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "知识图谱模块实现", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.7.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "知识节点设计", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.7.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "关系边设计", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.7.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "图谱可视化展示", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "5.8 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "云端数据同步实现", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.8.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "数据上传机制", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.8.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "数据下载机制", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "5.8.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "同步冲突处理", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),

            // ========== 第六章 系统测试 ==========
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [
                    new TextRun({ text: "第六章 ", font: "SimHei", size: 28, bold: true }),
                    new TextRun({ text: "系统测试", font: "SimHei", size: 28, bold: true })
                ],
                spacing: { before: 200, after: 100 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "6.1 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "测试概述", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.1.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "测试目的", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.1.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "测试原则", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.1.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "测试环境", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "6.2 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "功能测试", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.2.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "用户注册登录测试", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.2.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "班级管理功能测试", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.2.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "题目管理功能测试", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.2.4 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "答题功能测试", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.2.5 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "批改功能测试", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "6.3 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "性能测试", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.3.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "响应时间测试", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.3.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "并发用户测试", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.3.3 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "数据同步性能测试", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "6.4 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "兼容性测试", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.4.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "不同Android版本兼容性", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.4.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "不同屏幕尺寸适配", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "6.5 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "测试结果分析", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.5.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "测试用例通过率", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "6.5.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "缺陷统计与分析", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),

            // ========== 第七章 总结与展望 ==========
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [
                    new TextRun({ text: "第七章 ", font: "SimHei", size: 28, bold: true }),
                    new TextRun({ text: "总结与展望", font: "SimHei", size: 28, bold: true })
                ],
                spacing: { before: 200, after: 100 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "7.1 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "研究工作总结", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "7.2 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "系统特色与创新点", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "7.3 ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "研究不足与展望", font: "SimSun", size: 24, bold: true })
                ],
                spacing: { before: 60, after: 40 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "7.3.1 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "功能扩展方向", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 40, after: 30 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_3,
                children: [
                    new TextRun({ text: "7.3.2 ", font: "Times New Roman", size: 21 }),
                    new TextRun({ text: "技术优化方向", font: "SimSun", size: 21 })
                ],
                indent: { left: 420 },
                spacing: { before: 30, after: 30 }
            }),

            // ========== 参考文献 ==========
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [
                    new TextRun({ text: "参考文献", font: "SimHei", size: 28, bold: true })
                ],
                spacing: { before: 200, after: 100 }
            }),

            // ========== 致谢 ==========
            new Paragraph({
                heading: HeadingLevel.HEADING_1,
                children: [
                    new TextRun({ text: "致谢", font: "SimHei", size: 28, bold: true })
                ],
                spacing: { before: 200, after: 100 }
            }),

            // ========== 附录 ==========
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "附录A ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "核心代码", font: "SimSun", size: 24, bold: true })
                ],
                indent: { left: 420 },
                spacing: { before: 100, after: 60 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "附录B ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "系统使用说明书", font: "SimSun", size: 24, bold: true })
                ],
                indent: { left: 420 },
                spacing: { before: 60, after: 60 }
            }),
            new Paragraph({
                heading: HeadingLevel.HEADING_2,
                children: [
                    new TextRun({ text: "附录C ", font: "Times New Roman", size: 24, bold: true }),
                    new TextRun({ text: "攻读学位期间发表的学术论文", font: "SimSun", size: 24, bold: true })
                ],
                indent: { left: 420 },
                spacing: { before: 60, after: 60 }
            }),
        ]
    }]
});

// 保存文档
Packer.toBuffer(doc).then(buffer => {
    fs.writeFileSync("C:/Users/wuyx/Desktop/毕业论文目录_格式更新.docx", buffer);
    console.log("文档已更新: C:/Users/wuyx/Desktop/毕业论文目录_格式更新.docx");
    console.log("格式设置:");
    console.log("- 1级标题: 四号 (14pt)");
    console.log("- 2级标题: 小四 (12pt)");
    console.log("- 3级标题: 五号 (10.5pt)");
    console.log("- 章节编号数字: Times New Roman");
});

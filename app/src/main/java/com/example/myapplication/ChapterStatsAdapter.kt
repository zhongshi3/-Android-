package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChapterStatsAdapter(
    private val chapterStatsList: List<ChapterStats>
) : RecyclerView.Adapter<ChapterStatsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvChapterName: TextView = view.findViewById(R.id.tv_chapter_name)
        val tvQuestionStats: TextView = view.findViewById(R.id.tv_question_stats)
        val tvKnowledgeStats: TextView = view.findViewById(R.id.tv_knowledge_stats)
        val progressQuestionChapter: ProgressBar = view.findViewById(R.id.progress_question_chapter)
        val progressKnowledgeChapter: ProgressBar = view.findViewById(R.id.progress_knowledge_chapter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chapter_stats, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stats = chapterStatsList[position]

        holder.tvChapterName.text = stats.chapterName

        // 习题统计
        holder.tvQuestionStats.text = "已完成 ${stats.completedQuestions} / 总计 ${stats.totalQuestions}"
        val questionPercent = if (stats.totalQuestions > 0) {
            (stats.completedQuestions * 100) / stats.totalQuestions
        } else {
            0
        }
        holder.progressQuestionChapter.progress = questionPercent

        // 知识点统计
        holder.tvKnowledgeStats.text = "已学习 ${stats.learnedKnowledge} / 总计 ${stats.totalKnowledge}"
        val knowledgePercent = if (stats.totalKnowledge > 0) {
            (stats.learnedKnowledge * 100) / stats.totalKnowledge
        } else {
            0
        }
        holder.progressKnowledgeChapter.progress = knowledgePercent
    }

    override fun getItemCount(): Int = chapterStatsList.size
}

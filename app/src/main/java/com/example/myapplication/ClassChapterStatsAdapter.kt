package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * 班级章节统计适配器
 */
class ClassChapterStatsAdapter(
    private var chapterStats: List<ClassChapterStats>
) : RecyclerView.Adapter<ClassChapterStatsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvChapterName: TextView = view.findViewById(R.id.tv_chapter_name)
        val tvCompletionRate: TextView = view.findViewById(R.id.tv_completion_rate)
        val progressCompletion: ProgressBar = view.findViewById(R.id.progress_completion)
        val tvAccuracyRate: TextView = view.findViewById(R.id.tv_accuracy_rate)
        val progressAccuracy: ProgressBar = view.findViewById(R.id.progress_accuracy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class_chapter_stats, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stats = chapterStats[position]

        holder.tvChapterName.text = stats.chapterName
        holder.tvCompletionRate.text = "${stats.completionRate}%"
        holder.progressCompletion.progress = stats.completionRate
        holder.tvAccuracyRate.text = "${stats.accuracyRate}%"
        holder.progressAccuracy.progress = stats.accuracyRate
    }

    override fun getItemCount(): Int = chapterStats.size

    /**
     * 更新数据
     */
    fun updateData(newStats: List<ClassChapterStats>) {
        chapterStats = newStats
        notifyDataSetChanged()
    }
}

/**
 * 班级章节统计数据类
 */
data class ClassChapterStats(
    val chapterId: Int,
    val chapterName: String,
    val totalQuestions: Int,        // 题目总数
    val answeredQuestions: Int,     // 已答题数
    val correctAnswers: Int,        // 正确答案数
    val completionRate: Int,        // 完成率百分比
    val accuracyRate: Int          // 正确率百分比
)

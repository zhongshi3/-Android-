package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HomeworkGradingAdapter(
    private val questions: List<HomeworkGradingAdapter.QuestionGradingInfo>,
    private val showAllQuestions: Boolean = true,
    private val onItemClick: (QuestionGradingInfo) -> Unit
) : RecyclerView.Adapter<HomeworkGradingAdapter.ViewHolder>() {

    data class QuestionGradingInfo(
        val qId: Int,
        val totalCount: Int,      // 总答题人数
        val ungradedCount: Int,   // 未批改人数
        val studentIds: List<Long> = emptyList()
    )

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestionNumber: TextView = itemView.findViewById(R.id.tv_question_number)
        val tvUngradedCount: TextView = itemView.findViewById(R.id.tv_ungraded_count)
        val ivArrow: ImageView = itemView.findViewById(R.id.iv_arrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_homework_grading, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val question = questions[position]

        holder.tvQuestionNumber.text = "第 ${question.qId} 题"

        if (showAllQuestions) {
            // 显示所有题目模式
            val gradedCount = question.totalCount - question.ungradedCount
            if (question.ungradedCount > 0) {
                holder.tvUngradedCount.text = "待批改: ${question.ungradedCount} / ${question.totalCount}"
                holder.tvUngradedCount.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            } else {
                holder.tvUngradedCount.text = "已全部批改"
                holder.tvUngradedCount.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
            }
        } else {
            // 显示未批改模式
            if (question.ungradedCount > 0) {
                holder.tvUngradedCount.text = "待批改: ${question.ungradedCount} 人"
                holder.tvUngradedCount.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            } else {
                holder.tvUngradedCount.text = "已全部批改"
                holder.tvUngradedCount.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(question)
        }
    }

    override fun getItemCount(): Int = questions.size
}

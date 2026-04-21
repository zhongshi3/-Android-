package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.example.myapplication.CloudApiHelper.StudentAnswer

class QuestionAdapter(
    private val questions: List<QuestionEntity>,
    private val answerStatusMap: Map<Int, StudentAnswer> = emptyMap(),
    private val onItemClick: (QuestionEntity) -> Unit
) : RecyclerView.Adapter<QuestionAdapter.ViewHolder>() {

    // 状态常量（已迁移到 CloudApiHelper.AnswerStatus，建议统一使用）
    // const val STATUS_NOT_CHECKED = 0
    // const val STATUS_CORRECT = 1
    // const val STATUS_WRONG = 2

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestionNumber: TextView = itemView.findViewById(R.id.tv_question_number)
        val tvQuestionContent: TextView = itemView.findViewById(R.id.tv_question_content)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val question = questions[position]
        holder.tvQuestionNumber.text = question.questionNumber.toString()
        
        // 截取题目内容前50个字符
        val content = if (question.content.length > 50) {
            question.content.substring(0, 50) + "..."
        } else {
            question.content
        }
        holder.tvQuestionContent.text = content
        
        // 设置状态：优先显示云端答题状态
        val answerStatus = answerStatusMap[question.questionNumber]
        when {
            answerStatus != null -> {
                // 有云端答题状态
                when (answerStatus.status) {
                    CloudApiHelper.AnswerStatus.CORRECT -> {
                        holder.tvStatus.text = "正确"
                        holder.tvStatus.setBackgroundResource(R.drawable.bg_status_checked)
                    }
                    CloudApiHelper.AnswerStatus.WRONG -> {
                        holder.tvStatus.text = "错误"
                        holder.tvStatus.setBackgroundResource(R.drawable.bg_status_wrong)
                    }
                    CloudApiHelper.AnswerStatus.NOT_CHECKED -> {
                        holder.tvStatus.text = "待批改"
                        holder.tvStatus.setBackgroundResource(R.drawable.bg_status_not_checked)
                    }
                    else -> {
                        holder.tvStatus.text = "待答"
                        holder.tvStatus.setBackgroundResource(R.drawable.bg_status_not_started)
                    }
                }
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            }
            // TODO: 暂时屏蔽错题库标记显示，功能重新设计后恢复
            // question.inErrorBook -> {
            //     holder.tvStatus.text = "错题"
            //     holder.tvStatus.setBackgroundResource(R.drawable.bg_status_wrong)
            //     holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            // }
            else -> {
                holder.tvStatus.text = "待答"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_not_started)
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            }
        }
        
        // 点击事件
        holder.itemView.setOnClickListener {
            onItemClick(question)
        }
    }

    override fun getItemCount(): Int = questions.size
}
package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuestionManagementAdapter(
    private val questions: List<QuestionEntity>,
    private val onItemClick: (QuestionEntity) -> Unit,
    private val onDeleteClick: (QuestionEntity) -> Unit
) : RecyclerView.Adapter<QuestionManagementAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestionNumber: TextView = itemView.findViewById(R.id.tv_question_number)
        val tvQuestionContent: TextView = itemView.findViewById(R.id.tv_question_content)
        val btnDelete: Button = itemView.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question_management, parent, false)
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
        
        // 点击事件
        holder.itemView.setOnClickListener {
            onItemClick(question)
        }
        
        // 删除按钮事件
        holder.btnDelete.setOnClickListener {
            onDeleteClick(question)
        }
    }

    override fun getItemCount(): Int = questions.size
}

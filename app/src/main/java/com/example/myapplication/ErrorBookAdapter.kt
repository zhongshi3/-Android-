package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ErrorBookAdapter(
    private val errors: List<CloudApiHelper.ErrorQuestionInfo>,
    private val onItemClick: (CloudApiHelper.ErrorQuestionInfo) -> Unit
) : RecyclerView.Adapter<ErrorBookAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestionNumber: TextView = itemView.findViewById(R.id.tv_question_number)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_error_book, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val error = errors[position]
        holder.tvQuestionNumber.text = error.qId.toString()

        // 点击事件 - 跳转到错题详情
        holder.itemView.setOnClickListener {
            onItemClick(error)
        }
    }

    override fun getItemCount(): Int = errors.size
}

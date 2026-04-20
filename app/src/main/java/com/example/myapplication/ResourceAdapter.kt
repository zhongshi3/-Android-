package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 资源管理适配器
 * @param resources 资源列表
 * @param isTeacherMode 是否为教师模式（显示删除按钮）
 * @param onDownloadClick 下载按钮点击回调
 * @param onDeleteClick 删除按钮点击回调（教师模式）
 */
class ResourceAdapter(
    private val resources: List<ResourceInfo>,
    private val isTeacherMode: Boolean = false,
    private val onDownloadClick: (ResourceInfo) -> Unit = {},
    private val onDeleteClick: (ResourceInfo) -> Unit = {}
) : RecyclerView.Adapter<ResourceAdapter.ViewHolder>() {

    /**
     * 资源信息数据类
     */
    data class ResourceInfo(
        val resId: String,
        val resName: String,
        val url: String,
        val teacherId: Long,
        val createTime: Long
    ) {
        private val itemDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        /**
         * 从URL中提取文件名
         */
        fun getFileNameFromUrl(): String {
            return try {
                if (url.contains("/")) {
                    url.substring(url.lastIndexOf("/") + 1)
                } else if (url.contains("%2F")) {
                    // URL编码的文件名
                    java.net.URLDecoder.decode(url.substring(url.lastIndexOf("%2F") + 3), "UTF-8")
                } else {
                    resName
                }
            } catch (e: Exception) {
                resName
            }
        }

        /**
         * 获取文件类型图标
         */
        fun getFileTypeIcon(): Int {
            val fileName = getFileNameFromUrl().lowercase()
            return when {
                fileName.endsWith(".pdf") -> android.R.drawable.ic_menu_help
                fileName.endsWith(".doc") || fileName.endsWith(".docx") -> android.R.drawable.ic_menu_edit
                fileName.endsWith(".ppt") || fileName.endsWith(".pptx") -> android.R.drawable.ic_menu_gallery
                fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") || fileName.endsWith(".gif") -> android.R.drawable.ic_menu_camera
                fileName.endsWith(".mp4") || fileName.endsWith(".avi") ||
                fileName.endsWith(".mov") -> android.R.drawable.ic_menu_view
                fileName.endsWith(".mp3") || fileName.endsWith(".wav") -> android.R.drawable.ic_media_play
                else -> android.R.drawable.ic_menu_agenda
            }
        }

        /**
         * 格式化上传时间
         */
        fun getFormattedTime(): String {
            return itemDateFormat.format(Date(createTime))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resource, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val resource = resources[position]
        holder.bind(resource)
    }

    override fun getItemCount(): Int = resources.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivFileIcon: ImageView = itemView.findViewById(R.id.iv_file_icon)
        private val tvResourceName: TextView = itemView.findViewById(R.id.tv_resource_name)
        private val tvFileName: TextView = itemView.findViewById(R.id.tv_file_name)
        private val tvUploadTime: TextView = itemView.findViewById(R.id.tv_upload_time)
        private val btnDownload: ImageButton = itemView.findViewById(R.id.btn_download)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(resource: ResourceInfo) {
            tvResourceName.text = resource.resName
            tvFileName.text = resource.getFileNameFromUrl()
            tvUploadTime.text = resource.getFormattedTime()
            ivFileIcon.setImageResource(resource.getFileTypeIcon())

            // 下载按钮
            btnDownload.setOnClickListener {
                onDownloadClick(resource)
            }

            // 删除按钮（仅教师模式）
            if (isTeacherMode) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener {
                    onDeleteClick(resource)
                }
            } else {
                btnDelete.visibility = View.GONE
            }
        }
    }
}

package com.noticevault.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.noticevault.databinding.ItemNotificationBinding
import com.noticevault.db.NotificationEntity
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val onTap: (NotificationEntity) -> Unit
) : ListAdapter<NotificationEntity, NotificationAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<NotificationEntity>() {
            override fun areItemsTheSame(a: NotificationEntity, b: NotificationEntity) = a.id == b.id
            override fun areContentsTheSame(a: NotificationEntity, b: NotificationEntity) = a == b
        }
        val DATE_FMT = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

        val CATEGORY_COLORS = mapOf(
            "promotional"   to "#FF6B35",
            "social"        to "#4CAF50",
            "transactional" to "#2196F3",
            "urgent"        to "#F44336",
            "news"          to "#9C27B0",
            "system"        to "#607D8B",
            "other"         to "#9E9E9E",
            "uncategorized" to "#BDBDBD"
        )
    }

    inner class VH(val b: ItemNotificationBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val n = getItem(position)
        with(holder.b) {
            tvAppName.text  = n.appName
            tvTitle.text    = n.title.ifBlank { "(no title)" }
            tvContent.text  = n.content.ifBlank { "(no content)" }
            tvTime.text     = DATE_FMT.format(Date(n.timestamp))
            tvCategory.text = n.category
            tvCategory.setBackgroundColor(
                Color.parseColor(CATEGORY_COLORS[n.category] ?: "#9E9E9E")
            )
            root.alpha = if (n.isRead) 0.45f else 1f
            root.setOnClickListener { onTap(n) }
        }
    }
}

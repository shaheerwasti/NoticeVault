package com.noticevault.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appPackage: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val category: String = "uncategorized",
    val isRead: Boolean = false
)

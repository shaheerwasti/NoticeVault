package com.noticevault.report

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.noticevault.db.NotificationEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class CsvExporter(private val context: Context) {

    fun export(list: List<NotificationEntity>): Uri? = try {
        val dir  = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(dir, "noticevault_${System.currentTimeMillis()}.csv")
        val fmt  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        FileWriter(file).use { w ->
            w.appendLine("Time,App,Title,Content,Category,Read")
            list.forEach { n ->
                w.appendLine(
                    "\"${fmt.format(Date(n.timestamp))}\"," +
                    "\"${n.appName.csv()}\"," +
                    "\"${n.title.csv()}\"," +
                    "\"${n.content.csv()}\"," +
                    "\"${n.category}\"," +
                    "\"${if (n.isRead) "yes" else "no"}\""
                )
            }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    } catch (e: Exception) { null }

    private fun String.csv() = replace("\"", "\"\"")
}

package com.tadeaspikl.rssreader

import android.content.Context
import java.io.File

object StorageManipulation {
    const val filename = "SavedChannels.txt"

    fun loadChannels(context: Context): MutableList<String> {
        val file = File(context.filesDir.absolutePath, filename)
        if (!file.exists()) {
            file.createNewFile()
        }
        val lines = file.readLines()
        return lines.toMutableList()
    }

    fun saveChannels(context: Context, channels: List<String>) {
        context.openFileOutput(filename, Context.MODE_PRIVATE).use { out ->
            for (channel in channels) {
                out.write(channel.toByteArray())
                out.write("\n".toByteArray())
            }
        }
    }
}

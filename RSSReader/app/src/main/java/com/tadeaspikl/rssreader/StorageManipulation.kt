package com.tadeaspikl.rssreader

import android.content.Context


const val filename = "SavedChannels.txt"

fun loadChannels(context: Context): List<String> {
    return context.openFileInput(filename).bufferedReader().readLines()
}

fun saveChannels(context: Context, channels: List<String>) {
    context.openFileOutput(filename, Context.MODE_PRIVATE).use { out ->
        for (channel in channels) {
            out.write(channel.toByteArray())
        }
    }
}
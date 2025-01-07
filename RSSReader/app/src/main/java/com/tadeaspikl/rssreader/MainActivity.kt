package com.tadeaspikl.rssreader

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import com.tadeaspikl.rssreader.ui.theme.RSSReaderTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val channelUrls = mutableSetOf<String>()
        channelUrls.add("https://lorem-rss.herokuapp.com//feed")




        setContent {
            RSSReaderTheme {
                MainScaffold(channelUrls)
            }
        }
    }
}

suspend fun getAllChannels(urls: Iterable<String>): Iterable<RssChannel> {
    val rssParser = RssParser()
    return urls.map { url ->
        coroutineScope {
            async { rssParser.getRssChannel(url) }
        }
    }.awaitAll().toSet()
}


suspend fun receiveUpdates(channelUrls: Iterable<String>): List<RssItem> {
    val rssChannels = getAllChannels(channelUrls)
    val articles = rssChannels.flatMap { it.items }
    return articles//.sortedBy { it -> it.pubDate?.let { Instant.parse(it) } ?: Instant.EPOCH }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(channelUrls: Iterable<String>) {
    val coroutineScope = rememberCoroutineScope()
    val articles = remember { mutableStateOf<List<RssItem>>(emptyList()) }

    LaunchedEffect(channelUrls) {
        try {
            articles.value = receiveUpdates(channelUrls)
        } catch (e: Exception) {
            e.message?.let { Log.e("error", it) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("RSS Reader")
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                content = {Icon(Icons.Filled.Add, "Add")}
            )
        }
    ) { innerPadding -> LazyColumn(modifier = Modifier.padding(innerPadding)) {
        items(articles.value) { article ->
            article.title?.let { Text(it) }
        }
    } }
}
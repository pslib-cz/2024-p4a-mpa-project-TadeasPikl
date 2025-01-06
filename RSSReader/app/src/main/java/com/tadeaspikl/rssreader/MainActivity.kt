package com.tadeaspikl.rssreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.RssParserBuilder
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import com.tadeaspikl.rssreader.ui.theme.RSSReaderTheme
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val channelUrls = mutableSetOf<String>()
        channelUrls.add("lorem-rss.herokuapp.com/feed")

        val articles = ReceiveUpdates(channelUrls)



        setContent {
            RSSReaderTheme {
                MainScaffold()
            }
        }
    }
}

suspend fun GetAllChannels(urls: Iterable<String>): Iterable<RssChannel> {
    val rssParser: RssParser = RssParser()
    val channels = mutableSetOf<RssChannel>()
    for (url in urls) {
        channels.add(rssParser.getRssChannel(url))
    }
    return channels
}

fun ReceiveUpdates(channelUrls: Iterable<String>): Iterable<RssItem> {
    val rssChannels = runBlocking { GetAllChannels(channelUrls) }
    val articles = mutableListOf<RssItem>()

    for (channel in rssChannels) {
        articles.addAll(channel.items)
    }

    return articles.sortedWith { a, b -> a.pubDate.compareTo(b.pubDate) }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold() {
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

    } }
}


@Composable
fun ArticleTile(article: RssItem) {

}
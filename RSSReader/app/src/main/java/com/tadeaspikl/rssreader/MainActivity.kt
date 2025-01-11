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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import java.text.DateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val channelUrls = mutableSetOf<String>()
        channelUrls.add("https://lorem-rss.herokuapp.com/feed")
        channelUrls.add("https://maia.crimew.gay/feed.xml")
        channelUrls.add("https://www.reddit.com/r/furry_irl/.rss")
        channelUrls.add("https://afed.cz/rss")



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

    val dateFormats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd MMM yyyy HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )
    val rfc822Formatter: DateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault())
    rfc822Formatter.timeZone = TimeZone.getDefault()

    val sortedArticles = articles.sortedByDescending { item ->
            dateFormats
                .mapNotNull { format ->
                    try {
                        SimpleDateFormat(format, Locale.ENGLISH).parse(item.pubDate)
                    } catch (e: ParseException) {
                        null
                    }
                }
                .firstOrNull()
        }
        .map { item ->
            item.copy(pubDate = rfc822Formatter.format(
                    dateFormats.mapNotNull { format ->
                        try { SimpleDateFormat(format, Locale.ENGLISH).parse(item.pubDate) } catch (e: ParseException) { null }
                    }.firstOrNull() ?: Date(0) // Fallback to epoch if parsing fails
            ))
        }

    return sortedArticles
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
            e.message?.let { Log.e("ReceiveUpdates", it) }
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
            ArticleTile(article)
        }
    } }
}

@Composable
fun ArticleTile(article: RssItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { }
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        // Image thumbnail
        if (!article.image.isNullOrEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(model = article.image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No Image",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
        ) {
            // Title
            article.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            article.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Publication Date
            article.pubDate?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

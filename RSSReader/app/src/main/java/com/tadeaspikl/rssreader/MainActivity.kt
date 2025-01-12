package com.tadeaspikl.rssreader

import android.os.Bundle
import android.util.Log
import android.view.View
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
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

        val channelUrls = StorageManipulation.loadChannels(context = this)
//        channelUrls.add("https://lorem-rss.herokuapp.com/feed")
//        channelUrls.add("https://maia.crimew.gay/feed.xml")

        setContent {
            RSSReaderTheme {
                val navController = rememberNavController()
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
    val rfc822Formatter: DateFormat =
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault())
    rfc822Formatter.timeZone = TimeZone.getDefault()

    val sortedArticles = articles.sortedByDescending { item ->
        dateFormats
            .mapNotNull { format ->
                try {
                    item.pubDate?.let { SimpleDateFormat(format, Locale.ENGLISH).parse(it) }
                } catch (e: ParseException) {
                    null
                }
            }
            .firstOrNull()
    }
        .map { item ->
            item.copy(
                pubDate = rfc822Formatter.format(
                    dateFormats.mapNotNull { format ->
                        try {
                            item.pubDate?.let { SimpleDateFormat(format, Locale.ENGLISH).parse(it) }
                        } catch (e: ParseException) {
                            null
                        }
                    }.firstOrNull() ?: Date(0) // Fallback to epoch if parsing fails
                )
            )
        }

    return sortedArticles
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(channelUrls: MutableList<String>) {
    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val articles = remember { mutableStateOf<List<RssItem>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun refresh() = refreshScope.launch {
        refreshing = true
        try {
            articles.value = receiveUpdates(channelUrls)
        } catch (e: Exception) {
            e.message?.let { Log.e("ReceiveUpdates", it) }
        }
        refreshing = false
    }

    LaunchedEffect(true) {
        refresh()
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
                },
                actions = {
                    IconButton(
                        content = { Icon(Icons.Filled.Settings, "Settings") },
                        onClick = {  },
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                content = { Icon(Icons.Filled.Add, "Add") },
                onClick = { showDialog = true },
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { refresh() },
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            LazyColumn {
                items(articles.value) { article ->
                    ArticleTile(article)
                }
            }
        }
    }

    if (showDialog) {
        RssAddDialog(context = context, refresh = { refresh() }, channelUrls = channelUrls, onDismiss = { showDialog = false })
    }
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



@Composable
fun RssAddDialog(context: android.content.Context, refresh: () -> Unit, channelUrls: MutableList<String>, onDismiss: () -> Unit) {
    var rssUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add RSS Feed") },
        text = {
            OutlinedTextField(
                value = rssUrl,
                onValueChange = { rssUrl = it },
                label = { Text("RSS Feed URL") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    channelUrls.add(rssUrl)
                    StorageManipulation.saveChannels(context = context, channelUrls)
                    refresh()
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

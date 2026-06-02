package com.booknext.app.ui.reader.epub

import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.booknext.app.LocalActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubReaderScreen(
    file: File,
    title: String,
    initialPage: Int,
    fontSize: Int = 17,
    darkMode: Boolean = false,
    onBack: () -> Unit,
    onProgressChange: (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        EpubReaderWrapper(
            file = file,
            fontSize = fontSize,
            darkMode = darkMode,
            initialPage = initialPage,
            onProgressChange = onProgressChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@Composable
fun EpubReaderWrapper(
    file: File,
    fontSize: Int,
    darkMode: Boolean,
    initialPage: Int,
    onProgressChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current
    var error by remember { mutableStateOf<String?>(null) }

    if (error != null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val container = FrameLayout(ctx).apply {
                id = android.view.View.generateViewId()
            }

            MainScope().launch {
                try {
                    openEpubPublication(
                        activity = activity,
                        file = file,
                        container = container,
                        fontSize = fontSize,
                        darkMode = darkMode,
                        initialPage = initialPage,
                        onProgressChange = onProgressChange,
                        onError = { msg -> error = msg },
                    )
                } catch (e: Exception) {
                    error = "加载失败：${e.message}"
                }
            }

            container
        }
    )
}

private suspend fun openEpubPublication(
    activity: FragmentActivity,
    file: File,
    container: FrameLayout,
    fontSize: Int,
    darkMode: Boolean,
    initialPage: Int,
    onProgressChange: (Int) -> Unit,
    onError: (String) -> Unit,
) {
    val httpClient = org.readium.r2.shared.util.http.DefaultHttpClient()
    val assetRetriever = org.readium.r2.shared.util.asset.AssetRetriever(
        contentResolver = activity.contentResolver,
        httpClient = httpClient,
    )
    val publicationOpener = org.readium.r2.streamer.PublicationOpener(
        publicationParser = org.readium.r2.streamer.parser.DefaultPublicationParser(
            context = activity,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = null,
        )
    )

    val publication = publicationOpener.open(
        asset = assetRetriever.retrieve(file, org.readium.r2.shared.util.format.FormatHints()).fold(
            onSuccess = { it },
            onFailure = { err ->
                onError("文件读取失败：${err.message}")
                return
            }
        ),
        allowUserInteraction = false,
    ).fold(
        onSuccess = { it },
        onFailure = { err ->
            onError("EPUB 解析失败：${err.message}")
            return
        }
    )

    val prefs = EpubPreferences(
        fontSize = fontSize / 16.0,
        theme = if (darkMode) Theme.DARK else Theme.LIGHT,
        scroll = false,
        publisherStyles = true,
    )

    val locator: Locator? = publication.readingOrder
        .getOrNull(initialPage)
        ?.let { link ->
            val hrefString = link.href.toString()
            Locator(
                href = org.readium.r2.shared.util.Url(hrefString)!!,
                mediaType = link.mediaType ?: org.readium.r2.shared.util.mediatype.MediaType.EPUB,
            )
        }

    val navigatorFactory = EpubNavigatorFactory(publication = publication)
    val fragmentFactory = navigatorFactory.createFragmentFactory(
        initialLocator = locator,
        initialPreferences = prefs,
        listener = object : EpubNavigatorFragment.Listener {
            override fun onExternalLinkActivated(url: org.readium.r2.shared.util.AbsoluteUrl) {}
        },
        paginationListener = object : EpubNavigatorFragment.PaginationListener {
            override fun onPageChanged(pageIndex: Int, totalPages: Int, locator: Locator) {
                onProgressChange(pageIndex)
            }
            override fun onPageLoaded() {}
        },
    )

    withContext(Dispatchers.Main) {
        activity.supportFragmentManager.fragmentFactory = fragmentFactory
        activity.supportFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .replace(container.id, EpubNavigatorFragment::class.java, Bundle())
            .commitAllowingStateLoss()
    }
}

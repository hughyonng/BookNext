package com.booknext.app.reader

import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

@Singleton
class ReadiumManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) {
    val httpClient = DefaultHttpClient()

    val assetRetriever = AssetRetriever(
        context.contentResolver,
        httpClient,
    )

    val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context, httpClient, assetRetriever,
            pdfFactory = null,
        )
    )

    suspend fun openEpub(file: File): Publication? {
        val assetResult = assetRetriever.retrieve(file, FormatHints())
        val asset = assetResult.getOrNull() ?: return null
        val pubResult = publicationOpener.open(
            asset = asset, credentials = null, allowUserInteraction = false,
        )
        return pubResult.getOrNull()
    }
}
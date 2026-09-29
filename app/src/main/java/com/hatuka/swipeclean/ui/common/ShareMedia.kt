package com.hatuka.swipeclean.ui.common

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hatuka.swipeclean.R
import com.hatuka.swipeclean.core.media.MediaType

/**
 * Shares a photo or video through the system share sheet, with a short text that names the app
 * and links to its Play Store page. Nothing leaves the device unless the user picks a target app.
 */
fun Context.shareMedia(uri: Uri, type: MediaType) {
    val video = type == MediaType.VIDEO
    val link = "https://play.google.com/store/apps/details?id=$packageName"
    val text = getString(if (video) R.string.share_text_video else R.string.share_text_photo, getString(R.string.app_name), link)
    val send = Intent(Intent.ACTION_SEND).apply {
        setType(if (video) "video/*" else "image/*")
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        // The receiving app has no media permission of its own; grant read access to this one item.
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)))
}

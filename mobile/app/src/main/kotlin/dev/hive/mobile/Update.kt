package dev.hive.mobile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

// A newer phone app on GitHub (public, no sign-in), looked up when the app opens, at most once a day; the answer is
// kept, so the card stays until the update is installed or "Later" is pressed for it. A release carries a phone app only
// when mobile/ changed, so the newest release with an Orbit-Phone-<version>.apk is the one, and its version is the
// APK's own (from the file name), not the release tag.
object PhoneUpdate {
    data class Available(val version: String, val apk: String)

    private const val RELEASES = "https://api.github.com/repos/saiharsha03/orbit/releases?per_page=30"
    private const val DAY = 24 * 60 * 60 * 1000L
    private val http = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()

    fun installed(ctx: Context): String = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""

    // "3.0.10" is newer than "3.0.9": compared part by part as numbers
    fun newer(a: String, b: String): Boolean {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }

    // the update to offer now, or null; asks GitHub only when the last answer is more than a day old
    suspend fun check(ctx: Context): Available? {
        val p = ctx.getSharedPreferences("update", Context.MODE_PRIVATE)
        if (System.currentTimeMillis() - p.getLong("checked", 0) > DAY) {
            runCatching { fetch() }.getOrNull()?.let { a ->
                p.edit().putLong("checked", System.currentTimeMillis()).putString("version", a.version).putString("apk", a.apk).apply()
            }
        }
        val v = p.getString("version", "") ?: ""
        val apk = p.getString("apk", "") ?: ""
        if (v.isEmpty() || apk.isEmpty() || !newer(v, installed(ctx)) || p.getString("later", "") == v) return null
        return Available(v, apk)
    }

    fun later(ctx: Context, version: String) = ctx.getSharedPreferences("update", Context.MODE_PRIVATE).edit().putString("later", version).apply()

    private suspend fun fetch(): Available? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(RELEASES).header("Accept", "application/vnd.github+json").build()
        http.newCall(req).execute().use { r -> if (r.isSuccessful) newest(r.body?.string() ?: return@use null) else null }
    }

    // the newest published phone app in GitHub's release list (newest release first; drafts and pre-releases skipped)
    fun newest(releases: String): Available? {
        val list = JSONArray(releases)
        for (i in 0 until list.length()) {
            val rel = list.getJSONObject(i)
            if (rel.optBoolean("draft") || rel.optBoolean("prerelease")) continue
            val assets = rel.optJSONArray("assets") ?: continue
            for (k in 0 until assets.length()) {
                val a = assets.getJSONObject(k)
                val m = Regex("""^Orbit-Phone-(.+)\.apk$""").find(a.optString("name")) ?: continue
                val url = a.optString("browser_download_url")
                if (url.isNotEmpty()) return Available(m.groupValues[1], url)
            }
        }
        return null
    }
}

// the card on top of the lists when a newer phone app is out: Later hides it for that version; Update downloads the
// APK in the browser, and Android asks to install it over this one
@Composable
fun UpdateCard() {
    val ctx = LocalContext.current
    var offer by remember { mutableStateOf<PhoneUpdate.Available?>(null) }
    LaunchedEffect(Unit) { offer = PhoneUpdate.check(ctx) }
    val a = offer ?: return
    UpdateOffer(a.version, PhoneUpdate.installed(ctx), onLater = { PhoneUpdate.later(ctx, a.version); offer = null },
        onUpdate = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.apk)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } })
}

@Composable
fun UpdateOffer(version: String, installed: String, onLater: () -> Unit = {}, onUpdate: () -> Unit = {}) {
    Panel(Modifier.padding(top = 8.dp), pad = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconChip(Ic.download, C.ink)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Orbit Phone $version is out", style = T.title)
                Text("You have $installed. Android asks to install it after the download.", style = T.label.copy(lineHeight = 17.sp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Later", Modifier.weight(1f), onClick = onLater)
            Btn("Update", Modifier.weight(1f), primary = true, onClick = onUpdate)
        }
    }
}

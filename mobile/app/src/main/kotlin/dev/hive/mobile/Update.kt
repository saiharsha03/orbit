package dev.hive.mobile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// A newer phone app on the latest Orbit release: looked up on GitHub (public, no sign-in) when the app opens, at most
// once a day; the answer is kept, so the card stays until the update is installed or "Later" is pressed for it.
object PhoneUpdate {
    data class Available(val version: String, val apk: String)

    private const val LATEST = "https://api.github.com/repos/saiharsha03/orbit/releases/latest"
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
        val req = Request.Builder().url(LATEST).header("Accept", "application/vnd.github+json").build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return@use null
            val j = JSONObject(r.body?.string() ?: return@use null)
            val version = j.optString("tag_name").removePrefix("v")
            val assets = j.optJSONArray("assets") ?: return@use null
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").startsWith("Orbit-Phone-") && it.optString("name").endsWith(".apk") }
                ?.optString("browser_download_url").orEmpty()
            if (version.isEmpty() || apk.isEmpty()) null else Available(version, apk)
        }
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
    Card("Orbit Phone ${a.version} is available", C.work, actions = {
        Btn("Later") { PhoneUpdate.later(ctx, a.version); offer = null }
        Btn("Update", primary = true) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.apk)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }) {
        Text("You have ${PhoneUpdate.installed(ctx)}. Update downloads the new app; Android then asks to install it.", style = T.note.copy(fontSize = 13.5.sp))
    }
}

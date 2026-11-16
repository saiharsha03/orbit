package dev.hive.mobile

// the phone's update card offers the newest APK on GitHub, at the APK's own version, not the desktop release tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateTest {
    private fun rel(tag: String, vararg assets: String, draft: Boolean = false) =
        """{"tag_name":"$tag","draft":$draft,"prerelease":false,"assets":[${assets.joinToString(",") { """{"name":"$it","browser_download_url":"https://x/$it"}""" }}]}"""

    @Test fun newestApkWinsAtItsOwnVersion() {
        val json = "[" + listOf(rel("v3.0.8", "Orbit-Setup-3.0.8.exe"), rel("v3.0.7", "Orbit-Setup-3.0.7.exe", "Orbit-Phone-3.0.7.apk"), rel("v3.0.3", "Orbit-Phone-3.0.3.apk")).joinToString(",") + "]"
        assertEquals(PhoneUpdate.Available("3.0.7", "https://x/Orbit-Phone-3.0.7.apk"), PhoneUpdate.newest(json))
    }

    @Test fun draftsAndNoApkGiveNothing() {
        assertNull(PhoneUpdate.newest("[" + rel("v3.0.9", "Orbit-Phone-3.0.9.apk", draft = true) + "," + rel("v3.0.8", "Orbit-Setup-3.0.8.exe") + "]"))
    }
}

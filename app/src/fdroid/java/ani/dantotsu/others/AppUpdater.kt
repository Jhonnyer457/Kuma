package ani.dantotsu.others

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import ani.dantotsu.BuildConfig
import ani.dantotsu.Mapper
import ani.dantotsu.R
import ani.dantotsu.buildMarkwon
import ani.dantotsu.client
import ani.dantotsu.currContext
import ani.dantotsu.logError
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.snackString
import ani.dantotsu.toast
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.util.Logger
import ani.dantotsu.util.VersionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Actualizador de Kuma (sabor fdroid): consulta la ultima GitHub Release del repositorio
 * indicado en R.string.repo (Jhonnyer457/Kuma) y descarga la APK que coincide con la ABI
 * del dispositivo. Requiere que las Releases del repositorio sean publicas.
 */
object AppUpdater {

    suspend fun check(activity: FragmentActivity, post: Boolean = false) {
        if (post) snackString(currContext()?.getString(R.string.checking_for_update))
        val repo = activity.getString(R.string.repo)
        tryWithSuspend {
            val res = client.get("https://api.github.com/repos/$repo/releases/latest")
            if (res.code != 200) {
                Logger.log("Kuma updater: HTTP ${res.code} consultando $repo")
                if (post) {
                    snackString(
                        if (res.code == 404)
                            "No se pudo consultar $repo (404). Si el repositorio es privado, la app no puede ver sus Releases."
                        else
                            "No se pudo comprobar la actualizacion (HTTP ${res.code})"
                    )
                }
                return@tryWithSuspend
            }
            val release = Mapper.json.decodeFromString<GithubResponse>(res.text)
            val version = release.tagName.removePrefix("v").trim()
            val dontShow = PrefManager.getCustomVal("dont_ask_for_update_$version", false)
            val outdated = VersionUtil.isOutdated(version, BuildConfig.VERSION_NAME)

            if (outdated && (!dontShow || post) && !activity.isDestroyed) {
                showUpdateDialog(activity, version, release)
            } else if (post) {
                snackString(currContext()?.getString(R.string.no_update_found))
            }
            Unit
        }
    }

    private fun pickApkUrl(release: GithubResponse): String? {
        val apks = release.assets?.map { it.browserDownloadURL }
            ?.filter { it.endsWith(".apk", ignoreCase = true) } ?: return null
        val abis = android.os.Build.SUPPORTED_ABIS ?: emptyArray()
        for (abi in abis) {
            val a = abi.lowercase()
            apks.firstOrNull { u ->
                val n = u.lowercase()
                n.contains("-$a-") || n.contains("-$a.") || n.contains("_$a")
            }?.let { return it }
        }
        return apks.firstOrNull { it.contains("universal", ignoreCase = true) } ?: apks.firstOrNull()
    }

    private fun showUpdateDialog(activity: FragmentActivity, version: String, release: GithubResponse) {
        activity.runOnUiThread {
            CustomBottomDialog.newInstance().apply {
                setTitleText("Update " + currContext()!!.getString(R.string.available))
                addView(
                    TextView(activity).apply {
                        val markWon = try {
                            buildMarkwon(activity, false)
                        } catch (e: IllegalArgumentException) {
                            return@runOnUiThread
                        }
                        markWon.setMarkdown(this, release.body ?: "")
                    }
                )
                setCheck(
                    currContext()!!.getString(R.string.dont_show_again, version),
                    false
                ) { isChecked ->
                    if (isChecked) {
                        PrefManager.setCustomVal("dont_ask_for_update_$version", true)
                    }
                }
                setPositiveButton(currContext()!!.getString(R.string.lets_go)) {
                    MainScope().launch(Dispatchers.IO) {
                        try {
                            val apkUrl = pickApkUrl(release)
                            if (apkUrl != null) {
                                activity.downloadUpdate(version, apkUrl)
                            } else {
                                openLinkInBrowser(release.htmlUrl)
                            }
                        } catch (e: Exception) {
                            logError(e)
                        }
                    }
                    dismiss()
                }
                setNegativeButton(currContext()!!.getString(R.string.cope)) {
                    dismiss()
                }
                show(activity.supportFragmentManager, "dialog")
            }
        }
    }

    private fun Activity.downloadUpdate(version: String, url: String) {
        toast(getString(R.string.downloading_update, version))

        val downloadManager = this.getSystemService<DownloadManager>()!!

        val request = DownloadManager.Request(Uri.parse(url))
            .setMimeType("application/vnd.android.package-archive")
            .setTitle("Downloading Kuma $version")
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Kuma $version.apk"
            )
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val id = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            logError(e)
            -1
        }
        if (id == -1L) return
        ContextCompat.registerReceiver(
            this,
            object : BroadcastReceiver() {
                @SuppressLint("Range")
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        val downloadId = intent?.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, id
                        ) ?: id

                        downloadManager.getUriForDownloadedFile(downloadId)?.let {
                            openApk(this@downloadUpdate, it)
                        }
                    } catch (e: Exception) {
                        logError(e)
                    }
                }
            }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun openApk(context: Context, uri: Uri) {
        try {
            uri.path?.let {
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    data = uri
                }
                context.startActivity(installIntent)
            }
        } catch (e: Exception) {
            logError(e)
        }
    }

    @Serializable
    data class GithubResponse(
        @SerialName("html_url")
        val htmlUrl: String,
        @SerialName("tag_name")
        val tagName: String,
        val prerelease: Boolean,
        @SerialName("created_at")
        val createdAt: String,
        val body: String? = null,
        val assets: List<Asset>? = null
    ) {
        @Serializable
        data class Asset(
            @SerialName("browser_download_url")
            val browserDownloadURL: String
        )

        fun timeStamp(): Long {
            return dateFormat.parse(createdAt)!!.time
        }

        companion object {
            private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        }
    }
}

package com.example.elisdb.ui.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands a verified package to the system installer.
 *
 * Installing silently would require device-owner/MDM privileges, so the system
 * installer is opened and the user confirms the update (one tap). The package is
 * exposed through [FileProvider] because the download lives in the app cache.
 */
object ApkInstaller {
  private const val APK_MEDIA_TYPE = "application/vnd.android.package-archive"

  /** Whether this app may currently ask the system to install packages. */
  fun canInstall(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
      context.packageManager.canRequestPackageInstalls()

  /** Opens the system settings page where the user allows installs from this app. */
  fun requestPermission(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val intent =
      Intent(
          Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
          Uri.parse("package:${context.packageName}"),
        )
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
  }

  /** Opens the system installer for [file], which must live under the shared paths. */
  fun install(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent =
      Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, APK_MEDIA_TYPE)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  }
}

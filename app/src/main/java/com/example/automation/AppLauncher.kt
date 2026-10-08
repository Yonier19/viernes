package com.example.automation

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.utils.Constants
import java.text.Normalizer
import java.util.Locale

data class LaunchResult(
    val success: Boolean,
    val appLabel: String,
    val packageName: String? = null,
    val errorMessage: String? = null
)

class AppLauncher(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Resolves an app name or alias, checks if installed, and launches it.
     */
    fun openApp(rawTargetName: String): LaunchResult {
        val normalizedTarget = normalize(rawTargetName)

        // 1. Check aliases first (e.g. "mensajería" -> "whatsapp", "música" -> "spotify")
        val aliasedName = Constants.CATEGORY_ALIASES[normalizedTarget] ?: normalizedTarget

        // 2. Check predefined known package map
        val knownPackage = Constants.POPULAR_APPS[aliasedName]
        if (knownPackage != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(knownPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                val label = getAppLabelSafely(knownPackage) ?: aliasedName.replaceFirstChar { it.uppercase() }
                return LaunchResult(
                    success = true,
                    appLabel = label,
                    packageName = knownPackage
                )
            }
        }

        // 3. Scan installed launchable activities dynamically
        val foundPackage = findInstalledPackageByLabel(aliasedName)
        if (foundPackage != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(foundPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                val label = getAppLabelSafely(foundPackage) ?: aliasedName
                return LaunchResult(
                    success = true,
                    appLabel = label,
                    packageName = foundPackage
                )
            }
        }

        // 4. App not found or not installed
        return LaunchResult(
            success = false,
            appLabel = rawTargetName,
            errorMessage = "No encuentro esa aplicación instalada en tu dispositivo."
        )
    }

    /**
     * Searches installed apps matching label fuzzy search
     */
    private fun findInstalledPackageByLabel(query: String): String? {
        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)

            // Exact or contains match on app label
            for (info in resolveInfos) {
                val label = normalize(info.loadLabel(packageManager).toString())
                if (label == query || label.contains(query) || query.contains(label)) {
                    return info.activityInfo.packageName
                }
            }

            // Also check package name sub-string
            for (info in resolveInfos) {
                val pkgName = normalize(info.activityInfo.packageName)
                if (pkgName.contains(query)) {
                    return info.activityInfo.packageName
                }
            }
        } catch (_: Exception) {
            // Ignore security exception if querying is restricted
        }
        return null
    }

    private fun getAppLabelSafely(packageName: String): String? {
        return try {
            val appInfo: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun normalize(text: String): String {
        val lower = text.trim().lowercase(Locale.ROOT)
        // Remove diacritics / accents (e.g. cámara -> camara, música -> musica)
        val normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}

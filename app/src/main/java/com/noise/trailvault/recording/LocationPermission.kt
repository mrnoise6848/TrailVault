package com.noise.trailvault.recording

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

object LocationPermission {
    val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    fun granted(context: Context) = ContextCompat.checkSelfPermission(context,
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    fun settingsRequired(activity: Activity): Boolean =
        activity.getPreferences(Context.MODE_PRIVATE).getBoolean("location_requested", false) &&
            !granted(activity) && !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    fun markRequested(activity: Activity) {
        activity.getPreferences(Context.MODE_PRIVATE).edit().putBoolean("location_requested", true).apply()
    }
    fun openSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")))
    }
}

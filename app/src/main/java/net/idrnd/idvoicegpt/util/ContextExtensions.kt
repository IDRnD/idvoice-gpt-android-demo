package net.idrnd.idvoicegpt.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.widget.Toast

const val APP = "app"

fun Context.getAppPreferences(): SharedPreferences = getSharedPreferences(APP, Context.MODE_PRIVATE)

fun Context.openAppInfo() {
    val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    startActivity(intent)
}

fun Context.toast(message: CharSequence) =
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()

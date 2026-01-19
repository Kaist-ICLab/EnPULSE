package kaist.iclab.tracker

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

class MetaData(
    context: Context,
) {
    private val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

    @SuppressLint("HardwareIds")
    val deviceUuid: String = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )
    val deviceModel: String = android.os.Build.MODEL

    //    val deviceName: String = android.os.Build.DEVICE
    val deviceName: String =
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
    val osVersion: String = android.os.Build.VERSION.RELEASE

    val appId = packageInfo.packageName

    val appVersionName = packageInfo.versionName
    val appVersionCode = packageInfo.longVersionCode

    val libVersion = "0.0.0"
}
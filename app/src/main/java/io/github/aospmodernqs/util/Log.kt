package io.github.aospmodernqs.util

import de.robv.android.xposed.XposedBridge

/** Log no LSPosed (aba "Logs" do módulo). */
object Log {
    private const val TAG = "[AOSP-Modern-QS] "

    @Volatile
    var debug = false

    fun i(msg: String) = XposedBridge.log(TAG + msg)
    fun d(msg: String) { if (debug) XposedBridge.log(TAG + msg) }
    fun e(msg: String, t: Throwable? = null) {
        XposedBridge.log(TAG + "ERRO: " + msg)
        if (t != null) XposedBridge.log(t)
    }
}

package io.github.aospmodernqs.hook

import de.robv.android.xposed.AndroidAppHelper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.aospmodernqs.util.Log

/**
 * Classe interceptada: com.android.systemui.statusbar.BlurUtils
 *  - supportsBlursOnWindows(): força true para manter o blur de janela quando o usuário o ativa.
 *  - applyBlur(ViewRootImpl, int radius, boolean opaque): reescala o raio pedido pela SystemUI
 *    para o raio escolhido nas configurações (proporcional, então as animações continuam suaves).
 *
 * Observação: em One UI o blur é feito por mecanismo próprio da Samsung; esta classe pode não existir.
 */
object BlurHook {
    fun install(cl: ClassLoader) {
        val clazz = Targets.find(cl, Targets.BLUR_UTILS) ?: return

        XposedBridge.hookAllMethods(clazz, "supportsBlursOnWindows", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (ModulePrefs.enabled && ModulePrefs.blurEnabled) param.result = true
            }
        })

        XposedBridge.hookAllMethods(clazz, "applyBlur", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!(ModulePrefs.enabled && ModulePrefs.blurEnabled)) return
                val radius = param.args.getOrNull(1) as? Int ?: return
                val defaultMax = try {
                    XposedHelpers.callMethod(param.thisObject, "getMaxBlurRadius") as Int
                } catch (t: Throwable) {
                    Log.d("getMaxBlurRadius indisponível: ${t.message}"); return
                }
                if (defaultMax <= 0) return
                val density = AndroidAppHelper.currentApplication()?.resources?.displayMetrics?.density ?: 3f
                val userMax = ModulePrefs.blurRadiusDp * density
                param.args[1] = (radius * userMax / defaultMax).toInt()
            }
        })
    }
}

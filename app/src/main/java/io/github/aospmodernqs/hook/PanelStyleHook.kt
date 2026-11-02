package io.github.aospmodernqs.hook

import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.view.View
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.aospmodernqs.util.Log

/**
 * Classe interceptada: com.android.systemui.qs.QSPanel (construtores e onConfigurationChanged).
 *
 * QSPanel é o container dos tiles. Ao ser criado (e quando o tema claro/escuro muda), aplicamos
 * um fundo translúcido com cantos arredondados usando as cores dinâmicas do Material You
 * (android.R.color.system_neutral1_*), no estilo do Android 16.
 */
object PanelStyleHook {
    fun install(cl: ClassLoader) {
        val clazz = Targets.find(cl, Targets.PANELS) ?: return

        val applyAfter = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!ModulePrefs.enabled) return
                runCatching { apply(param.thisObject as View) }
                    .onFailure { Log.e("PanelStyleHook.apply", it) }
            }
        }
        XposedBridge.hookAllConstructors(clazz, applyAfter)
        runCatching {
            XposedHelpers.findAndHookMethod(clazz, "onConfigurationChanged", Configuration::class.java, applyAfter)
        }.onFailure { Log.d("QSPanel.onConfigurationChanged não encontrado: ${it.message}") }
    }

    private fun apply(view: View) {
        val ctx = view.context
        val night = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val base = ctx.getColor(
            if (night) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_50
        )
        val alpha = ModulePrefs.panelAlpha * 255 / 100
        val color = (base and 0x00FFFFFF) or (alpha shl 24)
        view.background = GradientDrawable().apply {
            setColor(color)
            cornerRadius = ModulePrefs.cornerRadiusDp * ctx.resources.displayMetrics.density
        }
    }
}

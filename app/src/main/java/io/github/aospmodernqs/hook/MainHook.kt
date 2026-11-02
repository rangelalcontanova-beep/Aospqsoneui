package io.github.aospmodernqs.hook

import android.util.TypedValue
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodReplace
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XResources
import io.github.aospmodernqs.shared.Keys
import io.github.aospmodernqs.util.Log

/**
 * Ponto de entrada (assets/xposed_init). O LSPosed chama:
 *  - handleLoadPackage: para cada pacote no escopo (aqui: com.android.systemui e o próprio módulo);
 *  - handleInitPackageResources: ao carregar os recursos da SystemUI (números de colunas, raio de canto).
 *
 * Cada instalador é isolado em try/catch: se um alvo não existir na sua ROM, os demais continuam.
 */
class MainHook : IXposedHookLoadPackage, IXposedHookInitPackageResources {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            Keys.MODULE_PACKAGE -> safe("auto-detecção") {
                XposedHelpers.findAndHookMethod(
                    "io.github.aospmodernqs.ModuleStatus", lpparam.classLoader, "isActive",
                    XC_MethodReplace.returnConstant(true)
                )
            }
            Targets.SYSTEMUI -> {
                Log.i("SystemUI carregada; instalando hooks (enabled=${ModulePrefs.enabled})")
                val cl = lpparam.classLoader
                safe("TileLayoutHook") { TileLayoutHook.install(cl) }
                safe("PanelStyleHook") { PanelStyleHook.install(cl) }
                safe("BlurHook") { BlurHook.install(cl) }
                safe("EditModeHook") { EditModeHook.install(cl) }
            }
        }
    }

    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        if (param.packageName != Targets.SYSTEMUI || !ModulePrefs.enabled) return
        val res = param.res
        // Número de colunas da grade de Quick Settings.
        safe("res:colunas") {
            res.setReplacement(Targets.SYSTEMUI, "integer", "quick_settings_num_columns", ModulePrefs.columns)
        }
        // Raio dos cantos usado pelos tiles/painel.
        safe("res:cantos") {
            res.setReplacement(
                Targets.SYSTEMUI, "dimen", "qs_corner_radius",
                XResources.DimensionReplacement(ModulePrefs.cornerRadiusDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
            )
        }
    }

    private inline fun safe(name: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Log.e("falha ao instalar $name", t)
        }
    }
}

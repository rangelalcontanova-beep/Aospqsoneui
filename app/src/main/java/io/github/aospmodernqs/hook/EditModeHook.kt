package io.github.aospmodernqs.hook

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.aospmodernqs.util.Log

/**
 * Classe interceptada: com.android.systemui.qs.customize.TileAdapter (onBindViewHolder).
 *
 * TileAdapter alimenta o editor de tiles (QSCustomizer). Após cada bind, anexamos um detector
 * de duplo toque ao item: duplo toque alterna o tile entre 1x1 e 2x1 e reaplica o layout
 * do painel (via [TileLayoutHook.invalidateAll]). O toque simples/arrastar nativo não é afetado,
 * pois o listener de toque sempre devolve false.
 *
 * Se o spec exibido no log (modo debug) não bater com o tile tocado, ajuste
 * [Targets.EDIT_POSITION_OFFSET].
 */
object EditModeHook {
    fun install(cl: ClassLoader) {
        val clazz = Targets.find(cl, Targets.TILE_ADAPTERS) ?: return

        XposedBridge.hookAllMethods(clazz, "onBindViewHolder", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!(ModulePrefs.enabled && ModulePrefs.editMode)) return
                val position = param.args.getOrNull(1) as? Int ?: return
                val holder = param.args[0] ?: return
                runCatching {
                    val itemView = XposedHelpers.getObjectField(holder, "itemView") as View
                    val tiles = XposedHelpers.getObjectField(param.thisObject, "mTiles") as? List<*> ?: return
                    val info = tiles.getOrNull(position + Targets.EDIT_POSITION_OFFSET) ?: return
                    val spec = XposedHelpers.getObjectField(info, "spec") as? String ?: return
                    Log.d("editor: posição $position -> spec '$spec'")
                    attach(itemView, spec)
                }.onFailure { Log.d("EditModeHook: ${it.message}") }
            }
        })
    }

    private fun attach(itemView: View, spec: String) {
        val detector = GestureDetector(itemView.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val large = ModulePrefs.toggleLarge(spec)
                TileLayoutHook.invalidateAll()
                Toast.makeText(itemView.context, "$spec → ${if (large) "2x1" else "1x1"}", Toast.LENGTH_SHORT).show()
                return true
            }
        })
        itemView.setOnTouchListener { _, ev -> detector.onTouchEvent(ev); false }
    }
}

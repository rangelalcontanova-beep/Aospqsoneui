package io.github.aospmodernqs.hook

import android.content.Context
import android.os.SystemClock
import de.robv.android.xposed.AndroidAppHelper
import de.robv.android.xposed.XSharedPreferences
import io.github.aospmodernqs.shared.Keys
import io.github.aospmodernqs.util.Log

/**
 * Leitura das configurações dentro da SystemUI (somente leitura, via XSharedPreferences).
 * O arquivo só é reverificado a cada 2 s para não gastar bateria nem CPU durante o layout.
 *
 * Tamanhos dos tiles editados no modo de edição ficam num SharedPreferences local da SystemUI,
 * pois a SystemUI não pode escrever nas preferências do módulo.
 */
object ModulePrefs {
    private const val RUNTIME = "aosp_modern_qs_runtime"

    private val xp: XSharedPreferences by lazy {
        XSharedPreferences(Keys.MODULE_PACKAGE, Keys.PREFS).apply { makeWorldReadable() }
    }
    private var lastCheck = 0L
    private var largeCache: Set<String>? = null
    private var largeCacheAt = 0L

    @Synchronized
    private fun prefs(): XSharedPreferences {
        val now = SystemClock.uptimeMillis()
        if (now - lastCheck > 2_000) {
            lastCheck = now
            if (xp.hasFileChanged()) {
                xp.reload()
                largeCache = null
            }
            Log.debug = xp.getBoolean(Keys.DEBUG, false)
        }
        return xp
    }

    val enabled: Boolean get() = prefs().getBoolean(Keys.ENABLED, true)
    val modernGrid: Boolean get() = prefs().getBoolean(Keys.MODERN_GRID, true)
    val columns: Int get() = prefs().getInt(Keys.COLUMNS, 4).coerceIn(2, 6)
    val editMode: Boolean get() = prefs().getBoolean(Keys.EDIT_MODE, true)
    val blurEnabled: Boolean get() = prefs().getBoolean(Keys.BLUR, true)
    val blurRadiusDp: Int get() = prefs().getInt(Keys.BLUR_RADIUS, 40).coerceIn(0, 100)
    val panelAlpha: Int get() = prefs().getInt(Keys.PANEL_ALPHA, 85).coerceIn(20, 100)
    val cornerRadiusDp: Int get() = prefs().getInt(Keys.CORNER_RADIUS, 28).coerceIn(0, 48)

    private fun parse(s: String?): Set<String> =
        s.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    /** Specs dos tiles 2x1: edição em tempo de execução vence, salvo se o usuário mudou a lista no app. */
    @Synchronized
    fun largeTiles(): Set<String> {
        val p = prefs()
        val now = SystemClock.uptimeMillis()
        largeCache?.let { if (now - largeCacheAt < 1_000) return it }

        val rev = p.getInt(Keys.LARGE_TILES_REV, 0)
        val seed = parse(p.getString(Keys.LARGE_TILES, Keys.DEFAULT_LARGE_TILES))
        val app = AndroidAppHelper.currentApplication()
        val result = if (app == null) {
            seed
        } else {
            val rt = app.getSharedPreferences(RUNTIME, Context.MODE_PRIVATE)
            if (rt.getInt("base_rev", -1) == rev && rt.contains("tiles")) parse(rt.getString("tiles", "")) else seed
        }
        largeCache = result
        largeCacheAt = now
        return result
    }

    /** Alterna o tile entre 1x1 e 2x1. Retorna true se agora é 2x1. */
    @Synchronized
    fun toggleLarge(spec: String): Boolean {
        val current = largeTiles().toMutableSet()
        val nowLarge = if (current.remove(spec)) false else { current.add(spec); true }
        val app = AndroidAppHelper.currentApplication() ?: return nowLarge
        app.getSharedPreferences(RUNTIME, Context.MODE_PRIVATE).edit()
            .putInt("base_rev", prefs().getInt(Keys.LARGE_TILES_REV, 0))
            .putString("tiles", current.joinToString(","))
            .apply()
        largeCache = null
        return nowLarge
    }
}

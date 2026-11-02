package io.github.aospmodernqs.hook

import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XC_MethodReplace
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.aospmodernqs.util.Log
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * Classe interceptada: com.android.systemui.qs.TileLayout (onMeasure / onLayout).
 *
 * No AOSP, TileLayout é um ViewGroup que mantém `mRecords` (lista de TileRecord com `tile` e
 * `tileView`) e posiciona todos os tiles em células iguais. Aqui substituímos medição e
 * posicionamento por uma grade com "spans": tiles listados em [ModulePrefs.largeTiles] ocupam
 * 2 colunas (2x1); os demais, 1 coluna (1x1).
 *
 * Segurança: qualquer exceção chama o método original; após 3 falhas o hook se desativa sozinho.
 *
 * Limitação conhecida: o PagedTileLayout calcula quantos tiles cabem por página como
 * colunas x linhas; com tiles 2x1 uma página pode ficar mais alta que o previsto.
 */
object TileLayoutHook {
    private class Slot(val view: View, val col: Int, val row: Int, val span: Int)

    private val layouts: MutableSet<ViewGroup> = Collections.newSetFromMap(WeakHashMap())
    private val specCache = WeakHashMap<View, String>()
    private var failures = 0

    fun install(cl: ClassLoader) {
        val clazz = Targets.find(cl, Targets.TILE_LAYOUTS) ?: return
        val int = Int::class.javaPrimitiveType
        XposedHelpers.findAndHookMethod(clazz, "onMeasure", int, int, object : XC_MethodReplace() {
            override fun replaceHookedMethod(param: MethodHookParam): Any? {
                if (!active()) return original(param)
                return try {
                    measure(param.thisObject as ViewGroup, param.args[0] as Int, param.args[1] as Int)
                    null
                } catch (t: Throwable) {
                    fail(t); original(param)
                }
            }
        })
        XposedHelpers.findAndHookMethod(
            clazz, "onLayout", Boolean::class.javaPrimitiveType, int, int, int, int,
            object : XC_MethodReplace() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    if (!active()) return original(param)
                    return try {
                        layout(param.thisObject as ViewGroup, param.args[3] as Int - param.args[1] as Int)
                        null
                    } catch (t: Throwable) {
                        fail(t); original(param)
                    }
                }
            })
    }

    /** Força novo layout em todas as grades vivas (após alternar o tamanho de um tile). */
    fun invalidateAll() {
        layouts.toList().forEach { it.post { it.requestLayout() } }
    }

    private fun active() = failures < 3 && ModulePrefs.enabled && ModulePrefs.modernGrid

    private fun original(p: MethodHookParam): Any? =
        XposedBridge.invokeOriginalMethod(p.method, p.thisObject, p.args)

    private fun fail(t: Throwable) {
        failures++
        Log.e("TileLayout falhou ($failures/3); usando layout original", t)
    }

    private fun specOf(record: Any, v: View): String = specCache.getOrPut(v) {
        runCatching {
            val tile = XposedHelpers.getObjectField(record, "tile")
            XposedHelpers.callMethod(tile, "getTileSpec") as String
        }.getOrDefault("")
    }

    private fun intField(o: Any, name: String, def: Int): Int =
        try { XposedHelpers.getIntField(o, name) } catch (_: Throwable) { def }

    private fun setInt(o: Any, name: String, v: Int) {
        try { XposedHelpers.setIntField(o, name, v) } catch (_: Throwable) { }
    }

    private fun place(layout: ViewGroup, cols: Int): Pair<List<Slot>, Int> {
        val records = XposedHelpers.getObjectField(layout, "mRecords") as List<*>
        val large = ModulePrefs.largeTiles()
        val out = ArrayList<Slot>(records.size)
        var col = 0
        var row = 0
        for (r in records) {
            r ?: continue
            val v = XposedHelpers.getObjectField(r, "tileView") as View
            if (v.visibility == View.GONE) continue
            val span = if (specOf(r, v) in large) min(2, cols) else 1
            if (col + span > cols) { col = 0; row++ }
            out += Slot(v, col, row, span)
            col += span
            if (col >= cols) { col = 0; row++ }
        }
        val rows = if (out.isEmpty()) 0 else out.maxOf { it.row } + 1
        return out to rows
    }

    private fun measure(layout: ViewGroup, wSpec: Int, @Suppress("UNUSED_PARAMETER") hSpec: Int) {
        layouts.add(layout)
        val d = layout.resources.displayMetrics.density
        val width = View.MeasureSpec.getSize(wSpec)
        val cols = ModulePrefs.columns
        val gapH = intField(layout, "mCellMarginHorizontal", (8 * d).toInt())
        val gapV = intField(layout, "mCellMarginVertical", (8 * d).toInt())
        var cellH = intField(layout, "mCellHeight", 0)
        if (cellH <= 0) cellH = (72 * d).toInt()

        val inner = width - layout.paddingLeft - layout.paddingRight
        val cellW = (inner - gapH * (cols - 1)) / cols
        val (slots, rows) = place(layout, cols)

        for (s in slots) {
            val w = cellW * s.span + gapH * (s.span - 1)
            s.view.measure(
                View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(cellH, View.MeasureSpec.EXACTLY)
            )
        }
        val height = rows * cellH + max(0, rows - 1) * gapV + layout.paddingTop + layout.paddingBottom
        setInt(layout, "mColumns", cols)
        setInt(layout, "mRows", rows)
        setInt(layout, "mCellWidth", cellW)
        XposedHelpers.callMethod(layout, "setMeasuredDimension", width, height)
    }

    private fun layout(layout: ViewGroup, width: Int) {
        val cols = ModulePrefs.columns
        val d = layout.resources.displayMetrics.density
        val gapH = intField(layout, "mCellMarginHorizontal", (8 * d).toInt())
        val gapV = intField(layout, "mCellMarginVertical", (8 * d).toInt())
        var cellH = intField(layout, "mCellHeight", 0)
        if (cellH <= 0) cellH = (72 * d).toInt()
        val inner = width - layout.paddingLeft - layout.paddingRight
        val cellW = (inner - gapH * (cols - 1)) / cols
        val rtl = layout.layoutDirection == View.LAYOUT_DIRECTION_RTL

        var bottom = 0
        for (s in place(layout, cols).first) {
            val w = cellW * s.span + gapH * (s.span - 1)
            val offset = s.col * (cellW + gapH)
            val x = if (rtl) width - layout.paddingRight - offset - w else layout.paddingLeft + offset
            val y = layout.paddingTop + s.row * (cellH + gapV)
            s.view.layout(x, y, x + w, y + cellH)
            bottom = max(bottom, y + cellH)
        }
        setInt(layout, "mLastTileBottom", bottom)
    }
}

package io.github.aospmodernqs.hook

import de.robv.android.xposed.XposedHelpers
import io.github.aospmodernqs.util.Log

/**
 * Nomes de classes da SystemUI que o módulo tenta interceptar.
 *
 * ATENÇÃO: os nomes abaixo são os do AOSP. ROMs diferentes (principalmente One UI/Samsung,
 * que usa classes "Sec*") podem renomear ou remover essas classes. Ligue o "Log de depuração"
 * e veja no log do LSPosed quais alvos foram encontrados; ajuste as listas conforme o seu
 * APK da SystemUI (jadx/apktool).
 */
object Targets {
    const val SYSTEMUI = "com.android.systemui"

    /** Grade de tiles view-based (AOSP: TileLayout; PagedTileLayout usa TileLayout por página). */
    val TILE_LAYOUTS = listOf(
        "com.android.systemui.qs.TileLayout",
        "com.android.systemui.qs.SecTileLayout"
    )

    /** Container do painel de tiles (recebe fundo translúcido/arredondado). */
    val PANELS = listOf(
        "com.android.systemui.qs.QSPanel",
        "com.android.systemui.qs.SecQSPanel"
    )

    /** Utilitário de blur de janela da SystemUI. */
    val BLUR_UTILS = listOf("com.android.systemui.statusbar.BlurUtils")

    /** Adapter do editor de tiles (QSCustomizer). */
    val TILE_ADAPTERS = listOf("com.android.systemui.qs.customize.TileAdapter")

    /** Ajuste fino: deslocamento entre a posição do adapter e o índice em TileAdapter.mTiles. */
    const val EDIT_POSITION_OFFSET = 0

    fun find(cl: ClassLoader, names: List<String>): Class<*>? {
        for (n in names) {
            val c = XposedHelpers.findClassIfExists(n, cl)
            if (c != null) {
                Log.i("alvo encontrado: $n")
                return c
            }
            Log.d("alvo ausente: $n")
        }
        Log.e("nenhum alvo encontrado em $names")
        return null
    }
}

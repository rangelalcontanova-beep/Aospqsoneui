package io.github.aospmodernqs.shared

/** Chaves compartilhadas entre a Activity de configurações e os hooks da SystemUI. */
object Keys {
    const val MODULE_PACKAGE = "io.github.aospmodernqs"
    const val PREFS = "aosp_modern_qs_prefs"

    const val ENABLED = "enabled"
    const val DEBUG = "debug_log"
    const val MODERN_GRID = "modern_grid"
    const val COLUMNS = "columns"
    const val LARGE_TILES = "large_tiles"
    const val LARGE_TILES_REV = "large_tiles_rev" // sobe quando o usuário edita a lista no app
    const val EDIT_MODE = "edit_mode"
    const val BLUR = "blur"
    const val BLUR_RADIUS = "blur_radius"
    const val PANEL_ALPHA = "panel_alpha"
    const val CORNER_RADIUS = "corner_radius"

    const val DEFAULT_LARGE_TILES = "internet,bt"
}

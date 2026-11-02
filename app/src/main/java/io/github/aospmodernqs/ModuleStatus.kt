package io.github.aospmodernqs

/**
 * Auto-detecção: o MainHook substitui [isActive] para retornar true quando
 * este próprio app é carregado pelo LSPosed. Sem hook, retorna false.
 */
object ModuleStatus {
    @JvmStatic
    fun isActive(): Boolean = false
}

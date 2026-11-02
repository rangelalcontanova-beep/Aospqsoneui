package io.github.aospmodernqs.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.aospmodernqs.ModuleStatus
import io.github.aospmodernqs.R
import io.github.aospmodernqs.shared.Keys

/**
 * Salva as opções em um arquivo legível pela SystemUI (XSharedPreferences).
 * Se o módulo não estiver ativo, o modo "world readable" é negado pelo sistema e
 * caímos para MODE_PRIVATE (a tela funciona, mas a SystemUI não lerá os valores).
 */
class SettingsFragment : PreferenceFragmentCompat() {

    @SuppressLint("WorldReadableFiles")
    @Suppress("DEPRECATION")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val pm = preferenceManager
        pm.sharedPreferencesName = Keys.PREFS
        try {
            pm.sharedPreferencesMode = Context.MODE_WORLD_READABLE
            setPreferencesFromResource(R.xml.preferences, rootKey)
        } catch (_: SecurityException) {
            pm.sharedPreferencesMode = Context.MODE_PRIVATE
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }

        findPreference<Preference>("status")?.summary = getString(
            if (ModuleStatus.isActive()) R.string.status_active else R.string.status_inactive
        )

        // Quando a lista de tiles largos muda no app, ela passa a valer sobre edições feitas no editor.
        findPreference<Preference>(Keys.LARGE_TILES)?.setOnPreferenceChangeListener { _, _ ->
            val sp = preferenceManager.sharedPreferences
            sp?.edit()?.putInt(Keys.LARGE_TILES_REV, sp.getInt(Keys.LARGE_TILES_REV, 0) + 1)?.apply()
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(listView) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bars.bottom)
            insets
        }
    }
}

package io.github.aospmodernqs.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** Tela de configurações do módulo (Material You via tema DynamicColors). */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(io.github.aospmodernqs.R.string.app_name)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }
}

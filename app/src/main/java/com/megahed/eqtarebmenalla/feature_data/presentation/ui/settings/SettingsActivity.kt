package com.megahed.eqtarebmenalla.feature_data.presentation.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.ThemeHelper

class SettingsActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences
    var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        ThemeHelper.applyTheme(preferences)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        title = getString(R.string.settings)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var preferences: SharedPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

            findPreference<androidx.preference.SwitchPreferenceCompat>("pref_dark_mode")?.setOnPreferenceChangeListener { _, newValue ->
                ThemeHelper.applyTheme(preferences)
                requireActivity().recreate()
                true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}
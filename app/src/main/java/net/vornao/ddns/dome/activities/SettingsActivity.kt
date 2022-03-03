package net.vornao.ddns.dome.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.vornao.ddns.dome.R
import net.vornao.ddns.dome.fragments.SettingsFragment

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_layout)

        //set app MainActivity toolbar
        supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, SettingsFragment())
                .commit()
    }
}
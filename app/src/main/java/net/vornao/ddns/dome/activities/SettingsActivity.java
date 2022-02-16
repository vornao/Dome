package net.vornao.ddns.dome.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import net.vornao.ddns.dome.R;
import net.vornao.ddns.dome.fragments.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        //set app MainActivity toolbar
        getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, new SettingsFragment())
                .commit();
    }
}

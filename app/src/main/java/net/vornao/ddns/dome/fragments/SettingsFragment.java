package net.vornao.ddns.dome.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import net.vornao.ddns.dome.R;
import net.vornao.ddns.dome.activities.AboutActivity;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);


        EditTextPreference editTextPreference = findPreference("mqtt_password");
        if (editTextPreference != null) {
            editTextPreference.setOnBindEditTextListener(
                    editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));

        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String MQTT_SERVER = sharedPreferences.getString("broker", "Select MQTT server name");
        String MQTT_PORT = sharedPreferences.getString("broker_port", "Default: 1883");
        String MQTT_USERNAME = sharedPreferences.getString("mqtt_username", "Select MQTT username");
        String MQTT_TOPIC = sharedPreferences.getString("mqtt-topic", null);


        EditTextPreference serverPreference = findPreference("broker");
        if (serverPreference != null) {
            serverPreference.setSummary(MQTT_SERVER);
        }

        EditTextPreference userPreference = findPreference("mqtt_username");
        if (userPreference != null) {
            userPreference.setSummary(MQTT_USERNAME);
        }

        EditTextPreference portPreference = findPreference("broker_port");
        if (portPreference != null) {
            portPreference.setSummary(MQTT_PORT);
        }

        EditTextPreference topicPreference = findPreference("mqtt-topic");
        if (topicPreference != null) {
            topicPreference.setSummary(MQTT_TOPIC);
        }


        EditTextPreference powerPreference = findPreference("emon_max_power");
        assert powerPreference != null;
        powerPreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

        Preference feedbackPreference = findPreference("feedback");
        assert feedbackPreference != null;
        feedbackPreference.setOnPreferenceClickListener(preference -> {

            Intent i = new Intent(Intent.ACTION_SENDTO);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{"l.miglior@studenti.unipi.it"});
            i.putExtra(Intent.EXTRA_SUBJECT, "[Dome] Application feedback");
            startActivity(i);
            return true;
        });

        Preference aboutPreference = findPreference("about");
        assert aboutPreference != null;
        aboutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                Intent startAbout = new Intent(getContext(), AboutActivity.class);
                startActivity(startAbout);
                return false;
            }
        });
    }
}
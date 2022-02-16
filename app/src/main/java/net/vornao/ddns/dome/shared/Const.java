package net.vornao.ddns.dome.shared;

import android.content.Context;

import androidx.annotation.NonNull;

import net.vornao.ddns.dome.R;

import java.util.HashMap;

public class Const {
    public static final String DEVICE_ID = "device-id";
    public static final String DEVICE_TYPE = "device-type";
    public static final String REMOTE_SWITCH = "remote-switch";
    public static final String THERMOSTAT = "thermostat";
    public static final String ENERGY_MONITOR = "energy-monitor";
    public static final String STATUS_KEY = "current-status";
    public static final String SWITCH_CHANGE_KEY = "mode";
    public static final String THERM_TEMP_KEY = "temperature";
    public static final String THERM_HUM_KEY = "humidity";
    public static final String THERM_THRESHOLD_KEY = "current-threshold";
    public static final String EMON_DATA_PATH_KEY = "data-path";
    public static final String EMON_POWER = "current-power";

    //config const

    public static final String MQTT_TOPIC_C = "mqtt-topic";
    public static final String MQTT_BROKER_C = "broker";
    public static final String MQTT_USER_C   = "mqtt_username";
    public static final String MQTT_PASS_C   = "mqtt_password";
    public static final String MQTT_PORT_C   = "broker_port";

    public static final HashMap<String, Integer> deviceType = new HashMap<String, Integer>(){
        {
            put(THERMOSTAT, 0);
            put(ENERGY_MONITOR, 1);
            put(REMOTE_SWITCH, 2);
        }
    };


    public static final HashMap<Integer, String> thermostatModes = new HashMap<Integer, String>(){
        {
            put(0,"Thermostat off");
            put(1, "Thermostat on");
            put(2, "Thermostat auto");
        }
    };

    public static final HashMap<Integer, String> daysOfWeek = new HashMap<Integer, String>() {
        {
            put(0,"Monday");
            put(1, "Tuesday");
            put(2, "Wednesday");
            put(3, "Thursday");
            put(4, "Friday");
            put(5, "Saturday");
            put(6, "Sunday");
        }
    };

    public static String getEnergyImpactString(float p, float MAX_POWER, @NonNull Context context){
        if(p < (MAX_POWER / 4)) return context.getResources().getString(R.string.low_impact);
        else if (p > (MAX_POWER / 4) && p < (MAX_POWER / 2)) return context.getResources().getString(R.string.medium_impact);
        else if (p > (MAX_POWER / 2) && p < (float)(MAX_POWER / 1.4)) return context.getResources().getString(R.string.high_impact);
        else return context.getResources().getString(R.string.very_high_impact);
    }

    @NonNull
    public static final HashMap<Integer, Integer> realWeek = new HashMap<Integer, Integer>(){
        {
            put(1, 7);
            put(2, 1);
            put(3, 2);
            put(4, 3);
            put(5, 4);
            put(6, 5);
            put(7, 6);
        }
    };

}

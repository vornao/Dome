package net.vornao.ddns.dome.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import net.vornao.ddns.dome.R;
import net.vornao.ddns.dome.activities.EnergyMonitorActivity;
import net.vornao.ddns.dome.activities.ThermostatActivity;
import net.vornao.ddns.dome.callbacks.UpdatesCallback;
import net.vornao.ddns.dome.devices.Device;
import net.vornao.ddns.dome.devices.EnergyMonitor;
import net.vornao.ddns.dome.devices.RemoteSwitch;
import net.vornao.ddns.dome.devices.Thermostat;
import net.vornao.ddns.dome.fragments.BottomSheetFragment;
import net.vornao.ddns.dome.handler.DatabaseHelper;
import net.vornao.ddns.dome.shared.Const;

import java.util.ArrayList;

public class RVAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final ArrayList<Device> deviceList;
    private final Context parentContext;
    private final RecyclerView rv;
    private final int MAX_POWER;
    private final UpdatesCallback updatesCallback;


    public RVAdapter(ArrayList<Device> devices, int maxPower, RecyclerView rv, UpdatesCallback uc, Context context){
        this.deviceList = devices;
        parentContext = context;

        this.MAX_POWER = maxPower;
        this.updatesCallback = uc;
        this.rv = rv;

    }
    public static class EnergyMonitorViewHolder extends RecyclerView.ViewHolder{
        final CardView cv;
        final TextView name;
        final TextView power;
        final TextView energy;
        final TextView offline;
        public EnergyMonitorViewHolder(@NonNull View itemView) {
            super(itemView);
            cv     = itemView.findViewById(R.id.emon_cv);
            name   = itemView.findViewById(R.id.emon_name);
            power  = itemView.findViewById(R.id.current_power);
            energy = itemView.findViewById(R.id.current_energy);
            offline= itemView.findViewById(R.id.offline_emon);
        }
    }

    public static class ThermostatViewHolder extends RecyclerView.ViewHolder{

        final CardView cv;
        final TextView name;
        final TextView temperature;
        final TextView humidity;
        final TextView offline;
        final LinearLayout linearLayout;

        public ThermostatViewHolder(@NonNull View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.thermostat_cardview);
            name = itemView.findViewById(R.id.device_name);
            temperature = itemView.findViewById(R.id.device_temp);
            humidity = itemView.findViewById(R.id.device_hum);
            offline = itemView.findViewById(R.id.offline_therm);
            linearLayout = itemView.findViewById(R.id.thermostat_card_ll);
        }
    }

    public static class RemoteSwitchViewHolder extends RecyclerView.ViewHolder{
        final CardView cv;
        final SwitchCompat toggle;
        final EditText name;
        final EditText desc;
        final TextView offline;


        public RemoteSwitchViewHolder(@NonNull View itemView){
            super(itemView);
            cv = itemView.findViewById(R.id.remote_switch_cardview);
            toggle = itemView.findViewById(R.id.rs_toggle);
            name = itemView.findViewById(R.id.switch_name);
            desc = itemView.findViewById(R.id.switch_desc);
            offline = itemView.findViewById(R.id.offline_switch);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return Const.deviceType.get(deviceList.get(position).getType());
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType){
            // thermostat
            case 0:
                View v0 = LayoutInflater.from(parent.getContext()).inflate(R.layout.thermostat_card, parent, false);
                return new ThermostatViewHolder(v0);
            case 1:
                View v1 = LayoutInflater.from(parent.getContext()).inflate(R.layout.emon_card, parent, false);
                return new EnergyMonitorViewHolder(v1);
            default:
                View v3 = LayoutInflater.from(parent.getContext()).inflate(R.layout.remote_switch_card, parent, false);
                return new RemoteSwitchViewHolder(v3);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {

        switch (holder.getItemViewType()){
            case 0:

                Thermostat thermostat = (Thermostat) deviceList.get(pos);
                ThermostatViewHolder tvh = (ThermostatViewHolder)holder;
                tvh.name.setText(thermostat.getName());
                tvh.name.setTextColor(parentContext.getColor(R.color.light_grey));

                if(!thermostat.isOffline()) {
                    tvh.name.setTextColor(parentContext.getColor(R.color.black));
                    tvh.offline.setVisibility(View.GONE);
                    tvh.temperature.setVisibility(View.VISIBLE);
                    tvh.humidity.setVisibility(View.VISIBLE);
                    tvh.humidity.setText(String.format(parentContext.getString(R.string.humidity_cardview), thermostat.getHum()));
                    tvh.temperature.setText(String.format(parentContext.getString(R.string.temperature_cardview), thermostat.getTemp()));
                }

                ((ThermostatViewHolder) holder).cv.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), ThermostatActivity.class);
                    intent.putExtra(Const.DEVICE_ID, thermostat.getId());
                    v.getContext().startActivity(intent);
                });

                break;

            case 1:
                EnergyMonitorViewHolder evh = (EnergyMonitorViewHolder)holder;
                EnergyMonitor energyMonitor = ((EnergyMonitor) deviceList.get(pos));

                evh.name.setText(energyMonitor.getName());
                evh.name.setTextColor(parentContext.getColor(R.color.light_grey));

                if(!energyMonitor.isOffline()) {
                    evh.name.setTextColor(parentContext.getColor(R.color.black));
                    evh.offline.setVisibility(View.GONE);
                    evh.energy.setVisibility(View.VISIBLE);
                    evh.power.setVisibility(View.VISIBLE);
                    evh.power.setText(String.format(parentContext.getString(R.string.power_cardview), (energyMonitor.getPower())));
                    evh.energy.setText(String.format(
                            "%s %s",
                            Const.getEnergyImpactString(
                                energyMonitor.getPower(),
                                    MAX_POWER,
                                    parentContext
                            ),
                            parentContext
                                    .getResources()
                                    .getString(R.string.lowercase_energy_impact))
                    );

                }

                ((EnergyMonitorViewHolder) holder).cv.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), EnergyMonitorActivity.class);
                    intent.putExtra(Const.DEVICE_ID, deviceList.get(pos).getId());
                    v.getContext().startActivity(intent);
                });

                break;
            default:

                RemoteSwitchViewHolder rvh = (RemoteSwitchViewHolder)holder;
                RemoteSwitch remoteSwitch = (RemoteSwitch) deviceList.get(pos);
                rvh.name.setText(remoteSwitch.getName());
                if(remoteSwitch.getDescription() == null) rvh.desc.setText(String.valueOf(remoteSwitch.getId()));
                else rvh.desc.setText(remoteSwitch.getDescription());
                rvh.name.setTextColor(parentContext.getColor(R.color.light_grey));
                rvh.name.setOnFocusChangeListener((view, b) -> {
                    if(b) updatesCallback.disableUpdates();
                });
                rvh.name.setOnEditorActionListener((textView, i, keyEvent) -> {
                    if(i == EditorInfo.IME_ACTION_DONE){
                        Log.i("EDITORACTION", rvh.name.getText().toString());
                        updateDeviceName(rvh.name.getText().toString(), deviceList.get(pos));
                        rvh.name.clearFocus();
                        updatesCallback.enableUpdates();
                        return false;
                    }
                    return true;
                });
                rvh.desc.setOnFocusChangeListener((view, b) -> {
                    if(b) updatesCallback.disableUpdates();
                });
                rvh.desc.setOnEditorActionListener((textView, i, keyEvent) -> {
                    if(i == EditorInfo.IME_ACTION_DONE){
                        Log.i("EDITORACTION", rvh.desc.getText().toString());
                        updateDeviceDescription(rvh.desc.getText().toString(), deviceList.get(pos));
                        rvh.desc.clearFocus();
                        updatesCallback.enableUpdates();
                        return false;
                    }
                    return true;
                });

                if(!remoteSwitch.isOffline()) {
                    rvh.name.setTextColor(parentContext.getColor(R.color.black));
                    rvh.desc.setTextColor(parentContext.getColor(R.color.light_grey));
                    rvh.desc.setVisibility(View.VISIBLE);
                    rvh.toggle.setVisibility(View.VISIBLE);
                    rvh.offline.setVisibility(View.GONE);
                }

                if(remoteSwitch.getMode() == 0){
                    rvh.toggle.setText(R.string.switch_off);
                    rvh.toggle.setChecked(false);
                    rvh.toggle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_outline_lightbulb_24, 0,0,0);
                } else{
                    rvh.toggle.setChecked(true);
                    rvh.toggle.setText(R.string.switch_on);
                    rvh.toggle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lightbulb, 0,0,0);
                }

                rvh.toggle.setOnClickListener(view -> {
                    if(remoteSwitch.getMode() < 1){
                        remoteSwitch.setOn();
                        rvh.toggle.setChecked(true);
                        rvh.toggle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lightbulb, 0,0,0);
                    }else {
                        remoteSwitch.setOff();
                        rvh.toggle.setChecked(false);
                        rvh.toggle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_outline_lightbulb_24, 0,0,0);
                    }
                });

                rvh.cv.setOnClickListener(view -> {
                    BottomSheetFragment b = new BottomSheetFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString(Const.DEVICE_ID, String.valueOf(remoteSwitch.getId()));
                    bundle.putString(Const.MQTT_TOPIC_C, remoteSwitch.getHome_id());
                    b.setArguments(bundle);
                    b.show(((AppCompatActivity) parentContext).getSupportFragmentManager(), "bsfd_fragment");
                });

                break;
        }
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }


    private void updateDeviceName(String newName, @NonNull Device d){

        d.setName(newName);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(rv.getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.DEVICE_NAME, newName);

        String selection = DatabaseHelper.DEVICE_id + "= ?";
        String [] args      = {String.valueOf(d.getId())};

        db.update(
                DatabaseHelper.DEVICE_TABLE,
                values,
                selection,
                args
        );
        db.close();
    }

    private void updateDeviceDescription(String newDesc, @NonNull Device d){
        d.setDescription(newDesc);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(rv.getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.DEVICE_INFO, newDesc);

        String selection = DatabaseHelper.DEVICE_id + "= ?";
        String [] args      = {String.valueOf(d.getId())};

        db.update(
                DatabaseHelper.DEVICE_TABLE,
                values,
                selection,
                args
        );
        db.close();
    }

}

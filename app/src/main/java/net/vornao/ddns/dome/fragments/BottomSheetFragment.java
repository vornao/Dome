package net.vornao.ddns.dome.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import net.vornao.ddns.dome.R;
import net.vornao.ddns.dome.shared.Const;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.device_details_bottomsheet, container, false);

    }

    @Override public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TextView deviceIP = view.findViewById(R.id.ddevice_ip);
        TextView deviceName = view.findViewById(R.id.ddevice_name);
        TextView devicePubTopic = view.findViewById(R.id.ddevice_pub_topic);
        TextView deviceSubTopic = view.findViewById(R.id.ddevice_sub_topic);

        // deviceIP.setText(String.format(getString(R.string.ip_address_info), "192.168.173.34"));
        try {
            assert getArguments() != null;
        }catch (AssertionError e){
            return;
        }
        deviceName.setText(String.format(getString(R.string.device_id_info), getArguments().getString(Const.DEVICE_ID)));
        deviceSubTopic.setText(String.format(getString(R.string.device_sub_info), getArguments().getString(Const.MQTT_TOPIC_C),getArguments().getString(Const.DEVICE_ID)));
        devicePubTopic.setText(String.format(getString(R.string.device_pub_info), getArguments().getString(Const.MQTT_TOPIC_C)));
    }

}

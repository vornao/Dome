package net.vornao.ddns.dome.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import net.vornao.ddns.dome.R;

import net.vornao.ddns.dome.shared.Const;

import java.util.HashMap;

public class HouseSelectionFragment extends BottomSheetDialogFragment {

    private final HashMap<String, Integer> indexMapping = new HashMap<>();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.change_house_bottomsheet, container, false);

    }

    @Override public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String[] houses = getArguments().getStringArray("available-topics");

        if(houses == null){
            Snackbar.make(getView(), R.string.generic_connection_error_message, Snackbar.LENGTH_SHORT);
            return;
        }

        TextView title = view.findViewById(R.id.titleChange);
        LinearLayout ll = view.findViewById(R.id.layoutHouseSelection);
        for (int i = 0; i < houses.length; i++) {

            TextView textView = new TextView(getContext());
            textView.setText(String.format(getString(R.string.house_selection_text), i, houses[i]));
            indexMapping.put(textView.getText().toString(), i);
            textView.setTextSize(18);
            textView.setBackground(getResources().getDrawable(R.drawable.ripple_teal, getContext().getTheme()));
            textView.setPaddingRelative(title.getPaddingLeft(), title.getPaddingTop()/2,0,title.getPaddingLeft()/2);
            textView.setOnClickListener(view1 -> {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
                try {
                    sp.edit().putString(Const.MQTT_TOPIC_C, houses[indexMapping.get(((TextView) view1).getText().toString())]).apply();
                }catch (NullPointerException e){
                    Log.d(this.getClass().getName(), e.toString());
                }finally {
                    dismiss();
                }
            });
            ll.addView(textView);
        }

    }
}

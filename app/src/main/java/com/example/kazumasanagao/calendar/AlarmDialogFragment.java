package com.example.kazumasanagao.calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;

/**
 * Created by kazumasanagao on 8/2/15.
 */
public class AlarmDialogFragment extends DialogFragment {

    public static AlarmDialogFragment newInstance()
    {
        return new AlarmDialogFragment();
    }

    private TimePicker tPicker;
    private Intent bootIntent;
    private PendingIntent alarmIntent;
    private AlarmManager alarm;
    private Switch sw;
    private Activity me;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.alerm_dialog, null, false);

        me = getActivity();

        tPicker  =  (TimePicker)view.findViewById(R.id.timePicker);
        tPicker.setIs24HourView(true);
        tPicker.setCurrentHour(getHour());
        tPicker.setCurrentMinute(getMinute());

        sw = (Switch)view.findViewById(R.id.alermSwitch);
        sw.setChecked(checkAlerm());

        tPicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                if (sw.isChecked()) {
                    saveAlerm();
                }
            }
        });
        sw.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    saveAlerm();
                } else {
                    deleteAlerm();
                }
            }
        });
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void saveAlerm() {
        bootIntent = new Intent(me, AlarmBroadcastReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(me, 0, bootIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarm = (AlarmManager) me.getSystemService(Context.ALARM_SERVICE);

        int hour = tPicker.getCurrentHour();
        int minute = tPicker.getCurrentMinute();
        Calendar startTime = Calendar.getInstance();
        startTime.set(Calendar.HOUR_OF_DAY, hour);
        startTime.set(Calendar.MINUTE, minute);
        startTime.set(Calendar.SECOND, 0);
        long alarmStartTime = startTime.getTimeInMillis();

        alarm.setRepeating(
                AlarmManager.RTC_WAKEUP,
                alarmStartTime,
                AlarmManager.INTERVAL_DAY,
                alarmIntent
        );

        SharedPreferences.Editor editor = me.getSharedPreferences("shared_preference", Context.MODE_PRIVATE).edit();
        editor.putInt("hour", hour);
        editor.putInt("minute", minute);
        editor.putBoolean("switch", true);
        editor.commit();
    }

    public void deleteAlerm() {
        bootIntent = new Intent(me, AlarmBroadcastReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(me, 0, bootIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarm = (AlarmManager) me.getSystemService(Context.ALARM_SERVICE);

        alarm.cancel(alarmIntent);
        alarmIntent.cancel();
        SharedPreferences.Editor editor = me.getSharedPreferences("shared_preference", Context.MODE_PRIVATE).edit();
        editor.putBoolean("switch", false);
        editor.commit();
    }

    public boolean checkAlerm() {
        return (PendingIntent.getBroadcast(me, 0,
                new Intent(me, AlarmBroadcastReceiver.class),
                PendingIntent.FLAG_NO_CREATE) != null);
    }

    public Integer getHour() {
        Bundle bundle = new Bundle();
        Map<String, ?> prefKV = me.getSharedPreferences("shared_preference", Context.MODE_PRIVATE).getAll();
        Set<String> keys = prefKV.keySet();
        for(String key : keys){
            Object value = prefKV.get(key);
            if(value instanceof String){
                bundle.putString(key, (String) value);
            }else if(value instanceof Integer){
                bundle.putString(key, value.toString());
            }
        }
        String hour = bundle.getString("hour", "21");
        return Integer.parseInt(hour);
    }

    public Integer getMinute() {
        Bundle bundle = new Bundle();
        Map<String, ?> prefKV = me.getSharedPreferences("shared_preference", Context.MODE_PRIVATE).getAll();
        Set<String> keys = prefKV.keySet();
        for(String key : keys){
            Object value = prefKV.get(key);
            if(value instanceof String){
                bundle.putString(key, (String) value);
            }else if(value instanceof Integer){
                bundle.putString(key, value.toString());
            }
        }
        String min = bundle.getString("minute", "0");
        return Integer.parseInt(min);
    }
}

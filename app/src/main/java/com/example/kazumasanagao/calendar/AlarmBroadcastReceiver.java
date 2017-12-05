package com.example.kazumasanagao.calendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;

/**
 * Created by kazumasanagao on 8/2/15.
 */
public class AlarmBroadcastReceiver extends BroadcastReceiver {
    private Context alarmReceiverContext;
    private int notificationProvisionalId;

    @Override
    public void onReceive(Context context, Intent receivedIntent) {

        Boolean outside_intent;
        String act = receivedIntent.getAction();
        if (act != null) {
            outside_intent = true;
        } else {
            outside_intent = false;
        }

        if (outside_intent) {
            if (receivedIntent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {

                Bundle bundle = new Bundle();
                Map<String, ?> prefKV = context.getSharedPreferences("shared_preference", Context.MODE_PRIVATE).getAll();
                Set<String> keys = prefKV.keySet();
                for(String key : keys){
                    Object value = prefKV.get(key);
                    if(value instanceof String){
                        bundle.putString(key, (String) value);
                    }else if(value instanceof Integer){
                        bundle.putString(key, value.toString());
                    }else if(value instanceof Boolean) {
                        bundle.putString(key, value.toString());
                    }
                }
                String s_hour = bundle.getString("hour", "0");
                String s_minute = bundle.getString("minute", "0");
                String s_switch = bundle.getString("switch", "false");
                Integer hour = Integer.parseInt(s_hour);
                Integer minute = Integer.parseInt(s_minute);
                Boolean Switch = Boolean.valueOf(s_switch);

                if (Switch) {
                    Intent bootIntent = new Intent(context, AlarmBroadcastReceiver.class);
                    PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, bootIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

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
                }
            }
        } else {
            alarmReceiverContext = context;

            notificationProvisionalId = receivedIntent.getIntExtra("notificationId", 0);
            NotificationManager myNotification = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = prepareNotification();
            myNotification.notify(notificationProvisionalId, notification);
        }
    }

    private Notification prepareNotification(){

        Intent bootIntent =
                new Intent(alarmReceiverContext, MainActivity.class);
        PendingIntent contentIntent =
                PendingIntent.getActivity(alarmReceiverContext, 0, bootIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                alarmReceiverContext);
        //builder.setSmallIcon(android.R.drawable.ic_dialog_info)
        builder.setSmallIcon(R.mipmap.star)
                .setTicker(alarmReceiverContext.getString(R.string.app_name))
                .setContentTitle(alarmReceiverContext.getString(R.string.alarm_message))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setContentIntent(contentIntent);

        //NotificationCompat.BigPictureStyle pictureStyle =
        //        new NotificationCompat.BigPictureStyle(builder);
        //pictureStyle.bigPicture(BitmapFactory.decodeResource(alarmReceiverContext.getResources(), R.drawable.cat));

        return builder.build();

    }

}


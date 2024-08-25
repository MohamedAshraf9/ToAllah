package com.megahed.eqtarebmenalla.alarm;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import com.megahed.eqtarebmenalla.R;
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.SplashScreenActivity;


import java.util.Calendar;
import java.util.Objects;

public class NotifyMessing extends BroadcastReceiver {

    private Bundle bundle;
    private String eventTitle;
    private String eventDescription;
    private final String TAG = this.getClass().getSimpleName();
    private int notificationId;
    Context context;

    private int eventColor;

    private final static String CHANNEL_ID = "NotifyMessing";

    @SuppressLint({"StringFormatInvalid", "StringFormatMatches"})
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (intent!=null && Objects.requireNonNull(intent.getAction()).equals("com.megahed.eqtarebmenalla.TIMEALARM")){
            bundle = intent.getExtras();
            assert bundle != null;
            eventTitle = bundle.getString("AlarmTitle", "No title");
            eventDescription = bundle.getString("AlarmNote", "No note");
            eventColor = bundle.getInt("AlarmColor", -49920);
            notificationId = bundle.getInt("notificationId", 0);


            showNotification();
            setNewAlarm();
        }


    }

    private void showNotification() {

        Uri ringtoneUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.azan);
        //Ringtone r = RingtoneManager.getRingtone(context, ringtoneUri);
        //r.play();

        long[] vibratePattern = new long[]{0L, 1000L};

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        NotificationManager mNotifyManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        context.grantUriPermission("com.android.systemui", ringtoneUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent activityIntent = new Intent(context, SplashScreenActivity.class);
        activityIntent.setAction(Constants.ACTION.MAIN_ACTION);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, activityIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            AudioAttributes att = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "Missing Notification", NotificationManager.IMPORTANCE_HIGH);
            mChannel.setDescription(eventDescription);
            mChannel.enableLights(true);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(vibratePattern);
            mChannel.setSound(ringtoneUri, att);
            mChannel.setBypassDnd(true);
            mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            mChannel.setShowBadge(true);

            if (mNotifyManager != null) {
                mNotifyManager.createNotificationChannel(mChannel);
            }



        }
            notificationBuilder.setContentTitle(eventTitle)
                    .setSmallIcon(R.drawable.prayer_icon)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                    .setVibrate(vibratePattern)
                    .setSound(ringtoneUri)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(eventDescription))
                    .setColor(eventColor)
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(pendingIntent);



        notificationBuilder.setContentText(eventDescription);

        if (mNotifyManager != null) {
            mNotifyManager.notify(notificationId, notificationBuilder.build());
        }


        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        @SuppressLint("InvalidWakeLockTag")
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        wl.acquire(10);



    }

    private void setNewAlarm() {
        Intent intent = new Intent(context, NotifyMessing.class);
        intent.putExtras(bundle);
        intent.setAction("com.megahed.eqtarebmenalla.TIMEALARM");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long triggerAtMillis = calendar.getTimeInMillis();
        if (triggerAtMillis != 0) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

            assert alarmManager != null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);

            }
            else {
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAtMillis,pendingIntent),pendingIntent);
            }


        }


    }

}
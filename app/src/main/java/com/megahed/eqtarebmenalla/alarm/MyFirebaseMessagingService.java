package com.megahed.eqtarebmenalla.alarm;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.megahed.eqtarebmenalla.R;

import java.util.Objects;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private final static String CHANNEL_ID = "MyChannelIdFireBase";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);


        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {

            showNotification(Objects.requireNonNull(remoteMessage.getNotification().getTitle()),remoteMessage.getNotification().getBody());



        }


    }


    private void showNotification(String title,String message) {


        //Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Uri ringtoneUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.consequence);
        Ringtone r = RingtoneManager.getRingtone(this, ringtoneUri);
        r.play();

        long[] vibratePattern = new long[]{0L, 1000L};

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //grantUriPermission("com.android.systemui", ringtoneUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        //Intent activityIntent = new Intent(context, SplashScreen.class);
        Intent activityIntent = new Intent(Intent.ACTION_VIEW);
        activityIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.megahed.eqtarebmenalla"));
        //startActivity(intent);
        //activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);






        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            AudioAttributes att = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "ToDo Notification", NotificationManager.IMPORTANCE_HIGH);
            mChannel.setDescription(message);
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
        notificationBuilder.setContentTitle(title)
                .setSmallIcon(R.drawable.prayer_icon)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setVibrate(vibratePattern)
                //.setOngoing(true)
                .setSound(ringtoneUri)
                //.setStyle(new NotificationCompat.BigTextStyle().bigText("AlarmNote"))
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent);

        notificationBuilder.setContentText(message);

        if (mNotifyManager != null) {
            mNotifyManager.notify(-2, notificationBuilder.build());
        }



    }


}

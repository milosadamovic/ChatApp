package com.example.chatapp.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.Util;
import com.example.chatapp.login.LoginActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ChatMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);

        Util.updateDeviceToken(this, s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        /**OVDE SI STAOOO, NOTIFIKACIJA SE SALJE I DRUGA STRANA PRIMA, ALI SE NE PRIKAZUJE, DOLAZI DO NEKE GRESKE*/

        Log.d("PRIMLJENO", "PRIMLJENO");
        String title = message.getData().get(Constants.NOTIFICATION_TITLE);
        String msg = message.getData().get(Constants.NOTIFICATION_MESSAGE);

        Intent intentChat = new Intent(this, LoginActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intentChat, PendingIntent.FLAG_ONE_SHOT);

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(Constants.CHANNEL_ID, Constants.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription(Constants.CHANNEL_DESC);
            notificationManager.createNotificationChannel(channel);
            notificationBuilder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
        }
        else  notificationBuilder = new NotificationCompat.Builder(this);


            notificationBuilder.setSmallIcon(R.drawable.logo);
            notificationBuilder.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
            notificationBuilder.setContentTitle(title);
            notificationBuilder.setAutoCancel(true);
            notificationBuilder.setSound(defaultSoundUri);
            notificationBuilder.setContentIntent(pendingIntent);
            notificationBuilder.setContentText(msg);

            notificationManager.notify(999, notificationBuilder.build());





    }
}
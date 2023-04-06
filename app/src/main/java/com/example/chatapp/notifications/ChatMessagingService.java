package com.example.chatapp.notifications;

import static java.lang.System.load;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.chatapp.R;
import com.example.chatapp.chats.ChatActivity;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.Util;
import com.example.chatapp.login.LoginActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;

public class ChatMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);

        Util.updateDeviceToken(this, s);
    }

    @Override
   public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String userId = message.getData().get(Constants.NOTIFICATION_ID);
        boolean isChatActivityRunning = Util.isActivityRunning(ChatActivity.class,this);


        Log.d("ChatMessagingService", "onMessageReceived() - isChatActivityRunning = " + isChatActivityRunning);
        Log.d("ChatMessagingService", "onMessageReceived() - Objects.equals(userId, ChatActivity.openChatUserId) = " + Objects.equals(userId, ChatActivity.openChatUserId));
        Log.d("ChatMessagingService", "onMessageReceived() - userId = " + userId);
        Log.d("ChatMessagingService", "onMessageReceived() - ChatActivity.openChatUserId = " + ChatActivity.openChatUserId);



        /**TODO
         * KADA KORISNIK KLIKNE NA NOTIFIKACIJU OTVARA MU SE CHAT ACTIVITY SA OSOBOM KOJA MU JE POSLALA NOTIFIKACIJU, AKO JE U PITANJU PORUKA
         * AKO JE U PITANJU ZAHTEV ZA PRIJATELJSTVO, OTVARA MU SE REQUESTS
         * AKO JE U PITANJU ODGOVOR NA ZAHTEV OTVARA MU SE CHAT FRAGMENT*/


        /**AKO SE VEC NALAZIMO U CETU SA OSOBOM OD KOJE PRIMAMO NOTIFIKACIJU NECEMO PRIKAZATI NOTIFIKACIJU*/
        if(isChatActivityRunning && Objects.equals(userId, ChatActivity.openChatUserId)) return;


        else {

                String title = message.getData().get(Constants.NOTIFICATION_TITLE);
                String msg = message.getData().get(Constants.NOTIFICATION_MESSAGE);


                Intent intentChat = new Intent(this, LoginActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intentChat, PendingIntent.FLAG_IMMUTABLE);

                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                NotificationCompat.Builder notificationBuilder;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(Constants.CHANNEL_ID, Constants.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);

                    channel.setDescription(Constants.CHANNEL_DESC);
                    notificationManager.createNotificationChannel(channel);
                    notificationBuilder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
                } else notificationBuilder = new NotificationCompat.Builder(this);


                notificationBuilder.setSmallIcon(R.drawable.logo);
                notificationBuilder.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
                notificationBuilder.setContentTitle(title);
                notificationBuilder.setAutoCancel(true);
                notificationBuilder.setSound(defaultSoundUri);
                notificationBuilder.setContentIntent(pendingIntent);

                // PROVERAVAMO DA LI JE PORUKA TEXT ILI FAJL
                if (msg.startsWith("https://firebasestorage.")) {
                    try {

                        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
                        Glide.with(this)
                                .asBitmap()
                                .load(msg)
                                .into(new CustomTarget<Bitmap>(200, 100) {
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                        bigPictureStyle.bigPicture(resource);
                                        notificationBuilder.setStyle(bigPictureStyle);
                                        notificationManager.notify(999, notificationBuilder.build());

                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {

                                    }
                                });


                    } catch (Exception e) {
                        notificationBuilder.setContentText("New File Received");
                    }
                } else {
                    notificationBuilder.setContentText(msg);
                    notificationManager.notify(999, notificationBuilder.build());
                }


            }
    }




}
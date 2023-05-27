package com.example.chatapp.notifications;

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
import android.os.Bundle;
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
import com.example.chatapp.util.Constants;
import com.example.chatapp.util.Extras;
import com.example.chatapp.util.Util;
import com.example.chatapp.login.LoginActivity;
import com.example.chatapp.password.ChangePasswordActivity;
import com.example.chatapp.profile.ProfileActivity;
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
        String notificationType = message.getData().get(Constants.NOTIFICATION_TYPE);

        boolean isChatActivityRunning = Util.isActivityRunning(ChatActivity.class,this);



        /**AKO SE VEC NALAZIMO U CETU SA OSOBOM OD KOJE PRIMAMO NOTIFIKACIJU NECEMO PRIKAZATI NOTIFIKACIJU
         *  (NE MOZEMO SE NALAZITI U CETU SA OSOBOM KOJA NAM ODOBRAVA ILI ODBIJA REQUEST ILI KOJA NAM SALJE REQUEST)*/
        if(isChatActivityRunning && Objects.equals(userId, ChatActivity.openChatUserId) && ChatActivity.isActivityResumed() && Objects.equals(notificationType, Constants.NOTIFICATION_TYPE_MESSAGE)) return;

        else {

                String title = message.getData().get(Constants.NOTIFICATION_TITLE);
                String msg = message.getData().get(Constants.NOTIFICATION_MESSAGE);
                String notificationId = "";

                /**PROVERAVAMO KOG JE TIPA NOTIFIKACIJA*/
                if(Objects.equals(notificationType, Constants.NOTIFICATION_TYPE_REPLY))
                {
                    notificationId = Constants.NOTIFICATION_TYPE_REPLYID;
                }
                else if(Objects.equals(notificationType,Constants.NOTIFICATION_TYPE_REQUEST))
                {
                    notificationId = Constants.NOTIFICATION_TYPE_REQUESTID;
                }
               else if(Objects.equals(notificationType, Constants.NOTIFICATION_TYPE_MESSAGE))
                {
                    notificationId = Constants.NOTIFICATION_TYPE_MESSAGEID;
                }


                Intent intentChat = new Intent(this, LoginActivity.class);
                intentChat.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intentChat, PendingIntent.FLAG_IMMUTABLE);
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                NotificationCompat.Builder notificationBuilder;


                NotificationChannel channel = new NotificationChannel(Constants.CHANNEL_ID, Constants.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription(Constants.CHANNEL_DESC);
                notificationManager.createNotificationChannel(channel);

                notificationBuilder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
                notificationBuilder.setSmallIcon(R.drawable.logo);
                notificationBuilder.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
                notificationBuilder.setContentTitle(title);
                notificationBuilder.setAutoCancel(true);
                notificationBuilder.setSound(defaultSoundUri);
                notificationBuilder.setContentIntent(pendingIntent);

                /**PROVERAVAMO DA LI JE PORUKA TEXT ILI FAJL*/

                if (msg.startsWith("https://firebasestorage.")) {
                    try {

                        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
                        String finalNotificationId = notificationId;
                        Glide.with(this)
                                .asBitmap()
                                .load(msg)
                                .into(new CustomTarget<Bitmap>(200, 100) {
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                        bigPictureStyle.bigPicture(resource);
                                        notificationBuilder.setStyle(bigPictureStyle);
                                        notificationManager.notify(Integer.parseInt(finalNotificationId), notificationBuilder.build());
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {

                                    }
                                });


                    } catch (Exception e) {
                        notificationBuilder.setContentText("New File Received");
                    }

                }

                else
                {
                    notificationBuilder.setContentText(msg);
                    notificationManager.notify(Integer.parseInt(notificationId), notificationBuilder.build());
                }


            }
    }




}
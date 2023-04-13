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



    /**TODO PRVI KORAK
     * REQUEST STATUS TREBA DA SE AZURIRA CIM DRUGI KORISNIK ODOBRI ILI ODBIJE ZAHTEV / ONEMOGUCITI DA NAKON PRIHVATANJA ZAHTEVA OD STRANE KORISNIKA, DRUGA STRANA MOZE DA OTKAZE ZAHTEV, JER SE NAKON PRIHVATANJA DODAJE TAJ USER U NODE CHAT
     * PROFILNA SLIKA JEDNOG KORISNIKA PRELAZI NA DRUGOG U LISTI NA CHAT FRAGMENTU
     * ISPRAVITI DA NE MOGU PORUKE DA SE PROSLEDJUJU SAMOM SEBI
     * OMOGUCITI DA SE U DOPISIVANJU UVEK VIDI POSLEDNJA POSLATA PORUKA I POSLEDNJA PRIMLJENA PORUKA
     * DODATI I BRISANJE PRIJATELJA
     * DODATI DA KADA KORISNIK NAPRAVI ACCOUNT, NE MOZE UCI U APLIKACIJU SVE DOK NE VERIFIKUJE NALOG PREKO MEJLA*/

    /**TODO DRUGI KORAK
     * NAKON PRVOG KORAKA PROVERITI :
     *      KAKO NOTIFIKACIJE FUNKCIONISU BEZ DA IH KORISNIK DOZVOLI U PERMISSIONS
     *      TESTIRANJE STATUS OFFLINE / ONLINE NE RADI KAKO TREBA
     *      TESTIRANJE STATUS TYPING... NE RADI KAKO TREBA
     *      PROBLEM KREIRANJA TOKENA (DA LI JE ZAISTA PROBLEM ?!)
     *      NOVA NOTIFIKACIJA SE PREPISUJE PREKO STARE (AKO IMAS VREMENA ISPRAVI, VIDI KAKO JE NA VIBERU/WA)
     *      JOS SE POZABAVITI LOGOUTOM
     *      PROCI POSLEDNJI TURORIJAL
     */

    /**TODO TRECI KORAK
     * OPTIMIZACIJA APLIKACIJE
     *     PROCI PDFOVE SA FAKSA
     *     PROCI SVE AKTIVNOSTI, FRAGMENTE I KLASE REDOM
     *     DOBRO NAUCI FIREBASE FUNKCIJE KOJE SE KORISTE U APLIKACIJI I SAM API
     *     PROBAJ IZ SVAKE AKTIVNOSTI IZVUCI FUNKCIONALNOSTI U JEDNU KLASU
     *     VIDI ZASTO SE NOTIFIKACIJE NEKAD SALJU, A NEKAD NE - PROBLEM JE BIO GENERISANJE TOKENA - ISTRAZI JOS
     */


    @Override
   public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String userId = message.getData().get(Constants.NOTIFICATION_ID);
        String notificationType = message.getData().get(Constants.NOTIFICATION_TYPE);

        boolean isChatActivityRunning = Util.isActivityRunning(ChatActivity.class,this);


        /*Log.d("ChatMessagingService", "onMessageReceived() - isChatActivityRunning = " + isChatActivityRunning);
        Log.d("ChatMessagingService", "onMessageReceived() - Objects.equals(userId, ChatActivity.openChatUserId) = " + Objects.equals(userId, ChatActivity.openChatUserId));
        Log.d("ChatMessagingService", "onMessageReceived() - userId = " + userId);
        Log.d("ChatMessagingService", "onMessageReceived() - ChatActivity.openChatUserId = " + ChatActivity.openChatUserId);*/


        /**AKO SE VEC NALAZIMO U CETU SA OSOBOM OD KOJE PRIMAMO NOTIFIKACIJU NECEMO PRIKAZATI NOTIFIKACIJU
         *  (NE MOZEMO SE NALAZITI U CETU SA OSOBOM KOJA NAM ODOBRAVA ILI ODBIJA REQUEST ILI KOJA NAM SALJE REQUEST)*/
        if(isChatActivityRunning && Objects.equals(userId, ChatActivity.openChatUserId) && ChatActivity.isActivityResumed()) {Log.d("ChatMessagingService", "onMessageReceived() - ChatActivity open"); return;}

        else {

                String title = message.getData().get(Constants.NOTIFICATION_TITLE);
                String msg = message.getData().get(Constants.NOTIFICATION_MESSAGE);
                String notificationId = "";

                /**PROVERAVAMO KOG JE TIPA NOTIFIKACIJA*/
                if(Objects.equals(notificationType,Constants.NOTIFICATION_TYPE_REQUEST)) notificationId = Constants.NOTIFICATION_TYPE_REQUESTID;
                if(Objects.equals(notificationType, Constants.NOTIFICATION_TYPE_REPLY)) notificationId = Constants.NOTIFICATION_TYPE_REPLYID;
                if(Objects.equals(notificationType, Constants.NOTIFICATION_TYPE_MESSAGE)) notificationId = Constants.NOTIFICATION_TYPE_MESSAGEID;



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
                } else {
                    notificationBuilder.setContentText(msg);
                    notificationManager.notify(Integer.parseInt(notificationId), notificationBuilder.build());
                }


            }
    }




}
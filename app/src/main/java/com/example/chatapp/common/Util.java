package com.example.chatapp.common;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import com.example.chatapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Util {


    public static boolean connectionAvailable(Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

        if(networkCapabilities != null && (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)))
            return true;
        else return false;

    }

    public static void updateDeviceToken(Context context, String token)
    {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        Log.d("Util", "updateDeviceToken() called");

        if(currentUser != null)
        {
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            DatabaseReference databaseReference = rootRef.child(NodeNames.TOKEN).child(currentUser.getUid());

            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put(NodeNames.DEVICE_TOKEN, token);

            databaseReference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    Log.d("Util", "updateDeviceToken() called");
                    if (!task.isSuccessful())
                    {
                        Toast.makeText(context, context.getString(R.string.failed_to_save_device_token, task.getException()), Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }

    }


    public static void sendNotification(Context context, String title, String message, String chatUserId, String userId, String notificationType)
    {

        Log.d("Util", "sendNotification() called ");

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference databaseReference = rootRef.child(NodeNames.TOKEN).child(chatUserId);


        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                /**KADA NEMA DEVICE TOKENA NEMA SLANJA NOTIFIKACIJE*/
                if(snapshot.child(NodeNames.DEVICE_TOKEN).getValue() != null)
                {

                    Log.d("Util", "sendNotification() called 2");
                    String deviceToken = snapshot.child(NodeNames.DEVICE_TOKEN).getValue().toString();

                    JSONObject notification = new JSONObject();
                    JSONObject notificationData = new JSONObject();

                    try {
                        notificationData.put(Constants.NOTIFICATION_TITLE,title);
                        notificationData.put(Constants.NOTIFICATION_MESSAGE,message);
                        notificationData.put(Constants.NOTIFICATION_ID,userId);
                        notificationData.put(Constants.NOTIFICATION_TYPE, notificationType);

                        notification.put(Constants.NOTIFICATION_TO, deviceToken);
                        notification.put(Constants.NOTIFICATION_DATA, notificationData);


                        String fcmApiUrl = "https://fcm.googleapis.com/fcm/send";
                        String contentType = "application/json";

                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                (Request.Method.POST, fcmApiUrl, notification, new Response.Listener<JSONObject>() {

                                    @Override
                                    public void onResponse(JSONObject response) {

                                        Log.d("Util", "sendNotification() called 3");
                                        Toast.makeText(context, "Notification Sent", Toast.LENGTH_SHORT).show();

                                    }
                                }, new Response.ErrorListener() {

                                    @Override
                                    public void onErrorResponse(VolleyError error) {

                                        Toast.makeText(context, context.getString(R.string.failed_to_send_notification, error.getMessage()), Toast.LENGTH_SHORT).show();

                                    }
                                }){

                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {

                                Map<String,String> params = new HashMap<>();
                                params.put("Authorization","key=" + Constants.FIREBASE_KEY);
                                params.put("Sender","id="+ Constants.SENDER_ID);
                                params.put("Content-Type",contentType);


                                return params;
                            }
                        };

                        RequestQueue requestQueue = Volley.newRequestQueue(context);
                        requestQueue.add(jsonObjectRequest);


                    } catch (JSONException e) {

                        Toast.makeText(context, context.getString(R.string.failed_to_send_notification, e.getMessage()), Toast.LENGTH_SHORT).show();
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(context, context.getString(R.string.failed_to_send_notification, error.getMessage()), Toast.LENGTH_SHORT).show();

            }
        });
    }


    public static void updateChatDetails(Context context, String currentUserId, String chatUserId, String lastMessage, String messageType)
    {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference chatRef = rootRef.child(NodeNames.CHATS).child(chatUserId).child(currentUserId);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String currentCount = "0";
                if(snapshot.child(NodeNames.UNREAD_COUNT).getValue() != null)
                    currentCount = snapshot.child(NodeNames.UNREAD_COUNT).getValue().toString();

                Map chatMap = new HashMap();
                chatMap.put(NodeNames.TIME_STAMP, ServerValue.TIMESTAMP);
                chatMap.put(NodeNames.LAST_MESSAGE, lastMessage);
                chatMap.put(NodeNames.LAST_MESSAGE_TIME, ServerValue.TIMESTAMP);

                /**PROVERA DA LI JE TIP PORUKE DELETED*/
                if(messageType.equals(Constants.MESSAGE_TYPE_DELETED))
                    chatMap.put(NodeNames.UNREAD_COUNT, Integer.valueOf(currentCount));
                else chatMap.put(NodeNames.UNREAD_COUNT, Integer.valueOf(currentCount)+1);

                Map chatUserMap = new HashMap();
                chatUserMap.put(NodeNames.CHATS + "/" + chatUserId + "/" + currentUserId, chatMap);

                rootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {

                        if(error != null)
                            Toast.makeText(context, context.getString(R.string.something_went_wrong, error.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(context, context.getString(R.string.something_went_wrong, error.getMessage()), Toast.LENGTH_SHORT).show();

            }
        });


    }


    public static String getTimeAgo(long time)
    {
        final int SECOND_MILLIS = 1000;
        final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
        final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
        final int DAY_MILLIS = 24 * HOUR_MILLIS;

        //time *= 1000;

        long now = System.currentTimeMillis();
        if(time > now || time <= 0) return "";

        final long diff = now - time;

       if(diff < MINUTE_MILLIS) return "just now";
       else if(diff < 2 * MINUTE_MILLIS) return "minute ago";
       else if (diff < 59 * MINUTE_MILLIS) return diff/MINUTE_MILLIS + " minutes ago";
       else if (diff < 90 * MINUTE_MILLIS) return "an hour ago";
       else if (diff < 24 * HOUR_MILLIS) return diff/HOUR_MILLIS + " hours ago";
       else if (diff < 48 * HOUR_MILLIS) return "yesterday";
       else return diff/DAY_MILLIS + " days ago";


    }

    public static void cancelNotifications(Context context, String id)
    {

        if(context != null) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();

            if (activeNotifications.length > 0)
            {
                notificationManager.cancel(Integer.parseInt(id));
            }

        }
    }

    public static boolean isActivityRunning(Class activityClass, Context context) {
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();

        for (ActivityManager.AppTask task : tasks) {
            ActivityManager.RecentTaskInfo taskInfo = task.getTaskInfo();
            ComponentName componentName = taskInfo.topActivity;

            if (componentName.getClassName().equals(activityClass.getName())) {
                return true;
            }
        }

        return false;
    }



    /**PRAVLJENJE PRIVATE_ID ZA SVAKOG USERA PRILIKOM PRIJAVLIVANJA I PRILIKOM UPDEJTOVANJA PROFILA*/
    public static void incrementUserCounter(final String userId, final  OnGetDataListener listener) {


        DatabaseReference drUserCounter = FirebaseDatabase.getInstance().getReference().child("UserCounter");
        final DatabaseReference[] drUsers = {FirebaseDatabase.getInstance().getReference().child("Users")};

        drUserCounter.child("userCounter").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Long currentValue = mutableData.getValue(Long.class);

                if (currentValue == null) {
                    mutableData.setValue(1);
                } else {
                    mutableData.setValue(currentValue + 1);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                if (databaseError != null)  Log.d("Util", "incrementUserCounter:onComplete: " + databaseError.getMessage());
                else if(b) {
                    HashMap<String,Object> hashMap = new HashMap<>();
                    hashMap.put(NodeNames.PRIVATE_ID, dataSnapshot.getValue());

                    drUsers[0].child(userId).updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful())
                                Log.d("Util","PRIVATE_ID Successfully added to User");
                        }
                    });
                }
                else Log.d("Util", "Transaction aborted");
            }
        });

    }

}

package com.example.chatapp.common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

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
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import java.util.HashMap;
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

        if(currentUser != null)
        {
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            DatabaseReference databaseReference = rootRef.child(NodeNames.TOKEN).child(currentUser.getUid());

            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put(NodeNames.DEVICE_TOKEN, token);

            databaseReference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if (!task.isSuccessful())
                    {
                        Toast.makeText(context, context.getString(R.string.failed_to_save_device_token, task.getException()), Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }

    }



    public static void sendNotification(Context context, String title, String message, String userId)
    {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference databaseReference = rootRef.child(NodeNames.TOKEN).child(userId);

        Log.d("HOCE","hello");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.child(NodeNames.DEVICE_TOKEN).getValue() != null)
                {
                    Log.d("NECE","hello");
                    String deviceToken = snapshot.child(NodeNames.DEVICE_TOKEN).getValue().toString();

                    JSONObject notification = new JSONObject();
                    JSONObject notificationData = new JSONObject();

                    try {
                        notificationData.put(Constants.NOTIFICATION_TITLE,title);
                        notificationData.put(Constants.NOTIFICATION_MESSAGE,message);

                        notification.put(Constants.NOTIFICATION_TO, deviceToken);
                        notification.put(Constants.NOTIFICATION_DATA, notificationData);


                        String fcmApiUrl = "https://fcm.googleapis.com/fcm/send";
                        String contentType = "application/json";

                        //Log.d("OVDE", "EEEEEEEJ");
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                (Request.Method.POST, fcmApiUrl, notification, new Response.Listener<JSONObject>() {

                                    @Override
                                    public void onResponse(JSONObject response) {

                                        Toast.makeText(context, "Notification Sent", Toast.LENGTH_SHORT).show();

                                    }
                                }, new Response.ErrorListener() {

                                    @Override
                                    public void onErrorResponse(VolleyError error) {

                                        Log.d("FEJL VOLLEY", error.getMessage());
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

                        Log.d("FEJL JSON", e.getMessage());
                        Toast.makeText(context, context.getString(R.string.failed_to_send_notification, e.getMessage()), Toast.LENGTH_SHORT).show();
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Log.d("FEJL DB", error.getMessage());
                Toast.makeText(context, context.getString(R.string.failed_to_send_notification, error.getMessage()), Toast.LENGTH_SHORT).show();

            }
        });
    }

    /*  PRIMER

    private void sendNotification() {
    // Create the FCM notification data
    JSONObject notification = new JSONObject();
    try {
        notification.put("title", "My Notification Title");
        notification.put("body", "My Notification Message");
    } catch (JSONException e) {
        e.printStackTrace();
    }

    // Create the FCM request body
    JSONObject requestBody = new JSONObject();
    try {
        requestBody.put("notification", notification);
        requestBody.put("to", "/topics/my_topic");
    } catch (JSONException e) {
        e.printStackTrace();
    }

    // Create the FCM request
    JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
            "https://fcm.googleapis.com/fcm/send", requestBody,
            response -> {
                Log.d(TAG, "Notification sent successfully");
            },
            error -> {
                Log.e(TAG, "Error sending notification: " + error.getMessage());
            }
    ) {
        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            // Set the FCM authorization header
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "key=YOUR_SERVER_KEY");
            headers.put("Content-Type", "application/json");
            return headers;
        }
    };

    // Add the request to the Volley request queue
    Volley.newRequestQueue(this).add(request);
}*/


}

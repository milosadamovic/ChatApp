package com.example.chatapp.login;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.chatapp.R;
import com.example.chatapp.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

public class UtilLogin {


    public static void getToken(Context ctx)
    {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(ctx, ctx.getString(R.string.failed_to_get_token, task.getException()), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String token = task.getResult();
                        Util.updateDeviceToken(ctx, token);
                        Log.d("TEST", "getToken() called");
                    }
                });
    }


}

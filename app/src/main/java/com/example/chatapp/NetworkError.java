package com.example.chatapp;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.chatapp.util.Util;


public class NetworkError extends AppCompatActivity {


    private TextView tvMessage;
    private ProgressBar pB;

    private ConnectivityManager.NetworkCallback networkCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_error_layout);


        tvMessage = findViewById(R.id.tvMessage);
        pB = findViewById(R.id.pB);

        networkCallback = new ConnectivityManager.NetworkCallback(){

            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                finish();
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                tvMessage.setText(R.string.no_internet);
            }
        };


        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);


        connectivityManager.registerNetworkCallback(new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(), networkCallback);

    }


    public void btnRetryClick(View v) {
        pB.setVisibility(View.VISIBLE);

        if (Util.connectionAvailable(this))
            finish();
        else {
            new Handler(Looper.getMainLooper()).postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            pB.setVisibility(View.GONE);
                        }
                    }, 1000);

         }
    }



    public void btnCloseClick(View v)
    {
        finishAffinity();
    }

}
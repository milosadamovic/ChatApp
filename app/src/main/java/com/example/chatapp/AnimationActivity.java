package com.example.chatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.chatapp.login.LoginActivity;

public class AnimationActivity extends AppCompatActivity {


    private ImageView ivLogo;
    private Animation scaleAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animation_layout);

        if(getSupportActionBar() != null)
            getSupportActionBar().hide();

        ivLogo = findViewById(R.id.ivLogo);

        scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_animation);

        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                    startActivity(new Intent(AnimationActivity.this, LoginActivity.class));
                    finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        ivLogo.startAnimation(scaleAnimation);
    }
}
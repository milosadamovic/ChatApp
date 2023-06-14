package com.example.chatapp.password;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.NetworkError;
import com.example.chatapp.R;
import com.example.chatapp.signup.SignupActivity;
import com.example.chatapp.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;


public class ResetPasswordActivity extends AppCompatActivity {


    private TextInputEditText etEmail;
    private TextView tvMessage;
    private LinearLayout llResetPassword, llMessage;
    private Button btnRetry;
    private View pB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etEmail = findViewById(R.id.etEmail);
        tvMessage = findViewById(R.id.tvMessage);
        llMessage = findViewById(R.id.llMessage);
        llResetPassword = findViewById(R.id.llResetPassword);
        btnRetry = findViewById(R.id.btnRetry);
        pB = findViewById(R.id.progressBar);

    }


    public void btnResetPasswordClick(View view)
    {
        String email = etEmail.getText().toString().trim();

        if(email.equals(""))
        {
            etEmail.setError(getString(R.string.enter_email));
        }
        else
        {

            if(Util.connectionAvailable(this))
            {
                pB.setVisibility(View.VISIBLE);

                FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        pB.setVisibility(View.GONE);

                        llResetPassword.setVisibility(View.GONE);
                        llMessage.setVisibility(View.VISIBLE);

                        if(task.isSuccessful())
                        {
                            tvMessage.setText(getString(R.string.reset_password_instructions,email));
                            new CountDownTimer(60000, 1000) {

                                @Override
                                public void onTick(long l) {
                                    btnRetry.setText(getString(R.string.resend_timer, String.valueOf(l/1000)));
                                    btnRetry.setOnClickListener(null);
                                }

                                @Override
                                public void onFinish() {
                                    btnRetry.setText(R.string.retry);
                                    btnRetry.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            llResetPassword.setVisibility(View.VISIBLE);
                                            llMessage.setVisibility(View.GONE);
                                        }
                                    });
                                }
                            }.start();
                        }
                        else
                        {
                            tvMessage.setText(getString(R.string.email_sent_failed, task.getException()));

                            btnRetry.setText(R.string.retry);
                            btnRetry.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    llResetPassword.setVisibility(View.VISIBLE);
                                    llMessage.setVisibility(View.GONE);
                                }
                            });
                        }
                    }
                });
            }
             else Toast.makeText(this, R.string.no_internet ,Toast.LENGTH_LONG).show();


        }
    }

    public void btnCloseClick(View view)
    {
        finish();
    }


}
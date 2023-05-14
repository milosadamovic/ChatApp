package com.example.chatapp.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.MainActivity;
import com.example.chatapp.MessageActivity;
import com.example.chatapp.R;
import com.example.chatapp.common.Util;
import com.example.chatapp.password.ResetPasswordActivity;
import com.example.chatapp.signup.SignupActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.function.ToDoubleBiFunction;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private String email,password;
    private View progressBar;
    private FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        /**DODATA LINIJA ISPOD*/
        FirebaseApp.initializeApp(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        progressBar = findViewById(R.id.progressBar);

    }


    public void btnLoginClick(View v) {

        email = etEmail.getText().toString().trim();
        password = etPassword.getText().toString().trim();

        if (email.equals(""))
        {
            etEmail.setError(getString(R.string.enter_email));
        }
        else if (password.equals(""))
        {
            etPassword.setError(getString(R.string.enter_password));
        }
        else
        {
            if(Util.connectionAvailable(this))
            {
                progressBar.setVisibility(View.VISIBLE);
                FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            FirebaseMessaging.getInstance().getToken()
                                    .addOnCompleteListener(new OnCompleteListener<String>() {
                                        @Override
                                        public void onComplete(@NonNull Task<String> task) {
                                            if (!task.isSuccessful()) {
                                                Toast.makeText(LoginActivity.this, getString(R.string.failed_to_get_token, task.getException()), Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            String token = task.getResult();
                                            Util.updateDeviceToken(LoginActivity.this, token);

                                        }
                                    });
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            else
            {
                startActivity(new Intent(LoginActivity.this, MessageActivity.class));
            }
        }
    }

    public void tvSignupClick(View view)
    {
        startActivity(new Intent(this, SignupActivity.class));
    }


    public void tvResetPasswordClick(View view)
    {
        startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
    }



    // TODO AUTOMATIC LOGIN, IF THE USER IS LOGGED IN HE DOESNT HAVE TO LOGIN AGAIN
    @Override
    protected void onStart() {

        Log.d("LoginActivity", "onStart() called");
        super.onStart();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();


        if(firebaseUser != null)
        {

            /**NAKON REGISTRACIJE(SIGN-UP) MORAMO KREIRATI TOKEN*/
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(new OnCompleteListener<String>() {
                        @Override
                        public void onComplete(@NonNull Task<String> task) {
                            if (!task.isSuccessful()) {
                               Toast.makeText(LoginActivity.this, getString(R.string.failed_to_get_token, task.getException()), Toast.LENGTH_SHORT).show();
                               return;
                            }

                            String token = task.getResult();
                            Util.updateDeviceToken(LoginActivity.this, token);

                        }
                    });

            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

    }

    @Override
    public void onBackPressed() {

        if(firebaseUser == null) return;
        super.onBackPressed();
    }
}
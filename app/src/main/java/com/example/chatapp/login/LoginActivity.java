package com.example.chatapp.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.main.MainActivity;
import com.example.chatapp.NetworkError;
import com.example.chatapp.R;
import com.example.chatapp.util.Util;
import com.example.chatapp.password.ResetPasswordActivity;
import com.example.chatapp.signup.SignupActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private View pB;

    private FirebaseUser currentUser;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        /**INICIJALIZACIJA FIREBASE-A*/
        FirebaseApp.initializeApp(this);


        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        pB = findViewById(R.id.progressBar);

    }


    public void btnLoginClick(View v) {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

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
                pB.setVisibility(View.VISIBLE);
                FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        pB.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            UtilLogin.getToken(getBaseContext());
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(getBaseContext(), "Login Failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            else
            {
                startActivity(new Intent(LoginActivity.this, NetworkError.class));
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


    @Override
    protected void onStart() {

        Log.d("LoginActivity", "onStart() called");
        super.onStart();

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();


        if(currentUser != null)
        {
            /**NAKON PRIJAVLJIVANJA NOVIH KORISNIKA OVDE IM SE GENERISE TOKEN, STARI KORISNISCIMA OSTAJE PRETHODNI UKOLIKO SE NISU ODJAVILI*/
            UtilLogin.getToken(this);


            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("LoginActivity", "onResume() called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("LoginActivity", "onPause() called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("LoginActivity", "onStop() called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("LoginActivity", "onDestroy() called");
    }

    @Override
    public void onBackPressed() {

        if(currentUser == null) return;
        super.onBackPressed();
    }
}
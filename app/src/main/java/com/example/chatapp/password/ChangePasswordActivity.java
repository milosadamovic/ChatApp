package com.example.chatapp.password;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {


    private TextInputEditText etOldPassword, etNewPassword, etConfirmPassword;
    private View pB;
    private static boolean isResumed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        pB = findViewById(R.id.progressBar);
    }


    public void btnChangePasswordClick(View view) {

        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (etOldPassword.equals("")) {
            etOldPassword.setError(getString(R.string.enter_old_password));
        }else if (newPassword.equals("")) {
            etNewPassword.setError(getString(R.string.enter_new_password));
        } else if (confirmPassword.equals("")) {
            etConfirmPassword.setError(getString(R.string.confirm_password));
        }else if (!newPassword.equals(confirmPassword)){
            etConfirmPassword.setError(getString(R.string.password_mismatch));
        }else{

            pB.setVisibility(View.VISIBLE);

            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();

            if(currentUser != null)
            {

                String email = currentUser.getEmail();

                AuthCredential credential = EmailAuthProvider.getCredential(email, oldPassword);

                currentUser.reauthenticate(credential)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {

                                    currentUser.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            pB.setVisibility(View.GONE);

                                            if(task.isSuccessful())
                                            {
                                                Toast.makeText(ChangePasswordActivity.this, R.string.password_changed_successfully, Toast.LENGTH_LONG).show();
                                                finish();
                                            }
                                            else
                                            {
                                                Toast.makeText(ChangePasswordActivity.this, getString(R.string.something_went_wrong,task.getException()),Toast.LENGTH_LONG).show();
                                                Log.d("ChangePasswordActivity", "Exception1: " + task.getException());
                                            }
                                        }
                                    });


                                } else {
                                    Toast.makeText(ChangePasswordActivity.this, getString(R.string.something_went_wrong,task.getException()),Toast.LENGTH_LONG).show();
                                    Log.d("ChangePasswordActivity", "Exception2: " + task.getException());
                                }
                            }
                        });
            }
        }
    }

    @Override
    protected void onResume() {
        isResumed = true;
        super.onResume();
    }

    @Override
    protected void onPause() {
        isResumed = false;
        super.onPause();
    }

    public static boolean isActivityResumed()
    {
        return isResumed;
    }

}
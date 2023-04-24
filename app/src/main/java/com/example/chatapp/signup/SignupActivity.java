package com.example.chatapp.signup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.chatapp.R;
import com.example.chatapp.common.NodeNames;
import com.example.chatapp.common.OnGetDataListener;
import com.example.chatapp.common.Util;
import com.example.chatapp.login.LoginActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class SignupActivity extends AppCompatActivity {


    private TextInputEditText etEmail, etName, etPassword, etConfirmPassword;
    private String email, name, password, confirmPassword;
    private ImageView ivProfile;

    private FirebaseUser firebaseUser;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private StorageReference fileStorage;
    private Uri localFileUri, serverFileUri;
    private View progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etName = findViewById(R.id.etName);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivProfile = findViewById(R.id.ivProfile);

        fileStorage = FirebaseStorage.getInstance().getReference(); // root folder of file storage
        progressBar = findViewById(R.id.progressBar);





    }

    public void pickImage(View v)
    {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickingImageActivityResultLauncher.launch(intent);
        }
        else
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},102);
        }
    }

    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
    ActivityResultLauncher<Intent> pickingImageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {

                        Intent data = result.getData();
                        if(data != null) localFileUri = data.getData();
                        ivProfile.setImageURI(localFileUri);
                    }
                }
            });

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 102)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickingImageActivityResultLauncher.launch(intent);
            }
            else
            {
                Toast.makeText(this,R.string.permission_required ,Toast.LENGTH_LONG).show();
            }
        }
    }

    public void updateNameAndPhoto()
    {
        String strFileName = firebaseUser.getUid() + ".jpg";

        final StorageReference fileRef = fileStorage.child("images/" + strFileName);

        progressBar.setVisibility(View.VISIBLE);
        fileRef.putFile(localFileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                progressBar.setVisibility(View.GONE);
                if(task.isSuccessful())
                {
                    fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            serverFileUri = uri;

                            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(etName.getText().toString().trim())
                                    .setPhotoUri(serverFileUri)
                                    .build();

                            firebaseUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful())
                                    {
                                        String userID = firebaseUser.getUid();
                                        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");

                                        HashMap<String,String> hashMap = new HashMap<>();
                                        hashMap.put(NodeNames.NAME, etName.getText().toString().trim());
                                        hashMap.put(NodeNames.EMAIL, etEmail.getText().toString().trim());
                                        hashMap.put(NodeNames.ONLINE, "true");
                                        hashMap.put(NodeNames.PHOTO, serverFileUri.getPath());
                                        Util.incrementUserCounter(userID, new OnGetDataListener() {
                                            @Override
                                            public void onSuccess(Object data) {
                                                Log.d("ProfileActivity", "PRIVATE_ID Successfully added to User" );
                                            }

                                            @Override
                                            public void onFailure(Exception exception) {
                                                Log.d("ProfileActivity", "UpdateOnlyName, exception " + exception);
                                            }
                                        });

                                        databaseReference.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Toast.makeText(SignupActivity.this, R.string.user_created_successfully, Toast.LENGTH_LONG).show();
                                                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                            }
                                        });

                                    }
                                    else
                                    {
                                        Toast.makeText(SignupActivity.this, getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_LONG);
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

    }

    public void updateOnlyName()
    {


        progressBar.setVisibility(View.VISIBLE);

        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(etName.getText().toString().trim())
                .build();

        firebaseUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                progressBar.setVisibility(View.GONE);

                if(task.isSuccessful())
                {
                    String userID = firebaseUser.getUid();
                    databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");

                    HashMap<String,String> hashMap = new HashMap<>();
                    hashMap.put(NodeNames.NAME, etName.getText().toString().trim());
                    hashMap.put(NodeNames.EMAIL, etEmail.getText().toString().trim());
                    hashMap.put(NodeNames.ONLINE, "true");
                    hashMap.put(NodeNames.PHOTO, "");
                    Util.incrementUserCounter(userID, new OnGetDataListener() {
                        @Override
                        public void onSuccess(Object data) {
                            Log.d("ProfileActivity", "PRIVATE_ID Successfully added to User" );
                        }

                        @Override
                        public void onFailure(Exception exception) {
                            Log.d("ProfileActivity", "UpdateOnlyName, exception " + exception);
                        }
                    });

                    progressBar.setVisibility(View.VISIBLE);

                    databaseReference.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            progressBar.setVisibility(View.GONE);

                            Toast.makeText(SignupActivity.this, R.string.user_created_successfully, Toast.LENGTH_LONG).show();

                            /**TESTIRANJE*/
                            firebaseAuth.signOut();

                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                        }
                    });

                }
                else
                {
                    Toast.makeText(SignupActivity.this, getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_LONG);
                }
            }
        });
    }

    public void btnSignupClick(View v) {
        email = etEmail.getText().toString().trim();
        name = etName.getText().toString().trim();
        password = etPassword.getText().toString().trim();
        confirmPassword = etConfirmPassword.getText().toString().trim();

        if (email.equals("")) {
            etEmail.setError(getString(R.string.enter_email));
        } else if (name.equals("")) {
            etName.setError(getString(R.string.enter_name));
        } else if (password.equals("")) {
            etPassword.setError(getString(R.string.enter_password));
        } else if (confirmPassword.equals("")){
            etConfirmPassword.setError(getString(R.string.confirm_password));
        }else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            etEmail.setError(getString(R.string.enter_correct_email));
        }else if(!password.equals(confirmPassword)){
            etConfirmPassword.setError(getString(R.string.password_mismatch));
        }else {

           progressBar.setVisibility(View.VISIBLE);

            firebaseAuth = FirebaseAuth.getInstance();

            firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {

                    progressBar.setVisibility(View.GONE);

                    if(task.isSuccessful())
                    {
                        firebaseUser = firebaseAuth.getCurrentUser();

                        /**OVDE TREBA URADITI VALIDACIJU SLANJEM MEJLA KORISNIKU*/


                        if (localFileUri != null)
                            updateNameAndPhoto();
                        else
                            updateOnlyName();
                    }
                    else
                    {
                        Toast.makeText(SignupActivity.this, getString(R.string.signup_failed, task.getException()), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }



}
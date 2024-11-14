package com.example.chatapp.signup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.chatapp.NetworkError;
import com.example.chatapp.R;
import com.example.chatapp.util.NodeNames;
import com.example.chatapp.login.LoginActivity;
import com.example.chatapp.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class SignupActivity extends AppCompatActivity {


    private TextInputEditText etEmail, etName, etPassword, etConfirmPassword;
    private ImageView ivProfile;
    private View pB;
    private String email, name, password, confirmPassword;

    private FirebaseUser currentUser;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference dbRefUsers;
    private StorageReference fileStorage;
    private Uri localFileUri, serverFileUri;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etName = findViewById(R.id.etName);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivProfile = findViewById(R.id.ivProfile);
        pB = findViewById(R.id.progressBar);

        fileStorage = FirebaseStorage.getInstance().getReference();
        dbRefUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void pickImage(View v)
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED)
        {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickingImageActivityResultLauncher.launch(intent);
        }
        else
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES},102);
        }
    }

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

    public void signupWithPhoto()
    {
        String strFileName = currentUser.getUid() + ".jpg";

        final StorageReference fileRef = fileStorage.child("images/" + strFileName);

        pB.setVisibility(View.VISIBLE);
        fileRef.putFile(localFileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                pB.setVisibility(View.GONE);
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

                            currentUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful())
                                    {
                                        String userID = currentUser.getUid();

                                        HashMap<String,String> hashMap = new HashMap<>();
                                        hashMap.put(NodeNames.NAME, etName.getText().toString().trim());
                                        hashMap.put(NodeNames.EMAIL, etEmail.getText().toString().trim());
                                        hashMap.put(NodeNames.PHOTO, serverFileUri.getPath());

                                        dbRefUsers.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                               // Toast.makeText(SignupActivity.this, R.string.user_created_successfully, Toast.LENGTH_LONG).show();
                                                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                            }
                                        });

                                    }
                                    else
                                    {
                                        Toast.makeText(SignupActivity.this, R.string.exception, Toast.LENGTH_LONG);
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

    }
     public void signupWithoutPhoto()
    {

        pB.setVisibility(View.VISIBLE);

        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(etName.getText().toString().trim())
                .build();

        currentUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                pB.setVisibility(View.GONE);

                if(task.isSuccessful())
                {
                    String userID = currentUser.getUid();

                    HashMap<String,String> hashMap = new HashMap<>();
                    hashMap.put(NodeNames.NAME, etName.getText().toString().trim());
                    hashMap.put(NodeNames.EMAIL, etEmail.getText().toString().trim());
                    hashMap.put(NodeNames.PHOTO, "");

                    pB.setVisibility(View.VISIBLE);

                    dbRefUsers.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            pB.setVisibility(View.GONE);

                            if(task.isSuccessful())
                            {
                                //Toast.makeText(SignupActivity.this, R.string.user_created_successfully, Toast.LENGTH_LONG).show();
                                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                            }
                        }
                    });

                }
                else
                {
                    Toast.makeText(SignupActivity.this, R.string.exception, Toast.LENGTH_LONG);
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

            if(Util.connectionAvailable(this))
            {
                pB.setVisibility(View.VISIBLE);

                firebaseAuth = FirebaseAuth.getInstance();

                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        pB.setVisibility(View.GONE);

                        if(task.isSuccessful())
                        {
                            currentUser = firebaseAuth.getCurrentUser();
                            if (localFileUri != null)
                                signupWithPhoto();
                            else
                                signupWithoutPhoto();
                        }
                        else
                        {
                            Toast.makeText(SignupActivity.this, R.string.exception, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
              else Toast.makeText(this, R.string.no_internet ,Toast.LENGTH_LONG).show();


        }
    }



}
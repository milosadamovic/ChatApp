package com.example.chatapp.profile;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.MainActivity;
import com.example.chatapp.R;
import com.example.chatapp.common.NodeNames;
import com.example.chatapp.common.OnGetDataListener;
import com.example.chatapp.common.Util;
import com.example.chatapp.findfriends.FindFriendsFragment;
import com.example.chatapp.login.LoginActivity;
import com.example.chatapp.password.ChangePasswordActivity;
import com.example.chatapp.requests.RequestsFragment;
import com.example.chatapp.signup.SignupActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {


    private TextInputEditText etEmail, etName;
    private String email, name;
    private ImageView ivProfile;

    private FirebaseUser firebaseUser;
    private DatabaseReference drUsers, drRequests, drTokens;
    private StorageReference fileStorage;
    private Uri localFileUri, serverFileUri;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        etEmail = findViewById(R.id.etEmail);
        etName = findViewById(R.id.etName);
        ivProfile = findViewById(R.id.ivProfile);

        fileStorage = FirebaseStorage.getInstance().getReference();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        progressBar = findViewById(R.id.progressBar);

        if(firebaseUser != null)
        {
            etName.setText(firebaseUser.getDisplayName());
            etEmail.setText(firebaseUser.getEmail());
            serverFileUri = firebaseUser.getPhotoUrl();

            if(serverFileUri != null)
            {
                Glide.with(this)
                        .load(serverFileUri)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(ivProfile);
            }

        }
    }


    public void btnLogoutClick(View view)
    {

        /** METODA 1 */

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();


        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        drUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        drRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());
        drTokens = FirebaseDatabase.getInstance().getReference().child(NodeNames.TOKEN).child(currentUser.getUid());


       if(firebaseAuth != null)
       {
           drTokens.setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
               @Override
               public void onComplete(@NonNull Task<Void> task) {

                   if(task.isSuccessful())
                   {
                       //drRequests.removeEventListener(RequestsFragment.valueEventListener);
                       //drUsers.removeEventListener(FindFriendsFragment.valueEventListener);
                       firebaseAuth.signOut();
                       startActivity(new Intent(ProfileActivity.this, LoginActivity.class));

                       finish();
                   }
                   else
                   {
                       Toast.makeText(ProfileActivity.this, getString(R.string.something_went_wrong, task.getException()), Toast.LENGTH_LONG);
                   }

               }
           });

       }

        /** METODA 2*/

      /**  FirebaseAuth firebaseAuth;
       // currentUser = FirebaseAuth.getInstance().getCurrentUser();
       // databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
       // drRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());
        firebaseAuth = FirebaseAuth.getInstance();
       // drRequests.removeEventListener(RequestsFragment.valueEventListener);
       // databaseReference.removeEventListener(FindFriendsFragment.valueEventListener);

        FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                    if (firebaseAuth.getCurrentUser() == null)
                    {
                        startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                        finish();
                    }
            }
        };

        firebaseAuth.addAuthStateListener(authStateListener);
        firebaseAuth.signOut();
*/
    }

    public void btnSaveClick(View view)
    {
        if(etName.getText().toString().trim().equals(""))
            etName.setError(getString(R.string.enter_name));
        else
        {
            if(localFileUri != null)
                updateNameAndPhoto();
            else
                updateOnlyName();
        }
    }


    public void changeImage(View view)
    {
        if(serverFileUri == null)
        {
            pickImage();
        }
        else
        {
            PopupMenu popupMenu = new PopupMenu(this, view);
            popupMenu.getMenuInflater().inflate(R.menu.menu_picture,popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    int id = menuItem.getItemId();
                    
                    if(id == R.id.mnuChangePicture)
                        pickImage();
                    else if (id == R.id.mnuRemovePic)
                        removePhoto();

                    return false;
                }
            });
            popupMenu.show();
        }
    }


    private void removePhoto()
    {

        progressBar.setVisibility(View.VISIBLE);

        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(etName.getText().toString().trim())
                .setPhotoUri(null)
                .build();

        firebaseUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                progressBar.setVisibility(View.GONE);

                if(task.isSuccessful())
                {
                    String userID = firebaseUser.getUid();
                    drUsers = FirebaseDatabase.getInstance().getReference().child("Users");

                    HashMap<String,String> hashMap = new HashMap<>();
                    hashMap.put(NodeNames.PHOTO, "");

                    drUsers.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(ProfileActivity.this, R.string.photo_removed_successfully, Toast.LENGTH_LONG).show();
                        }
                    });

                }
                else
                {
                    Toast.makeText(ProfileActivity.this, getString(R.string.failed_to_remove_photo, task.getException()), Toast.LENGTH_LONG);
                }
            }
        });



    }

    public void pickImage()
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

        // PRVO TREBA PROVERITI DA LI USER IMA SLIKU U STORAGEU

        if(serverFileUri == null)
        {
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
                                                drUsers = FirebaseDatabase.getInstance().getReference().child("Users");

                                                HashMap<String,String> hashMap = new HashMap<>();
                                                hashMap.put(NodeNames.NAME, etName.getText().toString().trim());
                                                hashMap.put(NodeNames.PHOTO, serverFileUri.getPath());
                                                hashMap.put(NodeNames.EMAIL, etEmail.getText().toString().trim());
                                                hashMap.put(NodeNames.ONLINE, "true");
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

                                                drUsers.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        finish();
                                                    }
                                                });

                                            }
                                            else
                                            {
                                                Toast.makeText(ProfileActivity.this, getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_LONG);
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    }
                });
        }
        else
        {
                fileRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {


                        fileRef.putFile(localFileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                                progressBar.setVisibility(View.GONE);

                                if (task.isSuccessful()) {
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
                                                    if (task.isSuccessful()) {
                                                        String userID = firebaseUser.getUid();
                                                        drUsers = FirebaseDatabase.getInstance().getReference().child("Users");

                                                        HashMap<String, String> hashMap = new HashMap<>();
                                                        hashMap.put(NodeNames.NAME, etName.getText().toString().trim());
                                                        hashMap.put(NodeNames.PHOTO, serverFileUri.getPath());
                                                        hashMap.put(NodeNames.EMAIL, etEmail.getText().toString().trim());
                                                        hashMap.put(NodeNames.ONLINE, "true");
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

                                                        drUsers.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                finish();
                                                            }
                                                        });

                                                    } else {
                                                        Toast.makeText(ProfileActivity.this, getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_LONG);
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        });


                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Toast.makeText(ProfileActivity.this, R.string.failed_to_delete_old_image, Toast.LENGTH_LONG).show();
                    }
                });
        }

       /* fileRef.putFile(localFileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
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
                                        hashMap.put(NodeNames.PHOTO, serverFileUri.getPath());

                                        databaseReference.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                finish();
                                            }
                                        });

                                    }
                                    else
                                    {
                                        Toast.makeText(ProfileActivity.this, getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_LONG);
                                    }
                                }
                            });
                        }
                    });
                }
            }
        }); */

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
                    drUsers = FirebaseDatabase.getInstance().getReference().child("Users");

                    HashMap<String,String> hashMap = new HashMap<>();
                    hashMap.put(NodeNames.NAME, etName.getText().toString().trim());
                    hashMap.put(NodeNames.EMAIL, etEmail.getText().toString().trim());
                    hashMap.put(NodeNames.ONLINE, "true");
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

                    if(serverFileUri != null) hashMap.put(NodeNames.PHOTO, serverFileUri.getPath());
                    else hashMap.put(NodeNames.PHOTO, "");

                    drUsers.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            finish();
                        }
                    });

                }
                else
                {
                    Toast.makeText(ProfileActivity.this, getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_LONG);
                }
            }
        });
    }


    public void btnChangePasswordClick(View view)
    {
          startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class));
    }



}
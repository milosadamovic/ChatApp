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
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.NetworkError;
import com.example.chatapp.findfriends.FindFriendsFragment;
import com.example.chatapp.main.MainActivity;
import com.example.chatapp.R;
import com.example.chatapp.requests.RequestsFragment;
import com.example.chatapp.util.NodeNames;
import com.example.chatapp.login.LoginActivity;
import com.example.chatapp.password.ChangePasswordActivity;
import com.example.chatapp.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {


    private TextInputEditText etEmail, etName;
    private ImageView ivProfile;
    private View pB;

    private DatabaseReference dbRefUsers, dbRefTokens, dbRefChats;
    private StorageReference fileStorage;
    private Uri localFileUri, serverFileUri;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    private Map<String, String> changeFlagValue;
    private boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if(!Util.connectionAvailable(this)) startActivity(new Intent(this, NetworkError.class));


        etEmail = findViewById(R.id.etEmail);
        etName = findViewById(R.id.etName);
        ivProfile = findViewById(R.id.ivProfile);

        fileStorage = FirebaseStorage.getInstance().getReference();

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        dbRefUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        dbRefChats = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS);

        pB = findViewById(R.id.progressBar);

        if(currentUser != null)
        {
            etName.setText(currentUser.getDisplayName());
            etEmail.setText(currentUser.getEmail());
            serverFileUri = currentUser.getPhotoUrl();

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

        if(Util.connectionAvailable(this))
        {
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();


            currentUser = FirebaseAuth.getInstance().getCurrentUser();
            dbRefTokens = FirebaseDatabase.getInstance().getReference().child(NodeNames.TOKEN).child(currentUser.getUid());

            dbRefTokens.setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if(task.isSuccessful())
                    {
                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                        firebaseAuth.signOut();
                    }
                    else
                    {
                        Toast.makeText(ProfileActivity.this, R.string.exception, Toast.LENGTH_LONG);
                    }

                }
            });

       }else  Toast.makeText(this, R.string.no_internet ,Toast.LENGTH_LONG).show();
    }

    public void btnSaveClick(View view)
    {
        if(etName.getText().toString().trim().equals(""))
            etName.setError(getString(R.string.enter_name));
        else
        {
            if(Util.connectionAvailable(this))
            {
                if(localFileUri != null)
                    updateNameAndPhoto();
                else
                    updateOnlyName();
            }
            else Toast.makeText(this, R.string.no_internet ,Toast.LENGTH_LONG).show();

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

        if(Util.connectionAvailable(this))
        {
            pB.setVisibility(View.VISIBLE);

            String strFileName = currentUser.getUid() + ".jpg";
            final StorageReference fileRef = fileStorage.child("images/" + strFileName);


            fileRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if(task.isSuccessful())
                    {
                        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                .setDisplayName(etName.getText().toString().trim())
                                .setPhotoUri(null)
                                .build();

                        serverFileUri = null;

                        currentUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                pB.setVisibility(View.GONE);

                                if(task.isSuccessful())
                                {
                                    String currUserId = currentUser.getUid();

                                    HashMap<String,String> hashMap = new HashMap<>();
                                    hashMap.put(NodeNames.NAME, etName.getText().toString().trim());
                                    hashMap.put(NodeNames.PHOTO, "");
                                    hashMap.put(NodeNames.EMAIL, etEmail.getText().toString().trim());

                                    dbRefUsers.child(currUserId).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if(task.isSuccessful())
                                            {
                                                dbRefChats.child(currUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                        if(snapshot.exists())
                                                        {
                                                            changeFlagValue = ServerValue.TIMESTAMP;
                                                            for (DataSnapshot ds : snapshot.getChildren())
                                                            {

                                                                if(ds.getKey() != null)
                                                                {
                                                                    String userId = ds.getKey();
                                                                    dbRefChats.child(userId).child(currUserId).child(NodeNames.CHANGE_IMAGE_FLAG).setValue(changeFlagValue);
                                                                }

                                                            }
                                                        }
                                                        Toast.makeText(ProfileActivity.this, R.string.photo_removed_successfully, Toast.LENGTH_LONG).show();
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                        Toast.makeText(ProfileActivity.this, R.string.exception, Toast.LENGTH_LONG);
                                                    }
                                                });
                                            }
                                        }
                                    });

                                }
                                else
                                {
                                    Toast.makeText(ProfileActivity.this, R.string.exception, Toast.LENGTH_LONG);
                                }
                            }
                        });
                    }

                }
            });
        }
        else Toast.makeText(this, R.string.no_internet ,Toast.LENGTH_LONG).show();


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
        final String strFileName = currentUser.getUid() + ".jpg";
        final StorageReference fileRef = fileStorage.child("images/" + strFileName);

        pB.setVisibility(View.VISIBLE);


        if(serverFileUri == null)
        {
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
                                                String currUserId = currentUser.getUid();

                                                HashMap<String,String> hashMap = new HashMap<>();
                                                hashMap.put(NodeNames.NAME, etName.getText().toString().trim());
                                                hashMap.put(NodeNames.PHOTO, serverFileUri.getPath());
                                                hashMap.put(NodeNames.EMAIL, etEmail.getText().toString().trim());

                                                dbRefUsers.child(currUserId).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {

                                                        if(task.isSuccessful())
                                                        {
                                                            dbRefChats.child(currUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                                    if(snapshot.exists())
                                                                    {
                                                                        changeFlagValue = ServerValue.TIMESTAMP;
                                                                        for (DataSnapshot ds : snapshot.getChildren())
                                                                        {

                                                                            if(ds.getKey() != null)
                                                                            {
                                                                                String userId = ds.getKey();
                                                                                dbRefChats.child(userId).child(currUserId).child(NodeNames.CHANGE_IMAGE_FLAG).setValue(changeFlagValue);
                                                                            }

                                                                        }
                                                                    }
                                                                    finish();
                                                                }

                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError error) {

                                                                    Toast.makeText(ProfileActivity.this, R.string.exception, Toast.LENGTH_LONG);
                                                                }
                                                            });
                                                        }


                                                    }
                                                });

                                            }
                                            else
                                            {
                                                Toast.makeText(ProfileActivity.this, R.string.exception, Toast.LENGTH_LONG);
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
                fileRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if(task.isSuccessful())
                        {
                            fileRef.putFile(localFileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                                    pB.setVisibility(View.GONE);

                                    if (task.isSuccessful()) {

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
                                                        if (task.isSuccessful()) {

                                                            String currUserId = currentUser.getUid();
                                                            flag = !flag;

                                                            HashMap<String, String> hashMap = new HashMap<>();
                                                            hashMap.put(NodeNames.NAME, etName.getText().toString().trim());
                                                            hashMap.put(NodeNames.PHOTO, serverFileUri.getPath());
                                                            hashMap.put(NodeNames.EMAIL, etEmail.getText().toString().trim());
                                                            hashMap.put(NodeNames.CHANGE_IMAGE_FLAG, String.valueOf(flag));

                                                           dbRefUsers.child(currUserId).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                    if(task.isSuccessful())
                                                                    {
                                                                        dbRefChats.child(currUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                            @Override
                                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                                                if(snapshot.exists())
                                                                                {
                                                                                    changeFlagValue = ServerValue.TIMESTAMP;
                                                                                    for (DataSnapshot ds : snapshot.getChildren())
                                                                                    {
                                                                                        if(ds.getKey() != null)
                                                                                        {
                                                                                            String userId = ds.getKey();
                                                                                            dbRefChats.child(userId).child(currUserId).child(NodeNames.CHANGE_IMAGE_FLAG).setValue(changeFlagValue);
                                                                                        }
                                                                                    }
                                                                                }
                                                                                finish();
                                                                            }

                                                                            @Override
                                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                                                Toast.makeText(ProfileActivity.this, R.string.exception, Toast.LENGTH_LONG);
                                                                            }
                                                                        });
                                                                    }

                                                                }
                                                            });



                                                        } else {
                                                            Toast.makeText(ProfileActivity.this, R.string.exception, Toast.LENGTH_LONG);
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Toast.makeText(ProfileActivity.this, R.string.exception, Toast.LENGTH_LONG).show();
                    }
                });
        }
    }

    public void updateOnlyName()
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
                    String currUserId = currentUser.getUid();

                    HashMap<String,String> hashMap = new HashMap<>();
                    hashMap.put(NodeNames.NAME, etName.getText().toString().trim());
                    hashMap.put(NodeNames.EMAIL, etEmail.getText().toString().trim());

                    if(serverFileUri != null) hashMap.put(NodeNames.PHOTO, serverFileUri.getPath());
                    else hashMap.put(NodeNames.PHOTO, "");

                    dbRefUsers.child(currUserId).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful())
                            {
                                dbRefChats.child(currUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                        if(snapshot.exists())
                                        {
                                            changeFlagValue = ServerValue.TIMESTAMP;
                                            for (DataSnapshot ds : snapshot.getChildren())
                                            {

                                                if(ds.getKey() != null)
                                                {
                                                    String userId = ds.getKey();
                                                    dbRefChats.child(userId).child(currUserId).child(NodeNames.CHANGE_IMAGE_FLAG).setValue(changeFlagValue);
                                                }

                                            }
                                        }
                                        finish();

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                        Toast.makeText(ProfileActivity.this, R.string.exception, Toast.LENGTH_LONG);
                                    }
                                });
                            }

                        }
                    });

                }
                else
                {
                    Toast.makeText(ProfileActivity.this, R.string.exception, Toast.LENGTH_LONG);
                }
            }
        });
    }


    public void btnChangePasswordClick(View view)
    {
          startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class));
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(!Util.connectionAvailable(this)) startActivity(new Intent(this, NetworkError.class));
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
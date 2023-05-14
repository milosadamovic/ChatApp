package com.example.chatapp.chats;

import static android.os.Environment.getExternalStorageDirectory;
import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.MainActivity;
import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.Extras;
import com.example.chatapp.common.NodeNames;
import com.example.chatapp.common.Util;
import com.example.chatapp.selectfriend.SelectFriendActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {


    private ImageView ivSend, ivAttachment, ivProfile;
    private TextView tvUserName, tvUserStatus;
    private EditText etMessage;
    private DatabaseReference mRootRef;
    private FirebaseAuth firebaseAuth;
    private String currentUserId, chatUserId;
    public static String openChatUserId;

    private RecyclerView rvMessages;
    private MessagesAdapter messagesAdapter;
    private List<MessageModel> messageList;


    /*private int currentPage = 1;
    private static final int RECORD_PER_PAGE = 30;*/

    private static final int REQUEST_CODE_PICK_IMAGE = 101;
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 102;
    private static final int REQUEST_CODE_PICK_VIDEO = 103;
    private static final int REQUEST_CODE_FORWARD_MESSAGE = 104;



    private DatabaseReference databaseReferenceMessages;
    private ChildEventListener childEventListener;

    private BottomSheetDialog bottomSheetDialog;

    private LinearLayout llProgress;
    private String userName, photoName;

    public static boolean isResumed = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d("ChatActivity", "onCreate() called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);



        /**POSTAVLJANJE ACTION BARA*/
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setTitle("");
            ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_action_bar, null);

            /*actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);*/
            actionBar.setElevation(0);
            actionBar.setCustomView(actionBarLayout);
            actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
        }


        ivProfile = findViewById(R.id.ivProfile2);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserStatus = findViewById(R.id.tvUserStatus);

        ivSend = findViewById(R.id.ivSend);
        ivAttachment = findViewById(R.id.ivAttachment);
        etMessage = findViewById(R.id.etMessage);
        llProgress = findViewById(R.id.llProgress);

        etMessage.requestFocus();
        ivSend.setOnClickListener(this);
        ivAttachment.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        currentUserId = firebaseAuth.getCurrentUser().getUid();


        /**NABAVLJANJE PODATAKA O KORISNIKU IZ PRETHODNE AKTIVNOSTI*/
        if(getIntent().hasExtra(Extras.USER_KEY))
        {
            chatUserId = getIntent().getStringExtra(Extras.USER_KEY);
            openChatUserId = chatUserId;
        }

        if(getIntent().hasExtra(Extras.USER_NAME))
        {
            userName = getIntent().getStringExtra(Extras.USER_NAME);
        }

        if(getIntent().hasExtra(Extras.PHOTO_NAME))
        {
            photoName = getIntent().getStringExtra(Extras.PHOTO_NAME);
        }


        /**POSTAVLJANJE IMENA USERA I SLIKE AKO JE IMA*/
        tvUserName.setText(userName);
        if(!TextUtils.isEmpty(photoName))
        {
            StorageReference photoRef = FirebaseStorage.getInstance().getReference().child(Constants.IMAGES_FOLDER).child(photoName);

            photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {

                    Glide.with(ChatActivity.this)
                            .load(uri)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(ivProfile);
                }
            });
        }


        rvMessages = findViewById(R.id.rvMessages);
        messageList = new ArrayList<>();

        messagesAdapter = new MessagesAdapter(this,messageList);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messagesAdapter);

        loadMessages();

        mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue(0);

        rvMessages.scrollToPosition(messageList.size() - 1);

        /**ATTACHMENT OPCIJE - BOTTOM DIALOG*/
        bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.chat_file_options, null);
        view.findViewById(R.id.llCamera).setOnClickListener(this);
        view.findViewById(R.id.llGallery).setOnClickListener(this);
        view.findViewById(R.id.llVideo).setOnClickListener(this);
        view.findViewById(R.id.ivClose).setOnClickListener(this);
        bottomSheetDialog.setContentView(view);


        /**PROVERA DA LI SE RADI O FORWARD PORUCI*/
        if(getIntent().hasExtra(Extras.MESSAGE) && getIntent().hasExtra(Extras.MESSAGE_ID) && getIntent().hasExtra(Extras.MESSAGE_TYPE))
        {
            String message = getIntent().getStringExtra(Extras.MESSAGE);
            String messageId = getIntent().getStringExtra(Extras.MESSAGE_ID);
            String messageType = getIntent().getStringExtra(Extras.MESSAGE_TYPE);

            DatabaseReference messageRef = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
            String newMessageId = messageRef.getKey();

            if(messageType.equals(Constants.MESSAGE_TYPE_TEXT))
                sendMessage(message, messageType, newMessageId);
            else
            {
                StorageReference rootRef = FirebaseStorage.getInstance().getReference();
                String folder = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
                String oldFileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? messageId + ".mp4" : messageId + ".jpg";
                String newFileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? newMessageId + ".mp4" : newMessageId + ".jpg";

                String localFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + oldFileName;
                File localFile = new File(localFilePath);

                StorageReference newFileRef = rootRef.child(folder).child(newFileName);

                rootRef.child(folder).child(oldFileName).getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                        UploadTask uploadTask = newFileRef.putFile(Uri.fromFile(localFile));
                        uploadProgress(uploadTask, newFileRef, newMessageId, messageType);
                    }

                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("GRESKA", e.getMessage());
                    }
                });
            }
        }


        /**SETOVANJE ONLINE/OFFLINE STATUSA*/
        DatabaseReference databaseReferenceUsers = mRootRef.child(NodeNames.USERS).child(chatUserId);
        databaseReferenceUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String status = "";

                if(snapshot.child(NodeNames.ONLINE).getValue() != null)
                    status = snapshot.child(NodeNames.ONLINE).getValue().toString();

                if(status.equals("true"))
                    tvUserStatus.setText(Constants.STATUS_ONLINE);
                else
                    tvUserStatus.setText(Constants.STATUS_OFFLINE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        /**SETOVANJE TYPING STATUSA*/
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                DatabaseReference currentUserRef = mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId);
                if(s.toString().matches(""))
                {
                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STOPPED);
                }
                else
                {
                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STARTED);
                }

                DatabaseReference chatUserRef = mRootRef.child(NodeNames.CHATS).child(chatUserId).child(currentUserId);

                chatUserRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if(snapshot.child(NodeNames.TYPING).getValue() != null)
                        {
                            String typingStatus = snapshot.child(NodeNames.TYPING).getValue().toString();

                            if(typingStatus.equals(Constants.TYPING_STARTED))
                                tvUserStatus.setText(Constants.STATUS_TYPING);
                            else
                                tvUserStatus.setText(Constants.STATUS_ONLINE);
                        }



                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }
        });


        /**BRISANJE NOTIFIKACIJE-PORUKA UKOLIKO POSTOJI*/
        Util.cancelNotifications(this, Constants.NOTIFICATION_TYPE_MESSAGEID);

    }


    private void sendMessage(String msg, String msgType, String pushId) {
        try {

            if (!msg.equals(""))
            {
                HashMap messageMap = new HashMap();
                messageMap.put(NodeNames.MESSAGE_ID, pushId);
                messageMap.put(NodeNames.MESSAGE, msg);
                messageMap.put(NodeNames.MESSAGE_TYPE, msgType);
                messageMap.put(NodeNames.MESSAGE_FROM, currentUserId);
                messageMap.put(NodeNames.MESSAGE_TIME, ServerValue.TIMESTAMP);


                String currentUserRef = NodeNames.MESSAGES + "/" + currentUserId + "/" + chatUserId;
                String chatUserRef = NodeNames.MESSAGES + "/" + chatUserId + "/" + currentUserId;

                HashMap messageUserMap = new HashMap();
                messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
                messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

                etMessage.setText("");

                mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {

                        if(error != null)
                        {
                            Toast.makeText(ChatActivity.this, getString(R.string.failed_to_send_message, error.getMessage()), Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            Toast.makeText(ChatActivity.this,R.string.message_sent_successfully, Toast.LENGTH_LONG).show();

                            String title="";

                            if(msgType.equals(Constants.MESSAGE_TYPE_TEXT))
                                title = "New Message";
                            else if(msgType.equals(Constants.MESSAGE_TYPE_IMAGE))
                                title = "New Image";
                            else if(msgType.equals(Constants.MESSAGE_TYPE_VIDEO))
                                title = "New Video";

                            Util.sendNotification(ChatActivity.this, title, msg, chatUserId, currentUserId, Constants.NOTIFICATION_TYPE_MESSAGE);

                            String lastMessage = !title.equals("New Message") ? title : msg;
                            Util.updateChatDetails(ChatActivity.this, currentUserId, chatUserId,lastMessage, msgType);
                        }
                    }
                });

            }

        } catch (Exception ex){

            Toast.makeText(ChatActivity.this, getString(R.string.failed_to_send_message, ex.getMessage()), Toast.LENGTH_LONG).show();

        }
    }

     private void loadMessages()
    {
        messageList.clear();

        databaseReferenceMessages = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId);
        Query messageQuery = databaseReferenceMessages.orderByChild(NodeNames.LAST_MESSAGE_TIME);
        //Query messageQuery = databaseReferenceMessages.limitToLast(currentPage * RECORD_PER_PAGE);


        Log.d("ChatActivity", "loadMessages() - HELLO FROM LOAD MESSAGES");


        if (childEventListener != null)
            messageQuery.removeEventListener(childEventListener);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                //Log.d("ChatActivity", "CHILD HAS BEEN ADDED");
                MessageModel message = snapshot.getValue(MessageModel.class);

                messageList.add(message);

                /**PAZI NA NOTIFY*/
               // messagesAdapter.notifyItemInserted(messageList.size()-1);
                messagesAdapter.notifyDataSetChanged();
                rvMessages.scrollToPosition(messageList.size()-1);


            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    loadMessages();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                    loadMessages();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        messageQuery.addChildEventListener(childEventListener);

    }

    @Override
    public void onClick(View view) {


        switch (view.getId())
        {
            case R.id.ivSend:

                if(Util.connectionAvailable(this))
                {
                    DatabaseReference userMessagePush = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push(); // generating unique message id
                    String pushId = userMessagePush.getKey();
                    sendMessage(etMessage.getText().toString().trim(), Constants.MESSAGE_TYPE_TEXT, pushId);
                }
                else
                {
                    Toast.makeText(this, R.string.no_internet, Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.ivAttachment:
                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                {
                    if(bottomSheetDialog != null)
                        bottomSheetDialog.show();
                }
                else
                {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }

                // hidding keyboard
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if(inputMethodManager != null)
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);

                break;

            case R.id.llCamera:
                bottomSheetDialog.dismiss();
                Intent intentCamera = new Intent(ACTION_IMAGE_CAPTURE);
                //TODO U options POLJE SE MOZE UBACITI I NEKA ANIMACIJA
                ActivityCompat.startActivityForResult(this, intentCamera, REQUEST_CODE_CAPTURE_IMAGE, null);
                break;

            case R.id.llGallery:
                bottomSheetDialog.dismiss();
                Intent intentImage = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                ActivityCompat.startActivityForResult(this, intentImage, REQUEST_CODE_PICK_IMAGE, null);
                break;

            case R.id.llVideo:
                bottomSheetDialog.dismiss();
                Intent intentVideo = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                ActivityCompat.startActivityForResult(this, intentVideo, REQUEST_CODE_PICK_VIDEO, null);
                break;

            case R.id.ivClose:
                bottomSheetDialog.dismiss();
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
        {
            if(requestCode == REQUEST_CODE_CAPTURE_IMAGE)
            {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

                uploadBytes(bytes, Constants.MESSAGE_TYPE_IMAGE);

            }
            else if(requestCode == REQUEST_CODE_PICK_IMAGE)
            {
                Uri uri = data.getData();
                Log.d("URI", uri.toString());
                uploadFile(uri, Constants.MESSAGE_TYPE_IMAGE);
            }
            else if(requestCode == REQUEST_CODE_PICK_VIDEO)
            {
                Uri uri = data.getData();
                uploadFile(uri, Constants.MESSAGE_TYPE_VIDEO);
            }

            else if(requestCode == REQUEST_CODE_FORWARD_MESSAGE)
            {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra(Extras.USER_KEY, data.getStringExtra(Extras.USER_KEY));
                intent.putExtra(Extras.USER_NAME, data.getStringExtra(Extras.USER_NAME));
                intent.putExtra(Extras.PHOTO_NAME, data.getStringExtra(Extras.PHOTO_NAME));

                intent.putExtra(Extras.MESSAGE, data.getStringExtra(Extras.MESSAGE));
                intent.putExtra(Extras.MESSAGE_ID, data.getStringExtra(Extras.MESSAGE_ID));
                intent.putExtra(Extras.MESSAGE_TYPE, data.getStringExtra(Extras.MESSAGE_TYPE));

                startActivity(intent);
                finish();
            }
        }
    }

    private void uploadFile(Uri uri, String messageType)
    {
        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
        String pushId = databaseReference.getKey();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
        String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? pushId + ".mp4" : pushId + ".jpg";

        StorageReference fileRef = storageReference.child(folderName).child(fileName);
        UploadTask uploadTask = fileRef.putFile(uri);

        uploadProgress(uploadTask, fileRef, pushId, messageType);
    }

    private void uploadBytes(ByteArrayOutputStream bytes, String messageType)
    {
        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
        String pushId = databaseReference.getKey();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
        String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? pushId + ".mp4" : pushId + ".jpg";

        StorageReference fileRef = storageReference.child(folderName).child(fileName);
        UploadTask uploadTask = fileRef.putBytes(bytes.toByteArray());

        uploadProgress(uploadTask, fileRef, pushId, messageType);
    }

    private void uploadProgress(final UploadTask task, StorageReference filePath, String pushId, String messageType)
    {
        View view = getLayoutInflater().inflate(R.layout.file_progress, null);
        ProgressBar pbProgress = view.findViewById(R.id.pbProgress);
        TextView tvProgress = view.findViewById(R.id.tvFileProgress);
        ImageView ivPlay = view.findViewById(R.id.ivPlay);
        ImageView ivPause = view.findViewById(R.id.ivPause);
        ImageView ivCancel = view.findViewById(R.id.ivCancel);

        ivPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.pause();
                ivPlay.setVisibility(View.VISIBLE);
                ivPause.setVisibility(View.GONE);
            }
        });

        ivPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.resume();
                ivPause.setVisibility(View.VISIBLE);
                ivPlay.setVisibility(View.GONE);
            }
        });

        ivCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.cancel();
                /** DODATO */
                llProgress.removeView(view);
                Toast.makeText(ChatActivity.this, "Sending video cancelled", Toast.LENGTH_SHORT).show();
            }
        });

        llProgress.addView(view);
        tvProgress.setText(getString(R.string.upload_progress, messageType, "0"));

        task.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {

                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();

                pbProgress.setProgress((int)progress);
                tvProgress.setText(getString(R.string.upload_progress, messageType, String.valueOf(pbProgress.getProgress())));
            }
        });

        task.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                llProgress.removeView(view);
                if(task.isSuccessful())
                {
                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            String downloadUrl = uri.toString();
                            sendMessage(downloadUrl, messageType, pushId);
                        }
                    });
                }

            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                llProgress.removeView(view);
                Toast.makeText(ChatActivity.this, getString(R.string.failed_to_upload, e.getMessage()), Toast.LENGTH_SHORT).show();

            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1)
        {
            if(grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                if(bottomSheetDialog != null)
                    bottomSheetDialog.show();
            }
            else
            {
                Toast.makeText(this, "Permission required to access files", Toast.LENGTH_SHORT).show();
            }
        }

    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int itemId = item.getItemId();
        switch (itemId)
        {
            case android.R.id.home:
                finish();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void deleteMessage(String messageId, String messageType, int messagePosition)
    {

        HashMap messageMap = new HashMap();
        messageMap.put(NodeNames.MESSAGE_ID, messageId);
        messageMap.put(NodeNames.MESSAGE, Constants.DELETED_MESSAGE_TEXT);
        messageMap.put(NodeNames.MESSAGE_TYPE, Constants.MESSAGE_TYPE_DELETED);
        messageMap.put(NodeNames.MESSAGE_FROM, currentUserId);
        messageMap.put(NodeNames.MESSAGE_TIME, ServerValue.TIMESTAMP);

        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).child(messageId);
        databaseReference.updateChildren(messageMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if(task.isSuccessful())
                {
                    DatabaseReference databaseReferenceChatUser = mRootRef.child(NodeNames.MESSAGES).child(chatUserId).child(currentUserId).child(messageId);
                    databaseReferenceChatUser.updateChildren(messageMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful())
                            {

                                /**PROVERA DA LI JE OBRISANA POSLEDNJA POSLATA PORUKA*/
                                Log.d("ChatActivity", "deleteMessage() - messagePosition: " + messagePosition + "  messageList.size() - 1: " + (messageList.size()-1));
                                if((messageList.size()-1) == messagePosition)
                                {
                                    String lastMessage = Constants.DELETED_MESSAGE_TEXT;
                                    String lastMessageType = Constants.MESSAGE_TYPE_DELETED;
                                    Util.updateChatDetails(ChatActivity.this,currentUserId, chatUserId, lastMessage, lastMessageType);
                                }


                                Toast.makeText(ChatActivity.this, R.string.message_deleted_successfully, Toast.LENGTH_SHORT).show();
                                if(!messageType.equals(Constants.MESSAGE_TYPE_TEXT))
                                {
                                    StorageReference rootRef = FirebaseStorage.getInstance().getReference();
                                    String folder = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
                                    String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? messageId + ".mp4" : messageId + ".jpg";
                                    StorageReference fileRef = rootRef.child(folder).child(fileName);

                                    fileRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if(!task.isSuccessful())
                                                Toast.makeText(ChatActivity.this, getString(R.string.failed_to_delete_file, task.getException()), Toast.LENGTH_SHORT).show();

                                        }
                                    });
                                }
                            }
                            else
                                Toast.makeText(ChatActivity.this, getString(R.string.failed_to_delete_message, task.getException()), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else
                {
                    Toast.makeText(ChatActivity.this, getString(R.string.failed_to_delete_message, task.getException()), Toast.LENGTH_SHORT).show();
                }

            }
        });




    }


    public void clearMessage(String messageId, String messageType)
    {
        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).child(messageId);
        databaseReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if(!task.isSuccessful())
                {
                    Toast.makeText(ChatActivity.this, getString(R.string.failed_to_clear_message, task.getException()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /**********************************VRATI MIN SDK NA 30 !!!!****************************/
    public void downloadFile(String messageId, String messageType, boolean isShare)
    {

        if (Environment.isExternalStorageManager()) {

            StorageReference rootRef = FirebaseStorage.getInstance().getReference();
            String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
            String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? messageId + ".mp4" : messageId + ".jpg";
            StorageReference fileRef = rootRef.child(folderName).child(fileName);

            // CUVANJE SLIKE NA OVOJ POZICIJI NA UREDJAJU
            //String localFilePath = getExternalFilesDir(null).getAbsolutePath() + "/" + fileName;
            String localFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + fileName;
            File localFile = new File(localFilePath);

            try {

                if(localFile.exists() || localFile.createNewFile())
                {
                    FileDownloadTask downloadTask = fileRef.getFile(localFile);

                    View view = getLayoutInflater().inflate(R.layout.file_progress, null);
                    ProgressBar pbProgress = view.findViewById(R.id.pbProgress);
                    TextView tvProgress = view.findViewById(R.id.tvFileProgress);
                    ImageView ivPlay = view.findViewById(R.id.ivPlay);
                    ImageView ivPause = view.findViewById(R.id.ivPause);
                    ImageView ivCancel = view.findViewById(R.id.ivCancel);

                    ivPause.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            downloadTask.pause();
                            ivPlay.setVisibility(View.VISIBLE);
                            ivPause.setVisibility(View.GONE);
                        }
                    });

                    ivPlay.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            downloadTask.resume();
                            ivPause.setVisibility(View.VISIBLE);
                            ivPlay.setVisibility(View.GONE);
                        }
                    });

                    ivCancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            downloadTask.cancel();
                            // DODATO
                            llProgress.removeView(view);
                            //Toast.makeText(ChatActivity.this, "Sending video cancelled", Toast.LENGTH_SHORT).show();
                        }
                    });

                    llProgress.addView(view);
                    tvProgress.setText(getString(R.string.download_progress, messageType, "0"));

                    downloadTask.addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull FileDownloadTask.TaskSnapshot snapshot) {

                            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();

                            pbProgress.setProgress((int)progress);
                            tvProgress.setText(getString(R.string.download_progress, messageType, String.valueOf(pbProgress.getProgress())));

                        }
                    });

                    downloadTask.addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {

                            llProgress.removeView(view);
                            if(task.isSuccessful())
                            {
                                MediaScannerConnection.scanFile(ChatActivity.this, new String[]{localFilePath}, null, new MediaScannerConnection.OnScanCompletedListener() {
                                    @Override
                                    public void onScanCompleted(String path, Uri uri) {

                                        if(isShare)
                                        {
                                            Intent intentShare = new Intent();
                                            intentShare.setAction(Intent.ACTION_SEND);
                                            intentShare.putExtra(Intent.EXTRA_STREAM, uri);
                                            if(messageType.equals(Constants.MESSAGE_TYPE_VIDEO))
                                                intentShare.setType("video/mp4");
                                            if(messageType.equals(Constants.MESSAGE_TYPE_IMAGE))
                                                intentShare.setType("image/jpg");
                                            startActivity(Intent.createChooser(intentShare, getString(R.string.share_with)));
                                        }
                                        else {
                                            Snackbar snackbar = Snackbar.make(llProgress, getString(R.string.file_downloaded_successfully), Snackbar.LENGTH_INDEFINITE);
                                            snackbar.setAction(R.string.view, new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    //Uri uri = Uri.parse(localFilePath);
                                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                                                    if (messageType.equals(Constants.MESSAGE_TYPE_VIDEO))
                                                        intent.setDataAndType(uri, "video/mp4");
                                                    else if (messageType.equals(Constants.MESSAGE_TYPE_IMAGE))
                                                        intent.setDataAndType(uri, "image/jpg");

                                                    startActivity(intent);

                                                }
                                            });
                                            snackbar.show();
                                        }
                                    }
                                });
                            }

                        }
                    });


                    downloadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            llProgress.removeView(view);
                            Toast.makeText(ChatActivity.this, getString(R.string.failed_to_download, e.getMessage()), Toast.LENGTH_SHORT).show();

                        }
                    });


                }else
                {
                    Toast.makeText(this, R.string.failed_to_store_file, Toast.LENGTH_SHORT).show();
                }

            }catch (Exception ex)
            {
                Toast.makeText(ChatActivity.this, getString(R.string.failed_to_download, ex.getMessage()), Toast.LENGTH_SHORT).show();
            }

        } else {

            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }

    }


    public void forwardMessage(String selectedMessageId, String selectedMessage, String selectedMessageType, String chatUserId) {

        Intent intent = new Intent(this, SelectFriendActivity.class);
        intent.putExtra(Extras.MESSAGE, selectedMessage);
        intent.putExtra(Extras.MESSAGE_ID, selectedMessageId);
        intent.putExtra(Extras.MESSAGE_TYPE, selectedMessageType);
        intent.putExtra(Extras.CHAT_USER_ID, chatUserId);

        ActivityCompat.startActivityForResult(this, intent, REQUEST_CODE_FORWARD_MESSAGE, null);
    }

    @Override
    public void onBackPressed() {
        mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue(0);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        super.onBackPressed();
    }


    public static boolean isActivityResumed() {
        return isResumed;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("ChatActivity:", "onStart() called");
    }

    @Override
    protected void onResume() {
        isResumed = true;
        Util.cancelNotifications(this, Constants.NOTIFICATION_TYPE_MESSAGEID);
        super.onResume();
        Log.d("ChatActivity:", "onResume() called");
    }

    @Override
    protected void onPause() {
        isResumed = false;
        super.onPause();
        Log.d("ChatActivity:", "onPause() called");
    }

    @Override
    protected void onStop() {
        mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue(0);
        Log.d("ChatActivity:", "onStop() called");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d("ChatActivity:", "onDestroy() called");
        super.onDestroy();
    }
}
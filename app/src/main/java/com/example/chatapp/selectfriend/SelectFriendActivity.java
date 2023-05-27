package com.example.chatapp.selectfriend;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.R;
import com.example.chatapp.util.Extras;
import com.example.chatapp.util.NodeNames;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SelectFriendActivity extends AppCompatActivity {

    private RecyclerView rvSelectFriend;
    private View pB;

    private SelectFriendAdapter adapter;
    private List<SelectFriendModel> selectFriendModels;
    private List<String>userIds;

    private DatabaseReference dbRefUsers, dbRefChats;
    private FirebaseUser currentUser;
    private ChildEventListener childEventListener;

    private String selectedMessage, selectedMessageId, selectedMessageType, chatUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friend);


        if(getIntent().hasExtra(Extras.MESSAGE))
        {
            selectedMessage = getIntent().getStringExtra(Extras.MESSAGE);
            selectedMessageId = getIntent().getStringExtra(Extras.MESSAGE_ID);
            selectedMessageType = getIntent().getStringExtra(Extras.MESSAGE_TYPE);
            chatUserId = getIntent().getStringExtra(Extras.CHAT_USER_ID);
        }

        rvSelectFriend = findViewById(R.id.rvSelectFriend);
        pB = findViewById(R.id.progressBar);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvSelectFriend.setLayoutManager(linearLayoutManager);

        selectFriendModels = new ArrayList<>();
        userIds = new ArrayList<>();
        adapter = new SelectFriendAdapter(this, selectFriendModels);
        rvSelectFriend.setAdapter(adapter);

        pB.setVisibility(View.VISIBLE);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        dbRefChats = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS).child(currentUser.getUid());
        dbRefUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

        selectFriendModels.clear();
        userIds.clear();

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {


                Log.d("SelectFriendActivity", "onChildAdded() called");

                String userId = snapshot.getKey();

                dbRefUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {


                        String userName = snapshot.child(NodeNames.NAME).getValue() != null ? snapshot.child(NodeNames.NAME).getValue().toString() : "";

                        if (!userId.equals(chatUserId)) {
                            SelectFriendModel friendModel = new SelectFriendModel(userId, userName, userId + ".jpg");
                            userIds.add(userId);
                            selectFriendModels.add(friendModel);
                            int indexOfUser = userIds.indexOf(userId);
                            adapter.notifyItemInserted(indexOfUser);
                        }

                        pB.setVisibility(View.GONE);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                        Toast.makeText(SelectFriendActivity.this, getString(R.string.something_went_wrong, error.getMessage()), Toast.LENGTH_LONG).show();

                    }
                });


            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.d("SelectFriendActivity", "onChildChanged() called");

                String userId = snapshot.getKey();

                dbRefUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {


                        String userName = snapshot.child(NodeNames.NAME).getValue() != null ? snapshot.child(NodeNames.NAME).getValue().toString() : "";

                        if (!userId.equals(chatUserId)) {
                            SelectFriendModel friendModel = new SelectFriendModel(userId, userName, userId + ".jpg");
                            int indexOfUser = userIds.indexOf(userId);
                            selectFriendModels.set(indexOfUser,friendModel);
                            adapter.notifyItemChanged(indexOfUser);
                        }

                        pB.setVisibility(View.GONE);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SelectFriendActivity.this, getString(R.string.something_went_wrong, error.getMessage()), Toast.LENGTH_LONG).show();
                    }
                });


            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                Log.d("SelectFriendActivity", "onChildRemoved() called");

                String userId = snapshot.getKey();
                int indexOfUser = userIds.indexOf(userId);
                userIds.remove(userId);
                selectFriendModels.remove(indexOfUser);
                adapter.notifyItemRemoved(indexOfUser);

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(SelectFriendActivity.this, getString(R.string.something_went_wrong, error.getMessage()), Toast.LENGTH_LONG).show();

            }
        };

        dbRefChats.addChildEventListener(childEventListener);
    }


    public void returnSelectedFriend(String userId, String userName, String photoName)
    {
        dbRefChats.removeEventListener(childEventListener);
        Intent intent = new Intent();

        intent.putExtra(Extras.USER_KEY, userId);
        intent.putExtra(Extras.USER_NAME, userName);
        intent.putExtra(Extras.PHOTO_NAME, photoName);

        intent.putExtra(Extras.MESSAGE, selectedMessage);
        intent.putExtra(Extras.MESSAGE_ID, selectedMessageId);
        intent.putExtra(Extras.MESSAGE_TYPE, selectedMessageType);

        setResult(Activity.RESULT_OK, intent);
        finish();
    }

}
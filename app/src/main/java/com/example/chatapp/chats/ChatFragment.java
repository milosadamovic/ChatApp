package com.example.chatapp.chats;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.DataRefreshCallback;
import com.example.chatapp.common.NodeNames;
import com.example.chatapp.common.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class ChatFragment extends Fragment{

    private RecyclerView rvChatList;
    private View progressBar;
    private TextView tvEmptyChatList;
    private ChatListAdapter chatListAdapter;
    private List<ChatListModel> chatListModelList;

    private DatabaseReference drChats, drUsers;
    private FirebaseUser currentUser;

    private ChildEventListener childEventListener;
    private Query query;

    private List<String> userIds;

    SwipeCallback swipeCallback;
    ItemTouchHelper itemTouchHelper;

    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d("ChatFragment", "onCreateView() called");

        // Inflate the layout for this fragment
        View rootView  = inflater.inflate(R.layout.fragment_chat, container, false);
        rootView.setId(R.id.chat_fragment);

        return rootView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {


        Log.d("ChatFragment", "onViewCreated() called");

        super.onViewCreated(view, savedInstanceState);

        rvChatList = view.findViewById(R.id.rvChats);
        tvEmptyChatList = view.findViewById(R.id.tvEmptyChatList);

        userIds = new ArrayList<>();
        chatListModelList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(getActivity(), chatListModelList);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());

        // last message is showing in chat
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        rvChatList.setLayoutManager(linearLayoutManager);

        rvChatList.setAdapter(chatListAdapter);

        progressBar = view.findViewById(R.id.progressBar);

        drUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        drChats = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS).child(currentUser.getUid());

        query = drChats.orderByChild(NodeNames.TIME_STAMP);


        childEventListener = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    updateList(snapshot, true, snapshot.getKey());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    updateList(snapshot, false, snapshot.getKey());
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {


            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        query.addChildEventListener(childEventListener);

        //progressBar.setVisibility(View.VISIBLE);
        tvEmptyChatList.setVisibility(View.VISIBLE);

        swipeCallback = new SwipeCallback(chatListAdapter,chatListModelList);
        itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(rvChatList);

    }


    private void updateList(DataSnapshot snapshot, boolean isNew, String userId)
    {
             progressBar.setVisibility(View.GONE);
             tvEmptyChatList.setVisibility(View.GONE);
             final String  lastMessage, lastMessageTime ,unreadCount;

             if(snapshot.child(NodeNames.LAST_MESSAGE).getValue() != null)
                 lastMessage = snapshot.child(NodeNames.LAST_MESSAGE).getValue().toString();
             else lastMessage = "";

             if(snapshot.child(NodeNames.LAST_MESSAGE_TIME).getValue() != null)
                 lastMessageTime = snapshot.child(NodeNames.LAST_MESSAGE_TIME).getValue().toString();
             else lastMessageTime = "";


             unreadCount = snapshot.child(NodeNames.UNREAD_COUNT).getValue() == null ? "0" : snapshot.child(NodeNames.UNREAD_COUNT).getValue().toString();

             drUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                 @Override
                 public void onDataChange(@NonNull DataSnapshot snapshot) {

                     String fullName = snapshot.child(NodeNames.NAME).getValue() != null ? snapshot.child(NodeNames.NAME).getValue().toString() : "";

                     String photoName = (snapshot.child(NodeNames.PHOTO).getValue()) != null ? userId + ".jpg" : "";

                     ChatListModel chatListModel = new ChatListModel(userId, fullName, photoName, unreadCount, lastMessage, lastMessageTime);

                     if(isNew)
                     {
                         chatListModelList.add(chatListModel);
                         userIds.add(userId);
                     }

                     else
                     {
                        int indexOfClickedUser = userIds.indexOf(userId);
                        chatListModelList.set(indexOfClickedUser, chatListModel);
                     }

                     chatListAdapter.notifyDataSetChanged();
                 }

                 @Override
                 public void onCancelled(@NonNull DatabaseError error) {

                     Toast.makeText(getActivity(), getActivity().getString(R.string.failed_to_fetch_chat_list, error.getMessage()), Toast.LENGTH_SHORT).show();

                 }
             });

    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d("ChatFragment", "onResume() called");
        Util.cancelNotifications(getContext(), Constants.NOTIFICATION_TYPE_MESSAGEID);
    }


   @Override
    public void onStart() {
        super.onStart();
        Log.d("ChatFragment", "onStart() called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        query.removeEventListener(childEventListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("ChatFragment", "onPause() called");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("ChatFragment", "onStop() called");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("ChatFragment", "onDestroyView() called");
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d("ChatFragment", "onAttach() called");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("ChatFragment", "onDetach() called");
    }


}
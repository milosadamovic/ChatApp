package com.example.chatapp.findfriends;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.NetworkError;
import com.example.chatapp.R;
import com.example.chatapp.util.Constants;
import com.example.chatapp.util.NodeNames;
import com.example.chatapp.util.Util;
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


public class FindFriendsFragment extends Fragment {

    private  TextView tvEmptyFriendsList;
    private  View pB;
    private  RecyclerView rvFindFriends;

    private DatabaseReference dbRefUsers, dbRefFriendRequests;
    private  FindFriendsAdapter adapter;
    private  List<String> userIds;
    private  List<FindFriendsModel> findFriendsModelList;
    private  FirebaseUser currentUser;
    private ChildEventListener childEventListener, childEventListener2;

    public FindFriendsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        rvFindFriends = view.findViewById(R.id.rvFindFriends);
        pB = view.findViewById(R.id.progressBar);
        tvEmptyFriendsList = view.findViewById(R.id.tvEmptyFriendsList);
        tvEmptyFriendsList.setVisibility(View.VISIBLE);

        rvFindFriends.setLayoutManager(new LinearLayoutManager(getActivity()));

        findFriendsModelList = new ArrayList<>();
        userIds = new ArrayList<>();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        dbRefUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        dbRefFriendRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS).child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        adapter = new FindFriendsAdapter(getActivity(), findFriendsModelList);
        rvFindFriends.setAdapter(adapter);



        findFriendsModelList.clear();
        userIds.clear();


        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                updateRequestStatus(snapshot.getKey(),snapshot,0);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                updateRequestStatus(snapshot.getKey(),snapshot,1);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), R.string.exception, Toast.LENGTH_SHORT).show();
            }
        };

        childEventListener2 = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                        updateRequestStatus(snapshot.getKey(),snapshot,2);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                        updateRequestStatus(snapshot.getKey(),snapshot,2);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                         updateRequestStatus(snapshot.getKey(),snapshot,2);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                pB.setVisibility(View.GONE);
                Toast.makeText(getContext(), R.string.exception, Toast.LENGTH_SHORT).show();

            }
        };

        dbRefUsers.addChildEventListener(childEventListener);
        dbRefFriendRequests.addChildEventListener(childEventListener2);

    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {

        super.onResume();

        if(Util.connectionAvailable(requireContext()))
        {
            Util.cancelNotifications(getContext(), Constants.NOTIFICATION_TYPE_REPLYID);
        }
        else startActivity(new Intent(requireContext(), NetworkError.class));

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    public void updateRequestStatus(String userId, DataSnapshot ds, int flag)
    {


        String userName = ds.child(NodeNames.NAME).getValue() != null ? ds.child(NodeNames.NAME).getValue().toString() : "";
        String photoName = (ds.child(NodeNames.PHOTO).getValue()) != null ? userId + ".jpg" : "";

        dbRefFriendRequests.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                switch(flag)
                {
                    case 0 :
                        if(snapshot.exists())
                        {
                            String requestType = snapshot.child(NodeNames.REQUEST_TYPE).getValue().toString();
                            if (requestType.equals(Constants.REQUEST_STATUS_SENT))
                            {
                                if (!userId.equals(currentUser.getUid()))
                                {
                                    tvEmptyFriendsList.setVisibility(View.GONE);

                                    FindFriendsModel friend = new FindFriendsModel(userName, photoName, userId, true);
                                    userIds.add(userId);
                                    findFriendsModelList.add(friend);
                                    int indexOfUser = userIds.indexOf(userId);
                                    adapter.notifyItemInserted(indexOfUser);
                                }
                            }
                        }
                        else
                        {
                            if (!userId.equals(currentUser.getUid()))
                            {
                                tvEmptyFriendsList.setVisibility(View.GONE);

                                FindFriendsModel friend = new FindFriendsModel(userName, photoName, userId, false);
                                userIds.add(userId);
                                findFriendsModelList.add(friend);
                                int indexOfUser = userIds.indexOf(userId);
                                adapter.notifyItemInserted(indexOfUser);
                            }
                        } break;

                    case 1 :
                        if(snapshot.exists())
                        {
                            String requestType = snapshot.child(NodeNames.REQUEST_TYPE).getValue().toString();
                            if (requestType.equals(Constants.REQUEST_STATUS_SENT))
                            {

                                if (!userId.equals(currentUser.getUid()))
                                {
                                    int indexOfUser = userIds.indexOf(userId);
                                    adapter.notifyItemChanged(indexOfUser);
                                }
                            }
                        }
                        else
                        {
                            if (!userId.equals(currentUser.getUid()))
                            {

                                int indexOfUser = userIds.indexOf(userId);
                                adapter.notifyItemChanged(indexOfUser);
                            }
                        } break;

                    case 2 :
                        if(snapshot.exists())
                        {
                            String requestType = snapshot.child(NodeNames.REQUEST_TYPE).getValue().toString();

                            if(requestType.equals(Constants.REQUEST_STATUS_RECEIVED) && userIds.contains(userId))
                            {
                                int indexOfUser = userIds.indexOf(userId);
                                findFriendsModelList.remove(indexOfUser);
                                userIds.remove(userId);
                                if(userIds.size() > 0) tvEmptyFriendsList.setVisibility(View.GONE);
                                else tvEmptyFriendsList.setVisibility(View.VISIBLE);
                                adapter.notifyItemRemoved(indexOfUser);
                            }

                            else if(requestType.equals(Constants.REQUEST_STATUS_ACCEPTED) && userIds.contains(userId))
                            {
                                int indexOfUser = userIds.indexOf(userId);
                                findFriendsModelList.remove(indexOfUser);
                                userIds.remove(userId);
                                if(userIds.size() > 0) tvEmptyFriendsList.setVisibility(View.GONE);
                                else tvEmptyFriendsList.setVisibility(View.VISIBLE);
                                adapter.notifyItemRemoved(indexOfUser);
                            }
                        }
                        else
                        {

                            if(!userIds.contains(userId))
                            {
                                dbRefUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                        String userName = snapshot.child(NodeNames.NAME).getValue() != null ? snapshot.child(NodeNames.NAME).getValue().toString() : "";
                                        String photoName = (snapshot.child(NodeNames.PHOTO).getValue()) != null ? userId + ".jpg" : "";

                                        FindFriendsModel friend = new FindFriendsModel(userName, photoName, userId, false);
                                        tvEmptyFriendsList.setVisibility(View.GONE);
                                        userIds.add(userId);
                                        findFriendsModelList.add(friend);
                                        int indexOfUser = userIds.indexOf(userId);
                                        adapter.notifyItemInserted(indexOfUser);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                        Toast.makeText(getContext(), R.string.exception, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            else
                            {
                                int indexOfUser = userIds.indexOf(userId);
                                findFriendsModelList.get(indexOfUser).setRequestSent(false);
                                adapter.notifyItemChanged(indexOfUser);
                                tvEmptyFriendsList.setVisibility(View.GONE);
                            }
                        } break;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                pB.setVisibility(View.GONE);

            }
        });
    }

}

package com.example.chatapp.requests;

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


public class RequestsFragment extends Fragment {


    private TextView tvEmptyRequestsList;
    private View pB;
    private RecyclerView rvRequests;

    private RequestAdapter adapter;
    private FirebaseUser currentUser;
    private DatabaseReference dbRefFriendRequests,dbRefUsers;
    private  List<String> userIds;
    private List<RequestModel> requestModelList;
    private ChildEventListener childEventListener;

    public RequestsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d("RequestFragment", "onCreateView() called");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_requests, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvRequests = view.findViewById(R.id.rvRequests);
        tvEmptyRequestsList = view.findViewById(R.id.tvEmptyRequestsList);
        pB = view.findViewById(R.id.progressBar);

        rvRequests.setLayoutManager( new LinearLayoutManager(getActivity()));

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        requestModelList = new ArrayList<>();
        userIds = new ArrayList<>();
        dbRefUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        dbRefFriendRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());

        adapter = new RequestAdapter(getActivity(), requestModelList);
        rvRequests.setAdapter(adapter);


        tvEmptyRequestsList.setVisibility(View.VISIBLE);

        requestModelList.clear();
        userIds.clear();

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.d("RequestFragment", "onChildAdded() called");
                updateRequests(snapshot.getKey());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                Log.d("RequestFragment", "onChildChanged() called");
                updateRequests(snapshot.getKey());
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                Log.d("RequestFragment", "onChildRemoved() called");
               updateRequests(snapshot.getKey());
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(getActivity(), getActivity().getString(R.string.failed_to_fetch_friend_requests, error.getMessage()),Toast.LENGTH_SHORT).show();
                pB.setVisibility(View.GONE);
            }
        };

        dbRefFriendRequests.addChildEventListener(childEventListener);


    }

    @Override
    public void onStart() {
        Log.d("RequestFragment", "onStart() called");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d("RequestFragment", "onResume() called");
        Util.cancelNotifications(getContext(), Constants.NOTIFICATION_TYPE_REQUESTID);
        Util.cancelNotifications(getContext(), Constants.NOTIFICATION_TYPE_REPLYID);
        super.onResume();
    }


    @Override
    public void onPause() {
        Log.d("RequestFragment", "onPause() called");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d("RequestFragment", "onStop() called");
        super.onStop();
    }


    public  void updateRequests(String userId) {

        pB.setVisibility(View.GONE);

        /**U SLUCAJU REQUEST_STATUS_SENT NISTA SE NE PRIKAZUJE
         // U SLUCAJU REQUEST_STATUS_RECEIVED PRIKAZUJEMO KORISNIKA
         //U SLUCAJU REQUEST_STATUS_ACCEPTER IZBACUJEMO KORISNIKA */

        dbRefFriendRequests.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists())
                {
                    String requestType = snapshot.child(NodeNames.REQUEST_TYPE).getValue().toString();
                    if (requestType.equals(Constants.REQUEST_STATUS_RECEIVED))
                    {

                        dbRefUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                String userName = snapshot.child(NodeNames.NAME).getValue() != null ? snapshot.child(NodeNames.NAME).getValue().toString() : "";
                                String photoName = (snapshot.child(NodeNames.PHOTO).getValue()) != null ? userId + ".jpg" : "";

                                RequestModel requestModel = new RequestModel(userId, userName, photoName);
                                userIds.add(userId);
                                requestModelList.add(requestModel);
                                int indexOfUser = userIds.indexOf(userId);
                                adapter.notifyItemInserted(indexOfUser);
                                tvEmptyRequestsList.setVisibility(View.GONE);

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                                Toast.makeText(getContext(), getString(R.string.failed_to_fetch_friend_requests, error.getMessage()), Toast.LENGTH_SHORT).show();
                                pB.setVisibility(View.GONE);
                            }
                        });
                    }
                    else if (requestType.equals(Constants.REQUEST_STATUS_ACCEPTED) && userIds.contains(userId))
                    {
                        int indexOfUser = userIds.indexOf(userId);
                        userIds.remove(userId);
                        requestModelList.remove(indexOfUser);
                        adapter.notifyItemRemoved(indexOfUser);
                    }
                }
                else
                {
                    /***KANCELOVAN ZAHTEV
                     // ODBIJEN ZAHTEV AKO JE TIPA REQUEST_STATUS_RECEIVED */
                    if(userIds.contains(userId))
                    {
                        int indexOfUser = userIds.indexOf(userId);
                        userIds.remove(userId);
                        requestModelList.remove(indexOfUser);
                        adapter.notifyItemRemoved(indexOfUser);
                    }

                    /**KANCELOVAN ZAHTEV
                     //ODBIJEN ZAHTEV AKO JE TIPA REQUEST_STATUS_SENT
                     // DO NOTHING */
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(getContext(), getString(R.string.failed_to_fetch_friend_requests, error.getMessage()), Toast.LENGTH_SHORT).show();
                pB.setVisibility(View.GONE);

            }
        });

    }


}
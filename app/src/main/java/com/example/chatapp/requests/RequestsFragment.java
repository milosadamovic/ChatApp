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
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.NodeNames;
import com.example.chatapp.common.Util;
import com.example.chatapp.findfriends.FindFriendsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;


public class RequestsFragment extends Fragment {


    private RecyclerView rvRequests;
    private RequestAdapter adapter;
    private List<RequestModel> requestModelList;
    private TextView tvEmptyRequestsList;

    public DatabaseReference drRequests, drUsers;
    private FirebaseUser currentUser;
    private View progressBar;

    private ValueEventListener valueEventListener;


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
        progressBar = view.findViewById(R.id.progressBar);

        rvRequests.setLayoutManager( new LinearLayoutManager(getActivity()));
        requestModelList = new ArrayList<>();
        adapter = new RequestAdapter(getActivity(), requestModelList);
        rvRequests.setAdapter(adapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        drUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        drRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());

        progressBar.setVisibility(View.VISIBLE);
        tvEmptyRequestsList.setVisibility(View.VISIBLE);

        loadData();

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

    public void loadData()
    {
        valueEventListener = new ValueEventListener() {

            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                requestModelList.clear();

                for(DataSnapshot ds : snapshot.getChildren())
                {
                    if(ds.exists())
                    {
                        String requestType = ds.child(NodeNames.REQUEST_TYPE).getValue().toString();
                        if(requestType.equals(Constants.REQUEST_STATUS_RECEIVED))
                        {
                            final String userId = ds.getKey();
                            drUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                    final String userName = snapshot.child(NodeNames.NAME).getValue().toString();
                                    String photoName = "";

                                    if(snapshot.child(NodeNames.PHOTO).getValue() != null)
                                    {
                                        // photoName = snapshot.child(NodeNames.PHOTO).getValue().toString();
                                        photoName = userId + ".jpg";
                                    }

                                    RequestModel requestModel = new RequestModel(userId, userName, photoName);
                                    requestModelList.add(requestModel);
                                    adapter.notifyDataSetChanged();
                                    tvEmptyRequestsList.setVisibility(View.GONE);

                                    /**TEST
                                     * FindFriendsFragment.refresh(getContext())*/

                                    Log.d("RequestFragment", "Hello from RequestFragment");
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                    Toast.makeText(getActivity(), getActivity().getString(R.string.failed_to_fetch_friend_requests, error.getMessage()),Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                        }
                        Log.d("RequestFragment", "request type: " + requestType);
                    }
                   // Log.d("RequestFragment", "Hello from RequestFragment");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), getActivity().getString(R.string.failed_to_fetch_friend_requests, error.getMessage()),Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }

        };
        drRequests.addValueEventListener(valueEventListener);
    }
}
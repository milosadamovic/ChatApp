package com.example.chatapp.findfriends;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;


public class FindFriendsFragment extends Fragment {


    private RecyclerView rvFindFriends;
    private FindFriendsAdapter findFriendsAdapter;
    private List<FindFriendsModel> findFriendsModelList;
    private TextView tvEmptyFriendsList;

    private DatabaseReference databaseReference, databaseReferenceFriendsRequests;
    private FirebaseUser currentUser;
    private View progressBar;
    private static int br = 0;

    public static ValueEventListener valueEventListener;


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
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyFriendsList = view.findViewById(R.id.tvEmptyFriendsList);

        rvFindFriends.setLayoutManager(new LinearLayoutManager(getActivity()));

        findFriendsModelList = new ArrayList<>();
        findFriendsAdapter = new FindFriendsAdapter(getActivity(), findFriendsModelList);
        rvFindFriends.setAdapter(findFriendsAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceFriendsRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());

        //progressBar.setVisibility(View.VISIBLE);
        tvEmptyFriendsList.setVisibility(View.VISIBLE);


        Query query = databaseReference.orderByChild(NodeNames.NAME);
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                findFriendsModelList.clear();

                for(DataSnapshot s : snapshot.getChildren())
                {
                    Log.d("SNAPBROJAC", "BR : " + ++br);
                    final String userId = s.getKey();

                   /* if(userId.equals(currentUser.getUid())) {

                        //TEST
                        return;
                    }*/

                    if(s.child(NodeNames.NAME).getValue()!=null)
                    {
                        final String fullName = s.child(NodeNames.NAME).getValue().toString();
                        //final String photoName = s.child(NodeNames.PHOTO).getValue().toString();
                        final String photoName = userId + ".jpg";

                        databaseReferenceFriendsRequests.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                if(snapshot.exists())
                                {
                                    Log.d("SNAPSHOT", "SNAP : " + snapshot);
                                    String requestType = snapshot.child(NodeNames.REQUEST_TYPE).getValue().toString();
                                    Log.d("SNAPTRUE", fullName + " : " + requestType);
                                    if(requestType.equals(Constants.REQUEST_STATUS_SENT))
                                    {
                                        if(!userId.equals(currentUser.getUid()))
                                        {
                                            findFriendsModelList.add(new FindFriendsModel(fullName, photoName, userId, true));
                                            findFriendsAdapter.notifyDataSetChanged();
                                        }
                                    }
                                }
                                else
                                {
                                    Log.d("SNAPFALSE", fullName + " : " + "false");
                                    if(!userId.equals(currentUser.getUid()))
                                    {
                                        findFriendsModelList.add(new FindFriendsModel(fullName, photoName, userId, false));
                                        findFriendsAdapter.notifyDataSetChanged();
                                    }
                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                                progressBar.setVisibility(View.GONE);

                            }
                        });


                        tvEmptyFriendsList.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), getContext().getString(R.string.failed_to_fetch_friends, error.getMessage()),Toast.LENGTH_SHORT).show();
            }
        };
        query.addValueEventListener(valueEventListener);


    }
}
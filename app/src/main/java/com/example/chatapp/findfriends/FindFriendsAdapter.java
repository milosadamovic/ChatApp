package com.example.chatapp.findfriends;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.util.Constants;
import com.example.chatapp.util.NodeNames;
import com.example.chatapp.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class FindFriendsAdapter extends RecyclerView.Adapter<FindFriendsAdapter.FindFriendsViewHolder> {

    private Context context;
    private List<FindFriendsModel> findFriendsModelList;
    private String userId;

    private DatabaseReference dbRefFriendRequests, dbRefUsers;
    private FirebaseUser currentUser;

    public FindFriendsAdapter(Context context, List<FindFriendsModel> findFriendsModelList) {
        this.context = context;
        this.findFriendsModelList = findFriendsModelList;
    }

    @NonNull
    @Override
    public FindFriendsAdapter.FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.find_friends_layout, parent,false);
        return new FindFriendsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FindFriendsAdapter.FindFriendsViewHolder holder, int position) {


           FindFriendsModel friendsModel = findFriendsModelList.get(position);

           StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://chatapp-ca8cb.appspot.com");
           StorageReference mountRef = storageRef.child(Constants.IMAGES_FOLDER + "/" + friendsModel.getPhotoName());


           dbRefUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
           String userID = friendsModel.getUserId();

           dbRefUsers.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
               @Override
               public void onDataChange(@NonNull DataSnapshot snapshot) {

                   String fullName = snapshot.child(NodeNames.NAME).getValue() != null ? snapshot.child(NodeNames.NAME).getValue().toString() : "";
                   holder.tvFullName.setText(fullName);
               }

               @Override
               public void onCancelled(@NonNull DatabaseError error) {
                   Toast.makeText(context, R.string.exception, Toast.LENGTH_SHORT).show();
               }
           });

            mountRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {

                    if(task.isSuccessful())
                    {
                        Uri uri = task.getResult();
                        Glide.with(context)
                                .load(uri)
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(holder.ivProfile);
                    }
                    else
                    {
                        Glide.with(context)
                                .load(R.drawable.default_profile)
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(holder.ivProfile);
                    }

                }
            });

           currentUser = FirebaseAuth.getInstance().getCurrentUser();
           dbRefFriendRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS);


           if(friendsModel.isRequestSent())
           {
               holder.btnSendRequest.setVisibility(View.GONE);
               holder.btnCancelRequest.setVisibility(View.VISIBLE);
           }
           else
           {
               holder.btnSendRequest.setVisibility(View.VISIBLE);
               holder.btnCancelRequest.setVisibility(View.GONE);
           }

           holder.btnSendRequest.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {

                   if(Util.connectionAvailable(context))
                   {
                       holder.btnSendRequest.setVisibility(View.GONE);
                       holder.pbRequest.setVisibility(View.VISIBLE);

                       userId = friendsModel.getUserId();

                       dbRefFriendRequests.child(currentUser.getUid()).child(userId).child(NodeNames.REQUEST_TYPE)
                               .setValue(Constants.REQUEST_STATUS_SENT).addOnCompleteListener(new OnCompleteListener<Void>() {
                           @Override
                           public void onComplete(@NonNull Task<Void> task) {

                               if(task.isSuccessful())
                               {
                                   dbRefFriendRequests.child(userId).child(currentUser.getUid()).child(NodeNames.REQUEST_TYPE)
                                           .setValue(Constants.REQUEST_STATUS_RECEIVED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                       @Override
                                       public void onComplete(@NonNull Task<Void> task) {

                                           if(task.isSuccessful())
                                           {
                                              // Toast.makeText(context, R.string.exception,Toast.LENGTH_SHORT).show();

                                               String title = "New Friend Request";
                                               String message = "Friend request from " + currentUser.getDisplayName();

                                              // Util.sendNotification(context, title, message, userId, currentUser.getUid(),Constants.NOTIFICATION_TYPE_REQUEST);

                                               holder.btnSendRequest.setVisibility(View.GONE);
                                               holder.pbRequest.setVisibility(View.GONE);
                                               holder.btnCancelRequest.setVisibility(View.VISIBLE);
                                           }

                                           else
                                           {
                                               Toast.makeText(context, R.string.exception,Toast.LENGTH_SHORT).show();
                                               holder.btnSendRequest.setVisibility(View.VISIBLE);
                                               holder.pbRequest.setVisibility(View.GONE);
                                               holder.btnCancelRequest.setVisibility(View.GONE);
                                           }

                                       }
                                   });
                               }
                               else
                               {
                                   Toast.makeText(context, R.string.exception,Toast.LENGTH_SHORT).show();
                                   holder.btnSendRequest.setVisibility(View.VISIBLE);
                                   holder.pbRequest.setVisibility(View.GONE);
                                   holder.btnCancelRequest.setVisibility(View.GONE);
                               }


                           }
                       });
                   }
                   else Toast.makeText(context, R.string.no_internet ,Toast.LENGTH_LONG).show();


               }
           });

           holder.btnCancelRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(Util.connectionAvailable(context))
                {
                    holder.btnCancelRequest.setVisibility(View.GONE);
                    holder.pbRequest.setVisibility(View.VISIBLE);

                    userId = friendsModel.getUserId();

                    dbRefFriendRequests.child(currentUser.getUid()).child(userId).child(NodeNames.REQUEST_TYPE)
                            .setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful())
                            {
                                dbRefFriendRequests.child(userId).child(currentUser.getUid()).child(NodeNames.REQUEST_TYPE)
                                        .setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if(task.isSuccessful())
                                        {
                                            //Toast.makeText(context, R.string.request_cancelled_successfully,Toast.LENGTH_SHORT).show();
                                            holder.btnSendRequest.setVisibility(View.VISIBLE);
                                            holder.pbRequest.setVisibility(View.GONE);
                                            holder.btnCancelRequest.setVisibility(View.GONE);
                                        }
                                        else
                                        {
                                            Toast.makeText(context, R.string.exception,Toast.LENGTH_SHORT).show();
                                            holder.btnSendRequest.setVisibility(View.GONE);
                                            holder.pbRequest.setVisibility(View.GONE);
                                            holder.btnCancelRequest.setVisibility(View.VISIBLE);
                                        }

                                    }
                                });
                            }
                            else
                            {
                                Toast.makeText(context, R.string.exception,Toast.LENGTH_SHORT).show();
                                holder.btnSendRequest.setVisibility(View.GONE);
                                holder.pbRequest.setVisibility(View.GONE);
                                holder.btnCancelRequest.setVisibility(View.VISIBLE);
                            }


                        }
                    });
                }
                else Toast.makeText(context, R.string.no_internet ,Toast.LENGTH_LONG).show();





            }
        });
    }


    @Override
    public int getItemCount() {
        return findFriendsModelList.size();
    }

    public class FindFriendsViewHolder extends RecyclerView.ViewHolder{

        private ImageView ivProfile;
        private TextView tvFullName;
        private Button btnSendRequest, btnCancelRequest;
        private ProgressBar pbRequest;


        public FindFriendsViewHolder(View itemView)
        {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            btnSendRequest = itemView.findViewById(R.id.btnSendRequest);
            btnCancelRequest = itemView.findViewById(R.id.btnCancelRequest);
            pbRequest = itemView.findViewById(R.id.pbRequest);
        }
    }
}

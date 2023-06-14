package com.example.chatapp.requests;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private Context context;
    private List<RequestModel> requestModelList;
    private DatabaseReference dbRefFriendRequests, dbRefChats;
    private FirebaseUser currentUser;


    public RequestAdapter(Context context, List<RequestModel> requestModelList) {
        this.context = context;
        this.requestModelList = requestModelList;
    }

    @NonNull
    @Override
    public RequestAdapter.RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.friend_request_layout, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestAdapter.RequestViewHolder holder, int position) {



        RequestModel requestModel = requestModelList.get(position);

        holder.tvFullName.setText(requestModel.getUserName());

        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://chatapp-ca8cb.appspot.com");
        StorageReference mountRef = storageRef.child(Constants.IMAGES_FOLDER + "/" + requestModel.getPhotoName());

        mountRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {

                Glide.with(context)
                        .load(uri)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(holder.ivProfile);
            }
        });


        dbRefFriendRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS);
        dbRefChats = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();


        holder.btnAcceptRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if(Util.connectionAvailable(context))
                {
                    holder.pbDecision.setVisibility(View.VISIBLE);
                    holder.btnDenyRequest.setVisibility(View.GONE);
                    holder.btnAcceptRequest.setVisibility(View.GONE);

                    final String userId = requestModel.getUserId();

                    dbRefChats.child(currentUser.getUid()).child(userId).child(NodeNames.TIME_STAMP).setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful())
                            {
                                dbRefChats.child(userId).child(currentUser.getUid()).child(NodeNames.TIME_STAMP).setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if(task.isSuccessful())
                                        {
                                            dbRefFriendRequests.child(currentUser.getUid()).child(userId).child(NodeNames.REQUEST_TYPE).setValue(Constants.REQUEST_STATUS_ACCEPTED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                    if(task.isSuccessful())
                                                    {
                                                        dbRefFriendRequests.child(userId).child(currentUser.getUid()).child(NodeNames.REQUEST_TYPE).setValue(Constants.REQUEST_STATUS_ACCEPTED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                if(task.isSuccessful())
                                                                {
                                                                    String title = "Friend Request Accepted";
                                                                    String message = "Friend request accepted by " + currentUser.getDisplayName();

                                                                    Util.cancelNotifications(context, Constants.NOTIFICATION_TYPE_REQUESTID);
                                                                    Util.sendNotification(context, title, message, userId,currentUser.getUid(),Constants.NOTIFICATION_TYPE_REPLY);

                                                                    holder.pbDecision.setVisibility(View.GONE);
                                                                    holder.btnDenyRequest.setVisibility(View.VISIBLE);
                                                                    holder.btnAcceptRequest.setVisibility(View.VISIBLE);
                                                                }
                                                                else handleException(holder, task.getException());

                                                            }
                                                        });
                                                    }
                                                    else handleException(holder, task.getException());

                                                }
                                            });
                                        }
                                        else handleException(holder, task.getException());
                                    }
                                });
                            }
                            else handleException(holder, task.getException());
                        }
                    });
                }
                else Toast.makeText(context, R.string.no_internet ,Toast.LENGTH_LONG).show();





            }
        });

        holder.btnDenyRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(Util.connectionAvailable(context))
                {
                    holder.pbDecision.setVisibility(View.VISIBLE);
                    holder.btnDenyRequest.setVisibility(View.GONE);
                    holder.btnAcceptRequest.setVisibility(View.GONE);

                    final String userId = requestModel.getUserId();


                    dbRefFriendRequests.child(currentUser.getUid()).child(userId).child(NodeNames.REQUEST_TYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful())
                            {
                                dbRefFriendRequests.child(userId).child(currentUser.getUid()).child(NodeNames.REQUEST_TYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if(task.isSuccessful())
                                        {
                                            holder.pbDecision.setVisibility(View.GONE);
                                            holder.btnDenyRequest.setVisibility(View.VISIBLE);
                                            holder.btnAcceptRequest.setVisibility(View.VISIBLE);
                                            Util.cancelNotifications(context, Constants.NOTIFICATION_TYPE_REQUESTID);
                                            //Toast.makeText(context,R.string.request_denied_successfully,Toast.LENGTH_SHORT).show();
                                        }
                                        else
                                        {
                                            Toast.makeText(context, R.string.exception, Toast.LENGTH_SHORT).show();
                                            holder.pbDecision.setVisibility(View.GONE);
                                            holder.btnDenyRequest.setVisibility(View.VISIBLE);
                                            holder.btnAcceptRequest.setVisibility(View.VISIBLE);
                                        }
                                    }
                                });
                            }
                            else
                            {
                                Toast.makeText(context, R.string.exception, Toast.LENGTH_SHORT).show();
                                holder.pbDecision.setVisibility(View.GONE);
                                holder.btnDenyRequest.setVisibility(View.VISIBLE);
                                holder.btnAcceptRequest.setVisibility(View.VISIBLE);
                            }

                        }
                    });
                }
                else Toast.makeText(context, R.string.no_internet ,Toast.LENGTH_LONG).show();

            }
        });



    }

    private void handleException(RequestViewHolder holder, Exception exception) {

        Toast.makeText(context, R.string.exception, Toast.LENGTH_SHORT).show();
        holder.pbDecision.setVisibility(View.GONE);
        holder.btnDenyRequest.setVisibility(View.VISIBLE);
        holder.btnAcceptRequest.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return requestModelList.size();
    }

    public class RequestViewHolder extends RecyclerView.ViewHolder{

        private TextView tvFullName;
        private ImageView ivProfile;
        private Button btnAcceptRequest, btnDenyRequest;
        private ProgressBar pbDecision;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            btnAcceptRequest = itemView.findViewById(R.id.btnAcceptRequest);
            btnDenyRequest = itemView.findViewById(R.id.btnDenyRequest);
            pbDecision = itemView.findViewById(R.id.pbDecision);
        }
    }
}

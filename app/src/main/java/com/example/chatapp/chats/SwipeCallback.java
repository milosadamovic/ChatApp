package com.example.chatapp.chats;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.NodeNames;
import com.example.chatapp.common.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;


public class SwipeCallback extends ItemTouchHelper.Callback {

    private  ChatListAdapter mAdapter;
    private  List<ChatListModel> chatList;



    public SwipeCallback(ChatListAdapter adapter, List<ChatListModel> dataList) {
        mAdapter = adapter;
        chatList = dataList;
    }


    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int swipeFlags =  ItemTouchHelper.END;
        return makeMovementFlags(0, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        int position = viewHolder.getAdapterPosition();

       FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
       String chatUserId = chatList.get(position).getUserId();
       DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();


        AlertDialog.Builder builder = new AlertDialog.Builder(viewHolder.itemView.getContext());
        builder.setMessage("Do you really want to delete this friend ?");
        builder.setTitle("Delete a friend");
        builder.setCancelable(false);

        builder.setPositiveButton("Yes", (DialogInterface.OnClickListener) (dialog, which) -> {


            rootRef.child(NodeNames.CHATS).child(currentUser.getUid()).child(chatUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if(task.isSuccessful())
                    {
                        rootRef.child(NodeNames.CHATS).child(chatUserId).child(currentUser.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                if(task.isSuccessful())
                                {
                                    rootRef.child(NodeNames.FRIEND_REQUESTS).child(chatUserId).child(currentUser.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {


                                            if(task.isSuccessful())
                                            {
                                                rootRef.child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid()).child(chatUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {


                                                            if(task.isSuccessful())
                                                            {
                                                                String title = currentUser.getDisplayName() + " deleted you from his friends";
                                                                String message = "You are no longer friends with " + currentUser.getDisplayName();

                                                                Util.sendNotification(viewHolder.itemView.getContext(),title, message, chatUserId,  currentUser.getUid(), Constants.NOTIFICATION_TYPE_DELETED);
                                                                chatList.remove(position);
                                                                mAdapter.notifyItemRemoved(position);

                                                            }
                                                            else handleException(viewHolder,task.getException());

                                                    }
                                                });
                                            }
                                            else handleException(viewHolder, task.getException());

                                        }
                                    });
                                }
                                else handleException(viewHolder, task.getException());
                            }
                        });
                    }
                    else handleException(viewHolder, task.getException());
                }
            });

        });

        builder.setNegativeButton("No", (DialogInterface.OnClickListener) (dialog, which) -> {

            mAdapter.notifyDataSetChanged();
            dialog.cancel();
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();


       // mAdapter.onItemSwiped(viewHolder.getAdapterPosition());


    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {


        float threshold = viewHolder.itemView.getWidth() * 0.7f;
        float swipeFraction = Math.abs(dX) / viewHolder.itemView.getWidth();

        View newView = LayoutInflater.from(viewHolder.itemView.getContext()).inflate(R.layout.delete_friend_layout, null);

        View newLayout = newView.findViewById(R.id.llDeleteFriend);
        View oldLayout = viewHolder.itemView.findViewById(R.id.llChatList);

        FrameLayout container = viewHolder.itemView.findViewById(R.id.flSwipeAction);
        if (container.getChildCount() == 1) {
            container.addView(newView);
            Log.d("SwipeCallback", "VIEW ADDED");
        }


        oldLayout.setAlpha(1-swipeFraction);
        newLayout.setAlpha(swipeFraction);
        newLayout.setTranslationX(dX - threshold);
        oldLayout.setTranslationX(-dX);

        Log.d("SwipeCallback", "onChildDraw() called, swipeFraction = " + swipeFraction);
        Log.d("SwipeCallback", "onChildDraw() called, threshold = " + threshold);
        Log.d("SwipeCallback", "onChildDraw() called, dX = " + dX);




        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
   public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {

        // get the swipe fraction
        float swipeFraction = Math.min(Math.abs(dX) / viewHolder.itemView.getWidth(), 1f);


        // get references to the old and new layouts
        View newLayout = viewHolder.itemView.findViewById(R.id.llDeleteFriend);
        View oldLayout = viewHolder.itemView.findViewById(R.id.llChatList);


        oldLayout.setAlpha(1-swipeFraction);
        newLayout.setAlpha(swipeFraction);


        //Log.d("SwipeCallback", "onChildDrawOver() called");
        super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {

        View newLayout = viewHolder.itemView.findViewById(R.id.llDeleteFriend);
        View oldLayout = viewHolder.itemView.findViewById(R.id.llChatList);


        oldLayout.setAlpha(1);
        newLayout.setAlpha(0);
        oldLayout.setTranslationX(0);

      // Log.d("SwipeCallback", "clearView() called");
        super.clearView(recyclerView, viewHolder);

    }

    private void handleException(RecyclerView.ViewHolder viewHolder, Exception exception) {

        Toast.makeText(viewHolder.itemView.getContext(), viewHolder.itemView.getContext().getString(R.string.failed_to_delete_friend, exception), Toast.LENGTH_SHORT).show();

    }

}

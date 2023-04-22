package com.example.chatapp.chats;

import android.graphics.Canvas;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;


public class SwipeCallback extends ItemTouchHelper.Callback {

    private final ChatListAdapter mAdapter;

    public SwipeCallback(ChatListAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(0, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();

        // Set the swipe position to show the second layout
       /* mSwipePosition = position;
        mAdapter.onItemSwiped(viewHolder.getAdapterPosition());*/
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

            View itemView = viewHolder.itemView;
            int itemWidth = itemView.getWidth();

            float fraction = Math.min(1f, Math.abs(dX) / itemView.getWidth());
            float xPos = itemWidth * (1 - fraction);
            //float alpha = (float) Math.pow(fraction, 2);
            float alpha = fraction;

            LinearLayout newLayout = viewHolder.itemView.findViewById(R.id.llDeleteFriend);
            if (dX > 0) {
                //newLayout.setVisibility(View.VISIBLE);
               // newLayout.setAlpha(alpha);
                newLayout.setTranslationX(dX);
            } else {
               // alpha = 0;
               // newLayout.setAlpha(alpha);
                newLayout.setTranslationX(-dX);
                //newLayout.setVisibility(View.INVISIBLE);
            }


            newLayout.setVisibility(View.VISIBLE);
            newLayout.setAlpha(alpha);
            // Set the new layout view's translationX value based on the current swipe position
           // newLayout.setTranslationX(dX);
           // getDefaultUIUtil().onDraw(c, recyclerView, viewHolder.itemView, dX, dY, actionState, isCurrentlyActive);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {

        super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

    }
}

package com.example.chatapp.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.chatapp.chats.ChatFragment;
import com.example.chatapp.findfriends.FindFriendsFragment;
import com.example.chatapp.requests.RequestsFragment;

class Adapter extends FragmentStateAdapter {

    final int tabCount = 3;

    public Adapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {

        switch (position)
        {
            case 0:
                  return new ChatFragment();
            case 1:
                  return new RequestsFragment();
            case 2:
                  return new FindFriendsFragment();
        }

        return null;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
}
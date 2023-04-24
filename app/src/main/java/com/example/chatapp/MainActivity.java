package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.chatapp.chats.ChatFragment;
import com.example.chatapp.chats.SwipeCallback;
import com.example.chatapp.common.NodeNames;
import com.example.chatapp.findfriends.FindFriendsFragment;
import com.example.chatapp.profile.ProfileActivity;
import com.example.chatapp.requests.RequestsFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {


    public static TabLayout tabLayout;
    private ViewPager2 viewPager;
    private int tabPosition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("MainActivity", "onCreate() called");

        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tabMain);
        viewPager = findViewById(R.id.vpMain);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        DatabaseReference databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS).child(firebaseAuth.getCurrentUser().getUid());
        databaseReferenceUsers.child(NodeNames.ONLINE).setValue(true);
        databaseReferenceUsers.child(NodeNames.ONLINE).onDisconnect().setValue(false);

        setViewPager();

    }


    class Adapter extends FragmentStateAdapter {

        public Adapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {

            switch (position)
            {
                case 0:
                    ChatFragment chatFragment = new ChatFragment();
                    return chatFragment;
                case 1:
                    RequestsFragment requestsFragment = new RequestsFragment();
                    return requestsFragment;
                case 2:
                    FindFriendsFragment findFriendsFragment = new FindFriendsFragment();
                    return findFriendsFragment;
            }

            return null;
        }

        @Override
        public int getItemCount() {
            return tabLayout.getTabCount();
        }
    }



    private void setViewPager()
    {
        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_chat));
        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_requests));
        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_findfriends));

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        Adapter adapter = new Adapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                tabPosition = tab.getPosition();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

         viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.mnuProfile)
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));

        return super.onOptionsItemSelected(item);
    }


    public void onStart() {
        Log.d("MainActivity", "onStart() called");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d("MainActivity", "onResume() called");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d("MainActivity", "onPause() called");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d("MainActivity", "onStop() called");
        super.onStop();
    }

}
package com.example.chatapp.main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.chatapp.NetworkError;
import com.example.chatapp.R;
import com.example.chatapp.login.LoginActivity;
import com.example.chatapp.profile.ProfileActivity;
import com.example.chatapp.util.Extras;
import com.example.chatapp.util.NodeNames;
import com.example.chatapp.util.Util;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {


    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    DatabaseReference dbRefTokens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tabMain);
        viewPager = findViewById(R.id.vpMain);


        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        dbRefTokens = FirebaseDatabase.getInstance().getReference().child(NodeNames.TOKEN).child(firebaseAuth.getCurrentUser().getUid());
        dbRefTokens.child(NodeNames.ONLINE).setValue(true);

        dbRefTokens.child(NodeNames.ONLINE).onDisconnect().setValue(false);

        setViewPager();

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
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
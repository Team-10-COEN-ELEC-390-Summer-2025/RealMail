package com.example.realmail;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;

public class HomeActivity extends AppCompatActivity {

   FrameLayout frameLayout1;
   TabLayout tablayout1;


    protected Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        frameLayout1 = (FrameLayout) findViewById(R.id.framelayout); //id
        tablayout1 = (TabLayout)  findViewById(R.id.tablayout);

// default fragment to the home
        getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new HomeFragment()).addToBackStack(null)
                .commit();

        tablayout1.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Fragment fragment = null;
                switch (tab.getPosition()){
                    case 0:
                    fragment = new HomeFragment();
                    break;
                    case 1:
                        fragment = new HistoryFragment();
                        break;
                }
                //  change the framgment what ever we change
               getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                       .commit();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        toolbar = findViewById(R.id.hometoolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.cam_on);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.camera){

        }

        if(item.getItemId() == R.id.settings){
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
        }



        return super.onOptionsItemSelected(item);
    }
}
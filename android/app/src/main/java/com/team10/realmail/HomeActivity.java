package com.team10.realmail;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {


    FrameLayout frameLayout1; //whatever contents  contain in each tab
    TabLayout tablayout1; // like different tabs ,camera ,histor and summary


    protected Toolbar toolbar; // toolbar
  
   


    // Session management constants (same as MainActivity)
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String SESSION_EXPIRY = "session_expiry";

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
        //initating
        toolbar = findViewById(R.id.hometoolbar);
        setSupportActionBar(toolbar);
        frameLayout1 = (FrameLayout) findViewById(R.id.framelayout); //id
        tablayout1 = (TabLayout) findViewById(R.id.tablayout);

// default fragment to the home
        getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new SummaryFragment()).addToBackStack(null)
                .commit();
        getSupportActionBar().setTitle("Summary");

        tablayout1.getTabAt(1).select(); // selects the second tab Summary

        tablayout1.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override

            //everytime click on the tab it changes the content accordlingly
            public void onTabSelected(TabLayout.Tab tab) {
                Fragment fragment = null;
                switch (tab.getPosition()) {
                    case 0:
                        fragment = new HomeFragment();
                        getSupportActionBar().setTitle("Camera");
                        break;
                    case 1:
                        fragment = new SummaryFragment();
                        getSupportActionBar().setTitle("Summary");
                        break;
                    case 2:
                        fragment = new HistoryFragment();
                        getSupportActionBar().setTitle("History");
                        break;
                }
                //  change the framgment what ever we change
                getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
            }

            @Override
            //auto genratred
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        // initating the toolbar
        toolbar = findViewById(R.id.hometoolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.cam_on);

    }

    @Override
    //oncreat is inflate the menu of toolbar
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    //whenever click on the item toolbar it does something
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();


        if (item.getItemId() == R.id.camera) {

        }

        if (item.getItemId() == R.id.settings)
        {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);//click on setting incon
            // so it goes back to home
            startActivity(intent); // start setting activity
        }

       

        if (id == R.id.action_logout) {
            showLogoutConfirmation();
            return true;
        } else if (id == R.id.action_settings) {
            // Handle settings if you have it
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Show logout confirmation dialog
     */
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Perform logout by clearing session and Firebase auth
     */
    private void performLogout() {
        // Clear session data
        SharedPreferences loginPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = loginPrefs.edit();
        editor.remove(SESSION_EXPIRY);
        editor.apply();

        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // Show message and redirect to login
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

    }
}
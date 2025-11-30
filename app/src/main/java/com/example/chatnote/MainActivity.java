package com.example.chatnote;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;

import com.example.chatnote.fragment.ChatsFragment;
import com.example.chatnote.fragment.NoteFragment;
import com.example.chatnote.fragment.PostsFragment;
import com.example.chatnote.fragment.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Chưa đăng nhập, chuyển về LoginActivity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        bottomNav = findViewById(R.id.bottomNav);

        // Load default fragment là ChatsFragment
        loadFragment(new ChatsFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;

            int itemId = item.getItemId();

            Bundle bundle = new Bundle();
            bundle.putString("userId", currentUser.getUid()); // gửi userId cho tất cả fragment

            if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (itemId == R.id.nav_posts) {
                selectedFragment = new PostsFragment();
            } else if (itemId == R.id.nav_note) {
                selectedFragment = new NoteFragment();
            } else {
                selectedFragment = new ChatsFragment();
            }

            selectedFragment.setArguments(bundle);
            loadFragment(selectedFragment);
            return true;
        });

    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}

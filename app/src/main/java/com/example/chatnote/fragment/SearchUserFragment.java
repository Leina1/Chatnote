package com.example.chatnote.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatnote.Adapter.UserAdapter;
import com.example.chatnote.Model.User;
import com.example.chatnote.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchUserFragment extends Fragment {

    private EditText searchInput;
    private ImageButton imgBack;
    private RecyclerView recyclerView;
    private TextView tvNoResults;
    private UserAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private List<User> allUsers = new ArrayList<>(); // Lưu tất cả users
    private String currentUserId;
    private FirebaseFirestore db;

    public SearchUserFragment() {
        // Constructor mặc định
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lấy current user ID
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_searchuser, container, false);

        // Initialize views
        searchInput = view.findViewById(R.id.search_username_input);
        imgBack = view.findViewById(R.id.imgBack);
        recyclerView = view.findViewById(R.id.vew_chat);
        tvNoResults = view.findViewById(R.id.tvNoResults); // Thêm TextView cho "Không tìm thấy"

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserAdapter(getContext(), userList, currentUserId);
        recyclerView.setAdapter(adapter);

        // Back button
        imgBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Load all users first
        loadAllUsers();

        // Search listener
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadAllUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUsers.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setUserId(document.getId());

                        // Không thêm current user vào list
                        if (!user.getUserId().equals(currentUserId)) {
                            allUsers.add(user);
                        }
                    }
                    Log.d("SearchUserFragment", "Loaded " + allUsers.size() + " users");
                })
                .addOnFailureListener(e -> {
                    Log.e("SearchUserFragment", "Error loading users: " + e.getMessage());
                });
    }

    private void filterUsers(String query) {
        userList.clear();

        if (query.isEmpty()) {
            adapter.notifyDataSetChanged();
            updateEmptyView();
            return;
        }

        String lowerQuery = query.toLowerCase();

        for (User user : allUsers) {
            boolean matchFound = false;

            // Tìm theo tên
            if (user.getFullName() != null &&
                    user.getFullName().toLowerCase().contains(lowerQuery)) {
                matchFound = true;
            }

            // Tìm theo email
            if (!matchFound && user.getEmail() != null &&
                    user.getEmail().toLowerCase().contains(lowerQuery)) {
                matchFound = true;
            }

            // Tìm theo số điện thoại
            if (!matchFound && user.getPhone() != null &&
                    user.getPhone().contains(query)) {
                matchFound = true;
            }

            if (matchFound) {
                userList.add(user);
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (tvNoResults != null) {
            if (userList.isEmpty() && !searchInput.getText().toString().isEmpty()) {
                tvNoResults.setVisibility(View.VISIBLE);
                tvNoResults.setText("Không tìm thấy người dùng");
                recyclerView.setVisibility(View.GONE);
            } else {
                tvNoResults.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
}
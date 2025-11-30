package com.example.chatnote.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatnote.Adapter.ChatListAdapter;
import com.example.chatnote.ChatActivity;
import com.example.chatnote.Model.Chat;
import com.example.chatnote.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ChatsFragment extends Fragment {

    private static final String TAG = "ChatsFragment";

    private String currentUserId;
    private TextView search;
    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private List<Chat> chatList = new ArrayList<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mainchat, container, false);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        } else {
            Log.e(TAG, "No user logged in");
            return view;
        }

        // Initialize views
        search = view.findViewById(R.id.search_username_input);
        ImageButton searchBtn = view.findViewById(R.id.search_user_btn);
        recyclerView = view.findViewById(R.id.vew_chat);

        // Setup RecyclerView
        setupRecyclerView();

        // Load chat list
        loadChatList();

        // Search click listener
        View.OnClickListener openSearchFragment = v -> {
            SearchUserFragment searchFragment = new SearchUserFragment();
            Bundle bundle = new Bundle();
            bundle.putString("userId", currentUserId);
            searchFragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, searchFragment)
                    .addToBackStack(null)
                    .commit();
        };

        searchBtn.setOnClickListener(openSearchFragment);
        search.setOnClickListener(openSearchFragment);

        return view;
    }

    private void setupRecyclerView() {
        ChatListAdapter.OnChatClickListener listener =
                (chat, otherUserId, otherUserName, otherUserAvatar) -> {
                    // Mở ChatActivity
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    intent.putExtra("chatId", chat.getId());
                    intent.putExtra("receiverId", otherUserId);
                    intent.putExtra("receiverName", otherUserName);
                    intent.putExtra("receiverImage", otherUserAvatar);
                    startActivity(intent);
                };

        adapter = new ChatListAdapter(getContext(), chatList, currentUserId, listener);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadChatList() {
        db.collection("chats")
                .whereArrayContains("members", currentUserId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading chats: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        chatList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Chat chat = doc.toObject(Chat.class);
                            if (chat != null) {
                                chat.setId(doc.getId());
                                chatList.add(chat);
                            }
                        }

                        adapter.notifyDataSetChanged();

                        // Show/hide empty state
                        if (chatList.isEmpty()) {
                            // Có thể thêm TextView "Chưa có cuộc trò chuyện nào"
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload khi quay lại fragment
        if (currentUserId != null) {
            loadChatList();
        }
    }
}
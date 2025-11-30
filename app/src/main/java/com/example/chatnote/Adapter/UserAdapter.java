package com.example.chatnote.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatnote.ChatActivity;
import com.example.chatnote.Model.User;
import com.example.chatnote.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private List<User> userList;
    private String currentUserId;
    private FirebaseFirestore db;

    public UserAdapter(Context context, List<User> userList, String currentUserId) {
        this.context = context;
        this.userList = userList;
        this.currentUserId = currentUserId;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        // Set name
        holder.tvName.setText(user.getFullName() != null ?
                user.getFullName() : "Người dùng");

        // Set phone or email
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            holder.tvPhone.setText("SĐT: " + user.getPhone());
        } else if (user.getEmail() != null) {
            holder.tvPhone.setText(user.getEmail());
        } else {
            holder.tvPhone.setText("Chưa có thông tin");
        }

        // Load avatar
        if (user.getProfilePicUrl() != null && !user.getProfilePicUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getProfilePicUrl())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_profile);
        }

        // Click chat button
        holder.btnChat.setOnClickListener(v -> {
            checkOrCreateChat(user);
        });

        // Click on item
        holder.itemView.setOnClickListener(v -> {
            checkOrCreateChat(user);
        });
    }

    private void checkOrCreateChat(User user) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "Bạn cần đăng nhập để chat!", Toast.LENGTH_SHORT).show();
            return;
        }

        String myUserId = currentUser.getUid();
        String receiverId = user.getUserId();

        if (myUserId.equals(receiverId)) {
            Toast.makeText(context, "Không thể chat với chính mình!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra xem đã có chat room chưa
        db.collection("chats")
                .whereArrayContains("members", myUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String chatId = null;

                    // Tìm chat room có cả 2 người
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                        List<String> members = (List<String>) doc.get("members");
                        if (members != null && members.contains(receiverId)) {
                            chatId = doc.getId();
                            break;
                        }
                    }

                    if (chatId != null) {
                        // Đã có chat room, mở chat
                        openChatActivity(chatId, user);
                    } else {
                        // Chưa có, tạo mới
                        createNewChat(myUserId, receiverId, user);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Lỗi kiểm tra chat: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewChat(String myUserId, String receiverId, User receiver) {
        Map<String, Object> chatData = new HashMap<>();

        List<String> members = new ArrayList<>();
        members.add(myUserId);
        members.add(receiverId);

        chatData.put("members", members);
        chatData.put("lastMessage", "");
        chatData.put("lastMessageTime", System.currentTimeMillis());
        chatData.put("createdAt", System.currentTimeMillis());

        db.collection("chats")
                .add(chatData)
                .addOnSuccessListener(documentReference -> {
                    String chatId = documentReference.getId();
                    openChatActivity(chatId, receiver);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Không thể tạo chat: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void openChatActivity(String chatId, User receiver) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("receiverId", receiver.getUserId());  // SỬA ĐÚNG KEY
        intent.putExtra("receiverName", receiver.getFullName());
        intent.putExtra("receiverImage", receiver.getProfilePicUrl());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName, tvPhone;
        ImageButton btnChat;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            btnChat = itemView.findViewById(R.id.btnChat);
        }
    }
}
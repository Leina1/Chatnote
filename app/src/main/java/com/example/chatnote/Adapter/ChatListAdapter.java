package com.example.chatnote.Adapter;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatnote.Model.Chat;
import com.example.chatnote.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(Chat chat, String otherUserId, String otherUserName, String otherUserAvatar);
    }

    private Context context;
    private List<Chat> chatList;
    private String currentUserId;
    private OnChatClickListener listener;
    private FirebaseFirestore db;

    public ChatListAdapter(Context context, List<Chat> chatList, String currentUserId, OnChatClickListener listener) {
        this.context = context;
        this.chatList = chatList;
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);

        // Tìm ID người kia
        String otherUserId = chat.getMembers().get(0).equals(currentUserId)
                ? chat.getMembers().get(1)
                : chat.getMembers().get(0);

        // Load thông tin người kia
        db.collection("users").document(otherUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String userName = doc.getString("fullName");
                        String avatar = doc.getString("profilePicUrl");

                        holder.tvName.setText(userName != null ? userName : "User");

                        if (avatar != null && !avatar.isEmpty()) {
                            Glide.with(context)
                                    .load(avatar)
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .circleCrop()
                                    .into(holder.imgAvatar);
                        } else {
                            holder.imgAvatar.setImageResource(R.drawable.ic_profile);
                        }

                        // Click listener với check block
                        holder.itemView.setOnClickListener(v -> {
                            checkBlockAndOpenChat(chat, otherUserId, userName, avatar);
                        });
                    }
                });

        // Set last message và time
        String lastMsg = chat.getLastMessage();
        holder.tvLastMessage.setText(lastMsg != null && !lastMsg.isEmpty()
                ? lastMsg : "Bắt đầu cuộc trò chuyện");
        holder.tvTime.setText(chat.getTimeString());

        // Handle more button với block và report
        holder.btnMore.setOnClickListener(v -> {
            showOptionsMenu(holder.btnMore, otherUserId, chat.getId());
        });
    }

    private void checkBlockAndOpenChat(Chat chat, String otherUserId, String userName, String avatar) {
        // Check block status trước khi mở chat
        db.collection("blocks")
                .whereIn("blockerId", Arrays.asList(currentUserId, otherUserId))
                .whereIn("blockedId", Arrays.asList(currentUserId, otherUserId))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean currentBlocksOther = false;
                    boolean otherBlocksCurrent = false;

                    for (DocumentSnapshot doc : querySnapshot) {
                        String blockerId = doc.getString("blockerId");
                        String blockedId = doc.getString("blockedId");

                        if (blockerId != null && blockedId != null) {
                            if (blockerId.equals(currentUserId) && blockedId.equals(otherUserId)) {
                                currentBlocksOther = true;
                            }
                            if (blockerId.equals(otherUserId) && blockedId.equals(currentUserId)) {
                                otherBlocksCurrent = true;
                            }
                        }
                    }

                    if (currentBlocksOther && otherBlocksCurrent) {
                        Toast.makeText(context, "Hai bạn đã chặn nhau. Không thể trò chuyện.",
                                Toast.LENGTH_SHORT).show();
                    } else if (currentBlocksOther) {
                        Toast.makeText(context, "Bạn đã chặn người này. Bỏ chặn để nhắn tin.",
                                Toast.LENGTH_SHORT).show();
                    } else if (otherBlocksCurrent) {
                        Toast.makeText(context, "Bạn đã bị người này chặn. Không thể nhắn tin.",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Không bị block, mở chat
                        if (listener != null) {
                            listener.onChatClick(chat, otherUserId, userName, avatar);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Lỗi kiểm tra trạng thái", Toast.LENGTH_SHORT).show();
                });
    }

    private void showOptionsMenu(View anchor, String otherUserId, String chatId) {
        // Kiểm tra xem đã block chưa
        db.collection("blocks")
                .whereEqualTo("blockerId", currentUserId)
                .whereEqualTo("blockedId", otherUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean isBlocked = !querySnapshot.isEmpty();

                    PopupMenu popup = new PopupMenu(context, anchor);
                    popup.getMenuInflater().inflate(R.menu.menu_chat_options, popup.getMenu());

                    MenuItem blockItem = popup.getMenu().findItem(R.id.action_block);
                    if (isBlocked) {
                        blockItem.setTitle("Bỏ chặn");
                    } else {
                        blockItem.setTitle("Chặn người này");
                    }

                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.action_block) {
                            if (isBlocked) {
                                // Bỏ chặn
                                for (DocumentSnapshot doc : querySnapshot) {
                                    doc.getReference().delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(context, "Đã bỏ chặn!",
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                }
                            } else {
                                // Chặn
                                blockUser(otherUserId);
                            }
                            return true;
                        } else if (item.getItemId() == R.id.action_report) {
                            // Báo cáo
                            showReportDialog(otherUserId, chatId);
                            return true;
                        } else if (item.getItemId() == R.id.action_delete) {
                            // Xóa cuộc trò chuyện
                            deleteChat(chatId);
                            return true;
                        }
                        return false;
                    });

                    popup.show();
                });
    }

    private void blockUser(String blockedId) {
        HashMap<String, Object> block = new HashMap<>();
        block.put("blockerId", currentUserId);
        block.put("blockedId", blockedId);
        block.put("timestamp", System.currentTimeMillis());

        db.collection("blocks")
                .add(block)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(context, "Đã chặn người dùng này!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Chặn thất bại: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showReportDialog(String reportedUserId, String chatId) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * context.getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText edtTitle = new EditText(context);
        edtTitle.setHint("Tiêu đề báo cáo");

        EditText edtContent = new EditText(context);
        edtContent.setHint("Nội dung báo cáo chi tiết");
        edtContent.setMinLines(3);
        edtContent.setGravity(Gravity.TOP);

        layout.addView(edtTitle);
        layout.addView(edtContent);

        new android.app.AlertDialog.Builder(context)
                .setTitle("Báo cáo người dùng")
                .setView(layout)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String title = edtTitle.getText().toString().trim();
                    String content = edtContent.getText().toString().trim();

                    if (title.isEmpty() || content.isEmpty()) {
                        Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    HashMap<String, Object> report = new HashMap<>();
                    report.put("reporterId", currentUserId);
                    report.put("reportedUserId", reportedUserId);
                    report.put("chatId", chatId);
                    report.put("title", title);
                    report.put("content", content);
                    report.put("timestamp", System.currentTimeMillis());
                    report.put("status", "pending"); // pending, reviewed, resolved

                    db.collection("reports")
                            .add(report)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(context, "Đã gửi báo cáo thành công!",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Gửi báo cáo thất bại!",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteChat(String chatId) {
        new android.app.AlertDialog.Builder(context)
                .setTitle("Xóa cuộc trò chuyện")
                .setMessage("Bạn có chắc chắn muốn xóa cuộc trò chuyện này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Có thể soft delete hoặc hard delete
                    // Ở đây chỉ xóa khỏi danh sách hiển thị
                    for (int i = 0; i < chatList.size(); i++) {
                        if (chatList.get(i).getId().equals(chatId)) {
                            chatList.remove(i);
                            notifyItemRemoved(i);
                            break;
                        }
                    }
                    Toast.makeText(context, "Đã xóa cuộc trò chuyện", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName, tvLastMessage, tvTime;
        ImageButton btnMore;

        public ChatViewHolder(View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}
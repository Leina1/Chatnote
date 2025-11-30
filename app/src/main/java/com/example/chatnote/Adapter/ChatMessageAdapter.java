package com.example.chatnote.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatnote.Model.Message;
import com.example.chatnote.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private List<Message> messages;
    private String currentUserId;
    private String receiverAvatar;

    public ChatMessageAdapter(List<Message> messages, String currentUserId, String receiverAvatar) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.receiverAvatar = receiverAvatar;
    }

    public void updateReceiverAvatar(String avatarUrl) {
        this.receiverAvatar = avatarUrl;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        return msg.getSenderId().equals(currentUserId) ? 1 : 0;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == 1)
                ? R.layout.item_message_sent
                : R.layout.item_message_received;

        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message msg = messages.get(position);

        // Avatar cho tin nhắn received
        if (holder.imgAvatar != null) {
            if (receiverAvatar != null && !receiverAvatar.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(receiverAvatar)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(holder.imgAvatar);
            } else {
                holder.imgAvatar.setImageResource(R.drawable.ic_profile);
            }
        }

        // Text
        if (msg.getContent() != null && !msg.getContent().isEmpty()) {
            holder.txtMessage.setVisibility(View.VISIBLE);
            holder.txtMessage.setText(msg.getContent());
        } else {
            holder.txtMessage.setVisibility(View.GONE);
        }

        // Image
        if (holder.imgMessage != null) {
            if (msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {
                holder.imgMessage.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(msg.getImageUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(holder.imgMessage);
            } else {
                holder.imgMessage.setVisibility(View.GONE);
            }
        }

        // Time
        if (holder.txtTime != null && msg.getTimestamp() > 0) {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(msg.getTimestamp()));
            holder.txtTime.setText(time);
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage, txtTime;
        ImageView imgMessage, imgAvatar;

        public MessageViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
            imgMessage = itemView.findViewById(R.id.imgMessage);

            // Chỉ tin nhắn received mới có avatar
            if (viewType == 0) { // 0 = received
                imgAvatar = itemView.findViewById(R.id.imgAvatar);
            }
        }
    }
}
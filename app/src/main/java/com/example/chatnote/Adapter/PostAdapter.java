package com.example.chatnote.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatnote.Model.Post;
import com.example.chatnote.R;
import com.example.chatnote.fragment.CreatePostFragment;
import com.example.chatnote.fragment.PostDetailFragment;
import com.squareup.picasso.Picasso;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;
    private String currentUserId;
    private OnPostActionListener listener;

    public interface OnPostActionListener {
        void onEditClicked(Post post);
        void onDeleteClicked(Post post);
    }

    public PostAdapter(Context context, List<Post> postList, String currentUserId, OnPostActionListener listener) {
        this.context = context;
        this.postList = postList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.txtAuthor.setText(post.getFullName() != null ? post.getFullName() : "Người dùng");
        holder.txtTitle.setText(post.getTitle() != null ? post.getTitle() : "");
        holder.txtLikeNum.setText(String.valueOf(post.getLikeUserIds() != null ? post.getLikeUserIds().size() : 0));
        holder.txtCommentNum.setText(String.valueOf(post.getComments() != null ? post.getComments().size() : 0));
        holder.txtTime.setText(getTimeAgo(post.getTimestamp()));

        // Avatar người đăng
        if (post.getProfilePicUrl() != null && !post.getProfilePicUrl().isEmpty()) {
            Picasso.get().load(post.getProfilePicUrl()).into(holder.imgUserAvatar);
        } else {
            holder.imgUserAvatar.setImageResource(R.drawable.cartethyia1);
        }

        // Ảnh bài viết
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.imgPostImage.setVisibility(View.VISIBLE);
            Picasso.get().load(post.getImageUrl()).into(holder.imgPostImage);
        } else {
            holder.imgPostImage.setVisibility(View.GONE);
        }

        // Hiển thị nút More chỉ khi là bài của user hiện tại
        if (post.getUserId() != null && currentUserId != null && post.getUserId().equals(currentUserId)) {
            holder.btnMore.setVisibility(View.VISIBLE);
            holder.btnMore.setOnClickListener(v -> {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(context, holder.btnMore);
                popup.getMenuInflater().inflate(R.menu.menu_post_more, popup.getMenu());

                popup.setOnMenuItemClickListener(item -> {
                    if (listener == null) return false;

                    int id = item.getItemId();
                    if (id == R.id.action_edit) {
                        // mở CreatePostFragment ở chế độ edit
                        CreatePostFragment editFragment = CreatePostFragment.newInstance(post, currentUserId);
                        ((FragmentActivity) context).getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, editFragment)
                                .addToBackStack(null)
                                .commit();
                        return true;
                    } else if (id == R.id.action_delete) {
                        listener.onDeleteClicked(post);
                        return true;
                    }
                    return false;
                });


                popup.show();
            });
        } else {
            holder.btnMore.setVisibility(View.GONE);
        }

        // Click để xem chi tiết
        holder.itemView.setOnClickListener(v -> {
            PostDetailFragment detailFragment = PostDetailFragment.newInstance(post);
            ((FragmentActivity) context).getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });

    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imgUserAvatar, imgPostImage;
        TextView txtAuthor, txtTitle, txtLikeNum, txtCommentNum, txtTime;
        ImageButton btnMore;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUserAvatar = itemView.findViewById(R.id.imgUserAvatar);
            imgPostImage = itemView.findViewById(R.id.imgPostImage);
            txtAuthor = itemView.findViewById(R.id.txtAuthor);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtLikeNum = itemView.findViewById(R.id.txtLikeNum);
            txtCommentNum = itemView.findViewById(R.id.txtCommentNum);
            txtTime = itemView.findViewById(R.id.txtTime);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }

    private String getTimeAgo(long timestamp) {
        if (timestamp <= 0) return "Vừa xong";

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        if (hours < 24) return hours + " giờ trước";
        return days + " ngày trước";
    }
}

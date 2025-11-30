package com.example.chatnote.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatnote.Model.Comment;
import com.example.chatnote.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private Context context;
    private List<Comment> commentList;

    public CommentAdapter(Context context, List<Comment> commentList) {
        this.context = context;
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.txtCommentName.setText(comment.getUserName() != null ?
                comment.getUserName() : "Người dùng");
        holder.txtCommentContent.setText(comment.getContent());

        if (comment.getUserAvatar() != null && !comment.getUserAvatar().isEmpty()) {
            Picasso.get().load(comment.getUserAvatar()).into(holder.imgCommentAvatar);
        } else {
            holder.imgCommentAvatar.setImageResource(R.drawable.cartethyia1);
        }
    }

    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCommentAvatar;
        TextView txtCommentName, txtCommentContent;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCommentAvatar = itemView.findViewById(R.id.imgCommentAvatar);
            txtCommentName = itemView.findViewById(R.id.txtCommentName);
            txtCommentContent = itemView.findViewById(R.id.txtCommentContent);
        }
    }
}
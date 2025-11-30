package com.example.chatnote.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatnote.Adapter.CommentAdapter;
import com.example.chatnote.Model.Comment;
import com.example.chatnote.Model.Post;
import com.example.chatnote.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostDetailFragment extends Fragment {

    private static final String ARG_POST = "post";

    private Post post;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUserName;
    private String currentUserAvatar;
    private boolean isLiked = false;

    private ImageView imgUserAvatar, imgPostImage;
    private TextView txtAuthor, txtTime, txtTitle, txtContent, txtLikeNum;
    private ImageButton btnBack, btnLike, btnComment, btnSend;
    private LinearLayout commentInputLayout;
    private EditText edtComment;
    private RecyclerView rvComments;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList;

    public static PostDetailFragment newInstance(Post post) {
        PostDetailFragment fragment = new PostDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_POST, post);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_post_detail, container, false);

        // Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Khởi tạo views
        initViews(view);

        // Load thông tin user hiện tại
        loadCurrentUserInfo();

        if (getArguments() != null && getArguments().containsKey(ARG_POST)) {
            post = (Post) getArguments().getSerializable(ARG_POST);
            loadPostData();
            setupComments();
            checkLikeStatus();
        }

        // Setup click listeners
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        imgUserAvatar = view.findViewById(R.id.imgUserAvatar);
        imgPostImage = view.findViewById(R.id.imgPostImage);
        txtAuthor = view.findViewById(R.id.txtAuthor);
        txtTime = view.findViewById(R.id.txtTime);
        txtTitle = view.findViewById(R.id.txtTitle);
        txtContent = view.findViewById(R.id.txtContent);
        txtLikeNum = view.findViewById(R.id.txtLikeNum);
        btnBack = view.findViewById(R.id.btnBack);
        btnLike = view.findViewById(R.id.btnLike);
        btnComment = view.findViewById(R.id.btnComment);
        btnSend = view.findViewById(R.id.btnSend);
        commentInputLayout = view.findViewById(R.id.comment_input_layout);
        edtComment = view.findViewById(R.id.edtComment);
        rvComments = view.findViewById(R.id.rvComments);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnLike.setOnClickListener(v -> handleLike());

        btnComment.setOnClickListener(v -> {
            commentInputLayout.setVisibility(View.VISIBLE);
            edtComment.requestFocus();
        });

        btnSend.setOnClickListener(v -> sendComment());
    }

    private void loadCurrentUserInfo() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserName = documentSnapshot.getString("fullName");
                        currentUserAvatar = documentSnapshot.getString("profilePicUrl");
                    }
                });
    }

    private void setupComments() {
        commentList = new ArrayList<>();

        if (post.getComments() != null) {
            for (Map<String, Object> map : post.getComments()) {
                Comment c = new Comment();

                c.setUserId((String) map.get("userId"));
                c.setUserName((String) map.get("userName"));
                c.setUserAvatar((String) map.get("userAvatar"));
                c.setContent((String) map.get("content"));

                // timestamp có thể là Long hoặc Double từ Firestore
                Object timeObj = map.get("timestamp");
                long time = timeObj instanceof Long ? (Long) timeObj :
                        ((Double) timeObj).longValue();
                c.setTimestamp(time);

                commentList.add(c);
            }
        }

        commentAdapter = new CommentAdapter(getContext(), commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvComments.setAdapter(commentAdapter);
    }

    private void checkLikeStatus() {
        if (post.getLikeUserIds() != null && post.getLikeUserIds().contains(currentUserId)) {
            isLiked = true;
            updateLikeButton(true);
        }
    }

    private void handleLike() {
        DocumentReference postRef = db.collection("posts").document(post.getPostId());

        if (isLiked) {
            // Unlike
            postRef.update("likeUserIds", FieldValue.arrayRemove(currentUserId))
                    .addOnSuccessListener(aVoid -> {
                        isLiked = false;
                        updateLikeButton(false);

                        // Cập nhật số like local
                        if (post.getLikeUserIds() != null) {
                            post.getLikeUserIds().remove(currentUserId);
                            txtLikeNum.setText(String.valueOf(post.getLikeUserIds().size()));
                        }

                        Toast.makeText(getContext(), "Đã bỏ thích", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Like
            postRef.update("likeUserIds", FieldValue.arrayUnion(currentUserId))
                    .addOnSuccessListener(aVoid -> {
                        isLiked = true;
                        updateLikeButton(true);

                        // Cập nhật số like local
                        if (post.getLikeUserIds() == null) {
                            post.setLikeUserIds(new ArrayList<>());
                        }
                        post.getLikeUserIds().add(currentUserId);
                        txtLikeNum.setText(String.valueOf(post.getLikeUserIds().size()));

                        Toast.makeText(getContext(), "Đã thích", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateLikeButton(boolean liked) {
        if (liked) {
            btnLike.setColorFilter(getResources().getColor(R.color.like_color));
        } else {
            btnLike.setColorFilter(getResources().getColor(R.color.grey));
        }
    }

    private void sendComment() {
        String commentContent = edtComment.getText().toString().trim();

        if (commentContent.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập nội dung bình luận",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo comment object
        Comment newComment = new Comment();
        newComment.setUserId(currentUserId);
        newComment.setUserName(currentUserName);
        newComment.setUserAvatar(currentUserAvatar);
        newComment.setContent(commentContent);
        newComment.setTimestamp(System.currentTimeMillis());

        // Cập nhật lên Firebase
        DocumentReference postRef = db.collection("posts").document(post.getPostId());

        postRef.update("comments", FieldValue.arrayUnion(convertCommentToMap(newComment)))
                .addOnSuccessListener(aVoid -> {
                    // Thêm vào list local và cập nhật UI
                    commentList.add(newComment);
                    commentAdapter.notifyItemInserted(commentList.size() - 1);

                    // Clear input
                    edtComment.setText("");
                    commentInputLayout.setVisibility(View.GONE);

                    Toast.makeText(getContext(), "Đã gửi bình luận", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private Map<String, Object> convertCommentToMap(Comment comment) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", comment.getUserId());
        map.put("userName", comment.getUserName());
        map.put("userAvatar", comment.getUserAvatar());
        map.put("content", comment.getContent());
        map.put("timestamp", comment.getTimestamp());
        return map;
    }

    private void loadPostData() {
        if (post == null) return;

        txtAuthor.setText(post.getFullName());
        txtTime.setText(getTimeAgo(post.getTimestamp()));
        txtTitle.setText(post.getTitle());
        txtContent.setText(post.getContent());
        txtLikeNum.setText(String.valueOf(post.getLikeUserIds() != null ?
                post.getLikeUserIds().size() : 0));

        if (post.getProfilePicUrl() != null && !post.getProfilePicUrl().isEmpty()) {
            Picasso.get().load(post.getProfilePicUrl()).into(imgUserAvatar);
        } else {
            imgUserAvatar.setImageResource(R.drawable.cartethyia1);
        }

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            imgPostImage.setVisibility(View.VISIBLE);
            Picasso.get().load(post.getImageUrl()).into(imgPostImage);
        } else {
            imgPostImage.setVisibility(View.GONE);
        }
    }

    private String getTimeAgo(long timestamp) {
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
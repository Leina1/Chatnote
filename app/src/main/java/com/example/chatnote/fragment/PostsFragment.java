package com.example.chatnote.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatnote.Adapter.PostAdapter;
import com.example.chatnote.Model.Post;
import com.example.chatnote.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostsFragment extends Fragment {

    private RecyclerView recyclerAllPosts;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private FirebaseFirestore db;
    private String currentUserId;
    private ImageButton btnUserPosts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_posts, container, false);

        // Khởi tạo views
        recyclerAllPosts = view.findViewById(R.id.rvAllPosts);
        btnUserPosts = view.findViewById(R.id.btnUserPosts);

        // Setup RecyclerView
        recyclerAllPosts.setLayoutManager(new LinearLayoutManager(getContext()));

        // Khởi tạo data
        postList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        // Lấy current user ID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        postAdapter = new PostAdapter(getContext(), postList, currentUserId, new PostAdapter.OnPostActionListener() {
            @Override
            public void onEditClicked(Post post) {
                editPost(post);
            }

            @Override
            public void onDeleteClicked(Post post) {
                deletePost(post);
            }
        });


        recyclerAllPosts.setAdapter(postAdapter);

        // Load tất cả bài viết
        loadAllPosts();

        // Button để xem bài viết của user
        btnUserPosts.setOnClickListener(v -> {
            MainPostFragment mainPostFragment = new MainPostFragment();
            Bundle bundle = new Bundle();
            bundle.putString("userId", currentUserId);
            mainPostFragment.setArguments(bundle);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, mainPostFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void loadAllPosts() {
        // Load tất cả bài viết, sau đó sắp xếp thủ công
        db.collection("posts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();
                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            post.setPostId(doc.getId());
                            postList.add(post);
                        }
                    }

                    // Sắp xếp thủ công theo timestamp giảm dần
                    Collections.sort(postList, (p1, p2) ->
                            Long.compare(p2.getTimestamp(), p1.getTimestamp()));

                    postAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi tải bài viết: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void handlePostMoreOptions(Post post) {
        // Tạo dialog hoặc menu để xử lý Edit/Delete
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(getContext());

        builder.setTitle("Chọn hành động")
                .setItems(new String[]{"Chỉnh sửa", "Xóa bài"}, (dialog, which) -> {
                    if (which == 0) {
                        // Edit post
                        editPost(post);
                    } else if (which == 1) {
                        // Delete post
                        deletePost(post);
                    }
                })
                .show();
    }

    private void editPost(Post post) {
        // Chuyển sang màn hình edit (tạo EditPostFragment nếu cần)
        Toast.makeText(getContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
    }

    private void deletePost(Post post) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa bài viết này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection("posts")
                            .document(post.getPostId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Đã xóa bài viết",
                                        Toast.LENGTH_SHORT).show();
                                loadAllPosts(); // Reload list
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Lỗi xóa bài: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
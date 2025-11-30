package com.example.chatnote.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainPostFragment extends Fragment {

    private String userId;
    private ImageButton btnAddPost;
    private RecyclerView recyclerPosts;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private FirebaseFirestore db;
    private ImageView btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_mainpost, container, false);

        // Khởi tạo views
        recyclerPosts = view.findViewById(R.id.recyclerPosts);
        btnAddPost = view.findViewById(R.id.img_add);
        btnBack = view.findViewById(R.id.btnBack);

        recyclerPosts.setLayoutManager(new LinearLayoutManager(getContext()));

        postList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        // Lấy userId từ arguments
        if (getArguments() != null && getArguments().containsKey("userId")) {
            userId = getArguments().getString("userId");
        } else {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Setup adapter
        postAdapter = new PostAdapter(getContext(), postList, userId, new PostAdapter.OnPostActionListener() {
            @Override
            public void onEditClicked(Post post) {
                Toast.makeText(getContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClicked(Post post) {
                deletePost(post);
            }
        });
        recyclerPosts.setAdapter(postAdapter);

        // Load CHỈ bài viết của user này
        loadUserPosts();

        // Button thêm bài viết mới
        btnAddPost.setOnClickListener(v -> {
            CreatePostFragment createPostFragment = new CreatePostFragment();
            Bundle bundle = new Bundle();
            bundle.putString("userId", userId);
            createPostFragment.setArguments(bundle);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, createPostFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Button quay lại
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        return view;
    }

    private void loadUserPosts() {
        // Load CHỈ bài viết của user được chỉ định
        db.collection("posts")
                .whereEqualTo("userId", userId)
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

                    // Sắp xếp thủ công theo timestamp giảm dần (mới nhất lên trên)
                    Collections.sort(postList, new Comparator<Post>() {
                        @Override
                        public int compare(Post p1, Post p2) {
                            return Long.compare(p2.getTimestamp(), p1.getTimestamp());
                        }
                    });

                    postAdapter.notifyDataSetChanged();

                    if (postList.isEmpty()) {
                        Toast.makeText(getContext(), "Bạn chưa có bài viết nào",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void handlePostMoreOptions(Post post) {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(getContext());

        builder.setTitle("Chọn hành động")
                .setItems(new String[]{"Chỉnh sửa", "Xóa bài"}, (dialog, which) -> {
                    if (which == 0) {
                        // Edit post
                        Toast.makeText(getContext(), "Tính năng đang phát triển",
                                Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        // Delete post
                        deletePost(post);
                    }
                })
                .show();
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
                                loadUserPosts(); // Reload list
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Lỗi: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
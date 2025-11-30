package com.example.chatnote.fragment;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.chatnote.Model.Post;
import com.example.chatnote.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class CreatePostFragment extends Fragment {

    private String userId;
    private String fullName;
    private String profilePicUrl;
    private Uri selectedImageUri;

    private ImageView imgUserAvatar, imgSelected;
    private TextView txtUserName;
    private EditText edtTitle, edtContent;
    private Button btnSelectImage, btnPost;
    private ImageButton btnBack;

    private FirebaseFirestore db;

    private boolean isEditMode = false;
    private String postIdToEdit;
    private String existingImageUrl;

    // ActivityResultLauncher chọn ảnh
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imgSelected.setVisibility(View.VISIBLE);
                    imgSelected.setImageURI(selectedImageUri);
                }
            });

    // Request permission
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) openImagePicker();
                else Toast.makeText(getActivity(), "Permission denied to access storage", Toast.LENGTH_SHORT).show();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_createpost, container, false);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }
        imgUserAvatar = view.findViewById(R.id.imgUserAvatar);
        imgSelected = view.findViewById(R.id.imgSelected);
        txtUserName = view.findViewById(R.id.txtUserName);
        edtTitle = view.findViewById(R.id.edtTitle);
        edtContent = view.findViewById(R.id.edtContent);
        btnSelectImage = view.findViewById(R.id.btnSelectImage);
        btnPost = view.findViewById(R.id.btnPost);
        btnBack = view.findViewById(R.id.btnBack);

        // Lấy userId
        if (getArguments() != null) {
            userId = getArguments().getString("userId", userId); // ưu tiên từ arguments
            isEditMode = getArguments().getBoolean("isEditMode", false);

            if (isEditMode) {
                postIdToEdit = getArguments().getString("postId");
                edtTitle.setText(getArguments().getString("title"));
                edtContent.setText(getArguments().getString("content"));
                existingImageUrl = getArguments().getString("imageUrl");

                if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                    imgSelected.setVisibility(View.VISIBLE);
                    Picasso.get().load(existingImageUrl).into(imgSelected);
                }

                btnPost.setText("Lưu thay đổi");
            }
        }

        db = FirebaseFirestore.getInstance();

        loadUserInfo();

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnSelectImage.setOnClickListener(v -> checkStoragePermission());

        btnPost.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String content = edtContent.getText().toString().trim();

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(getActivity(), "Tiêu đề và nội dung không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEditMode) {
                // Edit post
                if (selectedImageUri != null) {
                    uploadImageToCloudinary(selectedImageUri, title, content);
                } else {
                    updatePostToFirestore(existingImageUrl, title, content);
                }
            } else {
                // Tạo bài mới
                if (selectedImageUri != null) {
                    uploadImageToCloudinary(selectedImageUri, title, content);
                } else {
                    savePostToFirestore(null, title, content);
                }
            }
        });


        return view;
    }

    private void loadUserInfo() {
        if (userId == null) return;

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        fullName = documentSnapshot.getString("fullName");
                        profilePicUrl = documentSnapshot.getString("profilePicUrl");

                        txtUserName.setText(fullName != null ? fullName : "Người dùng");
                        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                            Picasso.get().load(profilePicUrl).into(imgUserAvatar);
                        } else imgUserAvatar.setImageResource(R.drawable.cartethyia1);
                    }
                });
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            else openImagePicker();
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            else openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void createPost() {
        String title = edtTitle.getText().toString().trim();
        String content = edtContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(getActivity(), "Tiêu đề và nội dung không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            uploadImageToCloudinary(selectedImageUri, title, content);
        } else {
            savePostToFirestore(null, title, content);
        }
    }

    private void uploadImageToCloudinary(Uri uri, String title, String content) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("upload", ".jpg", requireContext().getCacheDir());
            BufferedSink sink = Okio.buffer(Okio.sink(tempFile));
            sink.writeAll(Okio.source(inputStream));
            sink.close();

            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", tempFile.getName(),
                            RequestBody.create(tempFile, MediaType.parse("image/*")))
                    .addFormDataPart("upload_preset", "chatnote_preset")
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/diumhctnb/image/upload")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    if (!response.isSuccessful()) return;
                    String resBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(resBody);
                        String imageUrl = json.getString("secure_url");
                        requireActivity().runOnUiThread(() -> {
                            if (isEditMode) {
                                // Nếu đang edit, update bài viết cũ
                                updatePostToFirestore(imageUrl, edtTitle.getText().toString().trim(),
                                        edtContent.getText().toString().trim());
                            } else {
                                // Nếu tạo bài mới, lưu bài mới
                                savePostToFirestore(imageUrl, edtTitle.getText().toString().trim(),
                                        edtContent.getText().toString().trim());
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Lỗi đọc ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePostToFirestore(@Nullable String imageUrl, String title, String content) {
        Map<String, Object> postMap = new HashMap<>();
        postMap.put("userId", userId);
        postMap.put("fullName", fullName);
        postMap.put("profilePicUrl", profilePicUrl != null ? profilePicUrl : "");
        postMap.put("title", title);
        postMap.put("content", content);
        postMap.put("imageUrl", imageUrl != null ? imageUrl : "");
        postMap.put("timestamp", System.currentTimeMillis());
        postMap.put("likeUserIds", new ArrayList<String>());
        postMap.put("comments", new ArrayList<Map<String, String>>());

        db.collection("posts")
                .add(postMap)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getActivity(), "Đăng bài thành công", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Đăng bài thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    public static CreatePostFragment newInstance(Post post, String currentUserId) {
        CreatePostFragment fragment = new CreatePostFragment();
        Bundle args = new Bundle();
        args.putBoolean("isEditMode", true);
        args.putString("postId", post.getPostId());
        args.putString("title", post.getTitle());
        args.putString("content", post.getContent());
        args.putString("imageUrl", post.getImageUrl());
        args.putString("userId", currentUserId); // thêm userId
        fragment.setArguments(args);
        return fragment;
    }
    private void updatePostToFirestore(@Nullable String imageUrl, String title, String content) {
        if (postIdToEdit == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("content", content);
        updates.put("imageUrl", imageUrl != null ? imageUrl : "");

        db.collection("posts").document(postIdToEdit)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Cập nhật bài viết thành công", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getActivity(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}

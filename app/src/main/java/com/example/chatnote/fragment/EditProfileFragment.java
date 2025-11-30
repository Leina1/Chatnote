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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.chatnote.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
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

public class EditProfileFragment extends Fragment {

    private ImageView profileImageView;
    private Button btnSave;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Uri imageUri;
    private String userId;

    // Image picker launcher
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    profileImageView.setImageURI(imageUri); // Hiển thị ảnh tạm thời
                }
            });

    // Permission for image picker
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openImagePicker();
                } else {
                    Toast.makeText(getActivity(), "Permission denied to access storage", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_editprofile, container, false);

        profileImageView = view.findViewById(R.id.editProfileImage);
        btnSave = view.findViewById(R.id.btnSaveProfile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Lấy userId
        Bundle args = getArguments();
        if (args != null && args.getString("userId") != null) {
            userId = args.getString("userId");
        } else {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) userId = currentUser.getUid();
        }

        // Load avatar từ Firestore
        loadUserData();

        // Click avatar để chọn ảnh mới
        profileImageView.setOnClickListener(v -> checkStoragePermission());

        // Lưu thông tin
        btnSave.setOnClickListener(v -> saveProfile());

        return view;
    }

    private void loadUserData() {
        if (userId == null) return;

        DocumentReference docRef = db.collection("users").document(userId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Load ảnh
                String profilePicUrl = documentSnapshot.getString("profilePicUrl");
                if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                    Picasso.get().load(profilePicUrl).into(profileImageView);
                }

                // Load thông tin text
                String fullName = documentSnapshot.getString("fullName");
                String bio = documentSnapshot.getString("bio");
                String phone = documentSnapshot.getString("phone"); // lấy phone thay vì email

                com.google.android.material.textfield.TextInputEditText etFullName =
                        getView().findViewById(R.id.editFullName);
                com.google.android.material.textfield.TextInputEditText etBio =
                        getView().findViewById(R.id.editBio);
                com.google.android.material.textfield.TextInputEditText etPhone =
                        getView().findViewById(R.id.editPhone); // sửa tên biến cho dễ hiểu

                if (etFullName != null) etFullName.setText(fullName != null ? fullName : "");
                if (etBio != null) etBio.setText(bio != null ? bio : "");
                if (etPhone != null) etPhone.setText(phone != null ? phone : "");

                // Phone có thể edit nên không disable
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getActivity(), "Failed to load user data", Toast.LENGTH_SHORT).show());
    }


    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            } else openImagePicker();
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveProfile() {
        if (userId == null) return;

        String fullName = ((com.google.android.material.textfield.TextInputEditText)
                getView().findViewById(R.id.editFullName)).getText().toString().trim();
        String bio = ((com.google.android.material.textfield.TextInputEditText)
                getView().findViewById(R.id.editBio)).getText().toString().trim();
        String phone = ((com.google.android.material.textfield.TextInputEditText)
                getView().findViewById(R.id.editPhone)).getText().toString().trim(); // lấy phone

        if (imageUri != null) {
            uploadImageToCloudinary(imageUri, fullName, bio, phone);
        } else {
            updateUserProfile(null, fullName, bio, phone);
        }
    }

    private void uploadImageToCloudinary(Uri uri, String fullName, String bio, String email) {
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
                    .addFormDataPart("upload_preset", "chatnote_preset") // preset Cloudinary
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
                        requireActivity().runOnUiThread(() -> updateUserProfile(imageUrl, fullName, bio, email));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Error reading image", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserProfile(String profileUrl, String fullName, String bio, String phone) {
        DocumentReference docRef = db.collection("users").document(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("fullName", fullName);
        data.put("bio", bio);
        data.put("phone", phone); // lưu phone thay vì email
        if (profileUrl != null) data.put("profilePicUrl", profileUrl);

        docRef.update(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    // Quay lại màn hình trước đó
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Update failed", Toast.LENGTH_SHORT).show());
    }


}

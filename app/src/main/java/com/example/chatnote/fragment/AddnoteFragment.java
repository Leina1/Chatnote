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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.chatnote.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
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

public class AddnoteFragment extends Fragment {
    private String noteId; // ID note để chỉnh sửa
    private String existingImageUrl;
    private EditText edtTitle, edtContent;
    private ImageView imgNoteAttachment;
    private ImageButton btnRemoveImage;
    private Button btnAttachImage, btnSaveNote;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private String userId;
    private Uri imageUri;
    private Runnable listener;

    // Image picker launcher
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    imgNoteAttachment.setImageURI(imageUri);
                    btnRemoveImage.setVisibility(View.VISIBLE); // hiện nút xóa
                }
            });

    // Permission launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) openImagePicker();
                else Toast.makeText(getActivity(), "Permission denied to access storage", Toast.LENGTH_SHORT).show();
            });

    public void setListener(Runnable listener) {
        this.listener = listener;
    }
    public static AddnoteFragment newInstance(String noteId, String title, String content, String imageUrl) {
        AddnoteFragment fragment = new AddnoteFragment();
        Bundle args = new Bundle();
        args.putString("noteId", noteId);
        args.putString("title", title);
        args.putString("content", content);
        args.putString("imageUrl", imageUrl);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_addnote, container, false);

        edtTitle = view.findViewById(R.id.edtNoteTitle);
        edtContent = view.findViewById(R.id.edtNoteContent);
        imgNoteAttachment = view.findViewById(R.id.imgNoteAttachment);
        btnRemoveImage = view.findViewById(R.id.btnRemoveImage);
        btnAttachImage = view.findViewById(R.id.btnAttachImage);
        btnSaveNote = view.findViewById(R.id.btnSaveNote);
        btnBack = view.findViewById(R.id.btnBackAddNote);

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            Toast.makeText(getContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Load note nếu chỉnh sửa
        if (getArguments() != null) {
            noteId = getArguments().getString("noteId");
            edtTitle.setText(getArguments().getString("title", ""));
            edtContent.setText(getArguments().getString("content", ""));
            existingImageUrl = getArguments().getString("imageUrl", "");
            if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                Glide.with(this).load(existingImageUrl).into(imgNoteAttachment);
                btnRemoveImage.setVisibility(View.VISIBLE);
            }
        }

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        btnAttachImage.setOnClickListener(v -> checkStoragePermission());

        btnRemoveImage.setOnClickListener(v -> {
            imageUri = null;
            imgNoteAttachment.setImageResource(R.drawable.pic);
            btnRemoveImage.setVisibility(View.GONE);
        });

        btnSaveNote.setOnClickListener(v -> saveNote());

        return view;
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

    private void saveNote() {
        String title = edtTitle.getText().toString().trim();
        String content = edtContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập tiêu đề hoặc nội dung", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            uploadImageToCloudinary(imageUri, title, content);
        } else {
            // Nếu chỉnh sửa mà không thay đổi ảnh, dùng existingImageUrl
            saveNoteToFirestore(existingImageUrl != null ? existingImageUrl : "", title, content);
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
                            Toast.makeText(getActivity(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    if (!response.isSuccessful()) return;
                    String resBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(resBody);
                        String imageUrl = json.getString("secure_url");
                        requireActivity().runOnUiThread(() -> saveNoteToFirestore(imageUrl, title, content));
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

    private void saveNoteToFirestore(String imageUrl, String title, String content) {
        Map<String, Object> noteData = new HashMap<>();
        noteData.put("userId", userId);
        noteData.put("title", title);
        noteData.put("content", content);
        noteData.put("time", new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));
        noteData.put("imageUrl", imageUrl);

        if (noteId != null) {
            // Update note đã có
            db.collection("notes").document(noteId)
                    .update(noteData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Cập nhật ghi chú thành công", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.run();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // Thêm note mới
            db.collection("notes").add(noteData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), "Thêm ghi chú thành công", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.run();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}

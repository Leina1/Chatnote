package com.example.chatnote;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatnote.Model.Message;
import com.example.chatnote.Adapter.ChatMessageAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;
import okio.BufferedSink;
import okio.Okio;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

public class ChatActivity extends AppCompatActivity {
    private String chatId, receiverId, currentUserId;
    private String receiverName, receiverAvatar; // Lưu avatar URL
    private RecyclerView recyclerView;
    private ChatMessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private EditText edtMessage;
    private ImageButton btnSend;
    private ImageView imgAvatar, imgAddImage, imgBack;
    private TextView txtUserName;

    private FirebaseFirestore db;
    private static final int PICK_IMAGE_REQUEST = 222;
    private Uri imageUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get data from intent
        chatId = getIntent().getStringExtra("chatId");
        receiverId = getIntent().getStringExtra("receiverId");
        receiverName = getIntent().getStringExtra("receiverName");
        receiverAvatar = getIntent().getStringExtra("receiverImage");

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewMessages);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        imgAvatar = findViewById(R.id.imgAvatar);
        txtUserName = findViewById(R.id.txtUserName);
        imgAddImage = findViewById(R.id.btnAddImage);
        imgBack = findViewById(R.id.imgBack);

        db = FirebaseFirestore.getInstance();

        // Setup adapter với avatar
        adapter = new ChatMessageAdapter(messageList, currentUserId, receiverAvatar);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Load user info vào header
        loadUserInfo(receiverId);

        // Load messages
        loadMessagesRealtime();

        checkBlockStatus();

        // Click listeners
        imgBack.setOnClickListener(v -> onBackPressed());

        btnSend.setOnClickListener(v -> {
            String content = edtMessage.getText().toString().trim();
            if (TextUtils.isEmpty(content)) return;

            Message msg = new Message(currentUserId, content, System.currentTimeMillis());

            db.collection("chats").document(chatId)
                    .collection("messages")
                    .add(msg)
                    .addOnSuccessListener(ref -> {
                        edtMessage.setText("");
                        db.collection("chats").document(chatId)
                                .update("lastMessage", content,
                                        "lastMessageTime", System.currentTimeMillis());
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi gửi tin nhắn", Toast.LENGTH_SHORT).show());
        });

        imgAddImage.setOnClickListener(v -> openImagePicker());
    }

    private void loadUserInfo(String userId) {
        if (userId == null) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String displayName = doc.getString("fullName");
                        String avatarUrl = doc.getString("profilePicUrl");

                        if (displayName != null) {
                            txtUserName.setText(displayName);
                            receiverName = displayName;
                        }

                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            receiverAvatar = avatarUrl;
                            // Dùng Glide để tránh ảnh bị xoay
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .circleCrop()
                                    .into(imgAvatar);

                            // Update adapter với avatar mới
                            adapter.updateReceiverAvatar(avatarUrl);
                        } else {
                            imgAvatar.setImageResource(R.drawable.ic_profile);
                        }
                    }
                });
    }

    private void loadMessagesRealtime() {
        db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    messageList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Message msg = doc.toObject(Message.class);
                        messageList.add(msg);
                    }
                    adapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(Math.max(0, messageList.size() - 1));
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            uploadImageToCloudinary(imageUri);
        }
    }

    private void sendImageMessage(String imageUrl) {
        String caption = edtMessage.getText().toString().trim();
        Message imgMsg = new Message(currentUserId, caption, imageUrl, System.currentTimeMillis());

        db.collection("chats").document(chatId)
                .collection("messages")
                .add(imgMsg)
                .addOnSuccessListener(ref -> {
                    edtMessage.setText("");
                    db.collection("chats").document(chatId)
                            .update("lastMessage", "[Đã gửi ảnh]",
                                    "lastMessageTime", System.currentTimeMillis());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi gửi ảnh", Toast.LENGTH_SHORT).show());
    }

    private Bitmap rotateImageIfRequired(Uri uri, Bitmap img) throws Exception {
        InputStream input = getContentResolver().openInputStream(uri);
        ExifInterface ei = new ExifInterface(input);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        input.close();

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return img;
        }
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    private void uploadImageToCloudinary(Uri uri) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
            Bitmap rotatedBitmap = rotateImageIfRequired(uri, bitmap);

            File tempFile = File.createTempFile("upload", ".jpg", getCacheDir());
            java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

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
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Lỗi upload: " + response.code(), Toast.LENGTH_SHORT).show());
                        return;
                    }
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        String imageUrl = jsonObject.getString("secure_url");
                        runOnUiThread(() -> sendImageMessage(imageUrl));
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Lỗi parse URL", Toast.LENGTH_SHORT).show());
                    }
                }
            });

        } catch (Exception e) {
            Toast.makeText(ChatActivity.this, "Lỗi xử lý file ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void checkBlockStatus() {
        db.collection("blocks")
                .whereIn("blockerId", Arrays.asList(currentUserId, receiverId))
                .whereIn("blockedId", Arrays.asList(currentUserId, receiverId))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean iBlocked = false;
                    boolean iAmBlocked = false;

                    for (DocumentSnapshot doc : querySnapshot) {
                        String blockerId = doc.getString("blockerId");
                        String blockedId = doc.getString("blockedId");

                        if (blockerId != null && blockedId != null) {
                            if (blockerId.equals(currentUserId) && blockedId.equals(receiverId)) {
                                iBlocked = true;
                            }
                            if (blockerId.equals(receiverId) && blockedId.equals(currentUserId)) {
                                iAmBlocked = true;
                            }
                        }
                    }

                    handleBlockUI(iBlocked, iAmBlocked);
                });
    }

    private void handleBlockUI(boolean iBlocked, boolean iAmBlocked) {
        if (iBlocked && iAmBlocked) {
            Toast.makeText(this, "Cả hai đã chặn nhau", Toast.LENGTH_LONG).show();
            finish();
        } else if (iAmBlocked) {
            edtMessage.setEnabled(false);
            edtMessage.setHint("Bạn đã bị chặn");
            btnSend.setEnabled(false);
        } else if (iBlocked) {
            edtMessage.setEnabled(false);
            edtMessage.setHint("Bạn đã chặn người này");
            btnSend.setEnabled(false);
        }
    }
}
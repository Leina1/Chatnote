package com.example.chatnote;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText editTextFullName, editTextEmailRegister,
            editTextPasswordRegister, editTextConfirmPassword;
    private Button btnRegister;
    private TextView textBackToLogin;
    private ProgressBar progressBar; // Thêm progress bar
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase với error handling
        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error: " + e.getMessage());
            Toast.makeText(this, "Lỗi khởi tạo Firebase", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize views
        initViews();

        // Set click listeners
        setClickListeners();
    }

    private void initViews() {
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextEmailRegister = findViewById(R.id.editTextEmailRegister);
        editTextPasswordRegister = findViewById(R.id.editTextPasswordRegister);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        textBackToLogin = findViewById(R.id.textBackToLogin);

        // Nếu có progress bar trong layout
        // progressBar = findViewById(R.id.progressBar);
    }

    private void setClickListeners() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        textBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển về màn hình đăng nhập
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void registerUser() {
        String fullName = editTextFullName.getText().toString().trim();
        String email = editTextEmailRegister.getText().toString().trim();
        String password = editTextPasswordRegister.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Validate input
        if (!validateInput(fullName, email, password, confirmPassword)) {
            return;
        }

        // Show loading
        showLoading(true);

        // Create user with Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Lưu thông tin user vào Firestore
                            saveUserToFirestore(user, fullName, email);
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        showLoading(false);

                        // Xử lý các lỗi cụ thể
                        String errorMessage = "Đăng ký thất bại";
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            if (error.contains("email address is already in use")) {
                                errorMessage = "Email này đã được sử dụng";
                            } else if (error.contains("email address is badly formatted")) {
                                errorMessage = "Email không hợp lệ";
                            } else if (error.contains("weak password")) {
                                errorMessage = "Mật khẩu quá yếu";
                            }
                        }
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInput(String fullName, String email,
                                  String password, String confirmPassword) {
        if (TextUtils.isEmpty(fullName)) {
            editTextFullName.setError("Vui lòng nhập họ và tên");
            editTextFullName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmailRegister.setError("Vui lòng nhập email");
            editTextEmailRegister.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmailRegister.setError("Email không hợp lệ");
            editTextEmailRegister.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPasswordRegister.setError("Vui lòng nhập mật khẩu");
            editTextPasswordRegister.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            editTextPasswordRegister.setError("Mật khẩu phải có ít nhất 6 ký tự");
            editTextPasswordRegister.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Mật khẩu không khớp");
            editTextConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void saveUserToFirestore(FirebaseUser user, String fullName, String email) {
        String userId = user.getUid();
        Log.d(TAG, "Saving user to Firestore with ID: " + userId);

        // Create user data
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("fullName", fullName);
        userData.put("email", email);
        userData.put("profilePicUrl", "");
        userData.put("bio", "");
        userData.put("phone", "");
        userData.put("status", "online");
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("lastActive", System.currentTimeMillis());
        userData.put("isVerified", false);

        // Save to Firestore
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data saved successfully");
                    // Send verification email
                    sendVerificationEmail(user);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user data: " + e.getMessage());
                    showLoading(false);

                    // Xử lý lỗi cụ thể
                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        Toast.makeText(RegisterActivity.this,
                                "Lỗi quyền truy cập Firestore. Vui lòng kiểm tra Security Rules",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Lỗi lưu thông tin: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Verification email sent");
                        Toast.makeText(RegisterActivity.this,
                                "Đăng ký thành công! Vui lòng kiểm tra email để xác minh tài khoản.",
                                Toast.LENGTH_LONG).show();

                        // Chuyển về màn hình đăng nhập
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.putExtra("email", user.getEmail());
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "Failed to send verification email", task.getException());
                        Toast.makeText(RegisterActivity.this,
                                "Đăng ký thành công nhưng không gửi được email xác minh",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            btnRegister.setEnabled(false);
            btnRegister.setText("Đang đăng ký...");
            // Nếu có progress bar
            // progressBar.setVisibility(View.VISIBLE);
        } else {
            btnRegister.setEnabled(true);
            btnRegister.setText("Đăng ký");
            // progressBar.setVisibility(View.GONE);
        }
    }
}
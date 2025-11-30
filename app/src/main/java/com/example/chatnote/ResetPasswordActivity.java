package com.example.chatnote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText edtOtp, edtNewPassword, edtConfirmPassword;
    private Button btnConfirm;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String userEmail;  // Email người dùng đã nhập ở LoginActivity/Quên MK
    private String generatedOtp; // OTP tạm thời

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        edtOtp = findViewById(R.id.edtOtp);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnConfirm = findViewById(R.id.btnConfirm);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Lấy email từ intent
        userEmail = getIntent().getStringExtra("email");
        if (userEmail == null) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Gửi OTP về email (mô phỏng)
        sendOtpToEmail(userEmail);

        btnConfirm.setOnClickListener(v -> {
            String otpInput = edtOtp.getText().toString().trim();
            String newPass = edtNewPassword.getText().toString().trim();
            String confirmPass = edtConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(otpInput) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!otpInput.equals(generatedOtp)) {
                Toast.makeText(this, "OTP không đúng", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "Mật khẩu mới không trùng khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cập nhật mật khẩu mới
            resetPassword(userEmail, newPass);
        });
    }

    private void sendOtpToEmail(String email) {
        // Tạo OTP 6 số
        generatedOtp = String.format("%06d", new Random().nextInt(999999));

        // TODO: Gửi email bằng SMTP/SendGrid/Firebase Function
        // Ở đây minh họa bằng Toast
        Toast.makeText(this, "OTP đã gửi đến email: " + generatedOtp, Toast.LENGTH_LONG).show();
    }

    private void resetPassword(String email, String newPassword) {
        // Firebase Auth chỉ reset password khi user đăng nhập hoặc bằng email link
        // Cách đơn giản: Tạm đăng nhập bằng email link hoặc yêu cầu đăng nhập lại, ở đây mình giả lập
        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Cập nhật password
                mAuth.signInWithEmailAndPassword(email, "tempPassword123").addOnCompleteListener(signInTask -> {
                    if (signInTask.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    Toast.makeText(ResetPasswordActivity.this,
                                            "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                                    finish(); // Quay về LoginActivity
                                } else {
                                    Toast.makeText(ResetPasswordActivity.this,
                                            "Lỗi cập nhật mật khẩu: " + updateTask.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(ResetPasswordActivity.this,
                                "Lỗi xác thực tạm thời: " + signInTask.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Email không tồn tại", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

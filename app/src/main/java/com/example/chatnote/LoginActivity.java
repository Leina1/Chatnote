package com.example.chatnote;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // Views
    private EditText editTextEmail, editTextPassword;
    private Button btnLogin, btnGoogleSignIn;
    private TextView tvForgotPassword;
    private ImageView imageViewRegister;
    private TextView textRegister;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // KHÔNG check currentUser ở đây nữa để tránh loop
        // Chỉ check sau khi đăng nhập thành công

        // Configure Google Sign-In
        configureGoogleSignIn();

        // Initialize views
        initViews();

        // Set click listeners
        setClickListeners();

        // Get email from RegisterActivity if exists
        String email = getIntent().getStringExtra("email");
        if (email != null) {
            editTextEmail.setText(email);
        }
    }

    private void initViews() {
        editTextEmail = findViewById(R.id.editTextTextEmailAddress);
        editTextPassword = findViewById(R.id.editTextNumberPassword);
        btnLogin = findViewById(R.id.bntlogin);
        btnGoogleSignIn = findViewById(R.id.google_signIn);
        tvForgotPassword = findViewById(R.id.TvforgotPassword);
        imageViewRegister = findViewById(R.id.imageViewRegister);
        textRegister = findViewById(R.id.textRegister);
    }

    private void configureGoogleSignIn() {
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Lấy từ google-services.json
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize ActivityResultLauncher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        Log.d(TAG, "Google sign-in cancelled");
                        Toast.makeText(this, "Đăng nhập Google bị hủy", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setClickListeners() {
        // Login button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginWithEmail();
            }
        });

        // Google Sign-In button
        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        // Register link
        View.OnClickListener registerListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        };

        imageViewRegister.setOnClickListener(registerListener);
        textRegister.setOnClickListener(registerListener);

        // Forgot password
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleForgotPassword();
            }
        });
    }

    private void loginWithEmail() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Vui lòng nhập email");
            editTextEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Email không hợp lệ");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Vui lòng nhập mật khẩu");
            editTextPassword.requestFocus();
            return;
        }

        // Show loading
        showLoading(true);

        // Sign in with Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        Toast.makeText(LoginActivity.this,
                                "Đăng nhập thành công!",
                                Toast.LENGTH_SHORT).show();

                        navigateToMainActivity();
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());

                        String errorMessage = "Đăng nhập thất bại";
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            if (error.contains("no user record")) {
                                errorMessage = "Email không tồn tại";
                            } else if (error.contains("password is invalid")) {
                                errorMessage = "Mật khẩu không chính xác";
                            } else if (error.contains("user has been disabled")) {
                                errorMessage = "Tài khoản đã bị vô hiệu hóa";
                            }
                        }

                        Toast.makeText(LoginActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signInWithGoogle() {
        // Sign out first to show account chooser
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.d(TAG, "Google sign-in successful, ID Token: " + account.getIdToken());

            // Authenticate with Firebase
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Log.e(TAG, "Google sign-in failed, error code: " + e.getStatusCode());
            Toast.makeText(this, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        showLoading(true);

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Check if user exists in Firestore
                        if (user != null) {
                            checkAndCreateUserInFirestore(user);
                        }
                    } else {
                        showLoading(false);
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(LoginActivity.this,
                                "Xác thực thất bại",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndCreateUserInFirestore(FirebaseUser user) {
        String userId = user.getUid();
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            showLoading(false);

            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && !document.exists()) {
                    // New user - create document in Firestore
                    createNewGoogleUser(userRef, user);
                } else {
                    // Existing user - navigate to MainActivity
                    Toast.makeText(LoginActivity.this,
                            "Chào mừng trở lại!",
                            Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                }
            } else {
                Log.w(TAG, "Error checking user in Firestore", task.getException());
                Toast.makeText(LoginActivity.this,
                        "Lỗi kiểm tra thông tin người dùng",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNewGoogleUser(DocumentReference userRef, FirebaseUser user) {
        Map<String, Object> newUser = new HashMap<>();
        newUser.put("userId", user.getUid());
        newUser.put("fullName", user.getDisplayName());
        newUser.put("email", user.getEmail());
        newUser.put("profilePicUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        newUser.put("bio", "");
        newUser.put("phone", "");
        newUser.put("status", "online");
        newUser.put("createdAt", System.currentTimeMillis());
        newUser.put("lastActive", System.currentTimeMillis());
        newUser.put("isVerified", true); // Google already verified

        userRef.set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New Google user created in Firestore");
                    Toast.makeText(LoginActivity.this,
                            "Tạo tài khoản thành công!",
                            Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating new user in Firestore", e);
                    Toast.makeText(LoginActivity.this,
                            "Lỗi tạo tài khoản",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void handleForgotPassword() {
        String email = editTextEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Nhập email để khôi phục mật khẩu");
            editTextEmail.requestFocus();
            return;
        }

        // Kiểm tra email đã được tạo chưa
        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean isRegistered = task.getResult().getSignInMethods() != null &&
                        !task.getResult().getSignInMethods().isEmpty();
                if (isRegistered) {
                    // Email tồn tại => gửi email reset password hoặc OTP
                    sendResetEmail(email);
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Email này chưa được đăng ký",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(LoginActivity.this,
                        "Lỗi kiểm tra email: " + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hàm gửi email reset password
    private void sendResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Email khôi phục đã được gửi!",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Lỗi gửi email khôi phục: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            btnLogin.setEnabled(false);
            btnLogin.setText("Đang đăng nhập...");
            btnGoogleSignIn.setEnabled(false);
        } else {
            btnLogin.setEnabled(true);
            btnLogin.setText("Đăng nhập");
            btnGoogleSignIn.setEnabled(true);
        }
    }
}
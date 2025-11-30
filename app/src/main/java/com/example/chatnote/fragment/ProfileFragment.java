package com.example.chatnote.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.chatnote.LoginActivity;
import com.example.chatnote.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Source;

public class ProfileFragment extends Fragment {

    private String userId;
    private ImageView profileImage;
    private TextView displayName, email, bio;
    private Button btnEditProfile, btnLogout;

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Lấy userId từ Bundle nếu có, nếu không dùng user hiện tại
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }

        // Ánh xạ view
        profileImage = view.findViewById(R.id.profileImage);
        displayName = view.findViewById(R.id.displayName);
        email = view.findViewById(R.id.email);
        bio = view.findViewById(R.id.bio);
        btnEditProfile = view.findViewById(R.id.editProfileBtn);
        btnLogout = view.findViewById(R.id.btnLogout);

        db = FirebaseFirestore.getInstance();

        loadUserInfo();

        // Nút chỉnh sửa hồ sơ
        btnEditProfile.setOnClickListener(v -> {
            EditProfileFragment editProfileFragment = new EditProfileFragment();
            Bundle bundle = new Bundle();
            bundle.putString("userId", userId);
            editProfileFragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, editProfileFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Logout
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show();

            // Mở LoginActivity
            startActivity(new Intent(getActivity(), LoginActivity.class));
            requireActivity().finish();
        });

        return view;
    }

    private void loadUserInfo() {
        if (userId == null) {
            Toast.makeText(getContext(), "Không tìm thấy userId", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference docRef = db.collection("users").document(userId);

        // Bật cache Firestore (nếu chưa bật ở app)
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        // Lấy dữ liệu từ cache trước, nếu có
        docRef.get(Source.CACHE).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && isAdded()) {
                updateProfileUI(documentSnapshot);
            }
        }).addOnFailureListener(e -> {
            // Nếu cache không có, fetch từ server
            docRef.get(Source.SERVER).addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && isAdded()) {
                    updateProfileUI(documentSnapshot);
                }
            }).addOnFailureListener(ex -> {
                Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        // Lắng nghe realtime update
        docRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null || documentSnapshot == null || !documentSnapshot.exists() || !isAdded()) return;
            updateProfileUI(documentSnapshot);
        });
    }

    // Hàm helper cập nhật UI
    private void updateProfileUI(DocumentSnapshot documentSnapshot) {
        String fullName = documentSnapshot.getString("fullName");
        String emailStr = documentSnapshot.getString("email");
        String profilePicUrl = documentSnapshot.getString("profilePicUrl");
        String bioStr = documentSnapshot.getString("bio");

        displayName.setText(fullName != null ? fullName : "(Chưa có tên)");
        email.setText(emailStr != null ? emailStr : "(Chưa có email)");
        bio.setText(bioStr != null ? bioStr : "Chưa có giới thiệu.");

        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(profilePicUrl)
                    .thumbnail(0.1f) // load trước hình nhỏ
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_placeholder);
        }
    }
}

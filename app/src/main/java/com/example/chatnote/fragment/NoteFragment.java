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

import com.example.chatnote.Adapter.NoteAdapter;
import com.example.chatnote.Model.ModelNote;
import com.example.chatnote.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class NoteFragment extends Fragment {

    private String userId;
    private RecyclerView recyclerNotes;
    private NoteAdapter noteAdapter;
    private List<ModelNote> noteList;
    private FirebaseFirestore db;
    private ImageButton btnAddNote;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note, container, false);

        recyclerNotes = view.findViewById(R.id.recyclerNotes);
        btnAddNote = view.findViewById(R.id.btnAddNote);

        recyclerNotes.setLayoutManager(new LinearLayoutManager(getContext()));
        noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(noteList);
        recyclerNotes.setAdapter(noteAdapter);

        db = FirebaseFirestore.getInstance();

        // Nhận userId từ bundle hoặc fallback về FirebaseAuth
        if (getArguments() != null && getArguments().containsKey("userId")) {
            userId = getArguments().getString("userId");
        } else {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) userId = currentUser.getUid();
        }

        loadUserNotes();

        btnAddNote.setOnClickListener(v -> {
            AddnoteFragment addnoteFragment = new AddnoteFragment();
            addnoteFragment.setListener(this::loadUserNotes);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, addnoteFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }


    public void loadUserNotes() {
        if (userId == null) return;

        db.collection("notes")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    noteList.clear();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String title = doc.getString("title");
                            String content = doc.getString("content");
                            String imageUrl = doc.getString("imageUrl");
                            String time = doc.getString("time");
                            noteList.add(new ModelNote(doc.getId(), title, content, imageUrl, time));
                        }
                    }

                    noteAdapter.notifyDataSetChanged();

                    if (noteList.isEmpty()) {
                        Toast.makeText(getContext(), "Chưa có ghi chú nào", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

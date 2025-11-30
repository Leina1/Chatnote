package com.example.chatnote.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatnote.Model.ModelNote;
import com.example.chatnote.R;
import com.example.chatnote.fragment.AddnoteFragment;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final List<ModelNote> noteList;

    public NoteAdapter(List<ModelNote> noteList) {
        this.noteList = noteList;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        ModelNote note = noteList.get(position);
        holder.tvTitle.setText(note.getTitle());
        holder.tvContent.setText(note.getContent());
        holder.tvTime.setText(note.getTime());

        if (note.getImageUrl() != null && !note.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(note.getImageUrl())
                    .into(holder.imageNote);
        } else {
            holder.imageNote.setImageResource(R.drawable.pic);
        }

        // Xử lý Edit
        holder.btnEdit.setOnClickListener(v -> {
            // Mở AddnoteFragment để chỉnh sửa
            AddnoteFragment editFragment = AddnoteFragment.newInstance(
                    note.getId(), note.getTitle(), note.getContent(), note.getImageUrl()
            );
            if (holder.itemView.getContext() instanceof AppCompatActivity) {
                ((AppCompatActivity) holder.itemView.getContext())
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, editFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return; // safety check
            if (adapterPosition >= noteList.size()) return; // thêm check extra

            ModelNote noteToDelete = noteList.get(adapterPosition);

            FirebaseFirestore.getInstance().collection("notes")
                    .document(noteToDelete.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Lại kiểm tra list trước khi remove
                        if (adapterPosition < noteList.size()) {
                            noteList.remove(adapterPosition);
                            notifyItemRemoved(adapterPosition);
                            notifyItemRangeChanged(adapterPosition, noteList.size());
                            Toast.makeText(holder.itemView.getContext(), "Đã xoá ghi chú", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(holder.itemView.getContext(), "Xoá thất bại", Toast.LENGTH_SHORT).show()
                    );
        });

    }


    @Override
    public int getItemCount() {
        return noteList.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTime;
        ImageView imageNote;
        ImageButton btnEdit, btnDelete;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.text_name);
            tvContent = itemView.findViewById(R.id.text_description);
            tvTime = itemView.findViewById(R.id.text_time);
            imageNote = itemView.findViewById(R.id.image_product);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

package com.example.smartgrow.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgrow.R;
import com.example.smartgrow.core.SharedPrefManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArchiveFragment extends Fragment implements CommunityPostAdapter.OnPostInteractionListener {

    private RecyclerView rvArchive;
    private CommunityPostAdapter adapter;
    private List<CommunityPostModel> postList;
    private TextView tvEmpty;
    private DatabaseReference postsRef;
    private String currentUsername;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUsername = SharedPrefManager.getInstance(requireContext()).getUsername();
        postsRef = FirebaseDatabase.getInstance().getReference("posts");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_archive, container, false);

        rvArchive = view.findViewById(R.id.rv_archived_posts);
        tvEmpty = view.findViewById(R.id.tv_empty);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        setupRecyclerView();
        fetchArchivedPosts();

        return view;
    }

    private void setupRecyclerView() {
        postList = new ArrayList<>();
        adapter = new CommunityPostAdapter(postList, currentUsername, this);
        rvArchive.setLayoutManager(new LinearLayoutManager(getContext()));
        rvArchive.setAdapter(adapter);
    }

    private void fetchArchivedPosts() {
        postsRef.orderByChild("username").equalTo(currentUsername).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                postList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    CommunityPostModel post = snap.getValue(CommunityPostModel.class);
                    if (post != null && post.isArchived()) {
                        post.setPostId(snap.getKey());
                        postList.add(post);
                    }
                }
                Collections.sort(postList, (p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()));
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(postList.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onMoreClick(View v, CommunityPostModel post) {
        BottomSheetDialog optionsSheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_post_options_sheet, null);
        optionsSheet.setContentView(sheetView);

        sheetView.findViewById(R.id.layout_owner_options).setVisibility(View.VISIBLE);
        sheetView.findViewById(R.id.layout_other_user_options).setVisibility(View.GONE);
        
        TextView label = (TextView) ((ViewGroup)sheetView.findViewById(R.id.item_archive_post)).getChildAt(1);
        if (label != null) label.setText("Restore to Feed");

        sheetView.findViewById(R.id.item_archive_post).setOnClickListener(view -> {
            optionsSheet.dismiss();
            postsRef.child(post.getPostId()).child("archived").setValue(false).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Post restored to Community! 🌿", Toast.LENGTH_SHORT).show();
            });
        });

        sheetView.findViewById(R.id.item_move_trash).setOnClickListener(view -> {
            optionsSheet.dismiss();
            postsRef.child(post.getPostId()).removeValue();
            Toast.makeText(getContext(), "Post permanently deleted.", Toast.LENGTH_SHORT).show();
        });

        sheetView.findViewById(R.id.item_edit_post).setVisibility(View.GONE);

        optionsSheet.show();
    }

    @Override public void onLikeClick(CommunityPostModel post) {}
    @Override public void onCommentClick(CommunityPostModel post) {}
    @Override public void onUserClick(String username) {}
}

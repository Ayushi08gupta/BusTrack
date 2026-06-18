package com.college.bustrack.fragments;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.college.bustrack.R;
import com.college.bustrack.models.User;
import com.google.android.material.chip.Chip;
import java.util.List;

public class UserManagementAdapter extends RecyclerView.Adapter<UserManagementAdapter.UserViewHolder> {

    private List<User> userList;
    private UserActionListener listener;

    public interface UserActionListener {
        void onSelectionChanged();
        void onDelete(User user);
    }

    public UserManagementAdapter(List<User> userList, UserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_management, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvName.setText(user.getName());
        holder.tvEmail.setText(user.getEmail());
        
        String role = user.getRole() != null ? user.getRole().toUpperCase() : "STUDENT";
        holder.chipRole.setText(role);
        
        if ("DRIVER".equals(role)) {
            holder.viewRoleIndicator.setBackgroundColor(Color.parseColor("#FF9800")); // Orange for Driver
            holder.chipRole.setChipBackgroundColorResource(android.R.color.holo_orange_light);
        } else {
            holder.viewRoleIndicator.setBackgroundColor(Color.parseColor("#3F51B5")); // Blue for Student
            holder.chipRole.setChipBackgroundColorResource(android.R.color.holo_blue_light);
        }

        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(user.isSelected());
        
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            user.setSelected(isChecked);
            listener.onSelectionChanged();
        });

        holder.btnDelete.setOnClickListener(v -> listener.onDelete(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;
        Chip chipRole;
        CheckBox cbSelect;
        View viewRoleIndicator;
        ImageButton btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            chipRole = itemView.findViewById(R.id.chipRole);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            viewRoleIndicator = itemView.findViewById(R.id.viewRoleIndicator);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

package com.college.bustrack;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.college.bustrack.models.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onAction(User user, String action);
    }

    private List<User> users;
    private OnUserActionListener listener;

    public UserAdapter(List<User> users, OnUserActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    public void updateUsers(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        
        // Null-safe name and email
        holder.tvName.setText(user.getName() != null ? user.getName() : "Unknown");
        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "No Email");
        
        // Null-safe role
        String role = user.getRole();
        holder.tvRole.setText(role != null ? role.toUpperCase() : "STUDENT");

        // Status styling
        if (user.isActive()) {
            holder.btnStatus.setImageResource(android.R.drawable.ic_menu_view);
            holder.btnStatus.setColorFilter(Color.parseColor("#4CAF50"));
        } else {
            holder.btnStatus.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            holder.btnStatus.setColorFilter(Color.RED);
        }

        holder.btnStatus.setOnClickListener(v -> {
            if (listener != null) listener.onAction(user, "status");
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onAction(user, "delete");
        });
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole;
        ImageButton btnStatus, btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            tvRole = itemView.findViewById(R.id.tvUserRole);
            btnStatus = itemView.findViewById(R.id.btnToggleStatus);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
        }
    }
}

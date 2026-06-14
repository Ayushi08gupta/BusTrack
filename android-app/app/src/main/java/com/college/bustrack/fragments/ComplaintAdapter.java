package com.college.bustrack.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.college.bustrack.R;
import com.college.bustrack.models.Complaint;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import java.util.List;

public class ComplaintAdapter extends RecyclerView.Adapter<ComplaintAdapter.ComplaintViewHolder> {

    private List<Complaint> complaintList;
    private ComplaintActionListener listener;

    public interface ComplaintActionListener {
        void onAction(Complaint complaint);
    }

    public ComplaintAdapter(List<Complaint> complaintList, ComplaintActionListener listener) {
        this.complaintList = complaintList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ComplaintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_complaint, parent, false);
        return new ComplaintViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComplaintViewHolder holder, int position) {
        Complaint complaint = complaintList.get(position);
        holder.tvType.setText(complaint.getType());
        holder.tvDescription.setText(complaint.getDescription());
        holder.tvStudentName.setText("By: " + (complaint.getStudentName() != null ? complaint.getStudentName() : "Unknown"));
        
        holder.chipStatus.setText(complaint.getStatus().toUpperCase());
        
        // Color coding status
        switch (complaint.getStatus().toLowerCase()) {
            case "pending":
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light);
                break;
            case "in-progress":
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_blue_light);
                break;
            case "resolved":
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
                break;
        }

        holder.btnAction.setOnClickListener(v -> listener.onAction(complaint));
    }

    @Override
    public int getItemCount() {
        return complaintList.size();
    }

    public void updateData(List<Complaint> newList) {
        this.complaintList = newList;
        notifyDataSetChanged();
    }

    static class ComplaintViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDescription, tvStudentName;
        Chip chipStatus;
        MaterialButton btnAction;

        public ComplaintViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvComplaintType);
            tvDescription = itemView.findViewById(R.id.tvComplaintDescription);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            btnAction = itemView.findViewById(R.id.btnTakeAction);
        }
    }
}

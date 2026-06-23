package com.college.bustrack.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.college.bustrack.R;
import com.college.bustrack.models.Bus;
import com.google.android.material.chip.Chip;
import java.util.List;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder> {

    public interface OnAssignmentActionListener {
        void onDelete(Bus bus);
    }

    private List<Bus> busList;
    private OnAssignmentActionListener listener;

    public AssignmentAdapter(List<Bus> busList) {
        this.busList = busList;
    }

    public void setOnAssignmentActionListener(OnAssignmentActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assignment, parent, false);
        return new AssignmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
        Bus bus = busList.get(position);
        holder.tvBusNumber.setText("Bus #" + bus.getBusNumber());
        
        if (bus.getVehicleNumber() != null) {
            holder.tvBusNumber.append(" (" + bus.getVehicleNumber() + ")");
        }
        
        if (bus.getRouteId() != null) {
            holder.tvRouteName.setText(bus.getRouteId().getRouteName() != null ? bus.getRouteId().getRouteName() : "Unnamed Route");
            if (bus.getRouteId().getStops() != null && !bus.getRouteId().getStops().isEmpty()) {
                holder.tvNextStop.setText("Starts at: " + bus.getRouteId().getStops().get(0).getName());
            } else {
                holder.tvNextStop.setText("No stops defined");
            }
        } else {
            holder.tvRouteName.setText("No route designed");
            holder.tvNextStop.setText("--");
        }

        if (bus.getDriverId() != null) {
            holder.tvDriverName.setText("Driver: " + bus.getDriverId().getName());
        } else {
            holder.tvDriverName.setText("Driver: Unassigned");
        }

        boolean isActive = "active".equalsIgnoreCase(bus.getStatus());
        holder.chipStatus.setText(isActive ? "LIVE" : "IDLE");
        holder.chipStatus.setChipBackgroundColorResource(isActive ? android.R.color.holo_green_dark : android.R.color.darker_gray);

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(bus);
        });
    }

    @Override
    public int getItemCount() {
        return busList != null ? busList.size() : 0;
    }

    public void updateData(List<Bus> newList) {
        this.busList = newList;
        notifyDataSetChanged();
    }

    static class AssignmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvBusNumber, tvRouteName, tvDriverName, tvNextStop;
        Chip chipStatus;
        View btnDelete;

        public AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBusNumber = itemView.findViewById(R.id.tvBusNumber);
            tvRouteName = itemView.findViewById(R.id.tvRouteName);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvNextStop = itemView.findViewById(R.id.tvNextStop);
            chipStatus = itemView.findViewById(R.id.chipRouteStatus);
            btnDelete = itemView.findViewById(R.id.btnDeleteAssignment);
        }
    }
}

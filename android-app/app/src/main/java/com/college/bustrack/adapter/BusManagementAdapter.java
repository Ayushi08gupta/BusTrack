package com.college.bustrack.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.college.bustrack.R;
import com.college.bustrack.models.Bus;
import java.util.List;

public class BusManagementAdapter extends RecyclerView.Adapter<BusManagementAdapter.BusViewHolder> {

    private List<Bus> buses;
    private OnBusActionListener listener;

    public interface OnBusActionListener {
        void onDelete(Bus bus);
    }

    public BusManagementAdapter(List<Bus> buses, OnBusActionListener listener) {
        this.buses = buses;
        this.listener = listener;
    }

    public void updateData(List<Bus> newList) {
        this.buses = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bus_management, parent, false);
        return new BusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BusViewHolder holder, int position) {
        Bus bus = buses.get(position);
        holder.tvBusNumber.setText("Bus #" + bus.getBusNumber());
        
        String status = bus.getStatus() != null ? bus.getStatus() : "Inactive";
        holder.tvBusStatus.setText("Status: " + status.toUpperCase());
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(bus);
        });
    }

    @Override
    public int getItemCount() {
        return buses != null ? buses.size() : 0;
    }

    static class BusViewHolder extends RecyclerView.ViewHolder {
        TextView tvBusNumber, tvBusStatus;
        ImageButton btnDelete;

        public BusViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBusNumber = itemView.findViewById(R.id.tvBusNumber);
            tvBusStatus = itemView.findViewById(R.id.tvBusStatus);
            btnDelete = itemView.findViewById(R.id.btnDeleteBus);
        }
    }
}

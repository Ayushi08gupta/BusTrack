package com.college.bustrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.college.bustrack.models.Bus;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class BusAdapter extends RecyclerView.Adapter<BusAdapter.BusViewHolder> {

    private List<Bus> busList;
    private final OnBusClickListener listener;

    public interface OnBusClickListener {
        void onBusClick(Bus bus);
    }

    public BusAdapter(List<Bus> busList, OnBusClickListener listener) {
        this.busList = busList;
        this.listener = listener;
    }

    public void updateBuses(List<Bus> newBuses) {
        this.busList = newBuses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bus_card, parent, false);
        return new BusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BusViewHolder holder, int position) {
        Bus bus = busList.get(position);
        holder.bind(bus, listener);
    }

    @Override
    public int getItemCount() {
        return busList.size();
    }

    static class BusViewHolder extends RecyclerView.ViewHolder {
        TextView tvBusNumber, tvStatus, tvRoutePath, tvDriverName, tvLastUpdated;
        MaterialCardView statusChipCard;
        MaterialButton btnTrackNow;

        public BusViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBusNumber = itemView.findViewById(R.id.tvBusNumber);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvRoutePath = itemView.findViewById(R.id.tvRoutePath);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvLastUpdated = itemView.findViewById(R.id.tvLastUpdated);
            statusChipCard = itemView.findViewById(R.id.statusChipCard);
            btnTrackNow = itemView.findViewById(R.id.btnTrackNow);
        }

        public void bind(Bus bus, OnBusClickListener listener) {
            tvBusNumber.setText(bus.getBusNumber());
            
            boolean isActive = "active".equalsIgnoreCase(bus.getStatus());
            tvStatus.setText(isActive ? "🟢 LIVE" : "⚪ OFFLINE");
            
            if (bus.getRouteId() != null) {
                tvRoutePath.setText(bus.getRouteId().getRouteName());
            } else {
                tvRoutePath.setText("Route not assigned");
            }

            tvDriverName.setText("Driver: " + (bus.getDriverId() != null ? bus.getDriverId().getName() : "N/A"));
            
            // For now, static update text as per principle 2
            tvLastUpdated.setText("Updated just now");

            btnTrackNow.setOnClickListener(v -> listener.onBusClick(bus));
            itemView.setOnClickListener(v -> listener.onBusClick(bus));
        }
    }
}
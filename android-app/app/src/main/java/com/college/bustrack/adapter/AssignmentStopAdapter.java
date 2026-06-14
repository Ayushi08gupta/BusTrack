package com.college.bustrack.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.college.bustrack.R;
import com.college.bustrack.models.Stop;
import java.util.List;

public class AssignmentStopAdapter extends RecyclerView.Adapter<AssignmentStopAdapter.ViewHolder> {

    private List<Stop> stops;
    private OnStopActionListener listener;

    public interface OnStopActionListener {
        void onRemove(int position);
        void onSetTime(int position);
    }

    public AssignmentStopAdapter(List<Stop> stops, OnStopActionListener listener) {
        this.stops = stops;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assignment_stop, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Stop stop = stops.get(position);
        holder.tvStopOrder.setText(String.valueOf(position + 1));
        holder.tvStopName.setText(stop.getName());
        holder.tvArrivalTime.setText(stop.getEta() != null ? stop.getEta() : "Set Arrival Time");

        holder.itemView.setOnClickListener(v -> listener.onSetTime(position));
        holder.btnRemove.setOnClickListener(v -> listener.onRemove(position));
    }

    @Override
    public int getItemCount() {
        return stops.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStopOrder, tvStopName, tvArrivalTime;
        ImageButton btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStopOrder = itemView.findViewById(R.id.tvStopOrder);
            tvStopName = itemView.findViewById(R.id.tvStopName);
            tvArrivalTime = itemView.findViewById(R.id.tvArrivalTime);
            btnRemove = itemView.findViewById(R.id.btnRemoveStop);
        }
    }
}

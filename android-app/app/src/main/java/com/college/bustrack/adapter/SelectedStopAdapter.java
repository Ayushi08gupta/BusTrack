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

public class SelectedStopAdapter extends RecyclerView.Adapter<SelectedStopAdapter.StopViewHolder> {

    private List<Stop> stops;
    private OnStopRemoveListener listener;

    public interface OnStopRemoveListener {
        void onRemove(int position);
    }

    public SelectedStopAdapter(List<Stop> stops, OnStopRemoveListener listener) {
        this.stops = stops;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_stop, parent, false);
        return new StopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StopViewHolder holder, int position) {
        Stop stop = stops.get(position);
        holder.tvStopOrder.setText(String.valueOf(position + 1));
        holder.tvStopName.setText(stop.getName());
        holder.btnRemoveStop.setOnClickListener(v -> listener.onRemove(position));
    }

    @Override
    public int getItemCount() {
        return stops.size();
    }

    static class StopViewHolder extends RecyclerView.ViewHolder {
        TextView tvStopOrder, tvStopName;
        ImageButton btnRemoveStop;

        public StopViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStopOrder = itemView.findViewById(R.id.tvStopOrder);
            tvStopName = itemView.findViewById(R.id.tvStopName);
            btnRemoveStop = itemView.findViewById(R.id.btnRemoveStop);
        }
    }
}

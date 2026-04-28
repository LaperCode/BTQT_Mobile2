package com.example.ex_2_the_weather_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WeatherForecastAdapter extends RecyclerView.Adapter<WeatherForecastAdapter.ViewHolder> {
    private final List<WeatherForecastItem> items = new ArrayList<>();
    private final boolean horizontal;

    public WeatherForecastAdapter(boolean horizontal) {
        this.horizontal = horizontal;
    }

    public void setItems(List<WeatherForecastItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forecast, parent, false);
        if (!horizontal) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            view.setLayoutParams(params);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeatherForecastItem item = items.get(position);
        holder.icon.setText(WeatherIconUtils.emojiFor(item.getIconCode(), item.getDescription()));
        holder.label.setText(item.getLabel());
        holder.value.setText(item.getValue());
        holder.description.setText(item.getDescription());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView icon;
        final TextView label;
        final TextView value;
        final TextView description;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.tvIcon);
            label = itemView.findViewById(R.id.tvLabel);
            value = itemView.findViewById(R.id.tvValue);
            description = itemView.findViewById(R.id.tvDescription);
        }
    }
}
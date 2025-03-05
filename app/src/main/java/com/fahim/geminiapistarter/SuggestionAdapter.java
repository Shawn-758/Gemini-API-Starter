package com.fahim.geminiapistarter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder> {

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String suggestion);
    }

    private List<String> suggestions;
    private OnSuggestionClickListener listener;

    public SuggestionAdapter(List<String> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.suggestion_item, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        String suggestion = suggestions.get(position);
        holder.bind(suggestion);
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    class SuggestionViewHolder extends RecyclerView.ViewHolder {
        TextView suggestionTextView;

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            suggestionTextView = itemView.findViewById(R.id.suggestionTextView);
        }

        public void bind(String suggestion) {
            suggestionTextView.setText(suggestion);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSuggestionClick(suggestion);
                }
            });
        }
    }
}
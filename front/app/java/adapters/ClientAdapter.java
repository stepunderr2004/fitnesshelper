package com.example.fitnesshelper.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.db.User;

import java.util.List;

public class ClientAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<ClientItem> items;
    private final OnClientActionListener assignListener;
    private final OnClientActionListener unassignListener;

    public interface OnClientActionListener {
        void onAction(User user);
    }

    public ClientAdapter(List<ClientItem> items, OnClientActionListener assignListener, OnClientActionListener unassignListener) {
        this.items = items;
        this.assignListener = assignListener;
        this.unassignListener = unassignListener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client, parent, false);
            return new ClientViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ClientItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).titleText.setText(item.headerTitle);
        } else if (holder instanceof ClientViewHolder) {
            User user = item.user;
            ClientViewHolder vh = (ClientViewHolder) holder;
            vh.usernameText.setText(user.username);
            vh.nameText.setText(user.name != null ? user.name : "");
            if (user.trainerId != null) {
                vh.actionButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                vh.actionButton.setOnClickListener(v -> unassignListener.onAction(user));
            } else {
                vh.actionButton.setImageResource(android.R.drawable.ic_input_add);
                vh.actionButton.setOnClickListener(v -> assignListener.onAction(user));
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        HeaderViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.headerTitle);
        }
    }

    static class ClientViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, nameText;
        ImageButton actionButton;
        ClientViewHolder(View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.clientUsername);
            nameText = itemView.findViewById(R.id.clientName);
            actionButton = itemView.findViewById(R.id.clientActionButton);
        }
    }
}
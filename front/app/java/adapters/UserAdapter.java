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

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<User> users;
    private final OnUserActionListener editListener;
    private final OnUserActionListener deleteListener;

    public interface OnUserActionListener {
        void onAction(User user);
    }

    public UserAdapter(List<User> users, OnUserActionListener editListener, OnUserActionListener deleteListener) {
        this.users = users;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.usernameText.setText(user.username);
        holder.roleText.setText(user.role);
        holder.nameText.setText(user.name != null ? user.name : "");
        holder.editButton.setOnClickListener(v -> editListener.onAction(user));
        holder.deleteButton.setOnClickListener(v -> deleteListener.onAction(user));
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, roleText, nameText;
        ImageButton editButton, deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.userUsername);
            roleText = itemView.findViewById(R.id.userRole);
            nameText = itemView.findViewById(R.id.userName);
            editButton = itemView.findViewById(R.id.editUserButton);
            deleteButton = itemView.findViewById(R.id.deleteUserButton);
        }
    }
}
package com.example.fitnesshelper.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.fitnesshelper.R;
import com.example.fitnesshelper.helpers.SessionManager;
import com.example.fitnesshelper.activities.LoginActivity;
import com.example.fitnesshelper.network.RetrofitClient;

public class SyncErrorDialogFragment extends DialogFragment {

    private static final String ARG_MESSAGE = "message";

    public static SyncErrorDialogFragment newInstance(String message) {
        SyncErrorDialogFragment fragment = new SyncErrorDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_sync_error, container, false);

        String message = getArguments() != null ? getArguments().getString(ARG_MESSAGE) : "Ошибка синхронизации";

        TextView messageText = view.findViewById(R.id.messageText);
        ImageButton closeButton = view.findViewById(R.id.closeButton);
        Button exitButton = view.findViewById(R.id.exitButton);

        messageText.setText(message);

        closeButton.setOnClickListener(v -> dismiss());
        exitButton.setOnClickListener(v -> {
            SessionManager.clearSession(requireContext());
            RetrofitClient.resetInstance();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
            dismiss();
        });

        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}
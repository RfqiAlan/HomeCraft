package com.example.furniture.utils;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.furniture.R;

/**
 * Utility class to show custom SweetAlert style dialogs.
 */
public class SweetDialog {

    public static final int TYPE_SUCCESS = 1;
    public static final int TYPE_ERROR = 2;
    public static final int TYPE_WARNING = 3;
    public static final int TYPE_INFO = 4;

    private final Context context;
    private AlertDialog dialog;
    
    private String titleText;
    private String messageText;
    private String confirmText = "OK";
    private String cancelText = "Cancel";
    
    private int type = TYPE_INFO;
    private boolean showCancel = false;
    
    private View.OnClickListener confirmListener;
    private View.OnClickListener cancelListener;

    public SweetDialog(Context context, int type) {
        this.context = context;
        this.type = type;
    }

    public SweetDialog setTitleText(String title) {
        this.titleText = title;
        return this;
    }

    public SweetDialog setContentText(String message) {
        this.messageText = message;
        return this;
    }

    public SweetDialog setConfirmText(String confirmText) {
        this.confirmText = confirmText;
        return this;
    }

    public SweetDialog setCancelText(String cancelText) {
        this.cancelText = cancelText;
        return this;
    }

    public SweetDialog showCancelButton(boolean show) {
        this.showCancel = show;
        return this;
    }

    public SweetDialog setConfirmClickListener(View.OnClickListener listener) {
        this.confirmListener = listener;
        return this;
    }

    public SweetDialog setCancelClickListener(View.OnClickListener listener) {
        this.cancelListener = listener;
        return this;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.SweetDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_sweet, null);
        builder.setView(view);

        ImageView ivIcon = view.findViewById(R.id.iv_sweet_icon);
        TextView tvTitle = view.findViewById(R.id.tv_sweet_title);
        TextView tvMessage = view.findViewById(R.id.tv_sweet_message);
        Button btnConfirm = view.findViewById(R.id.btn_sweet_confirm);
        Button btnCancel = view.findViewById(R.id.btn_sweet_cancel);

        // Set Texts
        if (titleText != null) tvTitle.setText(titleText);
        if (messageText != null) tvMessage.setText(messageText);
        btnConfirm.setText(confirmText);
        btnCancel.setText(cancelText);

        // Set Icon based on Type
        switch (type) {
            case TYPE_SUCCESS:
                ivIcon.setImageResource(R.drawable.ic_check_circle);
                break;
            case TYPE_ERROR:
                ivIcon.setImageResource(R.drawable.ic_close_circle);
                break;
            case TYPE_WARNING:
                ivIcon.setImageResource(R.drawable.ic_warning_circle);
                break;
            case TYPE_INFO:
            default:
                ivIcon.setImageResource(R.drawable.ic_info_circle);
                break;
        }

        // Setup Buttons
        if (showCancel) {
            btnCancel.setVisibility(View.VISIBLE);
        }

        dialog = builder.create();
        dialog.setCancelable(false);

        btnConfirm.setOnClickListener(v -> {
            if (confirmListener != null) {
                confirmListener.onClick(v);
            } else {
                dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (cancelListener != null) {
                cancelListener.onClick(v);
            } else {
                dismiss();
            }
        });

        dialog.show();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}

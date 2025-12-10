package com.kiduyu.klaus.ebookfinaldownload.utils;

import android.app.Dialog;
import android.content.Context;
import android.widget.TextView;
import com.folioreader.R;

public class ProgressDialog {
    Context ctx;
    Dialog dialog;

    public ProgressDialog(Context ctx) {
        this.ctx = ctx;
        dialog = new Dialog(ctx, R.style.full_screen_dialog);

    }

    public Dialog show(String text) {
        if (dialog == null) {
            dialog = new Dialog(ctx, R.style.full_screen_dialog);
        }
        dialog.setContentView(R.layout.progress_dialog);
        ((TextView) dialog.findViewById(R.id.label_loading)).setText(text);
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }
    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }


    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}
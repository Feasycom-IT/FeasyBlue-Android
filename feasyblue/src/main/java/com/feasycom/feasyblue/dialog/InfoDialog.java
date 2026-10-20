package com.feasycom.feasyblue.dialog;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.feasycom.feasyblue.R;

/**
 * Copyright 2017 Shenzhen Feasycom Technology co.,Ltd
 */
public class InfoDialog extends BaseDialog {
    TextView dialogInfo;
    private final Activity activity;
    String info;
    public InfoDialog(Context context, String info) {
        super((Activity) context);
        this.activity = (Activity) context;
        this.info = info;
        initUI(info);
    }

    private void initUI(String info) {
        ViewGroup parent = findViewById(android.R.id.content);
        View v = getLayoutInflater().inflate(R.layout.dialog_info, parent, false);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        this.addContentView(v, lp);
        dialogInfo = v.findViewById(R.id.dialog_info);
        setCanceledOnTouchOutside(false);
        dialogInfo.setText(info);
    }

    public void setInfo(final String info) {
        this.info = info;
        activity.runOnUiThread(() -> dialogInfo.setText(info));
    }

    public String getInfo() {
        return this.info;
    }

}

package com.feasycom.feasyblue.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.feasycom.feasyblue.R;
import com.feasycom.feasyblue.presenters.CommandPresenters;
import com.github.iielse.switchbutton.SwitchView;

public class ForceAtCommandView extends LinearLayout {
    TextView label;
    EditText contextEdit;
    SwitchView flagSwitch;
    String labelString;

    Handler handler = new Handler();
    Runnable flagSwitchClickRunnable = new Runnable() {
        @Override
        public void run() {
            flagSwitch.setEnabled(true);
        }
    };

    public ForceAtCommandView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        View view = View.inflate(context, R.layout.force_at_command, this);
//        ButterKnife.bind(view);
        label = (TextView) view.findViewById(R.id.label);
        contextEdit = (EditText) view.findViewById(R.id.context);

        flagSwitch = (SwitchView) view.findViewById(R.id.flag_switch);
        flagSwitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                flagSwitch.setEnabled(false);
                handler.postDelayed(flagSwitchClickRunnable, 300);
                if (contextEdit.getText().toString().length() > 0) {
                    contextEdit.setEnabled(!flagSwitch.isOpened());
                    if (flagSwitch.isOpened()) {
                        CommandPresenters.INSTANCE.plus();
                    } else {
                        CommandPresenters.INSTANCE.minus();
                    }
                } else {
                    if (flagSwitch.isOpened()) {
                        flagSwitch.setOpened(false);
                    }
                    Toast.makeText(getContext(), getResources().getString(R.string.none), Toast.LENGTH_SHORT).show();
                }

            }
        });

        //引用资源文件 需要用TypedArray
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.forceAtCommandView);
        labelString = ta.getString(R.styleable.forceAtCommandView_label);
        label.setText(labelString);
    }

    public void setLabel(String label) {
        this.label.setText(label);
    }

    public void setInputType(int type) {
        contextEdit.setInputType(type);
    }

    public void setMaxLength(int i) {
        contextEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(i)});

    }

    public void setContext(String context) {
        contextEdit.setText(context);
    }

    public String getCommandInfo() {
        if (flagSwitch.isOpened()) {
            if (getContext().getString(R.string.pin).equals(labelString)) {
                return "AT+PIN=" + contextEdit.getText().toString();
            } else if (getContext().getString(R.string.deviceName).equals(labelString)) {
                return contextEdit.getText().toString();
            } else if (getContext().getString(R.string.baud).equals(labelString)) {
                return "AT+BAUD=" + contextEdit.getText().toString();
            }
        }
        return null;
    }

    public void setCommandInfo(String info) {
        contextEdit.setText(info);
        flagSwitch.setOpened(false);
        // CommandPresenters.INSTANCE.plus();
    }
}
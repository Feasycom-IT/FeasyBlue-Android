package com.feasycom.feasyblue.customview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.feasycom.feasyblue.R;

public class NavigationBar extends RelativeLayout {
    public TextView tv_nb_title, tv_nb_subTitle;
    public Button btn_left, btn_right;
    public CheckBox cb_ble, cb_spp;
    public View navigation_bar;

    public NavigationBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        navigation_bar = LayoutInflater.from(context).inflate(R.layout.navigation_bar, null, true);
        //TextView
        tv_nb_title = (TextView) navigation_bar.findViewById(R.id.tv_nb_title);
        tv_nb_subTitle = (TextView) navigation_bar.findViewById(R.id.tv_nb_subTitle);

        //Button
        btn_left = (Button) navigation_bar.findViewById(R.id.btn_left);
        btn_right = (Button) navigation_bar.findViewById(R.id.btn_right);

        //CheckBox
        cb_ble = (CheckBox) navigation_bar.findViewById(R.id.cb_ble);
        cb_spp = (CheckBox) navigation_bar.findViewById(R.id.cb_spp);

        addView(navigation_bar, 0);
    }
}

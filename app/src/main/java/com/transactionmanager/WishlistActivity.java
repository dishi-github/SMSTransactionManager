package com.transactionmanager;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WishlistActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setPadding(40, 40, 40, 40);
        TextView tv = new TextView(this);
        tv.setText("Wishlist Coming Soon");
        root.addView(tv);
        setContentView(root);
    }
}

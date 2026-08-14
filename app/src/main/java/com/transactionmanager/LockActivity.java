package com.transactionmanager;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class LockActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransactionDatabase db = new TransactionDatabase(this);
        String pin = db.getSetting("user_pin", "");

        if (pin.isEmpty()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#F8FAFC"));
        root.setPadding(dp(40), dp(40), dp(40), dp(40));

        TextView tv = new TextView(this);
        tv.setText("Transaction Manager");
        tv.setTextSize(24);
        tv.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        tv.setTextColor(Color.parseColor("#1E293B"));
        root.addView(tv);

        TextView sub = new TextView(this);
        sub.setText("Enter PIN to continue");
        sub.setPadding(0, 0, 0, dp(40));
        root.addView(sub);

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setGravity(Gravity.CENTER);
        input.setTextSize(32);
        root.addView(input, new LinearLayout.LayoutParams(-1, -2));

        Button btn = new Button(this);
        btn.setText("Unlock App");
        btn.setAllCaps(false);
        btn.setBackgroundColor(Color.parseColor("#6366F1"));
        btn.setTextColor(Color.WHITE);
        btn.setOnClickListener(v -> {
            if (input.getText().toString().equals(pin)) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
            }
        });
        
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(56));
        blp.setMargins(0, dp(24), 0, 0);
        root.addView(btn, blp);

        setContentView(root);
    }

    private int dp(int px) {
        return (int) (px * getResources().getDisplayMetrics().density + 0.5f);
    }
}

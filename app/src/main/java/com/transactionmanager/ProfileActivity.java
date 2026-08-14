package com.transactionmanager;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ProfileActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransactionDatabase db = new TransactionDatabase(this);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.parseColor("#F8FAFC"));

        TextView title = new TextView(this);
        title.setText("Profile Settings");
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(24));
        root.addView(title);

        EditText nameInput = new EditText(this);
        nameInput.setHint("User Name");
        nameInput.setText(db.getSetting("user_name", ""));
        root.addView(nameInput);

        EditText pinInput = new EditText(this);
        pinInput.setHint("Set 4-Digit PIN");
        pinInput.setText(db.getSetting("user_pin", ""));
        root.addView(pinInput);

        Button saveBtn = new Button(this);
        saveBtn.setText("Save Changes");
        saveBtn.setBackgroundColor(Color.parseColor("#6366F1"));
        saveBtn.setTextColor(Color.WHITE);
        saveBtn.setOnClickListener(v -> {
            db.setSetting("user_name", nameInput.getText().toString());
            db.setSetting("user_pin", pinInput.getText().toString());
            Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
            finish();
        });
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(56));
        lp.setMargins(0, dp(24), 0, 0);
        root.addView(saveBtn, lp);

        setContentView(root);
    }

    private int dp(int px) {
        return (int) (px * getResources().getDisplayMetrics().density + 0.5f);
    }
}

package com.transactionmanager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private TransactionDatabase db;
    private TextView balanceText, greetingText;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    
    private final int COLOR_PRIMARY = Color.parseColor("#6366F1");
    private final int COLOR_ACCENT = Color.parseColor("#10B981");
    private final int COLOR_BG = Color.parseColor("#F8FAFC");
    private final int COLOR_ERROR = Color.parseColor("#EF4444");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new TransactionDatabase(this);
        setContentView(buildLayout());
        
        if (db.getSetting("sync_anchor", "").isEmpty()) {
            Calendar cal = Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            db.setSetting("sync_anchor", String.valueOf(cal.getTimeInMillis()));
        }

        requestPermissions();
        refreshUI();
    }

    private void requestPermissions() {
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                .setTitle("SMS Access Required")
                .setMessage("Transaction Manager needs access to your messages to track bank alerts and UPI transactions when you trigger a sync.")
                .setPositiveButton("Continue", (dialog, which) -> {
                    requestPermissions(new String[]{Manifest.permission.READ_SMS}, 101);
                })
                .show();
        }
    }

    private View buildLayout() {
        ScrollView scrollView = new ScrollView(this); scrollView.setFillViewport(true); scrollView.setBackgroundColor(COLOR_BG);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(24), dp(24), dp(24), dp(24));
        scrollView.addView(root);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(dp(24), bars.top + dp(12), dp(24), bars.bottom + dp(12));
            return insets;
        });

        greetingText = new TextView(this); greetingText.setTextSize(14); greetingText.setTextColor(Color.parseColor("#64748B")); root.addView(greetingText);
        TextView title = new TextView(this); title.setText("Spent this Month"); title.setTextSize(26); title.setTextColor(Color.parseColor("#1E293B"));
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL)); root.addView(title);

        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setBackground(createCardBackground());
        card.setPadding(dp(24), dp(24), dp(24), dp(24)); card.setElevation(dp(4));
        balanceText = new TextView(this); balanceText.setTextSize(36); balanceText.setTextColor(Color.WHITE); balanceText.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        card.addView(balanceText); root.addView(card, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row1 = new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL); row1.setPadding(0, dp(32), 0, 0);
        row1.addView(actionButton("Expenses", () -> startActivity(new Intent(this, SummaryActivity.class))), new LinearLayout.LayoutParams(0, dp(60), 1));
        row1.addView(new View(this), new LinearLayout.LayoutParams(dp(12), 1));
        row1.addView(actionButton("Filters", () -> showBlockedRemarksDialog()), new LinearLayout.LayoutParams(0, dp(60), 1));
        root.addView(row1);

        LinearLayout row2 = new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL); row2.setPadding(0, dp(12), 0, 0);
        row2.addView(actionButton("Profile", () -> startActivity(new Intent(this, ProfileActivity.class))), new LinearLayout.LayoutParams(0, dp(60), 1));
        row2.addView(new View(this), new LinearLayout.LayoutParams(dp(12), 1));
        Button debitBtn = new Button(this); debitBtn.setText("Manual Exp"); debitBtn.setBackgroundColor(COLOR_ERROR); debitBtn.setTextColor(Color.WHITE);
        debitBtn.setAllCaps(false); debitBtn.setOnClickListener(v -> showTransactionDialog()); row2.addView(debitBtn, new LinearLayout.LayoutParams(0, dp(60), 1));
        root.addView(row2);

        return scrollView;
    }

    private Button actionButton(String text, Runnable action) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false); b.setBackgroundColor(COLOR_PRIMARY); b.setTextColor(Color.WHITE);
        b.setOnClickListener(v -> action.run()); return b;
    }

    private android.graphics.drawable.Drawable createCardBackground() {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[] {COLOR_PRIMARY, Color.parseColor("#4F46E5")});
        gd.setCornerRadius(dp(16)); return gd;
    }

    private void refreshUI() {
        SimpleDateFormat myf = new SimpleDateFormat("MM-yyyy", Locale.US);
        String thisMonth = myf.format(new Date());
        Calendar cal = Calendar.getInstance(); cal.add(Calendar.MONTH, -1);
        String lastMonth = myf.format(cal.getTime());

        balanceText.setText("₹" + String.format("%.2f", db.getMonthExpenses(thisMonth)));
        greetingText.setText("Last Month Spent: ₹" + String.format("%.2f", db.getMonthExpenses(lastMonth)));
    }

    private void showBlockedRemarksDialog() {
        List<String> blocked = db.getBlockedRemarks();
        if (blocked.isEmpty()) { Toast.makeText(this, "No filters active", Toast.LENGTH_SHORT).show(); return; }
        String[] items = blocked.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Excluded Merchants").setItems(items, (dialog, which) -> {
            String remark = items[which];
            new AlertDialog.Builder(this).setTitle("Restore?").setMessage("Allow txns from " + remark + "?")
                .setPositiveButton("Restore", (d, w) -> { db.unblockRemark(remark); refreshUI(); }).show();
        }).setPositiveButton("Close", null).show();
    }

    private void showTransactionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this); builder.setTitle("Add Expense");
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(20), dp(10), dp(20), dp(10));
        EditText amt = new EditText(this); amt.setHint("Amount"); amt.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); l.addView(amt);
        EditText rem = new EditText(this); rem.setHint("Merchant"); l.addView(rem); builder.setView(l);
        builder.setPositiveButton("Save", (d, w) -> {
            if (!amt.getText().toString().isEmpty()) {
                long now = System.currentTimeMillis();
                db.insertTransaction(Double.parseDouble(amt.getText().toString()), "DEBIT", rem.getText().toString(), new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date(now)), now);
                refreshUI();
            }
        }); builder.show();
    }

    @Override protected void onResume() { super.onResume(); refreshUI(); }
    private int dp(int px) { return (int) (px * getResources().getDisplayMetrics().density + 0.5f); }
}

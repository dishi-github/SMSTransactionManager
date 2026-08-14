package com.transactionmanager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SummaryActivity extends Activity {
    private TransactionDatabase db;
    private TextView totalText;
    private Button datePickerBtn;
    private Set<Integer> selectedIds = new HashSet<>();
    private List<TransactionDatabase.Transaction> transactions;
    private SimpleDateFormat displaySdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    private final int COLOR_PRIMARY = Color.parseColor("#6366F1");
    private final int COLOR_BG = Color.parseColor("#F8FAFC");
    private final int COLOR_TEXT_PRI = Color.parseColor("#1E293B");
    private final int COLOR_TEXT_SEC = Color.parseColor("#64748B");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new TransactionDatabase(this);
        LinearLayout mainRoot = new LinearLayout(this); mainRoot.setOrientation(LinearLayout.VERTICAL); mainRoot.setBackgroundColor(COLOR_BG);
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, insets) -> { Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); v.setPadding(0, bars.top + dp(10), 0, bars.bottom); return insets; });

        LinearLayout header = new LinearLayout(this); header.setOrientation(LinearLayout.HORIZONTAL); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(dp(20), dp(10), dp(20), dp(10));
        TextView title = new TextView(this); title.setText("Expenses"); title.setTextSize(22); title.setTextColor(COLOR_TEXT_PRI); title.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL)); header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        datePickerBtn = iconButton("📅 " + getAnchorDateDisplay(), v -> showDatePicker()); header.addView(datePickerBtn);
        header.addView(iconButton("🔄 Sync", v -> checkPermissionAndSync()));
        header.addView(iconButton("🗑 Clear", v -> clearRecords()));
        mainRoot.addView(header);

        ScrollView scrollView = new ScrollView(this); scrollView.setFillViewport(true); mainRoot.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout listRoot = new LinearLayout(this); listRoot.setOrientation(LinearLayout.VERTICAL); listRoot.setPadding(dp(20), 0, dp(20), dp(20)); scrollView.addView(listRoot);

        transactions = db.getAllTransactions();
        if (transactions.isEmpty()) {
            TextView empty = new TextView(this); empty.setText("No expenses."); empty.setGravity(Gravity.CENTER); empty.setPadding(0, dp(100), 0, 0); listRoot.addView(empty);
        } else {
            for (TransactionDatabase.Transaction t : transactions) listRoot.addView(createRow(t));
        }

        LinearLayout bottomBar = new LinearLayout(this); bottomBar.setOrientation(LinearLayout.HORIZONTAL); bottomBar.setPadding(dp(24), dp(20), dp(24), dp(20)); bottomBar.setBackgroundColor(Color.WHITE); bottomBar.setElevation(dp(8));
        totalText = new TextView(this); totalText.setText("Selected Sum: ₹0.00"); totalText.setTextSize(18); totalText.setTextColor(COLOR_PRIMARY); totalText.setTypeface(null, Typeface.BOLD); bottomBar.addView(totalText);
        mainRoot.addView(bottomBar);
        setContentView(mainRoot);
    }

    private String getAnchorDateDisplay() {
        String s = db.getSetting("sync_anchor", "");
        return s.isEmpty() ? "Select Date" : displaySdf.format(new Date(Long.parseLong(s)));
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        String s = db.getSetting("sync_anchor", ""); if (!s.isEmpty()) cal.setTimeInMillis(Long.parseLong(s));
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar sel = Calendar.getInstance(); sel.set(y, m, d, 0, 0, 0); sel.set(Calendar.MILLISECOND, 0);
            db.setSetting("sync_anchor", String.valueOf(sel.getTimeInMillis())); datePickerBtn.setText("📅 " + displaySdf.format(sel.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private Button iconButton(String text, View.OnClickListener listener) {
        Button b = new Button(this); b.setText(text); b.setTextSize(11); b.setAllCaps(false); b.setOnClickListener(listener); b.setBackgroundColor(Color.TRANSPARENT); b.setTextColor(COLOR_PRIMARY); b.setPadding(dp(8), 0, dp(8), 0); return b;
    }

    private LinearLayout createRow(TransactionDatabase.Transaction t) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(16), dp(16), dp(16), dp(16)); row.setBackground(createRowBg());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, dp(12)); row.setLayoutParams(lp);
        CheckBox cb = new CheckBox(this); cb.setOnCheckedChangeListener((v, isChecked) -> { if (isChecked) selectedIds.add(t.id); else selectedIds.remove(t.id); updateTotal(); }); row.addView(cb);
        
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); row.addView(content, new LinearLayout.LayoutParams(0, -2, 1));
        content.setPadding(dp(12), 0, 0, 0);

        TextView desc = new TextView(this); desc.setText(db.getNickname(t.description)); desc.setTextColor(COLOR_TEXT_PRI); desc.setTextSize(16); desc.setTypeface(null, Typeface.BOLD); content.addView(desc);
        TextView date = new TextView(this); date.setText(t.date); date.setTextColor(COLOR_TEXT_SEC); date.setTextSize(12); content.addView(date);

        TextView amt = new TextView(this); amt.setText("₹" + String.format("%.2f", t.amount)); amt.setTextColor(Color.parseColor("#EF4444")); amt.setTextSize(16); amt.setTypeface(null, Typeface.BOLD); row.addView(amt);

        row.setOnClickListener(v -> showOptionsDialog(t));
        return row;
    }

    private void showOptionsDialog(TransactionDatabase.Transaction t) {
        String[] options = {"Set Nickname", "Remove Nickname", "Exclude Merchant", "Delete Entry"};
        new AlertDialog.Builder(this).setTitle("Merchant: " + t.description).setItems(options, (dialog, which) -> {
            if (which == 0) {
                EditText input = new EditText(this); input.setHint("e.g. Groceries");
                new AlertDialog.Builder(this).setTitle("Set Friendly Name").setView(input).setPositiveButton("Save", (d, w) -> {
                    db.setNickname(t.description, input.getText().toString()); recreate();
                }).show();
            } else if (which == 1) {
                db.setNickname(t.description, null); recreate();
            } else if (which == 2) {
                db.blockRemark(t.description); Toast.makeText(this, "Filtered out", Toast.LENGTH_SHORT).show(); recreate();
            } else {
                db.removeTransaction(t.id); recreate();
            }
        }).show();
    }

    private android.graphics.drawable.Drawable createRowBg() {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(); gd.setColor(Color.WHITE); gd.setCornerRadius(dp(12)); return gd;
    }

    private void updateTotal() {
        double sum = 0; for (TransactionDatabase.Transaction t : transactions) { if (selectedIds.contains(t.id)) sum += t.amount; }
        totalText.setText("Selected Sum: ₹" + String.format("%.2f", sum));
    }

    private void clearRecords() {
        new AlertDialog.Builder(this).setTitle("Clear History?").setMessage("Delete all data?").setPositiveButton("Clear All", (d, w) -> { db.clearAllTransactions(); recreate(); }).show();
    }

    private void checkPermissionAndSync() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, 100);
        else syncMessages();
    }

    private void syncMessages() {
        try {
            String s = db.getSetting("sync_anchor", "");
            long startTimestamp = s.isEmpty() ? System.currentTimeMillis() : Long.parseLong(s);
            Uri uri = Uri.parse("content://sms/inbox"); Cursor c = getContentResolver().query(uri, new String[]{"body", "date"}, "date >= ?", new String[]{String.valueOf(startTimestamp)}, "date DESC");
            int found = 0;
            if (c != null && c.moveToFirst()) {
                do {
                    long ts = c.getLong(1);
                    SmsParser.TransactionInfo info = SmsParser.parse(c.getString(0), ts);
                    if (info != null && !db.isBlocked(info.remark)) { db.insertTransaction(info.amount, "DEBIT", info.remark, info.date, ts); found++; }
                } while (c.moveToNext()); c.close();
            }
            Toast.makeText(this, "Added " + found + " new items.", Toast.LENGTH_LONG).show(); recreate();
        } catch (Exception e) { Toast.makeText(this, "Sync failed", Toast.LENGTH_LONG).show(); }
    }

    private int dp(int px) { return (int) (px * getResources().getDisplayMetrics().density + 0.5f); }
}

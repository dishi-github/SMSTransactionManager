package com.transactionmanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class TransactionDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "transaction_manager.db";
    private static final int DATABASE_VERSION = 5;

    public TransactionDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, amount REAL, type TEXT, description TEXT, date TEXT, timestamp INTEGER, UNIQUE(amount, type, description, date))");
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT)");
        db.execSQL("CREATE TABLE wishlist (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, price REAL)");
        db.execSQL("CREATE TABLE blocked_remarks (remark TEXT PRIMARY KEY)");
        db.execSQL("CREATE TABLE merchant_nicknames (original_remark TEXT PRIMARY KEY, nickname TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS blocked_remarks (remark TEXT PRIMARY KEY)");
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS merchant_nicknames (original_remark TEXT PRIMARY KEY, nickname TEXT)");
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE transactions ADD COLUMN timestamp INTEGER DEFAULT 0");
            } catch (Exception ignored) {}
        }
    }

    public void blockRemark(String remark) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("remark", remark);
        db.insertWithOnConflict("blocked_remarks", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.delete("transactions", "description=?", new String[]{remark});
    }

    public void unblockRemark(String remark) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("blocked_remarks", "remark=?", new String[]{remark});
    }

    public List<String> getBlockedRemarks() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("blocked_remarks", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do { list.add(cursor.getString(0)); } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public boolean isBlocked(String remark) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("blocked_remarks", null, "remark=?", new String[]{remark}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public void setNickname(String original, String nick) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (nick == null || nick.trim().isEmpty()) {
            db.delete("merchant_nicknames", "original_remark=?", new String[]{original});
        } else {
            ContentValues values = new ContentValues();
            values.put("original_remark", original);
            values.put("nickname", nick);
            db.replace("merchant_nicknames", null, values);
        }
    }

    public String getNickname(String original) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("merchant_nicknames", new String[]{"nickname"}, "original_remark=?", new String[]{original}, null, null, null);
        String nick = original;
        if (cursor.moveToFirst()) nick = cursor.getString(0);
        cursor.close();
        return nick;
    }

    public void insertTransaction(double amount, String type, String description, String date, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 2-Minute Cooldown Check: Same amount within 120 seconds
        // We ignore description because multiple SMS for one txn (OTP + Confirmation) often differ in text.
        long twoMinutes = 120 * 1000;
        Cursor c = db.query("transactions", null,
                "amount=? AND abs(timestamp - ?) < ?",
                new String[]{String.valueOf(amount), String.valueOf(timestamp), String.valueOf(twoMinutes)},
                null, null, null);

        if (c.getCount() > 0) {
            c.close();
            return; // Duplicate amount detected within 2 minutes
        }
        c.close();

        ContentValues values = new ContentValues();
        values.put("amount", amount);
        values.put("type", type);
        values.put("description", description);
        values.put("date", date);
        values.put("timestamp", timestamp);
        db.insertWithOnConflict("transactions", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public double getTotalExpenses() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM transactions WHERE type='DEBIT'", null);
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    public double getMonthExpenses(String monthYear) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM transactions WHERE type='DEBIT' AND date LIKE ?", new String[]{"%" + monthYear + "%"});
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    public List<Transaction> getAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Sort by timestamp DESC (latest first)
        Cursor cursor = db.query("transactions", null, null, null, null, null, "timestamp DESC, id DESC");
        if (cursor.moveToFirst()) {
            do {
                list.add(new Transaction(cursor.getInt(0), cursor.getDouble(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void setSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("key", key);
        values.put("value", value);
        db.replace("settings", null, values);
    }

    public String getSetting(String key, String defaultValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("settings", new String[]{"value"}, "key=?", new String[]{key}, null, null, null);
        String val = defaultValue;
        if (cursor.moveToFirst()) val = cursor.getString(0);
        cursor.close();
        return val;
    }

    public void removeTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("transactions", "id=?", new String[]{String.valueOf(id)});
    }

    public void clearAllTransactions() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("transactions", null, null);
    }

    public static class Transaction {
        public int id;
        public double amount;
        public String type, description, date;
        public Transaction(int id, double amount, String type, String description, String date) {
            this.id = id; this.amount = amount; this.type = type; this.description = description; this.date = date;
        }
    }
}

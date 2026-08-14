package com.transactionmanager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {
    public static class TransactionInfo {
        public double amount;
        public String remark;
        public String date;

        public TransactionInfo(double amount, String remark, String date) {
            this.amount = amount;
            this.remark = remark;
            this.date = date;
        }
    }

    public static TransactionInfo parse(String message, long timestamp) {
        try {
            String lowerMsg = message.toLowerCase();
            if (lowerMsg.contains("sip")) return null;

            boolean isSpentFormat = lowerMsg.contains("spent") || lowerMsg.contains("txn");
            boolean isUpiFormat = lowerMsg.contains("upi") && (lowerMsg.contains(" to ") || lowerMsg.contains(" at "));
            if (!isSpentFormat && !isUpiFormat) return null;

            double amount = 0;
            String remark = "Unknown Merchant";
            SimpleDateFormat targetFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
            String dateStr = targetFormat.format(new Date(timestamp));

            // 1. Amount Extraction
            Pattern amountPattern = Pattern.compile("(?i)(?:Spent|Txn|Sent|Rs\\.?|INR)\\s*([\\d,.]+)");
            Matcher amountMatcher = amountPattern.matcher(message);
            if (amountMatcher.find()) {
                amount = Double.parseDouble(amountMatcher.group(1).replace(",", ""));
            } else return null;

            // 2. Remark Extraction (Specifically handles "At gpay-...")
            Pattern atPattern = Pattern.compile("(?i)At\\s+([a-zA-Z0-9.@\\-_]{4,})");
            Matcher atMatcher = atPattern.matcher(message);
            if (atMatcher.find()) {
                remark = atMatcher.group(1).trim();
            } else {
                Pattern toPattern = Pattern.compile("(?i)to\\s+([a-zA-Z0-9.@\\-_]{4,})");
                Matcher toMatcher = toPattern.matcher(message);
                if (toMatcher.find()) remark = toMatcher.group(1).trim();
            }
            remark = remark.replaceAll("[.,;]$|\\n.*", "");

            // 3. Date Parsing
            Pattern datePattern = Pattern.compile("(?i)on\\s+([\\d]{2,4}[-/][\\d]{2}(?:[-/][\\d]{2,4})?)");
            Matcher dateMatcher = datePattern.matcher(message);
            if (dateMatcher.find()) {
                String rawDate = dateMatcher.group(1).trim();
                try {
                    SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                    String datePart = "";
                    if (rawDate.matches("\\d{4}-\\d{2}-\\d{2}")) datePart = dateOnlyFormat.format(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(rawDate));
                    else if (rawDate.matches("\\d{2}-\\d{2}-\\d{2}")) datePart = dateOnlyFormat.format(new SimpleDateFormat("dd-MM-yy", Locale.US).parse(rawDate));
                    else if (rawDate.matches("\\d{2}-\\d{2}-\\d{4}")) datePart = dateOnlyFormat.format(new SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(rawDate));
                    else if (rawDate.matches("\\d{2}-\\d{2}")) {
                        Date p = new SimpleDateFormat("dd-MM", Locale.US).parse(rawDate);
                        Calendar cal = Calendar.getInstance(); cal.setTime(p); cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
                        datePart = dateOnlyFormat.format(cal.getTime());
                    }
                    if (!datePart.isEmpty()) dateStr = datePart + " " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
                } catch (Exception ignored) {}
            }

            return new TransactionInfo(amount, remark, dateStr);
        } catch (Exception e) { return null; }
    }
}

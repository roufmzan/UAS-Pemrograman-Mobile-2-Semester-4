package com.example.tugasku;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReminderScheduler {
    // Preference keys
    private static final String PREF_NAME = "TugaskuPreferences";
    private static final String KEY_REMINDER_LEAD = "reminder_lead_millis";
    // Default: 60 minutes before deadline
    public static final long DEFAULT_LEAD_MILLIS = 60L * 60L * 1000L;

    private static long parseDeadlineToMillis(String deadline) throws ParseException {
        if (deadline == null || deadline.isEmpty()) throw new ParseException("empty", 0);
        // Try with time first
        SimpleDateFormat withTime = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("id", "ID"));
        try {
            return withTime.parse(deadline).getTime();
        } catch (ParseException e) {
            // Fallback: date only at 23:59
            SimpleDateFormat dateOnly = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
            Calendar cal = Calendar.getInstance();
            cal.setTime(dateOnly.parse(deadline));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        }
    }

    private static int buildRequestCode(String title, String course, String deadline) {
        String key = String.valueOf(title) + "|" + String.valueOf(course) + "|" + String.valueOf(deadline);
        return Math.abs(key.hashCode());
    }

    public static void scheduleReminder(Context context, String title, String course, String deadline) {
        try {
            long deadlineMillis = parseDeadlineToMillis(deadline);
            long lead = getLeadMillis(context);
            long triggerAt = deadlineMillis - lead;
            if (triggerAt < System.currentTimeMillis()) {
                // Too late to schedule
                return;
            }

            int requestCode = buildRequestCode(title, course, deadline);

            Intent intent = new Intent(context, DeadlineReceiver.class);
            intent.putExtra(DeadlineReceiver.EXTRA_TITLE, title);
            intent.putExtra(DeadlineReceiver.EXTRA_COURSE, course);
            intent.putExtra(DeadlineReceiver.EXTRA_DEADLINE, deadline);
            intent.putExtra(DeadlineReceiver.EXTRA_REQUEST_CODE, requestCode);

            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void cancelReminder(Context context, String title, String course, String deadline) {
        int requestCode = buildRequestCode(title, course, deadline);
        Intent intent = new Intent(context, DeadlineReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pi != null) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                am.cancel(pi);
            }
            pi.cancel();
        }
    }

    public static long getLeadMillis(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_REMINDER_LEAD, DEFAULT_LEAD_MILLIS);
    }

    public static void setLeadMillis(Context context, long millis) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_REMINDER_LEAD, millis)
                .apply();
    }
}

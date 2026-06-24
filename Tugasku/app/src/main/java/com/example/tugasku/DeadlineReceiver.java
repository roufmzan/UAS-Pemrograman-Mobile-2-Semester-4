package com.example.tugasku;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class DeadlineReceiver extends BroadcastReceiver {
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_COURSE = "extra_course";
    public static final String EXTRA_DEADLINE = "extra_deadline";
    public static final String EXTRA_REQUEST_CODE = "extra_request_code";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TITLE);
        String course = intent.getStringExtra(EXTRA_COURSE);
        String deadline = intent.getStringExtra(EXTRA_DEADLINE);
        int requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, (int) System.currentTimeMillis());

        String notifTitle = "Tenggat tugas mendekat";
        String notifMessage = title + " (" + course + ") akan jatuh tempo pada " + deadline;

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                requestCode,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = NotificationHelper.baseBuilder(context, notifTitle, notifMessage)
                .setContentIntent(contentIntent);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(requestCode, builder.build());
    }
}

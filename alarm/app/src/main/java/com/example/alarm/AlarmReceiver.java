package com.example.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        android.util.Log.i("AlarmReceiver", "Alarm broadcast received; test=" + intent.getBooleanExtra("alarm_test", false));
        if (intent.getBooleanExtra("alarm_test", false)) {
            AlarmPlaybackService.start(context, -2, "Kiểm tra báo thức", 10, true, false);
            return;
        }
        int id = intent.getIntExtra("alarm_id", -1);
        AlarmItem item = AlarmStore.get(context, id);
        if (item == null || !item.enabled) {
            android.util.Log.w("AlarmReceiver", "Alarm item missing or disabled: " + id);
            return;
        }
        if (item.repeatCount > 0) {
            item.repeatCount--;
            if (item.repeatCount == 0) item.enabled = false;
            AlarmStore.upsert(context, item);
        }
        if (item.enabled) AlarmScheduler.scheduleOne(context, item);
        else AlarmScheduler.cancel(context, item.id);
        AlarmPlaybackService.start(context, item.id, item.label, item.durationSeconds, item.soundEnabled, item.vibrate);
    }
}

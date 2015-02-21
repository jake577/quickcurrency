package com.jesttek.quickcurrency.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.File;

public class ExchangeWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Intent intent = new Intent(context.getApplicationContext(), WidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.startService(intent);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for(int widgetId:appWidgetIds) {
            File deletePrefFile = new File(context.getApplicationInfo().dataDir + "/shared_prefs/" + ExchangeWidgetConfigActivity.PREFERENCE_FILE + widgetId + ".xml");
            deletePrefFile.delete();
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getAlarmIntent(context));
        super.onDisabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    public static void updateConfig(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        PendingIntent pendingIntent = getAlarmIntent(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sharedPreferences = context.getSharedPreferences(ExchangeWidgetConfigActivity.PREFERENCE_FILE, Context.MODE_PRIVATE);
        int rate = sharedPreferences.getInt(ExchangeWidgetConfigActivity.REFRESH_RATE_PREFERENCE, 1800000);
        am.setInexactRepeating(AlarmManager.RTC, 250, rate, pendingIntent);
    }

    /**
     * Get the intent used to update the widgets
     * @param context
     * @return
     */
    private static PendingIntent getAlarmIntent(Context context){
        Intent intent = new Intent(context, ExchangeWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, ExchangeWidgetProvider.class);
        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}

package com.fitnesscat.stepstracker

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews

class StepsWidgetProvider : BaseCatWidgetProvider() {

    override val layoutResId = R.layout.widget_steps
    override val stepCountViewId = R.id.widget_step_count
    override val alarmAction = "com.fitnesscat.stepstracker.WIDGET_UPDATE_GREEN"

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = UserPreferences(context)
        val steps = prefs.getTodayStepCount()
        val catCode = prefs.getCatCode()
        val stage = prefs.getCurrentStage()

        val drawableName = if (stage == 1) "cat_stage_1" else "cat_${catCode}_stage_$stage"
        val drawableRes = context.resources.getIdentifier(drawableName, "drawable", context.packageName)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_steps)
            views.setTextViewText(R.id.widget_step_count, steps.toString())
            if (drawableRes != 0) {
                views.setImageViewResource(R.id.widget_cat_image, drawableRes)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

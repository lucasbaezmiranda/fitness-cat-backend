package com.fitnesscat.stepstracker

class StepsWidgetMadProvider : BaseCatWidgetProvider() {
    override val layoutResId = R.layout.widget_mad
    override val stepCountViewId = R.id.widget_step_count
    override val alarmAction = "com.fitnesscat.stepstracker.WIDGET_UPDATE_MAD"
}

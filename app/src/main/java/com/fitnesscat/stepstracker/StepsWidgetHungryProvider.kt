package com.fitnesscat.stepstracker

class StepsWidgetHungryProvider : BaseCatWidgetProvider() {
    override val layoutResId = R.layout.widget_hungry
    override val stepCountViewId = R.id.widget_step_count
    override val alarmAction = "com.fitnesscat.stepstracker.WIDGET_UPDATE_HUNGRY"
}

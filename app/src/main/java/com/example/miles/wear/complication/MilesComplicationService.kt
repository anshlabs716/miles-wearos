package com.example.miles.wear.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.example.miles.wear.MilesWearApplication

class MilesComplicationService : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = 75f,
                    min = 40f,
                    max = 190f,
                    contentDescription = PlainComplicationText.Builder("75 BPM").build()
                ).setText(PlainComplicationText.Builder("75").build())
                 .setTitle(PlainComplicationText.Builder("BPM").build())
                 .build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("240").build(),
                    contentDescription = PlainComplicationText.Builder("240 kcal").build()
                ).setTitle(PlainComplicationText.Builder("kcal").build())
                 .build()
            }
            else -> null
        }
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        val metrics = try {
            MilesWearApplication.instance.sensorTracker.liveMetrics.value
        } catch (e: Exception) {
            null
        }

        val bpm = metrics?.heartRate?.takeIf { it > 0 }
        val cal = metrics?.caloriesKcal?.takeIf { it > 0 }

        val data: ComplicationData? = when (request.complicationType) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = (bpm ?: 0).toFloat(),
                    min = 40f,
                    max = 190f,
                    contentDescription = PlainComplicationText.Builder("${bpm ?: "--"} BPM").build()
                ).setText(PlainComplicationText.Builder((bpm ?: 0).toString()).build())
                 .setTitle(PlainComplicationText.Builder("BPM").build())
                 .build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder((cal ?: 0).toString()).build(),
                    contentDescription = PlainComplicationText.Builder("${cal ?: "--"} kcal").build()
                ).setTitle(PlainComplicationText.Builder("kcal").build())
                 .build()
            }
            else -> null
        }

        listener.onComplicationData(data)
    }
}

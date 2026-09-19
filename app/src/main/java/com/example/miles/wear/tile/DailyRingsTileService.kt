package com.example.miles.wear.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.example.miles.wear.MainActivity
import com.example.miles.wear.MilesWearApplication
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class DailyRingsTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val launchActivityClick = ModifiersBuilders.Clickable.Builder()
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(packageName)
                            .setClassName(MainActivity::class.java.name)
                            .build()
                    )
                    .build()
            )
            .build()

        val metrics = try {
            MilesWearApplication.instance.sensorTracker.liveMetrics.value
        } catch (e: Exception) {
            null
        }

        val steps = metrics?.steps ?: 4250
        val calories = metrics?.caloriesKcal ?: 185
        val bpm = metrics?.heartRate ?: 72

        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(dp(180f))
            .setHeight(dp(180f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText("DAILY ACTIVITY")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(11f))
                        .setColor(argb(0xFF78909C.toInt()))
                        .build()
                )
                .build()
        )

        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(6f)).build())

        // Row for Steps
        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText("👟 $steps / 10,000")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(14f))
                        .setColor(argb(0xFF00E676.toInt()))
                        .build()
                )
                .build()
        )

        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())

        // Row for Calories
        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText("🔥 $calories / 500 kcal")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(14f))
                        .setColor(argb(0xFFFF5722.toInt()))
                        .build()
                )
                .build()
        )

        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())

        // Row for BPM
        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText("❤️ $bpm BPM")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(14f))
                        .setColor(argb(0xFF00B0FF.toInt()))
                        .build()
                )
                .build()
        )

        val layout = LayoutElementBuilders.Layout.Builder()
            .setRoot(
                LayoutElementBuilders.Box.Builder()
                    .setWidth(dp(200f))
                    .setHeight(dp(200f))
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setBackground(
                                ModifiersBuilders.Background.Builder()
                                    .setColor(argb(0xFF000000.toInt()))
                                    .build()
                            )
                            .setClickable(launchActivityClick)
                            .build()
                    )
                    .addContent(column.build())
                    .build()
            )
            .build()

        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(layout)
                    .build()
            )
            .build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setTileTimeline(timeline)
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion("1")
            .build()
        return Futures.immediateFuture(resources)
    }
}

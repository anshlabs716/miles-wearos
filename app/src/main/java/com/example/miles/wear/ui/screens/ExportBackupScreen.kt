package com.example.miles.wear.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.wear.compose.material3.Text
import com.example.miles.wear.MilesWearApplication
import com.example.miles.wear.data.backup.MilesBackupManager
import com.example.miles.wear.data.export.WorkoutExporter
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real-data export + backup: GPX/CSV/JSON per workout or bulk, plus a full
 * JSON backup of every table + settings, with restore from on-device files.
 */
@Composable
fun ExportBackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val repository = MilesWearApplication.instance.repository

    val sessions by repository.allSessions.collectAsStateWithLifecycle(initialValue = emptyList())

    var backupFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var refresh by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    LaunchedEffect(refresh) {
        backupFiles = WorkoutExporter.list(context)
    }

    val flashClear: (String) -> Unit = { msg ->
        flash = msg
    }
    LaunchedEffect(flash) {
        if (flash != null) {
            kotlinx.coroutines.delay(2000L)
            flash = null
        }
    }

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(OLEDBlack)
                .focusRequester(focusRequester)
                .focusable()
                .onRotaryScrollEvent {
                    coroutineScope.launch { listState.scrollBy(it.verticalScrollPixels) }
                    true
                }
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = "💾 EXPORT & BACKUP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Real workouts · GPX · CSV · JSON",
                        fontSize = 9.sp,
                        color = MutedGray
                    )
                }
            }

            item {
                Chip(
                    onClick = onBack,
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF18181C), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth(0.94f),
                    label = {
                        Text("⬅ Back", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedGray)
                    }
                )
            }

            // Full backup / restore
            item {
                SectionTitle("FULL BACKUP (every table + settings)")
            }

            item {
                Chip(
                    onClick = {
                        if (busy) return@Chip
                        busy = true
                        coroutineScope.launch {
                            val file = MilesBackupManager.createBackup(context, repository)
                            busy = false
                            if (file != null) {
                                flashClear("Backup saved ✓")
                                refresh++
                                WorkoutExporter.share(context, file)
                            } else {
                                flashClear("Backup failed")
                            }
                        }
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = VividGreen, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 2.dp),
                    label = {
                        Text(
                            if (busy) "Creating…" else "➕ Create backup file",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }

            item {
                Chip(
                    onClick = {
                        if (busy) return@Chip
                        val latest = backupFiles.firstOrNull()
                        if (latest == null) { flashClear("No backup files yet"); return@Chip }
                        busy = true
                        coroutineScope.launch {
                            val ok = MilesBackupManager.restore(context, repository, latest)
                            busy = false
                            flashClear(if (ok) "Restored latest ✓" else "Restore failed")
                            refresh++
                        }
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF182A3A), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 2.dp),
                    label = {
                        Text(
                            if (busy) "Working…" else "📥 Restore latest backup",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }

            if (backupFiles.isNotEmpty()) {
                item { SectionTitle("BACKUP FILES ON DEVICE") }
                backupFiles.take(6).forEach { file ->
                    item {
                        BackupFileRow(file = file, onShare = { WorkoutExporter.share(context, file) }) {
                            coroutineScope.launch {
                                val ok = MilesBackupManager.restore(context, repository, file)
                                flashClear(if (ok) "Restored ${file.name.take(20)} ✓" else "Restore failed")
                                refresh++
                            }
                        }
                    }
                }
            }

            // Bulk export
            item { SectionTitle("EXPORT EVERYTHING") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    BulkExportChip("GPX", ElectricAmber) {
                        if (sessions.isEmpty()) { flashClear("No workouts yet"); return@BulkExportChip }
                        WorkoutExporter.write(context, "miles_all_${stamp()}.gpx", WorkoutExporter.gpxForAll(sessions)).also {
                            WorkoutExporter.share(context, it)
                        }
                        flashClear("GPX exported ✓")
                    }
                    BulkExportChip("CSV", NeonCyan) {
                        if (sessions.isEmpty()) { flashClear("No workouts yet"); return@BulkExportChip }
                        WorkoutExporter.write(context, "miles_all_${stamp()}.csv", WorkoutExporter.csvForAll(sessions)).also {
                            WorkoutExporter.share(context, it)
                        }
                        flashClear("CSV exported ✓")
                    }
                    BulkExportChip("JSON", CoralFlame) {
                        if (sessions.isEmpty()) { flashClear("No workouts yet"); return@BulkExportChip }
                        WorkoutExporter.write(context, "miles_all_${stamp()}.json", WorkoutExporter.jsonForAll(sessions)).also {
                            WorkoutExporter.share(context, it)
                        }
                        flashClear("JSON exported ✓")
                    }
                }
            }

            // Per-session export
            item { SectionTitle("PER-WORKOUT EXPORT") }
            if (sessions.isEmpty()) {
                item {
                    Text(
                        "No workouts recorded yet — do one for real, then export it.",
                        fontSize = 10.sp,
                        color = MutedGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                sessions.take(15).forEach { session ->
                    item {
                        SessionExportRow(
                            label = "${session.workoutType} • ${"%.2f".format(session.distanceMeters / 1000.0)} km",
                            sub = SimpleDateFormat("MMM d HH:mm", Locale.getDefault()).format(Date(session.startTime))
                        ) { kind ->
                            when (kind) {
                                "GPX" -> WorkoutExporter.write(context, "miles_${session.id}_${stamp()}.gpx", WorkoutExporter.gpxForSession(session))
                                "CSV" -> WorkoutExporter.write(context, "miles_${session.id}_${stamp()}.csv", WorkoutExporter.csvPoints(session))
                                else -> WorkoutExporter.write(context, "miles_${session.id}_${stamp()}.json", WorkoutExporter.jsonForSession(session))
                            }.also {
                                WorkoutExporter.share(context, it)
                            }
                            flashClear("$kind exported ✓")
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    flash?.let { msg ->
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = msg,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = VividGreen,
                modifier = Modifier.padding(bottom = 10.dp).background(Color(0xE6101610), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = ElectricAmber,
        letterSpacing = 0.5.sp,
        modifier = Modifier.fillMaxWidth(0.92f).padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BulkExportChip(label: String, color: Color, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF18181C), contentColor = Color.White),
        modifier = Modifier.weight(1f),
        label = {
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = color, textAlign = TextAlign.Center)
        }
    )
}

@Composable
private fun BackupFileRow(file: File, onShare: () -> Unit, onRestore: () -> Unit) {
    val sizeKb = file.length() / 1024
    val date = SimpleDateFormat("MMM d HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
    Column(
        Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 3.dp)
            .background(Color(0xFF18181C), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(text = file.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        Text(text = "$sizeKb KB • $date", fontSize = 8.sp, color = MutedGray)
        Spacer(modifier = Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Chip(
                onClick = onShare,
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF1A2A38), contentColor = Color.White),
                modifier = Modifier.weight(1f),
                label = {
                    Text("Share", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan, textAlign = TextAlign.Center)
                }
            )
            Chip(
                onClick = onRestore,
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF33141E), contentColor = Color.White),
                modifier = Modifier.weight(1f),
                label = {
                    Text("Restore", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CoralFlame, textAlign = TextAlign.Center)
                }
            )
        }
    }
}

@Composable
private fun SessionExportRow(label: String, sub: String, onExport: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 3.dp)
            .background(Color(0xFF18181C), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        Text(text = sub, fontSize = 8.sp, color = MutedGray)
        Spacer(modifier = Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("GPX", "CSV", "JSON").forEach { kind ->
                Chip(
                    onClick = { onExport(kind) },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF25252B), contentColor = Color.White),
                    modifier = Modifier.weight(1f),
                    label = {
                        Text(kind, fontSize = 9.sp, fontWeight = FontWeight.Black, color = ElectricAmber, textAlign = TextAlign.Center)
                    }
                )
            }
        }
    }
}

private fun stamp(): String =
    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
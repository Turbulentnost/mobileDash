package com.example.mobiledash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobiledash.data.KpiTile
import kotlin.math.min

private const val ViewAllPageOffset = 1

@Composable
fun CommercialSummaryBlock(
    tiles: List<KpiTile>,
    loading: Boolean,
    onOpenCommercial: () -> Unit,
) {
    val visibleTiles = tiles.take(4)
    val pageCount = if (visibleTiles.isEmpty()) 1 else visibleTiles.size + ViewAllPageOffset
    var pageIndex by remember { mutableIntStateOf(0) }
    var dragAmount by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(visibleTiles.size) {
        pageIndex = min(pageIndex, pageCount - 1)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Коммерческий блок",
                        color = DashboardDesign.Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Сводка для председателя совета директоров",
                        color = DashboardDesign.MutedText,
                        fontSize = 12.sp,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(pageCount) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAmount = 0f },
                            onHorizontalDrag = { _, dragDelta -> dragAmount += dragDelta },
                            onDragEnd = {
                                when {
                                    dragAmount < -70f && pageIndex < pageCount - 1 -> pageIndex++
                                    dragAmount > 70f && pageIndex > 0 -> pageIndex--
                                }
                            },
                        )
                    },
            ) {
                when {
                    loading -> CommercialLoadingTile()
                    visibleTiles.isEmpty() -> CommercialEmptyTile()
                    pageIndex >= visibleTiles.size -> CommercialViewAllTile(onOpenCommercial)
                    else -> CommercialTile(
                        tile = visibleTiles[pageIndex],
                        index = pageIndex,
                        onOpenCommercial = onOpenCommercial,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(pageCount) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (index == pageIndex) 18.dp else 7.dp, height = 7.dp)
                            .clip(CircleShape)
                            .background(if (index == pageIndex) DashboardDesign.Navy else DashboardDesign.Border),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommercialTile(
    tile: KpiTile,
    index: Int,
    onOpenCommercial: () -> Unit,
) {
    val iconPath = commercialIconPath(tile = tile, index = index)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FBFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(commercialIconBackground(tile.rag)),
                    contentAlignment = Alignment.Center,
                ) {
                    TempImageFile(
                        fileName = iconPath,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tile.title,
                        color = DashboardDesign.Text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = commercialValue(tile),
                        color = DashboardDesign.Navy,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (tile.kpiPercent.isNotBlank()) {
                        Text(
                            text = tile.kpiPercent,
                            color = DashboardDesign.MutedText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Button(
                onClick = onOpenCommercial,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DashboardDesign.Navy),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Подробнее", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CommercialViewAllTile(onOpenCommercial: () -> Unit) {
    Card(
        onClick = onOpenCommercial,
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardDesign.Navy),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .padding(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Нажмите, чтобы просмотреть все плитки",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 25.sp,
            )
        }
    }
}

@Composable
private fun CommercialLoadingTile() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FBFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("Загрузка коммерческого блока...", color = DashboardDesign.MutedText)
        }
    }
}

@Composable
private fun CommercialEmptyTile() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FBFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("Нет данных коммерческого блока", color = DashboardDesign.MutedText)
        }
    }
}

private fun commercialValue(tile: KpiTile): String {
    return listOf(tile.fact, tile.plan)
        .filter { it.isNotBlank() && it != "-" }
        .joinToString(" / ")
        .ifBlank { tile.kpiPercent.ifBlank { "Нет данных" } }
}

private fun commercialIconPath(tile: KpiTile, index: Int): String {
    val folder = commercialIconFolder(tile, index)
    val color = when (tile.rag.lowercase()) {
        "red" -> "red"
        "yellow" -> "yellow"
        else -> "green"
    }
    return "$folder/$color.png"
}

private fun commercialIconFolder(tile: KpiTile, index: Int): String {
    val text = "${tile.title} ${tile.id} ${tile.badge}".lowercase()
    return when {
        "отнош" in text || "2026/2025" in text || "2026_2025" in text -> "otntoshenie"
        "день" in text || "ден" in text -> "plandeneg"
        "договор" in text -> "dogovorplan"
        "отгруз" in text -> "otgruzki"
        else -> listOf("otgruzki", "dogovorplan", "plandeneg", "otntoshenie").getOrElse(index) { "otgruzki" }
    }
}

private fun commercialIconBackground(rag: String): Color {
    return when (rag.lowercase()) {
        "red" -> Color(0xFFFFE4E9)
        "yellow" -> Color(0xFFFFF8D7)
        else -> Color(0xFFE0FBE5)
    }
}

package com.lduarte1311.calsyncalarm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Componente de roda vertical tátil (Drum Roller Picker).
 * Usa LazyColumn com contentPadding e snap para simular uma roda de seleção estilo relógio.
 * Totalmente compatível com tema escuro/claro e Dynamic Color (Material You).
 *
 * @param items              Lista de valores a apresentar (ex: 0..59).
 * @param selectedIndex      Índice inicialmente selecionado (centrado ao abrir).
 * @param onSelectionChanged Callback com o índice real selecionado ao parar de rolar.
 * @param itemHeight         Altura de cada item da roda.
 * @param visibleItems       Número de itens visíveis (deve ser ímpar para realçar o centro).
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 44.dp,
    visibleItems: Int = 5
) {
    val listState = rememberLazyListState()
    val totalHeight = itemHeight * visibleItems
    // Padding vertical = metade da altura total para que o 1.º e o último item
    // possam ser centrados na barra de seleção.
    val verticalPad = itemHeight * (visibleItems / 2)

    // Cor extraída do tema para o drawWithContent (não pode ser lida dentro do lambda Canvas)
    val primaryColor   = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor   = MaterialTheme.colorScheme.surface

    // ── Scroll inicial: centraliza o valor selecionado ──────────────────────
    LaunchedEffect(selectedIndex) {
        listState.scrollToItem(
            index = selectedIndex.coerceIn(0, items.lastIndex),
            scrollOffset = 0
        )
    }

    // ── Índice centrado: item cujo centro está mais próximo do centro da viewport ──
    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val viewportCenter =
                (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { item ->
                    kotlin.math.abs((item.offset + item.size / 2) - viewportCenter)
                }
                ?.index
                ?.coerceIn(0, items.lastIndex)
                ?: selectedIndex.coerceIn(0, items.lastIndex)
        }
    }

    // ── Snap suave quando o utilizador para de rolar ─────────────────────────
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (!scrolling) {
                    val snapped = centeredIndex.coerceIn(0, items.lastIndex)
                    listState.animateScrollToItem(index = snapped, scrollOffset = 0)
                    onSelectionChanged(snapped)
                }
            }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Destaque da faixa central selecionada
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center)
                .background(
                    color = primaryColor.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small
                )
        )

        LazyColumn(
            state = listState,
            // Fix 1: contentPadding permite que o item 0 e o último subam/desçam até ao centro
            contentPadding = PaddingValues(vertical = verticalPad),
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .drawWithContent {
                    drawContent()
                    // Gradiente superior (fade)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(surfaceColor, Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.35f
                        )
                    )
                    // Gradiente inferior (fade)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, surfaceColor),
                            startY = size.height * 0.65f,
                            endY = size.height
                        )
                    )
                    // Linhas de delimitação do item central
                    val lineY1 = size.height * 0.4f
                    val lineY2 = size.height * 0.6f
                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f),
                        start = Offset(size.width * 0.08f, lineY1),
                        end   = Offset(size.width * 0.92f, lineY1),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f),
                        start = Offset(size.width * 0.08f, lineY2),
                        end   = Offset(size.width * 0.92f, lineY2),
                        strokeWidth = 1.5f
                    )
                }
        ) {
            itemsIndexed(items) { index, text ->
                val isCentered = index == centeredIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = text,
                        textAlign  = TextAlign.Center,
                        color      = if (isCentered) primaryColor
                                     else onSurfaceColor.copy(alpha = 0.38f),
                        fontWeight = if (isCentered) FontWeight.Bold else FontWeight.Normal,
                        fontSize   = if (isCentered) 22.sp else 16.sp,
                        style      = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

package kaist.iclab.wearabletracker.ema.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Checkbox
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import kaist.iclab.tracker.sensor.microema.WatchOption
import kaist.iclab.wearabletracker.R

/**
 * Horizontally scrollable option selector for RADIO questions with 3+ options.
 * Each option is a tappable chip; the selected one highlights in blue.
 * User scrolls left/right, then taps confirm.
 */
@Composable
internal fun HorizontalOptionInput(
    options: List<WatchOption>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit
) {
    val listState =
        rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, selectedIndex - 1))

    LaunchedEffect(selectedIndex) {
        if (options.isNotEmpty()) {
            val targetIndex = maxOf(0, selectedIndex - 1).coerceAtMost(options.lastIndex)
            listState.scrollToItem(targetIndex)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontally scrollable options
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            itemsIndexed(options) { index, option ->
                val isSelected = index == selectedIndex
                val bgColor = if (isSelected) AccentBlue else DarkSurface.copy(alpha = 0.6f)
                val textColor = if (isSelected) Color.Black else Color.White
                val borderMod = if (isSelected) {
                    Modifier.border(2.dp, AccentBlue.copy(alpha = 0.5f), CircleShape)
                } else {
                    Modifier
                }

                Button(
                    onClick = { onIndexChanged(index) },
                    modifier = Modifier
                        .size(width = 36.dp, height = 30.dp)
                        .then(borderMod),
                    colors = ButtonDefaults.buttonColors(backgroundColor = bgColor),
                    shape = CircleShape
                ) {
                    Text(
                        text = option.display,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Scale labels
        if (options.size >= 3) {
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.ema_low), fontSize = 10.sp, color = MutedGrey)
                Text(text = stringResource(R.string.ema_high), fontSize = 10.sp, color = MutedGrey)
            }
        }
    }
}

/**
 * Horizontal pager showing one categorical option at a time.
 * User swipes left/right to browse options; the visible option is always selected.
 */
@Composable
internal fun CategoricalOptionInput(
    options: List<WatchOption>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { options.size }
    )

    LaunchedEffect(selectedIndex) {
        if (pagerState.currentPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    // Sync pager scroll → selectedIndex
    LaunchedEffect(pagerState.currentPage) {
        onIndexChanged(pagerState.currentPage)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pager showing one option at a time
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            pageSpacing = 8.dp
        ) { page ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(34.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = options[page].display,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Page indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(options.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 6.dp else 4.dp)
                        .background(
                            color = if (index == pagerState.currentPage) AccentBlue else MutedGrey.copy(
                                alpha = 0.5f
                            ),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

/**
 * Vertically scrollable list of categorical options with checkboxes (multi-select).
 */
@Composable
internal fun CheckboxInput(
    options: List<WatchOption>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 10.dp)
    ) {
        itemsIndexed(options) { _, option: WatchOption ->
            val isChecked = selectedIds.contains(option.id)
            ToggleChip(
                checked = isChecked,
                onCheckedChange = { onToggle(option.id) },
                label = {
                    Text(
                        text = option.display,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                toggleControl = {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggle(option.id) }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                colors = ToggleChipDefaults.toggleChipColors(
                    checkedStartBackgroundColor = AccentBlue.copy(alpha = 0.2f),
                    checkedEndBackgroundColor = AccentBlue.copy(alpha = 0.2f),
                    uncheckedStartBackgroundColor = DarkSurface.copy(alpha = 0.6f),
                    uncheckedEndBackgroundColor = DarkSurface.copy(alpha = 0.6f),
                    checkedContentColor = Color.White,
                    uncheckedContentColor = Color.White,
                    checkedToggleControlColor = AccentBlue
                )
            )
        }
    }
}

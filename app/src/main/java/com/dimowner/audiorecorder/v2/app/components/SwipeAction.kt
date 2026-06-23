package com.dimowner.audiorecorder.v2.app.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue.EndToStart
import androidx.compose.material3.SwipeToDismissBoxValue.Settled
import androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

@Composable
fun <T> SwipeActionItemView(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    startAction: SwipeAction<T>?,
    endAction: SwipeAction<T>?,
    //startAction: (T) -> Unit,
    //endAction: (T) -> Unit,
    itemData: T,
    content: @Composable RowScope.() -> Unit
) {
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    // Called when user swipes past threshold
    LaunchedEffect(swipeToDismissBoxState.currentValue) {
        val currentValue = swipeToDismissBoxState.currentValue
        Timber.d("LaunchedEffect: $currentValue")
    }


    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        enableDismissFromStartToEnd = enabled && startAction != null,
        enableDismissFromEndToStart = enabled && endAction != null,
        modifier = modifier.fillMaxSize(),
        // Called when user lets go past threshold
        onDismiss = afterAction@{
            when (it) {
                StartToEnd -> startAction?.onAction(itemData)
                EndToStart -> endAction?.onAction(itemData)
                Settled -> return@afterAction
            }
            scope.launch {
                swipeToDismissBoxState.snapTo(Settled)
            }
            Timber.d("onDismiss: $it")
        },
        backgroundContent = {
            when (swipeToDismissBoxState.dismissDirection) {
                StartToEnd -> {
                    //Log.d("SwipeActionItem", "Progress: ${swipeToDismissBoxState.requireOffset()}")
                    startAction?.let { action ->
                        SwipeActionIcon(
                            action = action,
                            itemData = itemData,
                            offset = { swipeToDismissBoxState.requireOffset() },
                            progress = { swipeToDismissBoxState.progress },
                            alignment = Alignment.CenterStart
                        )
                    }
                }

                EndToStart -> {
                    endAction?.let { action ->
                        SwipeActionIcon(
                            action = action,
                            itemData = itemData,
                            offset = { swipeToDismissBoxState.requireOffset() },
                            progress = { swipeToDismissBoxState.progress },
                            alignment = Alignment.CenterEnd
                        )
                    }
                }

                Settled -> {}
            }
        },
        content = content
    )
}


sealed interface SwipeAction<T> {
    val color: Color
    val contentDescription: String?
    fun getIcon(itemData: T): Painter
    val onAction: (T) -> Unit

    data class Simple<T>(
        val icon: Painter,
        override val color: Color,
        override val contentDescription: String? = null,
        override val onAction: (T) -> Unit
    ) : SwipeAction<T> {
        override fun getIcon(itemData: T) = icon
    }

    data class Toggle<T>(
        val checkedIcon: Painter,
        val uncheckedIcon: Painter,
        val isChecked: (T) -> Boolean,
        override val color: Color,
        override val contentDescription: String? = null,
        override val onAction: (T) -> Unit
    ) : SwipeAction<T> {
        // ENCAPSULATION: The template defines how to pick the icon
        override fun getIcon(itemData: T) =
            if (isChecked(itemData)) checkedIcon else uncheckedIcon
    }
}


@Composable
private fun <T> SwipeActionIcon(
    action: SwipeAction<T>,
    itemData: T,
    offset: () -> Float,
    progress: () -> Float,
    alignment: Alignment,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = action.getIcon(itemData),
        contentDescription = action.contentDescription,
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    lerp(
                        action.color,
                        lerp(action.color, Color.LightGray, 0.5f),
                        progress()
                    )
                )
            }
            .wrapContentSize(alignment)
            .offset {
                val raw = (offset() / 2).toInt()
                val threshold = 60
                val x = if (raw > 0) {
                    // Swiping right: Stay at 0 until we pass threshold
                    max(raw - threshold, 0)
                } else {
                    // Swiping left: Stay at 0 until we pass negative threshold
                    min(raw + threshold, 0)
                }
                IntOffset(x, 0)
            }
            .padding(12.dp),
        tint = Color.White
    )
}

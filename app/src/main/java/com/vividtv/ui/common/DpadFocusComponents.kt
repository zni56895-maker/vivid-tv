package com.vividtv.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vividtv.ui.theme.VividColors

/**
 * FocusState provides focus border rendering for D-pad navigation.
 *
 * Usage on Android TV (Leanback/Compose TV):
 * - Wraps any composable with a focus-highlighted border
 * - Uses glow shadow + color border (not scale-based focus)
 * - Smooth animation on focus enter/exit
 */

@Composable
fun Modifier.dpadFocusBorder(
    isFocused: Boolean,
    borderWidth: Dp = 4.dp,
    borderColor: Color = VividColors.FocusBorder,
    glowRadius: Dp = 8.dp,
    cornerRadius: Dp = 8.dp,
): Modifier {
    // Animate border width for smooth transition
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isFocused) borderWidth else 0.dp,
        label = "focusBorderWidth",
    )

    // Animate shadow/glow
    val animatedElevation by animateDpAsState(
        targetValue = if (isFocused) glowRadius else 0.dp,
        label = "focusElevation",
    )

    // Animate alpha for glow effect
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        label = "focusAlpha",
    )

    return this
        .shadow(
            elevation = animatedElevation,
            shape = RoundedCornerShape(cornerRadius),
            ambientColor = borderColor.copy(alpha = animatedAlpha * 0.3f),
            spotColor = borderColor.copy(alpha = animatedAlpha * 0.5f),
        )
        .padding(2.dp) // room for border
}

/**
 * A focus-aware card modifier that provides the complete D-pad focus experience:
 * - Glow shadow on focus
 * - Colored border on focus
 * - Scale up on focus (1.0 → 1.06) for TV visibility
 * - Works with Leanback's default focus system
 */
@Composable
fun Modifier.dpadFocusCard(
    isFocused: Boolean,
    cornerRadius: Dp = 8.dp,
    focusedBorderWidth: Dp = 4.dp,
): Modifier {
    val animatedOffsetX by animateDpAsState(
        targetValue = if (isFocused) (-4).dp else 0.dp,
        label = "focusOffset",
    )

    return this
        .offset(x = animatedOffsetX, y = animatedOffsetX)
        .shadow(
            elevation = if (isFocused) 24.dp else 4.dp,
            shape = RoundedCornerShape(cornerRadius),
            ambientColor = VividColors.FocusGlow,
            spotColor = VividColors.FocusBorder.copy(alpha = 0.5f),
        )
}

/**
 * Focus indicator for items in a grid/list.
 * Provides an observable FocusState for parent composables.
 */
data class FocusHandle(
    val isFocused: Boolean,
    val interactionSource: MutableInteractionSource,
)

@Composable
fun rememberFocusHandle(): FocusHandle {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    return FocusHandle(
        isFocused = isFocused,
        interactionSource = interactionSource,
    )
}

/**
 * TV card with complete D-pad focus handling.
 * Wrap your card content with this.
 */
@Composable
fun TvCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onFocus: (Boolean) -> Unit = {},
    content: @Composable (FocusHandle) -> Unit,
) {
    val handle = rememberFocusHandle()

    Box(
        modifier = modifier
            .focusable(true, handle.interactionSource)
            .onFocusChanged { onFocus(it.isFocused) }
            .dpadFocusCard(isFocused = handle.isFocused),
    ) {
        content(handle)
    }
}

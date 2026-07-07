package com.kinetic.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import com.kinetic.keyboard.giphy.GiphyItem
import com.kinetic.keyboard.ui.theme.KbTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DELETE_INITIAL_MS = 350L
private const val DELETE_INTERVAL_MS = 50L

/** Which panel replaces the key rows (P5.5 emoji · P5.10 GIF/sticker). */
enum class PanelMode { NONE, EMOJI, GIF, STICKER }

enum class MediaStatus { IDLE, LOADING, ERROR, NO_KEY }

/** GIF/sticker panel state owned by the service. */
data class MediaUiState(
    val query: String = "",
    val searchActive: Boolean = false,
    val status: MediaStatus = MediaStatus.IDLE,
    val items: List<GiphyItem> = emptyList(),
)

/**
 * P5.10: GIPHY-backed GIF/sticker browser — search bar, staggered animated grid, and the
 * shared Gboard-style tab bar. Same height as the keyboard it replaces.
 */
@Composable
fun MediaPanel(
    mode: PanelMode,
    state: MediaUiState,
    theme: KbTheme,
    height: Dp,
    onSearchTap: () -> Unit,
    onTab: (PanelMode) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onPick: (GiphyItem) -> Unit,
) {
    val context = LocalContext.current
    val gifLoader = remember {
        ImageLoader.Builder(context)
            .components { add(ImageDecoderDecoder.Factory()) } // animated GIF/WebP (API 28+)
            .build()
    }

    Column(Modifier.fillMaxWidth().height(height).background(theme.background)) {
        // Search bar — tapping it swaps the panel for the key rows to type the query.
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .background(theme.key, RoundedCornerShape(17.dp))
                .semantics { contentDescription = "Search GIPHY" }
                .clickable(onClick = onSearchTap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🔍  " + state.query.ifBlank {
                    if (mode == PanelMode.STICKER) "Search stickers on GIPHY" else "Search GIFs on GIPHY"
                },
                color = if (state.query.isBlank()) theme.hint else theme.label,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (state.status) {
                MediaStatus.NO_KEY -> CenteredNote(
                    "GIPHY API key missing.\nGet a free key at developers.giphy.com and add\n" +
                        "giphy.apiKey=YOUR_KEY to local.properties, then rebuild.",
                    theme,
                )
                MediaStatus.LOADING -> CircularProgressIndicator(
                    color = theme.accent,
                    modifier = Modifier.align(Alignment.Center),
                )
                MediaStatus.ERROR -> CenteredNote(
                    "Couldn't reach GIPHY.\nCheck your connection, then tap search to retry.",
                    theme,
                )
                MediaStatus.IDLE ->
                    if (state.items.isEmpty()) {
                        CenteredNote("No results. Try another search.", theme)
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            verticalItemSpacing = 4.dp,
                        ) {
                            items(state.items, key = { it.id }) { item ->
                                AsyncImage(
                                    model = item.previewUrl,
                                    imageLoader = gifLoader,
                                    contentDescription = if (mode == PanelMode.STICKER) "Sticker" else "GIF",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp)
                                        .aspectRatio(
                                            item.previewWidth.toFloat() /
                                                item.previewHeight.coerceAtLeast(1),
                                        )
                                        .clickable { onPick(item) },
                                )
                            }
                        }
                    }
            }
        }

        MediaTabBar(active = mode, theme = theme, onTab = onTab, onBack = onBack, onDelete = onDelete)
    }
}

@Composable
private fun CenteredNote(text: String, theme: KbTheme) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text, color = theme.hint, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

/** Shown instead of the suggestion strip while typing a GIPHY query on the normal keys. */
@Composable
fun MediaSearchBar(query: String, theme: KbTheme, onSubmit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(46.dp).background(theme.stripBg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "🔍  " + query.ifBlank { "Type to search GIPHY, ⏎ to go" },
            color = if (query.isBlank()) theme.hint else theme.label,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        Text(
            text = "Search",
            color = theme.accent,
            fontSize = 15.sp,
            modifier = Modifier
                .semantics { role = Role.Button }
                .clickable(onClick = onSubmit)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/**
 * Gboard-style panel switcher (per the user's reference screenshot):
 * ABC · 😊 emoji · GIF · sticker · backspace. Shared by the emoji and media panels.
 */
@Composable
fun MediaTabBar(
    active: PanelMode,
    theme: KbTheme,
    onTab: (PanelMode) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(44.dp)) {
        BarKey("ABC", false, theme, Modifier.weight(2f), "Back to letters", onBack)
        BarKey("😊", active == PanelMode.EMOJI, theme, Modifier.weight(2f), "Emoji") {
            onTab(PanelMode.EMOJI)
        }
        BarKey("GIF", active == PanelMode.GIF, theme, Modifier.weight(2f), "GIFs") {
            onTab(PanelMode.GIF)
        }
        BarKey("🏷️", active == PanelMode.STICKER, theme, Modifier.weight(2f), "Stickers") {
            onTab(PanelMode.STICKER)
        }
        RepeatingDeleteKey(theme, Modifier.weight(2f), onDelete)
    }
}

@Composable
private fun BarKey(
    label: String,
    active: Boolean,
    theme: KbTheme,
    modifier: Modifier,
    description: String,
    onTap: () -> Unit,
) {
    Box(
        modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .background(if (active) theme.keyPressed else theme.keyModifier, RoundedCornerShape(7.dp))
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (active) theme.accent else theme.label, fontSize = 15.sp)
    }
}

@Composable
fun RepeatingDeleteKey(theme: KbTheme, modifier: Modifier, onDelete: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .background(if (pressed) theme.keyPressed else theme.keyModifier, RoundedCornerShape(7.dp))
            .semantics {
                role = Role.Button
                contentDescription = "Backspace"
                onClick { onDelete(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    onDelete()
                    val job: Job = scope.launch {
                        delay(DELETE_INITIAL_MS)
                        while (true) {
                            onDelete()
                            delay(DELETE_INTERVAL_MS)
                        }
                    }
                    tryAwaitRelease()
                    job.cancel()
                    pressed = false
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("⌫", color = theme.label, fontSize = 18.sp)
    }
}

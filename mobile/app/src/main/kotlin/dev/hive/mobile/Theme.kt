package dev.hive.mobile

// The look, in one place: the colours and type of the design (flat zones separated by full-width rules, state as a
// coloured uppercase word, one accent, square corners) and the few parts every screen is made of.
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// the design's tokens (dark): the desktop's v3 near-monochrome greys; colour only says state
object C {
    val bg = Color(0xFF1E1E1E)      // panes
    val side = Color(0xFF181818)    // the tab bar, code
    val raised = Color(0xFF252526)  // cards, the message box
    val lift = Color(0xFF2D2D2E)    // quiet buttons
    val band = Color(0xFF2F2F31)    // your own messages
    val sel = Color(0xFF2B2B2C)     // the selected row
    val rule = Color(0xFF2A2A2B)    // zone rules
    val rule2 = Color(0xFF3C3C3E)   // card borders, inputs, pills
    val hi = Color(0xFFEDEDED)      // titles, values, the strong button
    val ink = Color(0xFFD0D0D0)     // body text
    val dim = Color(0xFF9A9A9C)     // labels, secondary text
    val faint = Color(0xFF6E6E70)   // placeholders, times
    val accent = Color(0xFFEDEDED) // the light accent: cursor, selected toggle, "Add"
    val link = Color(0xFFB4B4B8)    // quiet actions
    val work = Color(0xFF7AA2F7)    // working
    val wait = Color(0xFFE0AF68)    // needs you
    val fail = Color(0xFFF7768E)    // failed, nearly out
    val done = Color(0xFF7FC79A)    // good
    val hold = Color(0xFFC9A86A)    // paused
    // the limit meters, each in its own colour
    val limit5h = Color(0xFF7AA2F7)
    val limit7d = Color(0xFFBB9AF7)
    val limitModel = Color(0xFFE0AF68)
}

// the shapes: rows and buttons 8, cards 12, pills 6
object Sh {
    val row = RoundedCornerShape(8.dp)
    val card = RoundedCornerShape(12.dp)
    val pill = RoundedCornerShape(6.dp)
}

// the phone's own faces (Roboto and its mono), as the system draws them: no bundled display face
val Sans = FontFamily.SansSerif
val Mono = FontFamily.Monospace

object T {
    val page = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, color = C.hi)
    val sub = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 21.sp, color = C.hi)
    val title = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, color = C.hi)
    val body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = C.ink)
    val conv = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, color = C.ink)
    val reply = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp, color = C.ink)
    val note = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, color = C.dim)
    val label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, color = C.dim)
    val state = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, lineHeight = 15.sp)
    val mono = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp, color = C.faint)
    val action = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, color = C.link)
}

val Pad = 18.dp

@Composable
fun HiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = C.accent, onPrimary = C.bg, background = C.bg, surface = C.bg, onBackground = C.ink, onSurface = C.ink, error = C.fail)) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalTextStyle provides T.body, content = content)
    }
}

// the colour of a state word
fun stateColor(word: String): Color = when (word.uppercase()) {
    "WORKING", "LOOP", "RUNNING", "WATCHER" -> C.work
    "WAITING", "NEEDS YOUR INPUT", "FINDING", "NEW" -> C.wait
    "FAILED" -> C.fail
    "DONE", "IN USE", "ONLINE", "SEEN BY CLAUDE", "CLEARED" -> C.done
    "PAUSED", "AUTO-CONTINUE" -> C.hold
    "WAKE-UP" -> Color(0xFFB9D7FF)
    else -> C.dim
}

// the words a state is shown in: sentence case, and "Needs your input" where the data says WAITING
fun stateLabel(word: String): String = if (word.uppercase() == "WAITING") "Needs your input" else word.lowercase().replaceFirstChar { it.uppercase() }

// state is a small rounded pill: the word in its colour, a hairline border tinted by it, never a fill
@Composable
fun StateWord(word: String, modifier: Modifier = Modifier, color: Color = stateColor(word)) =
    Text(stateLabel(word), modifier.border(1.dp, color.copy(alpha = 0.35f), Sh.pill).padding(horizontal = 7.dp, vertical = 2.dp), style = T.state, color = color, maxLines = 1)

@Composable
fun Rule(color: Color = C.rule) = Box(Modifier.fillMaxWidth().height(1.dp).background(color))

// a small state dot
@Composable
fun Dot(color: Color, size: Dp = 8.dp) = Box(Modifier.size(size).background(color, CircleShape))

// a row's leading icon: a ring with a glyph, and a state badge (a dot) at its corner
@Composable
fun KindIcon(glyph: String, badge: Color? = null) {
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(24.dp).border(1.5.dp, C.rule2, CircleShape), contentAlignment = Alignment.Center) { Text(glyph, style = T.note.copy(color = C.dim, fontSize = 12.sp, lineHeight = 14.sp)) }
        if (badge != null) Box(Modifier.align(Alignment.BottomEnd).size(10.dp).background(C.bg, CircleShape).padding(1.5.dp).background(badge, CircleShape))
    }
}

// a screen's head: the title, a count beside it, and one word or action at the right
@Composable
fun Head(title: String, count: String = "", right: String = "", rightColor: Color = C.dim, sub: Boolean = false, mark: Boolean = false, onRight: (() -> Unit)? = null, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = Pad, end = Pad, top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) Text("Back", Modifier.clip(Sh.pill).clickable(onClick = onBack).padding(end = 12.dp), style = T.action)
        // the mark (an H in a cell) on the screens that stand before the app: Locked, Pair
        if (mark) androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.ic_orbit_mark), contentDescription = null, modifier = Modifier.padding(end = 10.dp).size(24.dp))
        // the title takes what it needs, the count sits right after it, and the word at the right keeps the edge
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f, fill = false), style = if (sub) T.sub else T.page, maxLines = if (sub) 2 else 1, overflow = TextOverflow.Ellipsis)
            if (count.isNotEmpty()) Text(count, Modifier.padding(start = 10.dp), style = T.note)
        }
        if (right.isNotEmpty()) {
            // an action is an outlined quiet pill, a plain word is just grey
            val m = if (onRight != null) Modifier.clip(Sh.row).border(1.dp, C.rule2, Sh.row).clickable(onClick = onRight).padding(horizontal = 10.dp, vertical = 5.dp) else Modifier
            Text(right, Modifier.padding(start = 10.dp).then(m), style = T.note.copy(fontSize = 12.5.sp), color = rightColor, maxLines = 1)
        }
    }
    Rule()
}

// a section label: small, grey, the count at the right in the same grey
@Composable
fun Group(label: String, right: String = "", color: Color = C.faint) {
    Row(Modifier.fillMaxWidth().padding(start = Pad, end = Pad, top = 14.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = T.label.copy(fontWeight = FontWeight.Medium, fontSize = 11.5.sp), color = color)
        if (right.isNotEmpty()) Text(right, style = T.label.copy(fontWeight = FontWeight.Medium, fontSize = 11.5.sp), color = color)
    }
}

// the selected row: a rounded fill, no bar
fun Modifier.selected(on: Boolean): Modifier = if (!on) this else background(C.sel, Sh.row)

// one list row: a leading icon with its state badge, the title and a time, then a line with the state and what it is doing,
// then its actions at the right. No rules between rows.
@Composable
fun ListRow(
    title: String, meta: String = "", word: String = "", note: String = "", detail: String = "", on: Boolean = false,
    // metaWord: a word with a time in place of the age (WAKES 14:00), as a pill
    metaWord: String = "", metaColor: Color = C.hi,
    icon: String = "", badge: Color? = null, oneLine: Boolean = false,
    onClick: (() -> Unit)? = null, actions: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 1.dp).clip(Sh.row).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).selected(on).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.Top) {
        if (icon.isNotEmpty()) Box(Modifier.padding(end = 10.dp)) { KindIcon(icon, badge) }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f).padding(vertical = 3.dp), style = T.title, maxLines = if (oneLine) 1 else 2, overflow = TextOverflow.Ellipsis)
                if (metaWord.isNotEmpty()) StateWord(metaWord, Modifier.padding(start = 10.dp), metaColor)
                else if (meta.isNotEmpty()) Text(meta, Modifier.padding(start = 10.dp), style = T.mono, maxLines = 1)
            }
            if (word.isNotEmpty() || note.isNotEmpty()) Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (word.isNotEmpty()) StateWord(word, Modifier.padding(end = 8.dp))
                Text(note, style = T.note, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (detail.isNotEmpty()) Text(detail, Modifier.padding(top = 2.dp), style = T.mono.copy(color = C.dim), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (actions != null) Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), content = actions)
        }
    }
}

// a row's action: a quiet rounded button (red text for one that stops or cancels)
@Composable
fun Act(label: String, color: Color = C.hi, enabled: Boolean = true, onClick: () -> Unit) =
    Text(label, Modifier.clip(Sh.row).background(C.lift, Sh.row).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp), style = T.action, color = if (enabled) color else C.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)

// buttons: quiet (lift fill) for the rest, strong (light fill, dark text) for the main action, a red quiet one to stop
@Composable
fun Btn(label: String, modifier: Modifier = Modifier, primary: Boolean = false, danger: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val fill = when { !enabled -> C.lift; danger -> C.fail.copy(alpha = 0.16f); primary -> C.hi; else -> C.lift }
    val ink = when { !enabled -> C.faint; danger -> C.fail; primary -> C.bg; else -> C.hi }
    Box(
        modifier.heightIn(min = 38.dp).clip(Sh.row).background(fill, Sh.row).clickable(enabled = enabled, onClick = onClick).padding(vertical = 8.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = T.body.copy(fontWeight = FontWeight.Medium), color = ink, maxLines = 1) }
}

// a switch: a rounded track, light when on
@Composable
fun Toggle(on: Boolean, onChange: ((Boolean) -> Unit)?) {
    Box(
        Modifier.size(38.dp, 22.dp).clip(CircleShape).background(if (on) C.hi else C.lift, CircleShape).then(if (on) Modifier else Modifier.border(1.dp, C.rule2, CircleShape))
            .then(if (onChange != null) Modifier.clickable { onChange(!on) } else Modifier),
    ) { Box(Modifier.padding(start = if (on) 19.dp else 3.dp, top = 3.dp).size(16.dp).background(if (on) C.bg else C.dim, CircleShape)) }
}

// a setting: its name, one line about it, and a switch or a word at the right
@Composable
fun SettingRow(name: String, about: String = "", on: Boolean? = null, word: String = "", onClick: (() -> Unit)? = null, onChange: ((Boolean) -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 1.dp).clip(Sh.row).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, style = T.body.copy(fontWeight = FontWeight.Medium, color = C.hi))
            if (about.isNotEmpty()) Text(about, style = T.note.copy(fontSize = 12.5.sp))
        }
        if (word.isNotEmpty()) Text(word, Modifier.padding(start = 10.dp), style = T.mono)
        if (on != null) { Spacer(Modifier.width(10.dp)); Toggle(on, onChange) }
    }
}

// a form field as the design draws it: a dim label, the value on an underline, a word at the right
@Composable
fun Field(label: String, value: String, right: String = "", onClick: (() -> Unit)? = null) {
    Column((if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).fillMaxWidth().padding(start = Pad, end = Pad, top = 10.dp)) {
        Text(label, style = T.label)
        Row(Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 7.dp), verticalAlignment = Alignment.Bottom) {
            Text(value, Modifier.weight(1f), style = T.title.copy(fontWeight = FontWeight.Normal), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (right.isNotEmpty()) Text(right, style = T.note)
        }
        Rule(C.rule2)
    }
}

// text input in a quiet rounded box
@Composable
fun UnderlineInput(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, minLines: Int = 1, maxLines: Int = 6, style: TextStyle = T.body.copy(color = C.hi), rule: Color = C.rule2, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.padding(vertical = 6.dp).background(C.raised, Sh.row).border(1.dp, rule, Sh.row).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = style.copy(color = C.faint), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(value, onChange, Modifier.fillMaxWidth(), textStyle = style, cursorBrush = SolidColor(C.hi), minLines = minLines, maxLines = maxLines,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
        }
        if (trailing != null) { Spacer(Modifier.width(10.dp)); trailing() }
    }
}

// A card (an error, a confirm, a question): a border, 12 dp corners, a header strip a shade lighter with a state dot and
// the title, the body in grey, the actions right-aligned at the bottom (quiet first, the main one last).
@Composable
fun Card(title: String, dot: Color, modifier: Modifier = Modifier, actions: (@Composable RowScope.() -> Unit)? = null, body: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).clip(Sh.card).background(C.raised, Sh.card).border(1.dp, C.rule2, Sh.card)) {
        Row(Modifier.fillMaxWidth().background(C.hi.copy(alpha = 0.05f)).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Dot(dot)
            Text(title, Modifier.padding(start = 10.dp), style = T.title.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), content = body)
        if (actions != null) Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), content = actions)
    }
}

// A question before something that cannot be taken back, in the page (no floating dialog): a card with what will
// happen and the two buttons.
@Composable
fun ConfirmBar(title: String, text: String, confirm: String, danger: Boolean = false, cancel: String = "Cancel", onCancel: () -> Unit, onConfirm: () -> Unit, extra: (@Composable ColumnScope.() -> Unit)? = null) {
    Card(title, if (danger) C.fail else C.wait, actions = {
        Btn(cancel, onClick = onCancel)
        Btn(confirm, primary = !danger, danger = danger, onClick = onConfirm)
    }) {
        if (text.isNotEmpty()) Text(text, style = T.note.copy(fontSize = 13.5.sp, lineHeight = 20.sp))
        if (extra != null) extra()
    }
}

// a line under a head that says what went wrong, or how the link is: a failure is a card, anything else a grey line
@Composable
fun Notice(text: String, color: Color = C.fail) {
    if (text.isEmpty()) return
    if (color == C.fail) Card("Something went wrong", C.fail) { Text(text, style = T.note.copy(fontSize = 13.5.sp, lineHeight = 20.sp)) }
    else Text(text, Modifier.fillMaxWidth().padding(horizontal = Pad, vertical = 8.dp), style = T.note, color = color)
}

@Composable
fun Empty(text: String) = Text(text, Modifier.fillMaxWidth().padding(horizontal = Pad, vertical = 16.dp), style = T.note)

// the five tabs: a count in mono over the name, a rounded fill behind the one that is open
data class NavTab(val name: String, val count: String, val warn: Boolean = false)

@Composable
fun TabBar(tabs: List<NavTab>, open: Int, onOpen: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(C.side)) {
        Rule()
        Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 6.dp, vertical = 5.dp)) {
            tabs.forEachIndexed { i, t ->
                val on = i == open
                Column(
                    Modifier.weight(1f).fillMaxHeight().clip(Sh.row).background(if (on) C.hi.copy(alpha = 0.10f) else Color.Transparent, Sh.row).clickable { onOpen(i) },
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(t.count.ifEmpty { "_" }, style = T.mono.copy(color = when { t.warn -> C.wait; on -> C.hi; else -> C.dim }), maxLines = 1)
                    Text(t.name, Modifier.padding(top = 2.dp), style = T.label.copy(fontSize = 11.5.sp, color = if (on) C.hi else C.dim), maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// a thin rounded bar: how much of a limit is used, in the limit's own colour
@Composable
fun Meter(fraction: Float, color: Color, height: Dp = 5.dp) {
    val r = RoundedCornerShape(3.dp)
    Box(Modifier.fillMaxWidth().height(height).background(C.rule2, r)) { Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color, r)) }
}

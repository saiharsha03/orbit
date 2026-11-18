package dev.hive.mobile

// The look, in one place: a near-black page with raised, rounded cards, Inter for text and Space Grotesk for titles
// and numbers, one amber accent (the main button, the picked answer, "Needs you", the open tab) and colour for state
// only (blue working, green done, red failed). The few parts every screen is made of live here too.
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// the design's tokens, as the redesign gives them
object C {
    val bg = Color(0xFF0B0D12)        // the page, code boxes
    val side = Color(0xFF0B0D12)      // wells inside a card
    val raised = Color(0xFF14171F)    // cards, the tab bar, the message box
    val lift = Color(0xFF1C2029)      // quiet buttons, the current row
    val band = Color(0xFF1C2029)      // your own messages
    val sel = Color(0xFF1C2029)       // the selected row
    val track = Color(0xFF262B36)     // meter tracks, the selected segment
    val rule = Color(0xFF262B36)      // card borders
    val rule2 = Color(0xFF3A4152)     // button and input borders
    val line = Color(0xFF1F2430)      // the line between two rows of one card
    val headRule = Color(0xFF1C2029)  // under a screen's head
    val navRule = Color(0xFF2A3040)   // the tab bar and the message box
    val hi = Color(0xFFECEEF2)        // titles, values
    val ink = Color(0xFFC9CED8)       // body text
    val dim = Color(0xFF9AA1AE)       // labels, secondary text
    val faint = Color(0xFF6C7382)     // placeholders, switched-off things
    val accent = Color(0xFFF5B544)    // the one accent
    val onAccent = Color(0xFF1A1305)
    val accentBg = Color(0xFF2A2213)
    val accentRule = Color(0xFF4A3A18)
    val accentInk = Color(0xFFE9D2A6)
    val askBg = Color(0xFF1A1710)
    val link = Color(0xFFC9CED8)      // quiet actions
    val work = Color(0xFF7AA7FF)      // working
    val wait = accent                 // needs you
    val fail = Color(0xFFFF8A7A)      // failed, nearly out
    val failBg = Color(0xFF1C1416)
    val failRule = Color(0xFF4A2A2A)
    val failRule2 = Color(0xFF5A3434)
    val failChip = Color(0xFF3A1F1F)
    val failInk = Color(0xFFC4A3A0)
    val done = Color(0xFF5FD39A)      // good
    val doneBg = Color(0xFF121A16)
    val doneRule = Color(0xFF22382C)
    val doneChip = Color(0xFF1B3326)
    val doneInk = Color(0xFF9BC4AE)
    val hold = accentInk              // paused, retrying
    val heroTop = Color(0xFF1A2033)
    val heroBottom = Color(0xFF12151D)
    val heroRule = Color(0xFF232838)
    // the limit meters: the session in the accent, the week in blue, a model's in grey
    val limit5h = accent
    val limit7d = work
    val limitModel = ink
}

// the shapes: cards 22, the greeting 28, tiles 18, buttons 14, option buttons 16, wells 12, the bars 24
object Sh {
    val card = RoundedCornerShape(22.dp)
    val hero = RoundedCornerShape(28.dp)
    val tile = RoundedCornerShape(18.dp)
    val row = RoundedCornerShape(14.dp)
    val option = RoundedCornerShape(16.dp)
    val well = RoundedCornerShape(12.dp)
    val bar = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(50)
}

// Inter and Space Grotesk, bundled (both SIL Open Font License; res/raw holds the licence texts)
val Sans = FontFamily(Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_medium, FontWeight.Medium), Font(R.font.inter_semibold, FontWeight.SemiBold))
val Display = FontFamily(Font(R.font.space_grotesk_medium, FontWeight.Medium), Font(R.font.space_grotesk_semibold, FontWeight.SemiBold), Font(R.font.space_grotesk_bold, FontWeight.Bold))
val Mono = FontFamily.Monospace

object T {
    val wordmark = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.5).sp, color = C.hi)
    val page = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp, color = C.hi)
    val sub = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 23.sp, color = C.hi)
    val headline = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 19.sp, lineHeight = 25.sp, color = C.hi)
    val num = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, color = C.hi)
    val time = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, color = C.hi)
    val title = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, color = C.hi)
    val question = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, color = C.hi)
    val body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = C.ink)
    val conv = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp, color = C.ink)
    val reply = conv
    val note = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, color = C.dim)
    val label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, color = C.dim)
    val caps = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.6.sp, color = C.dim)
    val state = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 15.sp)
    val mono = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, color = C.ink)
    val action = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, color = C.hi)
}

val Pad = 16.dp

@Composable
fun HiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = C.accent, onPrimary = C.onAccent, background = C.bg, surface = C.bg, onBackground = C.ink, onSurface = C.ink, error = C.fail)) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalTextStyle provides T.body, content = content)
    }
}

// the colour of a state word
fun stateColor(word: String): Color = when (word.uppercase()) {
    "WORKING", "LOOP", "RUNNING", "WATCHER", "OPEN" -> C.work
    "WAITING", "NEEDS YOUR INPUT", "NEEDS YOU", "FINDING", "NEW" -> C.wait
    "FAILED" -> C.fail
    "DONE", "IN USE", "ONLINE", "SEEN BY CLAUDE", "CLEARED", "MERGED" -> C.done
    "PAUSED", "AUTO-CONTINUE" -> C.hold
    else -> C.dim
}

// the words a state is shown in: sentence case, and "Needs you" where the data says WAITING
fun stateLabel(word: String): String = if (word.uppercase() == "WAITING" || word.uppercase() == "NEEDS YOUR INPUT") "Needs you" else word.lowercase().replaceFirstChar { it.uppercase() }

// a state's tinted ground and border: the accent's are the design's own, the others a wash of the colour
private fun tintBg(c: Color): Color = when (c) { C.accent -> C.accentBg; C.dim, C.ink, C.hi, C.faint -> C.lift; else -> c.copy(alpha = 0.12f) }
private fun tintRule(c: Color): Color = when (c) { C.accent -> C.accentRule; C.dim, C.ink, C.hi, C.faint -> C.rule2; else -> c.copy(alpha = 0.30f) }

// state as a pill: the word in its colour on a wash of it
@Composable
fun Pill(text: String, color: Color = C.dim, modifier: Modifier = Modifier, small: Boolean = false) =
    Text(text, modifier.background(tintBg(color), Sh.pill).border(1.dp, tintRule(color), Sh.pill).padding(horizontal = if (small) 8.dp else 10.dp, vertical = if (small) 3.dp else 6.dp), style = T.state, color = color, maxLines = 1)

@Composable
fun StateWord(word: String, modifier: Modifier = Modifier, color: Color = stateColor(word)) = Pill(stateLabel(word), color, modifier, small = true)

@Composable
fun Rule(color: Color = C.headRule) = Box(Modifier.fillMaxWidth().height(1.dp).background(color))

// the line between two rows of one card
@Composable
fun RowRule() = Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(C.line))

@Composable
fun Dot(color: Color, size: Dp = 9.dp) = Box(Modifier.size(size).background(color, CircleShape))

@Composable
fun Ico(icon: ImageVector, tint: Color = C.hi, size: Dp = 20.dp, modifier: Modifier = Modifier, desc: String? = null) = Icon(icon, contentDescription = desc, modifier = modifier.size(size), tint = tint)

// a square icon button, 44 dp: raised and bordered, or filled with the accent for the screen's main action
@Composable
fun IconBtn(icon: ImageVector, desc: String, modifier: Modifier = Modifier, primary: Boolean = false, enabled: Boolean = true, fill: Color = C.raised, tint: Color = C.hi, shape: RoundedCornerShape = Sh.row, onClick: () -> Unit) {
    val on = primary && enabled
    Box(
        modifier.size(44.dp).clip(shape).background(if (on) C.accent else fill, shape).then(if (on) Modifier else Modifier.border(1.dp, if (fill == C.raised) C.rule else C.navRule, shape))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Ico(icon, if (on) C.onAccent else if (enabled) tint else C.faint, desc = desc) }
}

// an icon in a small tinted square: what leads a failed or finished card and the rows of More
@Composable
fun IconChip(icon: ImageVector, tint: Color = C.dim, fill: Color = C.lift) =
    Box(Modifier.size(36.dp).background(fill, Sh.well), contentAlignment = Alignment.Center) { Ico(icon, tint, 18.dp) }

// A screen's head. With onBack: a back button, the title with a line under it (the repo, a count), and what sits at the
// right. Without: the big title of a tab.
@Composable
fun Head(
    title: String, count: String = "", right: String = "", rightColor: Color = C.dim, sub: Boolean = false, mark: Boolean = false,
    onRight: (() -> Unit)? = null, onBack: (() -> Unit)? = null, subtitle: String = "", trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val word: @Composable RowScope.() -> Unit = {
        if (right.isNotEmpty()) {
            if (onRight != null) Act(right, rightColor.takeIf { it != C.dim } ?: C.hi, onClick = onRight)
            else Text(right, style = T.note, color = rightColor, maxLines = 1)
        }
        if (trailing != null) trailing()
    }
    if (onBack != null) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBtn(Ic.back, "Back", onClick = onBack)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = T.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val under = listOf(subtitle, count).filter { it.isNotEmpty() }.joinToString(" · ")
                if (under.isNotEmpty()) Text(under, style = T.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            word()
        }
        Rule()
    } else {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (mark) androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.ic_orbit_mark), contentDescription = null, modifier = Modifier.size(28.dp))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, Modifier.weight(1f, fill = false), style = T.page, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (count.isNotEmpty()) Text(count, Modifier.padding(bottom = 6.dp), style = T.note)
            }
            word()
        }
    }
}

// a section label: small capitals in grey, a count at the right
@Composable
fun Group(label: String, right: String = "", color: Color = C.dim) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), style = T.caps, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (right.isNotEmpty()) Text(right, style = T.label, maxLines = 1)
    }
}

fun Modifier.selected(on: Boolean): Modifier = if (!on) this else background(C.sel, Sh.row)

enum class Tone { Plain, Ask, Fail, Done }

// The card everything sits in: raised, a hairline border, 22 dp corners. tone: an amber wash for a question, red for
// a failure, green for something finished. pad 6 for a card of rows (each row brings its own 12).
@Composable
fun Panel(modifier: Modifier = Modifier, tone: Tone = Tone.Plain, pad: Dp = 16.dp, gap: Dp = 12.dp, picked: Boolean = false, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val fill = when (tone) { Tone.Ask -> C.askBg; Tone.Fail -> C.failBg; Tone.Done -> C.doneBg; else -> C.raised }
    val edge = when (tone) { Tone.Ask -> C.accentRule; Tone.Fail -> C.failRule; Tone.Done -> C.doneRule; else -> C.rule }
    Column(
        // picked: the card whose thread is open beside the list (unfolded) takes the accent's border
        modifier.fillMaxWidth().padding(horizontal = Pad, vertical = 5.dp).clip(Sh.card).background(fill, Sh.card).border(if (picked) 2.dp else 1.dp, if (picked) C.accent else edge, Sh.card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(pad),
        verticalArrangement = Arrangement.spacedBy(gap), content = content,
    )
}

// one row of a card: a state dot or an icon, the title over a grey line, a time or a word at the right, and its
// actions under it
@Composable
fun ListRow(
    title: String, meta: String = "", word: String = "", note: String = "", detail: String = "", on: Boolean = false,
    metaWord: String = "", metaColor: Color = C.dim,
    icon: String = "", badge: Color? = null, oneLine: Boolean = false, noteColor: Color = C.dim,
    onClick: (() -> Unit)? = null, actions: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().clip(Sh.option).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).selected(on).padding(12.dp), verticalAlignment = if (actions != null) Alignment.Top else Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (badge != null) Box(Modifier.padding(top = if (actions != null) 6.dp else 0.dp)) { Dot(badge) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = T.title, maxLines = if (oneLine) 1 else 2, overflow = TextOverflow.Ellipsis)
            if (word.isNotEmpty() || note.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (word.isNotEmpty()) StateWord(word)
                if (note.isNotEmpty()) Text(note, style = T.note, color = noteColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (detail.isNotEmpty()) Text(detail, style = T.mono.copy(fontSize = 12.sp, color = C.dim), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (actions != null) Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
        if (metaWord.isNotEmpty()) Pill(metaWord, metaColor, small = true)
        else if (meta.isNotEmpty()) Text(meta, style = T.label, maxLines = 1)
    }
}

// a row's action: a small outlined button (red for one that stops or cancels)
@Composable
fun Act(label: String, color: Color = C.hi, enabled: Boolean = true, onClick: () -> Unit) {
    val edge = if (color == C.fail) C.failRule2 else if (color == C.done) C.doneRule else C.rule2
    Box(Modifier.heightIn(min = 36.dp).clip(Sh.well).border(1.dp, if (enabled) edge else C.rule, Sh.well).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, style = T.action.copy(fontSize = 13.sp), color = if (enabled) color else C.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// buttons: outlined for the rest, filled with the accent for the main action, a red outline to stop
@Composable
fun Btn(label: String, modifier: Modifier = Modifier, primary: Boolean = false, danger: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val fill = when { !enabled -> C.lift; primary -> C.accent; else -> Color.Transparent }
    val ink = when { !enabled -> C.faint; primary -> C.onAccent; danger -> C.fail; else -> C.hi }
    val edge = when { !enabled || primary -> Color.Transparent; danger -> C.failRule2; else -> C.rule2 }
    Box(
        modifier.heightIn(min = 46.dp).clip(Sh.row).background(fill, Sh.row).border(1.dp, edge, Sh.row).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = T.action.copy(fontSize = 15.sp, fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Medium), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

// a switch: the accent when on
@Composable
fun Toggle(on: Boolean, onChange: ((Boolean) -> Unit)?) {
    Box(
        Modifier.size(46.dp, 28.dp).clip(CircleShape).background(if (on) C.accent else C.lift, CircleShape).then(if (on) Modifier else Modifier.border(1.dp, C.rule2, CircleShape))
            .then(if (onChange != null) Modifier.clickable(role = Role.Switch) { onChange(!on) } else Modifier),
    ) { Box(Modifier.padding(start = if (on) 21.dp else 4.dp, top = 4.dp).size(20.dp).background(if (on) C.onAccent else C.dim, CircleShape)) }
}

// a setting: its name, one line about it, and a switch or a word at the right (a row of a card)
@Composable
fun SettingRow(name: String, about: String = "", on: Boolean? = null, word: String = "", color: Color = C.hi, onClick: (() -> Unit)? = null, onChange: ((Boolean) -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(Sh.option).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).heightIn(min = 44.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(name, style = T.title, color = color)
            if (about.isNotEmpty()) Text(about, style = T.label.copy(lineHeight = 17.sp))
        }
        if (word.isNotEmpty()) Text(word, style = T.note, maxLines = 1)
        if (on != null) Toggle(on, onChange)
        else if (onClick != null && word.isEmpty()) Ico(Ic.next, C.dim, 18.dp)
    }
}

// text input in a rounded, bordered box
@Composable
fun UnderlineInput(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, minLines: Int = 1, maxLines: Int = 6, style: TextStyle = T.conv.copy(color = C.hi), rule: Color = C.rule, fill: Color = C.raised, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.heightIn(min = 46.dp).background(fill, Sh.row).border(1.dp, rule, Sh.row).padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = style.copy(color = C.faint), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(value, onChange, Modifier.fillMaxWidth(), textStyle = style, cursorBrush = SolidColor(C.accent), minLines = minLines, maxLines = maxLines,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
        }
        if (trailing != null) { Spacer(Modifier.width(10.dp)); trailing() }
    }
}

// A card with a small coloured title (an error, a confirm, an update): the body under it, the actions side by side at
// the bottom, the main one last.
@Composable
fun Card(title: String, dot: Color, modifier: Modifier = Modifier, actions: (@Composable RowScope.() -> Unit)? = null, body: @Composable ColumnScope.() -> Unit) {
    val tone = when (dot) { C.fail -> Tone.Fail; C.wait -> Tone.Ask; C.done -> Tone.Done; else -> Tone.Plain }
    val ink = when (dot) { C.fail -> C.fail; C.wait -> C.accentInk; C.done -> C.done; else -> C.dim }
    Panel(modifier, tone) {
        Text(title, style = T.note.copy(fontWeight = FontWeight.SemiBold), color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = body)
        if (actions != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), content = actions)
    }
}

// A question before something that cannot be taken back, in the page (no floating dialog): what will happen and the
// two buttons, side by side.
@Composable
fun ConfirmBar(title: String, text: String, confirm: String, danger: Boolean = false, cancel: String = "Cancel", onCancel: () -> Unit, onConfirm: () -> Unit, extra: (@Composable ColumnScope.() -> Unit)? = null) {
    Panel(tone = if (danger) Tone.Fail else Tone.Ask) {
        Text(title, style = T.question)
        if (text.isNotEmpty()) Text(text, style = T.body.copy(color = if (danger) C.failInk else C.ink))
        if (extra != null) extra()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(cancel, Modifier.weight(1f), onClick = onCancel)
            Btn(confirm, Modifier.weight(1f), primary = !danger, danger = danger, onClick = onConfirm)
        }
    }
}

// what went wrong, or how the link is: a failure is a red card with its icon, anything else a quiet line with a dot
@Composable
fun Notice(text: String, color: Color = C.fail) {
    if (text.isEmpty()) return
    if (color == C.fail) Panel(tone = Tone.Fail, pad = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconChip(Ic.alert, C.fail, C.failChip)
            Text(text, Modifier.weight(1f), style = T.note.copy(color = C.failInk, lineHeight = 19.sp))
        }
    } else Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Dot(color, 7.dp)
        Text(text, style = T.note.copy(lineHeight = 19.sp), color = if (color == C.dim) C.dim else C.ink)
    }
}

@Composable
fun Empty(text: String) { if (text.isNotEmpty()) Text(text, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), style = T.note.copy(lineHeight = 19.sp)) }

// a choice among a few: one bordered box, the picked one a shade lighter
@Composable
fun Segmented(options: List<Pair<String, String>>, picked: String, modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    Row(modifier.fillMaxWidth().padding(horizontal = Pad).background(C.raised, Sh.option).border(1.dp, C.rule, Sh.option).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (id, label) ->
            val on = id == picked
            Box(Modifier.weight(1f).heightIn(min = 40.dp).clip(Sh.well).background(if (on) C.track else Color.Transparent, Sh.well).clickable(role = Role.Tab) { onPick(id) }.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                Text(label, style = T.note.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium), color = if (on) C.hi else C.dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// a small rounded button in a row of them (a thread's Work, Rewind, Changes; its mode and model): lighter when open
@Composable
fun Chip(label: String, on: Boolean = false, value: String = "", color: Color = C.hi, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        Modifier.heightIn(min = 36.dp).clip(Sh.well).background(if (on) C.track else C.raised, Sh.well).border(1.dp, if (on) C.rule2 else C.rule, Sh.well).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Ico(icon, if (on) C.hi else C.dim, 15.dp)
        Text(label, style = T.note.copy(fontWeight = FontWeight.Medium), color = if (value.isEmpty()) (if (on) C.hi else C.ink) else C.dim, maxLines = 1)
        if (value.isNotEmpty()) Text(value, style = T.note.copy(fontWeight = FontWeight.Medium), color = color, maxLines = 1)
    }
}

// a tick box: green and ticked when done, an amber outline for the one in hand, grey otherwise
@Composable
fun Check(done: Boolean, current: Boolean = false, size: Dp = 22.dp) {
    val r = RoundedCornerShape(7.dp)
    if (done) Box(Modifier.size(size).background(C.doneChip, r), contentAlignment = Alignment.Center) { Ico(Ic.check, C.done, size * 0.62f) }
    else Box(Modifier.size(size).border(2.dp, if (current) C.accent else C.rule2, r))
}

// the tabs: an icon over its name, the open one in the accent; a count sits on the icon when something needs you.
// The bar is solid and has its own place under the lists, so nothing ever shows through it.
data class NavTab(val name: String, val icon: ImageVector, val count: Int = 0)

@Composable
fun TabBar(tabs: List<NavTab>, open: Int, onOpen: (Int) -> Unit) {
    Box(Modifier.fillMaxWidth().background(C.bg).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth().height(68.dp).clip(Sh.bar).background(C.raised, Sh.bar).border(1.dp, C.navRule, Sh.bar)) {
            tabs.forEachIndexed { i, t ->
                val on = i == open
                val tint = if (on) C.accent else C.dim
                Column(Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Tab) { onOpen(i) }, verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        Ico(t.icon, tint, 22.dp)
                        if (t.count > 0) Text(if (t.count > 9) "9+" else t.count.toString(), Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-5).dp).defaultMinSize(minWidth = 16.dp).background(C.accent, CircleShape).padding(horizontal = 4.dp),
                            style = T.state.copy(fontSize = 10.sp, lineHeight = 16.sp, color = C.onAccent, textAlign = androidx.compose.ui.text.style.TextAlign.Center))
                    }
                    Text(t.name, style = T.label.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium), color = tint, maxLines = 1)
                }
            }
        }
    }
}

// a rounded bar: how much of something is used or done
@Composable
fun Meter(fraction: Float, color: Color, height: Dp = 8.dp) {
    Box(Modifier.fillMaxWidth().height(height).background(C.track, Sh.pill)) { Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color, Sh.pill)) }
}

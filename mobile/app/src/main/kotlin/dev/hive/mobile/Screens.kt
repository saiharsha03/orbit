package dev.hive.mobile

// Every screen, drawn from plain values and callbacks: nothing here talks to Orbit, so the same code is what the
// app shows and what the screenshot tests render (src/test, ScreensTest). MainActivity.kt wires them to the relay.
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import org.json.JSONObject

// ---------------------------------------------------------------- Home

// the session's use as a ring: the number in the middle, the arc in the accent (red when nearly out)
@Composable
fun UsageRing(pct: Int, size: androidx.compose.ui.unit.Dp = 104.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width * 10f / 104f
            val inset = w / 2 + this.size.width * 5f / 104f
            val box = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
            drawArc(C.track, 0f, 360f, false, Offset(inset, inset), box, style = Stroke(w))
            if (pct > 0) drawArc(if (pct >= 90) C.fail else C.accent, -90f, 360f * pct.coerceIn(0, 100) / 100f, false, Offset(inset, inset), box, style = Stroke(w, cap = StrokeCap.Round))
        }
        Text("$pct%", style = T.num.copy(fontSize = 24.sp))
    }
}

// an answer button on a card: quiet, as wide as its words
@Composable
private fun Choice(label: String, enabled: Boolean = true, onClick: () -> Unit) =
    Box(Modifier.heightIn(min = 44.dp).clip(Sh.row).background(C.lift, Sh.row).border(1.dp, C.rule2, Sh.row).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(label, style = T.action, color = if (enabled) C.hi else C.faint, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }

// a number over its name, in the state's colour; washed in the accent when it is the one that needs you
@Composable
private fun RowScope.Tile(n: Int, name: String, color: Color, warm: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).clip(Sh.tile).background(if (warm) C.accentBg else C.raised, Sh.tile).border(1.dp, if (warm) C.accentRule else C.rule, Sh.tile).clickable(onClick = onClick).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(n.toString(), style = T.num, color = color)
        Text(name, style = T.label, color = if (warm) C.accentInk else C.dim, maxLines = 1)
    }
}

// Home: the wordmark and how the link to the computer is, Orbie with one or two sentences about now, three counts,
// then what needs you (each with its one or two direct actions), what is working and what is coming up.
// busy: the thread an action is running for. working: the threads list, for the Working card.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InboxScreen(
    box: Inbox, now: Long, right: String = "", notice: String = "", busy: String = "", loaded: Boolean = true,
    threads: List<ThreadRow> = emptyList(), usage: List<Limit>? = null, link: String = "Desk connected", linkColor: Color = C.done, open: String = "",
    onAnswer: (Need, String) -> Unit = { _, _ -> }, onOpen: (String) -> Unit = {}, onRetry: (Need) -> Unit = {}, onDismiss: (Need) -> Unit = {},
    onMore: () -> Unit = {}, onUsage: () -> Unit = {}, onThreads: () -> Unit = {}, onRunning: () -> Unit = {},
) {
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val working = threads.filter { it.state == "working" }
    val paused = threads.filter { it.continues > 0 && it.state == "idle" }
    val waiting = box.needs.count { it.kind != "failed" }
    val failed = box.needs.size - waiting
    val nWorking = maxOf(box.counts.working, working.size)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("orbit", style = T.wordmark)
            Row(Modifier.background(C.raised, Sh.pill).border(1.dp, C.rule, Sh.pill).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Dot(linkColor, 7.dp)
                Text(link, style = T.label, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            if (right.isNotEmpty()) Text(right, style = T.label, maxLines = 1)
            IconBtn(Ic.settings, "Settings and more", onClick = onMore)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().clipToBounds(), state = list, contentPadding = PaddingValues(bottom = 12.dp)) {
            if (notice.isNotEmpty()) item(key = "notice") { Notice(notice) }
            item(key = "hero") {
                Row(
                    Modifier.fillMaxWidth().padding(start = Pad, end = Pad, top = 8.dp).clip(Sh.hero)
                        .drawBehind { drawRect(Brush.radialGradient(0f to C.heroTop, 0.6f to C.heroBottom, 1f to C.heroBottom, center = Offset(size.width * 0.2f, 0f), radius = size.width * 0.95f)) }
                        .border(1.dp, C.heroRule, Sh.hero).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Orbie(116.dp, moons = nWorking)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(Words.greeting(now).uppercase(), style = T.caps.copy(fontSize = 12.sp))
                        Text(Words.homeSummary(waiting, failed, nWorking, loaded), style = T.headline)
                        Usage.session(usage ?: emptyList())?.let { l ->
                            Text("Usage ${l.pct.toInt()}% · ${Usage.sky(l.pct)}", Modifier.clip(Sh.pill).background(C.bg, Sh.pill).border(1.dp, C.rule, Sh.pill).clickable(onClick = onUsage).padding(horizontal = 10.dp, vertical = 5.dp), style = T.label, color = C.ink, maxLines = 1)
                        }
                    }
                }
            }
            item(key = "tiles") {
                Row(Modifier.fillMaxWidth().padding(start = Pad, end = Pad, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tile(box.needs.size, "Needs you", C.accent, warm = box.needs.isNotEmpty()) { scope.launch { list.animateScrollToItem(if (notice.isNotEmpty()) 3 else 2) } }
                    Tile(nWorking, "Working", C.work, onClick = onThreads)
                    Tile(box.counts.running, "Background", C.hi, onClick = onRunning)
                }
            }
            item(key = "needs-head") { Group("Needs you") }
            if (box.needs.isEmpty() && loaded) item(key = "needs-none") {
                Panel(tone = Tone.Done, pad = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconChip(Ic.check, C.done, C.doneChip)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Nothing needs you", style = T.title)
                            Text("Questions and failed turns show up here.", style = T.note, color = C.doneInk)
                        }
                    }
                }
            }
            items(box.needs, key = { it.kind + ":" + it.thread }) { n ->
                val free = busy != n.thread
                // a failed turn: one row, as the design has it. Retry sends "Try that again."; the cross takes it off the list
                if (n.kind == "failed") Panel(tone = Tone.Fail, pad = 14.dp, picked = n.thread == open, onClick = { onOpen(n.thread) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconChip(Ic.alert, C.fail, C.failChip)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(n.title, style = T.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(listOf("Turn failed " + Words.age(n.at, now).let { if (it.isEmpty()) "" else "$it ago" }, n.note).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · "), style = T.note, color = C.failInk, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Act("Retry", C.fail, enabled = free) { onRetry(n) }
                        Box(Modifier.size(36.dp).clip(Sh.well).clickable(enabled = free, role = Role.Button) { onDismiss(n) }, contentAlignment = Alignment.Center) { Ico(Ic.close, C.failInk, 16.dp, desc = "Dismiss") }
                    }
                } else Panel(picked = n.thread == open, onClick = { onOpen(n.thread) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(n.title + " · " + (if (n.askId.isNotEmpty()) "question" else "waiting"), Modifier.weight(1f), style = T.note, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(Words.age(n.at, now), Modifier.padding(start = 10.dp), style = T.label, maxLines = 1)
                    }
                    Text(n.question.ifEmpty { n.note }, style = T.question, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    // a question one tap can answer: its first two choices, then Reply for the rest
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        n.options.take(2).forEach { o -> Choice(o, enabled = free) { onAnswer(n, o) } }
                        Box(Modifier.heightIn(min = 44.dp).clip(Sh.row).clickable { onOpen(n.thread) }.padding(horizontal = if (n.options.isEmpty()) 2.dp else 16.dp), contentAlignment = Alignment.Center) { Text("Reply…", style = T.action, color = C.accent) }
                    }
                }
            }
            if (working.isNotEmpty() || paused.isNotEmpty()) item(key = "working") {
                Spacer(Modifier.height(12.dp))
                Panel(pad = 6.dp, gap = 2.dp) {
                    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("WORKING", Modifier.weight(1f), style = T.caps)
                        if (box.counts.running > 0) Text(Words.plural(box.counts.running, "background task"), Modifier.clip(Sh.well).clickable(onClick = onRunning).padding(4.dp), style = T.label, color = C.ink)
                    }
                    working.forEach { t ->
                        ListRow(t.title, meta = Words.age(t.updated, now).takeIf { now - t.updated >= 60_000 } ?: "", note = listOf(t.folder, t.note).filter { it.isNotEmpty() }.joinToString(" · "), badge = C.work, oneLine = true, onClick = { onOpen(t.id) })
                    }
                    paused.forEach { t ->
                        ListRow(t.title, note = "Paused by the usage limit · continues ${Words.clock(t.continues, now)}", noteColor = C.accentInk, badge = C.accent, oneLine = true, onClick = { onOpen(t.id) })
                    }
                }
            }
            if (box.coming.isNotEmpty()) item(key = "coming") {
                Group("Coming up", box.counts.scheduled.takeIf { it > box.coming.size }?.let { "${box.coming.size} of $it" } ?: "")
                Panel(pad = 6.dp, gap = 0.dp) {
                    box.coming.forEachIndexed { i, w ->
                        if (i > 0) RowRule()
                        WakeRow(w, now, first = i == 0, onClick = { onOpen(w.open) })
                    }
                }
            }
        }
    }
}

// a wake-up: its time in the display face (the next one in the accent), what it will say, and whose it is
@Composable
private fun WakeRow(w: Wake, now: Long, first: Boolean, onClick: () -> Unit, more: (@Composable ColumnScope.() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(Sh.option).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(Words.clock(w.at, now), Modifier.width(68.dp), style = T.time, color = if (first) C.accent else C.hi, maxLines = 2)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(w.prompt.ifEmpty { "Continue" }, style = T.title, maxLines = if (more != null) 6 else 2, overflow = TextOverflow.Ellipsis)
            Text(listOf(w.title, Words.wakeKind(w), "in ${Words.until(w.at, now)}").filter { it.isNotEmpty() }.joinToString(" · "), style = T.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (more != null) more()
        }
    }
}

// ---------------------------------------------------------------- Threads

private val sectionOrder = listOf("waiting", "working", "watchers", "pinned", "dependabot", "recent")
private val sectionNames = mapOf("watchers" to "Watchers", "pinned" to "Pinned", "waiting" to "Needs you", "working" to "Working", "dependabot" to "Dependabot", "recent" to "Recent")

@Composable
fun ThreadsScreen(
    rows: List<ThreadRow>, now: Long, query: String = "", drafts: Set<String> = emptySet(), open: String = "", notice: String = "", canNew: Boolean = true,
    onQuery: (String) -> Unit = {}, onOpen: (String) -> Unit = {}, onNew: () -> Unit = {},
) {
    val q = query.trim()
    val shown = if (q.isEmpty()) rows else rows.filter { it.title.contains(q, true) || it.folder.contains(q, true) }
    // Orbit sends each thread's section; an older Orbit sends none, so the list stays flat
    val groups = shown.groupBy { it.section }.toList().sortedBy { (k, _) -> sectionOrder.indexOf(k).let { if (it < 0) sectionOrder.size else it } }
    Column(Modifier.fillMaxSize()) {
        Head("Threads", count = rows.size.takeIf { it > 0 }?.toString() ?: "") { IconBtn(Ic.plus, "New thread", primary = true, enabled = canNew, onClick = onNew) }
        Row(Modifier.fillMaxWidth().padding(start = Pad, end = Pad, top = 14.dp, bottom = 4.dp).heightIn(min = 46.dp).background(C.raised, Sh.option).border(1.dp, C.rule, Sh.option).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Ico(Ic.search, C.dim, 18.dp)
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Search threads", style = T.conv.copy(color = C.faint), maxLines = 1)
                androidx.compose.foundation.text.BasicTextField(query, onQuery, Modifier.fillMaxWidth(), textStyle = T.conv.copy(color = C.hi), cursorBrush = androidx.compose.ui.graphics.SolidColor(C.accent), singleLine = true)
            }
            if (query.isNotEmpty()) Box(Modifier.size(32.dp).clip(Sh.well).clickable { onQuery("") }, contentAlignment = Alignment.Center) { Ico(Ic.close, C.dim, 16.dp, desc = "Clear the search") }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().clipToBounds(), contentPadding = PaddingValues(bottom = 12.dp)) {
            if (notice.isNotEmpty()) item(key = "notice") { Notice(notice) }
            groups.forEach { (section, group) ->
                item(key = "section:$section") {
                    if (section.isNotEmpty()) Group(sectionNames[section] ?: section, group.size.toString(), if (section == "waiting") C.accent else C.dim) else Spacer(Modifier.height(10.dp))
                    Panel(pad = 6.dp, gap = 0.dp) {
                        group.forEachIndexed { i, t ->
                            if (i > 0) RowRule()
                            // a thread stopped by a usage limit is paused until its auto-continue; that is not a failure
                            val badge = when { t.state == "working" -> C.work; t.state == "waiting" -> C.wait; t.state == "failed" -> C.fail; t.continues > 0 -> C.accentInk; else -> C.rule2 }
                            // a pending wake-up or auto-continue takes the place of the age; a thread with unsent text says so
                            val due = when { t.continues > 0 -> "Continues " + Words.clock(t.continues, now); t.wakes > 0 -> "Wakes " + Words.clock(t.wakes, now); else -> "" }
                            ListRow(t.title, meta = if (t.id in drafts) "Draft" else Words.age(t.updated, now), on = t.id == open, metaWord = due, metaColor = if (t.continues > 0) C.hold else C.dim,
                                note = listOf(t.folder, t.note).filter { it.isNotEmpty() }.joinToString(" · "), badge = badge, oneLine = true, onClick = { onOpen(t.id) })
                        }
                    }
                }
            }
            if (shown.isEmpty()) item(key = "none") { Empty(if (rows.isEmpty()) "No threads yet." else "No thread matches.") }
        }
    }
}

// ---------------------------------------------------------------- one thread

@Composable
private fun ItemRow(it: Item) {
    when (it.k) {
        // your own message: a bubble at the right. Under it the time and how far it got: sending, sent, seen by Claude.
        // It is the same bubble all the way through.
        "you", "sending" -> Box(Modifier.fillMaxWidth().padding(start = 56.dp, end = Pad, top = 6.dp, bottom = 6.dp), contentAlignment = Alignment.CenterEnd) {
            val shape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)
            Column(Modifier.background(C.band, shape).border(1.dp, C.rule, shape).padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 8.dp), horizontalAlignment = Alignment.End) {
                SelectionContainer { Text(it.t.ifBlank { "A message with only pictures" }, style = T.conv.copy(color = if (it.t.isBlank()) C.dim else C.hi)) }
                val time = if (it.at > 0) Words.clock(it.at, it.at) else ""
                val word = when { it.k == "sending" -> "Sending"; it.seen == true -> "Seen"; it.seen == false -> "Sent"; else -> "" }
                if (time.isNotEmpty() || word.isNotEmpty()) Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (time.isNotEmpty()) Text(time, style = T.label.copy(fontSize = 11.sp, color = C.faint))
                    if (word.isNotEmpty()) Text(word, style = T.label.copy(fontSize = 11.sp, color = if (it.seen == true && it.k == "you") C.dim else C.faint))
                    if (it.k == "you" && it.seen != null) Ico(Ic.check, if (it.seen) C.done else C.faint, 12.dp)
                }
            }
        }
        "claude" -> SelectionContainer {
            val code = T.mono.copy(color = C.hi, fontSize = 12.5.sp, lineHeight = 19.sp)
            val head = T.conv.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold, color = C.hi)
            Markdown(it.t, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                colors = markdownColor(text = C.ink, linkText = C.accent, codeBackground = C.raised, inlineCodeBackground = C.raised, dividerColor = C.rule),
                typography = markdownTypography(h1 = head.copy(fontSize = 20.sp, lineHeight = 26.sp), h2 = head.copy(fontSize = 18.sp, lineHeight = 24.sp), h3 = head.copy(fontSize = 16.sp), h4 = head, h5 = head, h6 = head,
                    text = T.conv, paragraph = T.conv, ordered = T.conv, bullet = T.conv, list = T.conv, quote = T.conv.copy(color = C.dim), code = code, inlineCode = code))
        }
        // a tool row, folded to one quiet line: the tool, then what it ran
        "tool" -> Row(Modifier.padding(horizontal = 20.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Ico(Ic.tool, C.faint, 14.dp)
            Text(it.t.substringBefore(' '), style = T.note.copy(color = C.dim, fontWeight = FontWeight.Medium), maxLines = 1)
            Text(it.t.substringAfter(' ', ""), style = T.note.copy(color = C.faint), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        else -> Text(it.t, Modifier.padding(horizontal = 20.dp, vertical = 6.dp), style = T.label)
    }
}

// Claude's question, on an amber wash: each choice a full-width button (the picked one takes the accent's border),
// a line for something else, and Send answer.
@Composable
fun AskCard(ask: Ask, sent: Boolean = false, onAnswer: (JSONObject) -> Unit = {}) {
    val picks = remember(ask.id) { mutableStateListOf<Set<String>>().apply { repeat(ask.questions.size) { add(emptySet()) } } }
    val other = remember(ask.id) { mutableStateListOf<String>().apply { repeat(ask.questions.size) { add("") } } }
    // question text -> answer (several picks comma-separated), or null while one is unanswered
    fun answers(): JSONObject? {
        val out = JSONObject()
        ask.questions.forEachIndexed { i, q ->
            val typed = other[i].trim()
            val parts = if (q.multi) picks[i].toList() + listOfNotNull(typed.ifEmpty { null }) else if (typed.isNotEmpty()) listOf(typed) else picks[i].take(1)
            if (parts.isEmpty()) return null
            out.put(q.question, parts.joinToString(", "))
        }
        return out
    }
    val a = answers()
    Panel(tone = Tone.Ask) {
        Text("Claude asks", style = T.note.copy(fontWeight = FontWeight.SemiBold), color = C.accentInk)
        ask.questions.forEachIndexed { i, q ->
            if (q.header.isNotEmpty()) Text(q.header, style = T.label)
            Text(q.question, style = T.question.copy(fontSize = 17.sp, lineHeight = 24.sp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                q.options.forEach { o ->
                    val on = o.label in picks[i] && (q.multi || other[i].isBlank())
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(Sh.option).background(if (on) C.accentBg else C.raised, Sh.option).border(if (on) 2.dp else 1.dp, if (on) C.accent else C.rule2, Sh.option)
                            .clickable(enabled = !sent, role = if (q.multi) Role.Checkbox else Role.RadioButton) {
                                picks[i] = if (q.multi) (if (o.label in picks[i]) picks[i] - o.label else picks[i] + o.label) else setOf(o.label)
                                if (!q.multi) other[i] = ""
                            }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                    ) {
                        Text(o.label, style = T.action.copy(fontSize = 15.sp, lineHeight = 20.sp))
                        if (o.description.isNotEmpty()) Text(o.description, style = T.note)
                    }
                }
                UnderlineInput(other[i], { other[i] = it }, if (q.multi) "And something else" else "Something else", Modifier.fillMaxWidth(), maxLines = 3, rule = C.rule2, fill = C.bg)
            }
        }
        Btn(if (sent) "Sent to Claude" else "Send answer", Modifier.fillMaxWidth(), primary = true, enabled = a != null && !sent) { a?.let(onAnswer) }
    }
}

// a file picked with the paperclip, ready to go with the next send (data: base64)
data class Attachment(val name: String, val mime: String, val data: String)

// The message box: a rounded bar with the text, the paperclip, and the accent's button that sends (or a red one that
// stops Claude while it works and nothing is typed). What is attached sits above the text; Queue and Interrupt sit
// above the box while Claude works and there is something to send. It has its own place under the conversation.
@Composable
fun Composer(
    draft: String, working: Boolean, canSend: Boolean, placeholder: String = "Reply to Claude…", attachments: List<Attachment> = emptyList(),
    onDraft: (String) -> Unit = {}, onSend: () -> Unit = {}, onAttach: (() -> Unit)? = null, onRemove: (Attachment) -> Unit = {}, onStop: (() -> Unit)? = null,
    onQueue: (() -> Unit)? = null, onInterrupt: (() -> Unit)? = null,
) {
    val ok = canSend && (draft.isNotBlank() || attachments.isNotEmpty())
    Column(Modifier.fillMaxWidth().background(C.bg).padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (working && ok && (onQueue != null || onInterrupt != null)) Row(Modifier.padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onQueue != null) Act("Queue for later", onClick = onQueue)
            if (onInterrupt != null) Act("Interrupt Claude", C.fail, onClick = onInterrupt)
        }
        Column(Modifier.fillMaxWidth().background(C.raised, Sh.bar).border(1.dp, C.navRule, Sh.bar).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
            attachments.forEach { a ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Ico(Ic.attach, C.dim, 14.dp)
                    Text(a.name, Modifier.weight(1f), style = T.note.copy(color = C.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.size(32.dp).clip(Sh.well).clickable { onRemove(a) }, contentAlignment = Alignment.Center) { Ico(Ic.close, C.dim, 14.dp, desc = "Remove ${a.name}") }
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).heightIn(min = 44.dp).padding(vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                    if (draft.isEmpty()) Text(placeholder, style = T.conv.copy(color = C.faint), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    androidx.compose.foundation.text.BasicTextField(draft, onDraft, Modifier.fillMaxWidth(), textStyle = T.conv.copy(color = C.hi), cursorBrush = androidx.compose.ui.graphics.SolidColor(C.accent), maxLines = 6,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences))
                }
                if (onAttach != null) IconBtn(Ic.attach, "Attach a photo or file", fill = C.lift, tint = C.ink, shape = Sh.option, onClick = onAttach)
                // stop while Claude works and nothing waits to be sent; otherwise send (it steers a working Claude)
                if (working && !ok && onStop != null) IconBtn(Ic.stop, "Stop Claude", fill = C.failChip, tint = C.fail, shape = Sh.option, onClick = onStop)
                else IconBtn(Ic.send, if (working) "Steer Claude" else "Send", primary = true, enabled = ok, fill = C.lift, shape = Sh.option, onClick = onSend)
            }
        }
    }
}

// error: what the last request failed with. leaving: Back was pressed with unsent text. sentAsks: answered here already.
// folder: the thread's repo, under its title.
@Composable
fun ThreadScreen(
    v: ThreadView, draft: String = "", online: Boolean = true, loaded: Boolean = true, error: String = "", notice: String = "", leaving: Boolean = false, sentAsks: Set<String> = emptySet(),
    list: LazyListState = rememberLazyListState(),
    onBack: () -> Unit = {}, onStay: () -> Unit = {}, onLeave: () -> Unit = {}, onDraft: (String) -> Unit = {}, onSend: () -> Unit = {},
    onStop: () -> Unit = {}, onStopTask: (Task) -> Unit = {}, onAnswer: (Ask, JSONObject) -> Unit = { _, _ -> },
    settings: ThreadSettings? = null, settingsOpen: String = "", onSettingsOpen: (String) -> Unit = {}, onSetting: (String, String) -> Unit = { _, _ -> },
    onQueue: (() -> Unit)? = null, onInterrupt: (() -> Unit)? = null,
    more: ThreadMore? = null, morePane: String = "", now: Long = 0, rewinding: String = "", onMorePane: (String) -> Unit = {}, onRewind: (Point) -> Unit = {},
    onChanges: (() -> Unit)? = null,
    attachments: List<Attachment> = emptyList(), onAttach: (() -> Unit)? = null, onRemove: (Attachment) -> Unit = {},
    folder: String = "",
) {
    Column(Modifier.fillMaxSize()) {
        Head(v.title.ifEmpty { if (loaded) "Thread" else "Opening" }, onBack = onBack, subtitle = folder) {
            if (v.asks.isNotEmpty()) Pill("Needs you", C.accent) else if (v.working) Pill("Working", C.work)
            if (v.working) IconBtn(Ic.stop, "Stop Claude", fill = C.failChip, tint = C.fail, onClick = onStop)
        }
        Notice(notice)
        Notice(error)
        if (more != null) ThreadMoreBar(more, morePane, now, online, rewinding, onMorePane, onRewind, onChanges)
        if (leaving) ConfirmBar("Leave without sending?", "What you typed has not been sent. It is kept as a draft in this thread.", "Leave", cancel = "Stay", onCancel = onStay, onConfirm = onLeave)
        // the conversation has its own clipped place between the head and the message box: nothing of it shows through either
        LazyColumn(Modifier.weight(1f).fillMaxWidth().clipToBounds(), state = list, contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)) {
            items(v.items) { ItemRow(it) }
            // the thread's running subagents, shells, workflows and Codex runs with their live line; Stop ends one
            if (v.tasks.isNotEmpty()) item(key = "tasks") {
                Panel(pad = 6.dp, gap = 0.dp) {
                    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp)) {
                        Text("RUNNING IN THIS THREAD", Modifier.weight(1f), style = T.caps)
                        Text(v.tasks.size.toString(), style = T.label)
                    }
                    v.tasks.forEachIndexed { i, t ->
                        if (i > 0) RowRule()
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Dot(C.work)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(t.title, style = T.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(listOf(t.kind, t.now).filter { it.isNotEmpty() }.joinToString(" · "), style = T.note, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Act("Stop", C.fail) { onStopTask(t) }
                        }
                    }
                }
            }
            items(v.asks, key = { "ask:" + it.id }) { a -> AskCard(a, sent = a.id in sentAsks) { onAnswer(a, it) } }
            if (v.working && v.asks.isEmpty()) item(key = "working") {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Dot(C.work); Text("Claude is on it", style = T.note) }
            }
            if (v.items.isEmpty() && v.asks.isEmpty() && loaded && !v.working) item(key = "none") { Empty("Nothing in this thread yet.") }
        }
        if (settings != null) ThreadSettingsBar(settings, settingsOpen, onSettingsOpen, onSetting)
        Composer(draft, v.working, online, if (v.working) "Steer Claude…" else "Reply to Claude…", attachments, onDraft = onDraft, onSend = onSend, onAttach = onAttach, onRemove = onRemove, onStop = onStop, onQueue = onQueue, onInterrupt = onInterrupt)
    }
}

// ---------------------------------------------------------------- Running

// stopAll: null = no question open; else whether "also pause wake-ups" is switched on. canPause: Orbit can do that.
@Composable
fun RunningScreen(
    rows: List<RunRow>, notice: String = "", busy: Set<String> = emptySet(), stopAll: Boolean? = null, canPause: Boolean = false, threads: Int = 0, scheduled: Int = 0, loaded: Boolean = true,
    onStop: (RunRow) -> Unit = {}, onOpen: (String) -> Unit = {}, onAskStopAll: () -> Unit = {}, onPause: (Boolean) -> Unit = {}, onCancelStopAll: () -> Unit = {}, onStopAll: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxSize()) {
        Head("Running", count = if (rows.isEmpty()) "" else Words.plural(rows.size, "background task"), onBack = onBack) { Act("Stop all", if (rows.isEmpty() && threads == 0) C.faint else C.fail, onClick = onAskStopAll) }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().clipToBounds(), contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)) {
            if (notice.isNotEmpty()) item(key = "notice") { Notice(notice) }
            if (stopAll != null) item(key = "stop") { StopAllBar(threads, rows.size, scheduled, stopAll, canPause, onPause, onCancelStopAll, onStopAll) }
            if (rows.isEmpty() && loaded) item(key = "none") { Empty("No background work is running.") }
            items(rows, key = { it.thread + ":" + it.id }) { r ->
                val failed = r.state == "failed"
                Panel(pad = 6.dp, gap = 0.dp) {
                    ListRow(r.title, note = listOf(r.kind, r.threadTitle).filter { it.isNotEmpty() }.joinToString(" · "), detail = r.now, badge = if (failed) C.fail else C.work, metaWord = if (failed) "Failed" else "", metaColor = C.fail, onClick = { onOpen(r.thread) }) {
                        if (!failed && r.id.isNotEmpty()) Act("Stop", C.fail, enabled = (r.thread + ":" + r.id) !in busy) { onStop(r) }
                        Act("Open thread", C.ink) { onOpen(r.thread) }
                    }
                }
            }
        }
    }
}

// "Stop everything?" with what it will and will not do, said before it does it
@Composable
fun StopAllBar(threads: Int, tasks: Int, scheduled: Int, pause: Boolean, canPause: Boolean, onPause: (Boolean) -> Unit, onCancel: () -> Unit, onConfirm: () -> Unit) {
    ConfirmBar(
        "Stop everything?",
        "Interrupts ${Words.plural(threads, "working thread")} and stops ${Words.plural(tasks, "background task")}. " + if (canPause) "Wake-ups stay scheduled unless you switch this on." else "Wake-ups stay scheduled.",
        "Stop everything", danger = true, onCancel = onCancel, onConfirm = onConfirm,
        extra = if (!canPause) null else ({
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Also pause wake-ups and loops", style = T.title)
                    Text("$scheduled scheduled", style = T.label)
                }
                Toggle(pause, onPause)
            }
        }),
    )
}

// ---------------------------------------------------------------- Scheduled

// Usage on top (the session as a ring, the session and the week as bars), then the wake-ups. A wake-up opens under
// its row to its whole prompt and what can be done with it.
@Composable
fun ScheduledScreen(
    wakes: List<Wake>, now: Long, canRun: Boolean = false, notice: String = "", busy: Set<String> = emptySet(), loaded: Boolean = true,
    onRun: (Wake) -> Unit = {}, onCancel: (Wake) -> Unit = {}, onOpen: (String) -> Unit = {},
    usage: List<Limit>? = null, onUsage: () -> Unit = {},
) {
    var open by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Head("Scheduled", count = wakes.size.takeIf { it > 0 }?.toString() ?: "")
        LazyColumn(Modifier.weight(1f).fillMaxWidth().clipToBounds(), contentPadding = PaddingValues(top = 10.dp, bottom = 12.dp)) {
            if (notice.isNotEmpty()) item(key = "notice") { Notice(notice) }
            val session = Usage.session(usage ?: emptyList())
            if (session != null) item(key = "usage") {
                val week = Usage.week(usage ?: emptyList())
                Row(Modifier.fillMaxWidth().padding(horizontal = Pad, vertical = 5.dp).clip(Sh.bar).background(C.raised, Sh.bar).border(1.dp, C.rule, Sh.bar).clickable(onClick = onUsage).padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    UsageRing(session.pct.toInt())
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        UsageLine("Session", Usage.resetMs(session)?.let { "resets ${Words.clock(it, now)}" } ?: "${session.pct.toInt()}%", session.pct, if (session.pct >= 90) C.fail else C.limit5h)
                        if (week != null) UsageLine("Week", "${week.pct.toInt()}%", week.pct, if (week.pct >= 90) C.fail else C.limit7d)
                    }
                }
            }
            item(key = "head") { Group("Wake-ups") }
            if (wakes.isEmpty() && loaded) item(key = "none") { Empty("Nothing is scheduled.") }
            if (wakes.isNotEmpty()) item(key = "wakes") {
                Panel(pad = 6.dp, gap = 0.dp) {
                    wakes.forEachIndexed { i, w ->
                        val k = w.kind + ":" + w.id
                        val free = k !in busy
                        if (i > 0) RowRule()
                        WakeRow(w, now, first = i == 0, onClick = { open = if (open == k) "" else k }, more = if (open != k) null else ({
                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Act("Open", C.ink) { onOpen(w.open) }
                                if (canRun) Act("Run now", enabled = free) { onRun(w) }
                                Act("Cancel", C.fail, enabled = free) { onCancel(w) }
                            }
                        }))
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageLine(name: String, right: String, pct: Double, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, style = T.note); Text(right, style = T.note, color = C.hi) }
        Meter((pct / 100).toFloat(), color, 6.dp)
    }
}

// ---------------------------------------------------------------- More

@Composable
private fun MoreRow(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, about: String = "", color: Color = C.hi, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(Sh.option).clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconChip(icon, if (color == C.fail) C.fail else C.ink, if (color == C.fail) C.failChip else C.lift)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = T.title, color = color)
            if (about.isNotEmpty()) Text(about, style = T.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Ico(Ic.next, C.dim, 18.dp)
    }
}

@Composable
fun MoreScreen(
    notice: String = "", stopAll: Boolean? = null, canPause: Boolean = false, threads: Int = 0, tasks: Int = 0, scheduled: Int = 0, done: String = "", accounts: Boolean = false,
    onAskStopAll: () -> Unit = {}, onPause: (Boolean) -> Unit = {}, onCancelStopAll: () -> Unit = {}, onStopAll: () -> Unit = {},
    onUsage: () -> Unit = {}, onTeams: () -> Unit = {}, onNew: () -> Unit = {}, onSettings: () -> Unit = {},
    onNotes: (() -> Unit)? = null, onTodos: (() -> Unit)? = null, onWatchers: (() -> Unit)? = null, onWorkLog: (() -> Unit)? = null,
    onRunning: (() -> Unit)? = null, onBack: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxSize()) {
        Head("More", onBack = onBack)
        Column(Modifier.weight(1f).fillMaxWidth().clipToBounds().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 12.dp)) {
            Notice(notice)
            Notice(done, C.done)
            Panel(pad = 6.dp, gap = 0.dp) {
                MoreRow(if (accounts) "Usage and accounts" else "Usage", Ic.usage, "The plan's limits and when they reset", onClick = onUsage)
                if (onRunning != null) { RowRule(); MoreRow("Running", Ic.running, if (tasks > 0) Words.plural(tasks, "background task") else "Subagents, shells and workflows", onClick = onRunning) }
                if (onNotes != null) { RowRule(); MoreRow("Notes", Ic.notes, onClick = onNotes) }
                if (onWatchers != null) { RowRule(); MoreRow("Watchers", Ic.eye, onClick = onWatchers) }
                if (onWorkLog != null) { RowRule(); MoreRow("Work log", Ic.log, "What you did, day by day", onClick = onWorkLog) }
                RowRule(); MoreRow("Teams", Ic.team, onClick = onTeams)
                RowRule(); MoreRow("New thread", Ic.plus, onClick = onNew)
            }
            Panel(pad = 6.dp, gap = 0.dp) { MoreRow("Settings", Ic.settings, "Quiet hours, notifications, pairing", onClick = onSettings) }
            if (stopAll != null) StopAllBar(threads, tasks, scheduled, stopAll, canPause, onPause, onCancelStopAll, onStopAll)
            else Panel(tone = Tone.Fail, pad = 6.dp, gap = 0.dp) { MoreRow("Stop everything", Ic.power, "Interrupts working threads and background work", C.fail, onAskStopAll) }
        }
    }
}

// ---------------------------------------------------------------- Usage and accounts

// accounts: null when this Orbit has no account profiles (the zone is left out). switching: the account asked about.
@Composable
fun UsageScreen(
    limits: List<Limit>?, now: Long, plan: String = "", accounts: List<Account>? = null, notice: String = "", error: String = "", switching: Account? = null, busy: Boolean = false,
    working: Int = 0, wakes: List<Wake> = emptyList(),
    onBack: () -> Unit = {}, onPick: (Account) -> Unit = {}, onCancelSwitch: () -> Unit = {}, onSwitch: (Account) -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        Head("Usage", onBack = onBack) { if (plan.isNotEmpty()) Pill(plan, C.dim) }
        Column(Modifier.weight(1f).fillMaxWidth().clipToBounds().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 12.dp)) {
            Notice(notice)
            Notice(error)
            if (limits != null && limits.isEmpty()) Empty("No reading yet.")
            // the usage guard: an amber card when a window is close to its limit
            Usage.guard(limits ?: emptyList(), now)?.let { g -> Card("Usage guard", C.wait) { Text(Usage.guardWords(g, now, working, wakes), style = T.body) } }
            if (!limits.isNullOrEmpty()) Panel(pad = 6.dp, gap = 0.dp) {
                limits.forEachIndexed { i, l ->
                    if (i > 0) RowRule()
                    // nearly out turns the number and the bar red; otherwise the bar keeps the limit's own colour
                    val warn = l.pct >= 90
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(l.label, Modifier.weight(1f), style = T.title)
                            Text("${l.pct.toInt()}%", style = T.num.copy(fontSize = 22.sp, lineHeight = 26.sp), color = if (warn) C.fail else C.hi)
                        }
                        Meter((l.pct / 100).toFloat(), if (warn) C.fail else limitColor(l))
                        val resets = Usage.resets(l, now)
                        if (resets.isNotEmpty()) Text(resets, style = T.label)
                        val ahead = Usage.forecast(l, now)
                        if (ahead.isNotEmpty()) Text(ahead, style = T.label, color = C.accentInk)
                    }
                }
            }
            if (accounts != null) {
                Group("Accounts", if (accounts.size > 1) "tap to switch" else "")
                if (switching != null) ConfirmBar("Switch to ${switching.name}?", "Orbit signs in as ${switching.detail.ifEmpty { switching.name }}. Threads that are working keep the account they started with.", if (busy) "Switching" else "Switch", onCancel = onCancelSwitch, onConfirm = { if (!busy) onSwitch(switching) })
                Panel(pad = 6.dp, gap = 0.dp) {
                    accounts.forEachIndexed { i, a ->
                        if (i > 0) RowRule()
                        Row(Modifier.fillMaxWidth().clip(Sh.option).clickable(enabled = !a.inUse && !busy) { onPick(a) }.selected(a.inUse).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(a.name, style = T.title)
                                if (a.detail.isNotEmpty()) Text(a.detail, style = T.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (a.inUse) Pill("In use", C.done, small = true) else if (a.note.isNotEmpty()) Text(a.note, style = T.label)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Teams

@Composable
fun TeamsScreen(teams: List<Team>?, notice: String = "", error: String = "", busy: String = "", onBack: () -> Unit = {}, onOpen: (String) -> Unit = {}, onPause: (Team) -> Unit = {}) {
    Column(Modifier.fillMaxSize()) {
        Head("Teams", count = teams?.size?.takeIf { it > 0 }?.let { Words.plural(it, "team") } ?: "", onBack = onBack)
        LazyColumn(Modifier.weight(1f).fillMaxWidth().clipToBounds(), contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)) {
            if (notice.isNotEmpty() || error.isNotEmpty()) item(key = "notice") { Notice(notice); Notice(error) }
            if (teams != null && teams.isEmpty()) item(key = "none") { Empty("No team is running.") }
            items(teams ?: emptyList(), key = { "team:" + it.id }) { t ->
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(t.name, Modifier.weight(1f), style = T.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        StateWord(t.word)
                    }
                    if (t.goal.isNotEmpty()) Text(t.goal, style = T.body, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Column(Modifier.fillMaxWidth().background(C.bg, Sh.option).border(1.dp, C.rule, Sh.option).padding(4.dp)) {
                        t.members.forEachIndexed { i, m ->
                            if (i > 0) RowRule()
                            Row(Modifier.fillMaxWidth().clip(Sh.well).clickable(enabled = m.open.isNotEmpty()) { onOpen(m.open) }.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Dot(stateColor(m.state))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(m.name, style = T.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(m.role, style = T.label, maxLines = 1)
                                }
                                StateWord(m.state)
                            }
                        }
                    }
                    if (t.posts.isNotEmpty()) {
                        Text("LATEST POSTS", style = T.caps.copy(fontSize = 12.sp))
                        t.posts.forEach { p ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(p.from, style = T.label.copy(fontWeight = FontWeight.SemiBold, color = C.ink), maxLines = 1)
                                Text(p.text, style = T.note, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Btn(if (t.word == "PAUSED") "Resume the team" else "Pause the team", Modifier.fillMaxWidth(), enabled = busy != t.id) { onPause(t) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- New thread

// folders: null until Orbit has said which there are
@Composable
fun NewThreadScreen(
    folders: List<String>?, folder: String = "", text: String = "", busy: Boolean = false, online: Boolean = true, error: String = "", notice: String = "", leaving: Boolean = false,
    onBack: () -> Unit = {}, onStay: () -> Unit = {}, onLeave: () -> Unit = {}, onFolder: (String) -> Unit = {}, onText: (String) -> Unit = {}, onStart: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        Head("New thread", onBack = onBack)
        Column(Modifier.weight(1f).fillMaxWidth().clipToBounds().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 12.dp)) {
            Notice(notice)
            Notice(error)
            if (leaving) ConfirmBar("Leave without starting?", "What you typed is kept here as a draft.", "Leave", cancel = "Stay", onCancel = onStay, onConfirm = onLeave)
            Group("What should Claude do?")
            UnderlineInput(text, onText, "Say what you want done", Modifier.fillMaxWidth().padding(horizontal = Pad, vertical = 5.dp), minLines = 4, maxLines = 10)
            Group("Repo", if (folders == null) "asking Orbit" else "")
            // the folders Orbit can start a thread in: the picked one is lifted and ticked
            if (!folders.isNullOrEmpty()) Panel(pad = 6.dp, gap = 0.dp) {
                folders.forEachIndexed { i, f ->
                    if (i > 0) RowRule()
                    Row(Modifier.fillMaxWidth().clip(Sh.option).clickable(role = Role.RadioButton) { onFolder(f) }.selected(f == folder).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(Words.folderName(f), Modifier.weight(1f), style = T.title, color = if (f == folder) C.hi else C.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (f == folder) Ico(Ic.check, C.accent, 18.dp)
                    }
                }
            }
            if (folders != null && folders.isEmpty()) Empty("Orbit has no workspace folder set, so the thread starts in its default folder.")
        }
        // not before Orbit has listed the folders: a thread is never started in a folder nobody chose
        Btn(if (busy) "Starting" else "Start in background", Modifier.fillMaxWidth().padding(start = Pad, end = Pad, top = 8.dp, bottom = 12.dp), primary = true, enabled = text.isNotBlank() && !busy && folders != null && online, onClick = onStart)
    }
}

// ---------------------------------------------------------------- Settings

// syncs: Orbit on the computer keeps the same quiet hours. unpair: the question is open.
@Composable
fun SettingsScreen(
    quiet: Quiet, buttons: Boolean, relay: String, linkWord: String, clock: String, syncs: Boolean = false, unpair: Boolean = false, online: Boolean = true, notice: String = "", version: String = "", computer: String = "",
    onBack: () -> Unit = {}, onQuiet: (Boolean) -> Unit = {}, onFrom: () -> Unit = {}, onTo: () -> Unit = {}, onButtons: (Boolean) -> Unit = {},
    onAskUnpair: () -> Unit = {}, onCancelUnpair: () -> Unit = {}, onUnpair: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        Head("Settings", onBack = onBack)
        Column(Modifier.weight(1f).fillMaxWidth().clipToBounds().verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
            Notice(notice)
            Group("Notifications")
            Panel(pad = 6.dp, gap = 0.dp) {
                SettingRow("Quiet hours", "${quiet.from} to ${quiet.to}. Failures still ring." + if (syncs) " Orbit on the computer keeps the same hours." else "", on = quiet.on, onChange = onQuiet)
                RowRule(); SettingRow("Quiet from", "Notifications arrive without sound after this", word = quiet.from, onClick = onFrom)
                RowRule(); SettingRow("Quiet until", "", word = quiet.to, onClick = onTo)
                RowRule(); SettingRow("Questions from Claude", "With answer buttons. A choice that ships, deletes or pays opens the app instead.", on = buttons, onChange = onButtons)
            }
            Group("This phone")
            Panel(pad = 6.dp, gap = 0.dp) {
                SettingRow("Unlock with fingerprint", "Always on. Locks after 2 minutes away.", word = "On")
                RowRule(); SettingRow("Relay", listOf(relay, clock).filter { it.isNotEmpty() }.joinToString(" · "), word = linkWord)
            }
            if (unpair) ConfirmBar("Unpair this phone?",
                "This phone forgets the pairing, and Orbit on your computer is told to drop it too" + (if (online) "." else ". The computer is not reachable now: unpair there as well, in Settings, Phone app.") + " To use the app again you pair it anew in Orbit.",
                "Unpair", danger = true, onCancel = onCancelUnpair, onConfirm = onUnpair)
            else Panel(pad = 6.dp, gap = 0.dp) { SettingRow("Unpair this phone", "Forget the pairing here and on the computer", color = C.fail, onClick = onAskUnpair) }
            val about = listOfNotNull(version.takeIf { it.isNotEmpty() }?.let { "Orbit $it" }, computer.takeIf { it.isNotEmpty() }?.let { "On the computer: $it" })
            if (about.isNotEmpty()) Text(about.joinToString("\n"), Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = T.label.copy(lineHeight = 18.sp))
        }
    }
}

// ---------------------------------------------------------------- Pair, Locked

@Composable
fun PairScreen(note: String = "", error: String = "", text: String = "", paste: Boolean = false, onScan: () -> Unit = {}, onPasteMode: () -> Unit = {}, onText: (String) -> Unit = {}, onPair: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("orbit", style = T.wordmark) }
        Text("Pair with Orbit", Modifier.padding(horizontal = 20.dp), style = T.page)
        Notice(note, C.dim)
        val steps = listOf(
            "In Orbit on your computer open Settings, General, Phone app and press Pair.",
            "Point this phone's camera at the code Orbit shows.",
            "The phone reaches the relay and checks its clock against the computer's.",
            "From then on a fingerprint or the screen lock opens the app.",
        )
        Spacer(Modifier.height(10.dp))
        Panel(gap = 14.dp) {
            steps.forEachIndexed { i, s ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(26.dp).background(C.lift, Sh.well), contentAlignment = Alignment.Center) { Text((i + 1).toString(), style = T.time.copy(fontSize = 13.sp, color = C.dim)) }
                    Text(s, Modifier.weight(1f).padding(top = 2.dp), style = T.body.copy(lineHeight = 21.sp))
                }
            }
        }
        Btn("Scan the code", Modifier.fillMaxWidth().padding(horizontal = Pad, vertical = 8.dp), primary = true, onClick = onScan)
        if (!paste) Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(onClick = onPasteMode).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) { Text("No camera? Paste the code instead.", style = T.note) }
        else Column(Modifier.padding(horizontal = Pad, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            UnderlineInput(text, onText, "Paste the code from Orbit", Modifier.fillMaxWidth(), minLines = 2, maxLines = 5, style = T.mono.copy(color = C.hi))
            Btn("Pair", Modifier.fillMaxWidth(), enabled = text.isNotBlank(), onClick = onPair)
        }
        Notice(error)
    }
}

@Composable
fun LockedScreen(note: String = "", broken: Boolean = false, onUnlock: () -> Unit = {}, onForget: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Orbie(132.dp)
        Text("orbit", Modifier.padding(top = 6.dp), style = T.wordmark.copy(fontSize = 30.sp, lineHeight = 36.sp))
        Row(Modifier.padding(top = 10.dp).background(C.raised, Sh.pill).border(1.dp, C.rule, Sh.pill).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Ico(Ic.lock, C.dim, 13.dp)
            Text("Locked", style = T.label)
        }
        // broken: the Keystore dropped the key (the screen lock was removed or reset), so the pairing cannot be read again
        Text(
            if (broken) "The pairing saved on this phone can no longer be opened, because the screen lock changed. Pair this phone again in Orbit on your computer."
            else "Unlock with your fingerprint or the screen lock. The app locks again after 2 minutes away.",
            Modifier.padding(top = 20.dp), style = T.body.copy(textAlign = TextAlign.Center, lineHeight = 21.sp),
        )
        if (broken) Btn("Pair again", Modifier.fillMaxWidth().padding(top = 20.dp), primary = true, onClick = onForget)
        else Btn("Unlock", Modifier.fillMaxWidth().padding(top = 20.dp), primary = true, onClick = onUnlock)
        if (!broken && note.isNotEmpty()) Text(note, Modifier.padding(top = 14.dp), style = T.note.copy(textAlign = TextAlign.Center), color = C.fail)
    }
}

// the tabs under every list screen; Home carries how many things need you
@Composable
fun Tabs(counts: Counts, open: Int, onOpen: (Int) -> Unit) {
    TabBar(listOf(NavTab("Home", Ic.home, counts.needs), NavTab("Threads", Ic.threads), NavTab("To-dos", Ic.todos), NavTab("Scheduled", Ic.clock)), open, onOpen)
}

// each limit in its own colour: the session the accent, the week blue, a model's grey
private fun limitColor(l: Limit): Color {
    val k = l.key.lowercase()
    return when {
        k.startsWith("five_hour") -> C.limit5h
        k == "seven_day" -> C.limit7d
        k.isEmpty() && ("session" in l.label.lowercase() || "hour" in l.label.lowercase()) -> C.limit5h
        k.isEmpty() && "week" in l.label.lowercase() -> C.limit7d
        else -> C.limitModel
    }
}

package dev.hive.mobile

// The 3.0 screens: a thread's mode, model and effort, the notes, the to-dos and the watchers. Like Screens.kt, drawn
// from plain values and callbacks; MainActivity.kt wires them to the relay. Each is offered only when Orbit on the
// computer says it can do it (hello's "can").
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.time.LocalDate

data class Pick(val value: String, val label: String)
data class ThreadSettings(val live: Boolean = false, val mode: String = "", val model: String = "", val effort: String = "", val modes: List<Pick> = emptyList(), val models: List<Pick> = emptyList(), val efforts: List<String> = emptyList(), val advisor: String = "")
data class Note(val id: String, val title: String, val text: String, val updated: Long)
data class Todo(val id: String, val text: String, val status: String, val due: String)
data class Watcher(val id: String, val name: String, val goal: String, val enabled: Boolean, val status: String, val note: String, val lastRun: Long, val nextRun: Long)

object ReadMore {
    private fun picks(j: JSONObject?, name: String) = objs(j?.optJSONArray(name)).map { Pick(it.optString("value"), it.optString("label")) }.filter { it.value.isNotEmpty() }
    fun settings(j: JSONObject?): ThreadSettings = ThreadSettings(j?.optBoolean("live") ?: false, j?.optString("mode") ?: "", j?.optString("model") ?: "", j?.optString("effort") ?: "",
        picks(j, "modes"), picks(j, "models"), strs(j?.optJSONArray("efforts")))
    fun notes(j: JSONObject): List<Note> = objs(j.optJSONArray("notes")).map { Note(it.optString("id"), it.optString("title"), it.optString("text"), it.optLong("updated")) }.filter { it.id.isNotEmpty() }
    fun todos(j: JSONObject): List<Todo> = objs(j.optJSONArray("todos")).map { Todo(it.optString("id"), it.optString("text"), it.optString("status", "todo"), it.optString("due")) }.filter { it.id.isNotEmpty() }
    fun watchers(j: JSONObject): List<Watcher> = objs(j.optJSONArray("watchers")).map {
        Watcher(it.optString("id"), it.optString("name"), it.optString("goal"), it.optBoolean("enabled"), it.optString("status"), it.optString("note"), it.optLong("lastRun"), it.optLong("nextRun"))
    }.filter { it.id.isNotEmpty() }
}

// "claude-opus-5-5" -> "Opus 5.5"; an alias or a name as it is
fun modelName(id: String): String {
    val m = Regex("claude-([a-z]+)-(\\d+)(?:-(\\d{1,2}))?(?!\\d)").find(id) ?: return id
    val (family, major, minor) = m.destructured
    return family.replaceFirstChar { it.uppercase() } + " " + major + if (minor.isNotEmpty()) ".$minor" else ""
}

// ---------------------------------------------------------------- a thread's mode, model and effort

// One row of small buttons above the message box: the mode, model, effort and advisor, each with its value. Tapping
// one opens its choices in a card over the row. open: "", "mode", "model", "effort" or "advisor".
@Composable
fun ThreadSettingsBar(s: ThreadSettings, open: String, onOpen: (String) -> Unit, onPick: (String, String) -> Unit) {
    val modeLabel = s.modes.firstOrNull { it.value == s.mode }?.label ?: s.mode.ifEmpty { "default" }
    Column(Modifier.fillMaxWidth().background(C.bg)) {
        val rows: List<Pick> = when (open) {
            "mode" -> s.modes
            "model" -> s.models
            "effort" -> s.efforts.map { Pick(it, it) }
            "advisor" -> listOf(Pick("off", "Off")) + s.models.filter { it.value != "default" }
            else -> emptyList()
        }
        val now = when (open) { "mode" -> s.mode; "model" -> s.model; "advisor" -> s.advisor.ifEmpty { "off" }; else -> s.effort }
        if (open.isNotEmpty()) Panel(pad = 6.dp, gap = 0.dp) {
            if (open != "mode" && !s.live) Text("Send this thread a message first: its $open can be changed once it is running.", Modifier.padding(10.dp), style = T.note)
            else Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                rows.forEach { p ->
                    Row(Modifier.fillMaxWidth().clip(Sh.option).clickable(role = Role.RadioButton) { onPick(open, p.value) }.selected(p.value == now).heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(p.label, Modifier.weight(1f), style = T.title, color = if (p.value == now) C.hi else C.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (p.value == now) Ico(Ic.check, C.accent, 18.dp, desc = "in use")
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip("Mode", open == "mode", modeLabel, if (s.mode == "bypassPermissions") C.fail else C.hi) { onOpen(if (open == "mode") "" else "mode") }
            Chip("Model", open == "model", if (s.model.isEmpty()) "default" else modelName(s.model)) { onOpen(if (open == "model") "" else "model") }
            Chip("Effort", open == "effort", s.effort.ifEmpty { "default" }) { onOpen(if (open == "effort") "" else "effort") }
            Chip("Advisor", open == "advisor", if (s.advisor.isEmpty()) "off" else modelName(s.advisor)) { onOpen(if (open == "advisor") "" else "advisor") }
        }
    }
}

// ---------------------------------------------------------------- Notes

// editing: null = the list; else the note being written (id "" = a new one). draft: its text.
@Composable
fun NotesScreen(
    notes: List<Note>?, editing: String? = null, draft: String = "", notice: String = "", error: String = "", saving: Boolean = false,
    onBack: () -> Unit = {}, onOpen: (Note) -> Unit = {}, onNew: () -> Unit = {}, onDraft: (String) -> Unit = {}, onSave: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        if (editing != null) {
            Head(if (editing.isEmpty()) "New note" else "Note", onBack = onBack)
            Column(Modifier.weight(1f).fillMaxWidth().clipToBounds().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 8.dp)) {
                Notice(notice); Notice(error)
                UnderlineInput(draft, onDraft, "Title on the first line, then anything", Modifier.fillMaxWidth().padding(horizontal = Pad, vertical = 5.dp), minLines = 12, maxLines = 400)
            }
            Btn(if (saving) "Saving" else "Save", Modifier.fillMaxWidth().padding(start = Pad, end = Pad, top = 8.dp, bottom = 12.dp), primary = true, enabled = draft.isNotBlank() && !saving, onClick = onSave)
        } else {
            Head("Notes", count = notes?.size?.let { Words.plural(it, "note") } ?: "", onBack = onBack) { IconBtn(Ic.plus, "New note", primary = true, onClick = onNew) }
            Notice(notice); Notice(error)
            if (notes == null) Empty("Reading your notes.")
            else if (notes.isEmpty()) Empty("No notes yet. The plus starts one.")
            else LazyColumn(Modifier.weight(1f).fillMaxWidth().clipToBounds(), contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)) {
                items(notes, key = { it.id }) { n ->
                    Panel(pad = 14.dp, gap = 4.dp, onClick = { onOpen(n) }) {
                        Text(n.title.ifEmpty { "Untitled" }, style = T.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val rest = n.text.lineSequence().drop(1).map { it.trim() }.filter { it.isNotEmpty() }.take(2).joinToString(" ")
                        if (rest.isNotEmpty()) Text(rest, style = T.note, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- To-dos

// The to-dos: Open, Need to talk and Holding as one choice of three; the open ones grouped by when they are due.
// onBack: null when this is a tab. today: for the groups (the tests fix it).
@Composable
fun TodosScreen(
    todos: List<Todo>?, draft: String = "", notice: String = "", error: String = "", showDone: Boolean = false,
    onBack: (() -> Unit)? = null, onDraft: (String) -> Unit = {}, onAdd: () -> Unit = {}, onToggle: (Todo) -> Unit = {}, onShowDone: () -> Unit = {},
    today: LocalDate = LocalDate.now(),
) {
    var side by rememberSaveable { mutableStateOf("todo") }
    var adding by rememberSaveable { mutableStateOf(false) }
    val all = todos ?: emptyList()
    val open = all.filter { it.status != "done" && it.status != "talk" && it.status != "holding" }
    val talk = all.filter { it.status == "talk" }
    val holding = all.filter { it.status == "holding" }
    val done = all.filter { it.status == "done" }
    Column(Modifier.fillMaxSize()) {
        Head("To-dos", onBack = onBack) { IconBtn(if (adding) Ic.close else Ic.plus, if (adding) "Close" else "Add a to-do", primary = !adding) { adding = !adding } }
        if (adding || draft.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(start = Pad, end = Pad, top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UnderlineInput(draft, onDraft, "Add a to-do", Modifier.weight(1f), maxLines = 3)
            Btn("Add", primary = true, enabled = draft.isNotBlank(), onClick = onAdd)
        }
        Segmented(listOf("todo" to "Open · ${open.size}", "talk" to "Need to talk · ${talk.size}", "holding" to "Holding · ${holding.size}"), side, Modifier.padding(top = 14.dp)) { side = it }
        if (todos == null) { Notice(notice); Notice(error); Empty(if (error.isEmpty()) "Reading your to-dos." else ""); return@Column }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().clipToBounds(), contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)) {
            if (notice.isNotEmpty() || error.isNotEmpty()) item(key = "notice") { Notice(notice); Notice(error) }
            val shown = when (side) { "talk" -> talk; "holding" -> holding; else -> open }
            if (shown.isEmpty()) item(key = "none") { Empty(when (side) { "talk" -> "Nothing to talk about."; "holding" -> "Nothing on hold."; else -> "Nothing open." }) }
            if (side != "todo") { if (shown.isNotEmpty()) item(key = "side") { Spacer(Modifier.height(12.dp)); TodoGroup(shown, today, onToggle) } }
            else listOf("overdue" to "Overdue", "today" to "Today", "week" to "This week", "later" to "Later", "none" to "No date").forEach { (k, name) ->
                val group = shown.filter { Words.dueGroup(it.due, today) == k }
                if (group.isNotEmpty()) item(key = "group:$k") {
                    Group(name, color = when (k) { "overdue" -> C.fail; "today" -> C.accent; else -> C.dim })
                    TodoGroup(group, today, onToggle)
                }
            }
            if (done.isNotEmpty()) item(key = "done") {
                Row(Modifier.fillMaxWidth().clickable(onClick = onShowDone)) { Group(if (showDone) "Done" else "Done · tap to show", done.size.toString()) }
                if (showDone) TodoGroup(done, today, onToggle)
            }
            item(key = "foot") { Text("Claude can tick these off, move them to Need to talk or Holding, or change a due date for you.", Modifier.padding(horizontal = 24.dp, vertical = 14.dp), style = T.note.copy(lineHeight = 19.sp)) }
        }
    }
}

@Composable
private fun TodoGroup(todos: List<Todo>, today: LocalDate, onToggle: (Todo) -> Unit) {
    Panel(pad = 6.dp, gap = 0.dp) {
        todos.forEachIndexed { i, t ->
            val done = t.status == "done"
            if (i > 0) RowRule()
            Row(Modifier.fillMaxWidth().clip(Sh.option).clickable(role = Role.Checkbox) { onToggle(t) }.heightIn(min = 44.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Check(done)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(t.text, style = T.title.copy(color = if (done) C.dim else C.hi, textDecoration = if (done) TextDecoration.LineThrough else null))
                    val due = if (t.due.isEmpty()) "" else Words.due(t.due, today)
                    if (due.isNotEmpty() && !done) Text(due, style = T.label, color = if (due.startsWith("was due")) C.fail else C.dim)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Watchers

// busy: the watcher a run was just asked for. now: the time, for "next in 2 h".
@Composable
fun WatchersScreen(
    watchers: List<Watcher>?, notice: String = "", error: String = "", busy: String = "", done: String = "", now: Long = 0,
    onBack: () -> Unit = {}, onRun: (Watcher) -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        Head("Watchers", count = watchers?.size?.let { Words.plural(it, "watcher") } ?: "", onBack = onBack)
        Notice(notice); Notice(error); Notice(done, C.done)
        if (watchers == null) Empty("Reading the watchers.")
        else if (watchers.isEmpty()) Empty("No watchers on this computer.")
        else LazyColumn(Modifier.weight(1f).fillMaxWidth().clipToBounds(), contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)) {
            items(watchers, key = { it.id }) { w ->
                Panel(gap = 8.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(w.name, Modifier.weight(1f), style = T.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        StateWord(if (!w.enabled) "OFF" else w.status.ifEmpty { "idle" })
                    }
                    if (w.goal.isNotEmpty()) Text(w.goal, style = T.body, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (w.note.isNotEmpty()) Text(w.note, style = T.note, color = C.accentInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val times = listOf(if (w.lastRun > 0) "last ${Words.age(w.lastRun, now)} ago" else "", if (w.nextRun > 0) "next in ${Words.until(w.nextRun, now)}" else "").filter { it.isNotEmpty() }.joinToString(" · ")
                        Text(times, Modifier.weight(1f), style = T.label)
                        Act(if (busy == w.id) "Starting" else "Run now", enabled = w.enabled && busy.isEmpty()) { onRun(w) }
                    }
                }
            }
        }
    }
}

package dev.hive.mobile

// The work log, read-only: one day's summary (Work and Personal bullets), the threads you typed in and your commits,
// as Orbit on the computer puts them together (worklog.ts). Days move with Previous and Next; All / Work / Personal
// picks a side.
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LogThread(val id: String, val title: String, val project: String, val personal: Boolean, val prompts: Int, val first: Long, val last: Long, val asked: List<String>)
data class LogCommit(val repo: String, val hash: String, val at: Long, val subject: String, val personal: Boolean)
data class WorkDay(val day: String, val summary: String, val threads: List<LogThread>, val commits: List<LogCommit>, val done: List<String>)

object WorkLog {
    private fun fmt() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    fun today(): String = fmt().format(Date())
    fun shift(day: String, n: Int): String {
        val c = Calendar.getInstance()
        c.time = fmt().parse(day) ?: Date()
        c.add(Calendar.DAY_OF_MONTH, n)
        return fmt().format(c.time)
    }
    fun label(day: String): String = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(fmt().parse(day) ?: Date())
    fun time(ms: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))

    fun read(j: JSONObject): WorkDay {
        val l = j.optJSONObject("log") ?: JSONObject()
        return WorkDay(
            l.optString("day"), l.optString("summary"),
            objs(l.optJSONArray("threads")).map { LogThread(it.optString("id"), it.optString("title"), it.optString("project"), it.optBoolean("personal"), it.optInt("prompts"), it.optLong("first"), it.optLong("last"), strs(it.optJSONArray("asked"))) },
            objs(l.optJSONArray("commits")).map { LogCommit(it.optString("repo"), it.optString("hash"), it.optLong("at"), it.optString("subject"), it.optBoolean("personal")) },
            objs(l.optJSONArray("done")).map { it.optString("text") },
        )
    }

    /** the summary's lines for one side: "## Work" / "## Personal" sections, all of it for "all" */
    fun summaryLines(text: String, side: String): List<String> {
        val lines = text.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
        if (side == "all") return lines
        val out = mutableListOf<String>()
        var on = false
        for (l in lines) {
            if (l.startsWith("## ")) { on = l.removePrefix("## ").trim().lowercase().startsWith(side); continue }
            if (on) out.add(l)
        }
        return out
    }

    fun keep(personal: Boolean, side: String) = side == "all" || personal == (side == "personal")
}

// "- **Orbit**: did a thing" with the bold part bold
@Composable
private fun SummaryLine(line: String) {
    if (line.startsWith("## ")) { Group(line.removePrefix("## ").trim()); return }
    val body = line.removePrefix("- ").removePrefix("* ")
    val text = buildAnnotatedString {
        var rest = body
        while (rest.isNotEmpty()) {
            val a = rest.indexOf("**")
            val b = if (a >= 0) rest.indexOf("**", a + 2) else -1
            if (a < 0 || b < 0) { append(rest); break }
            append(rest.substring(0, a))
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = C.hi)) { append(rest.substring(a + 2, b)) }
            rest = rest.substring(b + 2)
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = Pad, vertical = 4.dp)) {
        Text("-", Modifier.padding(end = 10.dp), style = T.body.copy(color = C.faint))
        Text(text, style = T.body)
    }
}

@Composable
fun WorkLogScreen(
    log: WorkDay?, day: String, side: String, notice: String = "", error: String = "",
    onBack: () -> Unit = {}, onSide: (String) -> Unit = {}, onDay: (Int) -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        Head("Work log", sub = true, onBack = onBack)
        Notice(notice); Notice(error)
        Row(Modifier.fillMaxWidth().padding(horizontal = Pad, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Previous", Modifier.clickable { onDay(-1) }.padding(end = 14.dp, top = 4.dp, bottom = 4.dp), style = T.action)
            Text(WorkLog.label(day), Modifier.weight(1f), style = T.title)
            val last = day >= WorkLog.today()
            Text("Next", Modifier.clickable(enabled = !last) { onDay(1) }.padding(start = 14.dp, top = 4.dp, bottom = 4.dp), style = T.action.copy(color = if (last) C.faint else C.link))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = Pad, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            for ((id, label) in listOf("all" to "All", "work" to "Work", "personal" to "Personal"))
                Text(label, Modifier.clickable { onSide(id) }.padding(vertical = 4.dp), style = T.action.copy(color = if (side == id) C.hi else C.dim, fontWeight = if (side == id) FontWeight.SemiBold else FontWeight.Normal))
        }
        Rule()
        if (log == null) { Empty("Reading the day."); return@Column }
        val threads = log.threads.filter { WorkLog.keep(it.personal, side) }
        val commits = log.commits.filter { WorkLog.keep(it.personal, side) }
        val summary = WorkLog.summaryLines(log.summary, side)
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item { Group("Summary") }
            if (summary.isEmpty()) item { Empty(if (log.summary.isBlank()) "No summary yet. Orbit writes it the next morning." else "Nothing on this side.") }
            items(summary) { SummaryLine(it) }
            item { Group("Threads", threads.size.toString()) }
            if (threads.isEmpty()) item { Empty("No prompts typed this day.") }
            items(threads, key = { "t" + it.id }) { t ->
                Column(Modifier.fillMaxWidth().padding(horizontal = Pad, vertical = 9.dp)) {
                    Text(t.title, style = T.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val span = if (t.last - t.first > 60_000) "${WorkLog.time(t.first)} to ${WorkLog.time(t.last)}" else WorkLog.time(t.first)
                    Text(listOf(span, t.project, Words.plural(t.prompts, "prompt"), if (side == "all") (if (t.personal) "personal" else "work") else "").filter { it.isNotEmpty() }.joinToString(", "), style = T.label)
                }
                Rule()
            }
            item { Group("Commits", commits.size.toString()) }
            if (commits.isEmpty()) item { Empty("No commits this day.") }
            items(commits, key = { "c" + it.hash + it.repo }) { c ->
                Column(Modifier.fillMaxWidth().padding(horizontal = Pad, vertical = 8.dp)) {
                    Text(c.subject, style = T.body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${WorkLog.time(c.at)}, ${c.repo}", style = T.label)
                }
            }
            if (log.done.isNotEmpty()) {
                item { Group("To-dos done", log.done.size.toString()) }
                items(log.done) { Text(it, Modifier.fillMaxWidth().padding(horizontal = Pad, vertical = 6.dp), style = T.body) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

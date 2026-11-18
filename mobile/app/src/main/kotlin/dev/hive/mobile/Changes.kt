package dev.hive.mobile

// 3.2: a thread's changes. Its branch, its pull request (checks, and why it cannot be merged when it cannot), the files
// it changes with their counts, one file's diff, and merging the PR after a second tap that shows exactly what merges.
// Orbit decides whether a merge is allowed (and checks again when it runs); the app only shows it.
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.json.JSONObject

// area: "branch" (committed, against the PR's base), "staged", "unstaged" or "untracked" (not committed yet)
data class ChangeFile(val path: String, val add: Int, val del: Int, val binary: Boolean, val area: String)
// block: why Orbit will not merge it now ("" = it will). checks: none, pending, failing or passing.
data class PrCard(val number: Int, val url: String, val title: String, val state: String, val draft: Boolean, val headSha: String, val base: String, val checks: String, val checksText: String, val repo: String, val method: String, val block: String)
data class Changes(val branch: String, val upstream: String, val ahead: Int, val behind: Int, val files: List<ChangeFile>, val more: Int, val note: String, val pr: PrCard?, val prUnavailable: String)
data class DiffView(val text: String, val totalLines: Int, val shown: Int, val truncated: Boolean)

object ReadChanges {
    // JSON null reads as "" (optString would say "null")
    private fun JSONObject.str(k: String): String = if (isNull(k)) "" else optString(k)

    fun changes(j: JSONObject?): Changes? {
        if (j == null) return null
        val info = j.optJSONObject("pr")
        val p = info?.optJSONObject("pr")
        val pr = if (info == null || p == null) null else (
            PrCard(p.optInt("number"), p.str("url"), p.str("title"), p.str("state"), p.optBoolean("isDraft"), p.str("headRefOid"), p.str("baseRefName"),
                p.optJSONObject("checks")?.str("state") ?: "none", p.str("checksText"), info.str("repo"), info.str("method"), info.str("block"))
        )
        return Changes(
            j.str("branch"), j.str("upstream"), j.optInt("ahead"), j.optInt("behind"),
            objs(j.optJSONArray("files")).map { ChangeFile(it.str("path"), it.optInt("add"), it.optInt("del"), it.optBoolean("binary"), it.str("area").ifEmpty { "branch" }) }.filter { it.path.isNotEmpty() },
            j.optInt("more"), j.str("note"), pr?.takeIf { it.number > 0 }, j.str("prUnavailable"),
        )
    }

    fun diff(j: JSONObject?): DiffView = if (j == null) DiffView("", 0, 0, false) else DiffView(j.str("text"), j.optInt("totalLines"), j.optInt("shown"), j.optBoolean("truncated"))

    // what Orbit said about a merge: (true, "Merged #12 (squash).") or (false, its reason)
    fun merge(j: JSONObject?): Pair<Boolean, String> {
        if (j == null) return false to "Orbit did not say whether it merged."
        return if (j.optBoolean("ok")) true to "Merged #${j.optInt("number")} (${j.str("method")})." else false to j.str("reason").ifEmpty { "Not merged." }
    }

    // a diff line's kind, for its colour
    fun lineKind(l: String): String = when {
        l.startsWith("… truncated") -> "note"
        l.startsWith("+++") || l.startsWith("---") || l.startsWith("diff --git") || l.startsWith("index ") -> "meta"
        l.startsWith("@@") -> "hunk"
        l.startsWith("+") -> "add"
        l.startsWith("-") -> "del"
        else -> "ctx"
    }

    fun allowed(p: PrCard): Boolean = p.block.isEmpty() && p.headSha.length == 40 && p.method.isNotEmpty()
}

private fun checksColor(s: String): Color = when (s) { "passing" -> C.done; "failing" -> C.fail; "pending" -> C.accent; else -> C.dim }
private fun areaWord(a: String): String = when (a) { "staged" -> "staged"; "unstaged" -> "uncommitted"; "untracked" -> "new, uncommitted"; else -> "" }

// canMerge: this Orbit merges from the phone and the computer is online. confirming: the PR the confirm shows (as it
// was on screen when Merge was tapped). result: what the last merge said, ok or not.
@Composable
fun ChangesScreen(
    c: Changes?, notice: String = "", error: String = "", result: Pair<Boolean, String>? = null, canMerge: Boolean = false, confirming: PrCard? = null, merging: Boolean = false,
    onBack: () -> Unit = {}, onFile: (ChangeFile) -> Unit = {}, onAskMerge: (PrCard) -> Unit = {}, onCancelMerge: () -> Unit = {}, onMerge: (PrCard) -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        Head("Changes", count = c?.let { Words.plural(it.files.size + it.more, "file") } ?: "", onBack = onBack, subtitle = c?.branch ?: "")
        if (c == null) { Notice(notice); Notice(error); Empty(if (error.isEmpty()) "Reading the changes." else ""); return@Column }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().clipToBounds(), contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)) {
            if (notice.isNotEmpty() || error.isNotEmpty()) item(key = "notice") { Notice(notice); Notice(error) }
            result?.let { (ok, text) -> item(key = "result") { Notice(text, if (ok) C.done else C.fail) } }
            confirming?.let { p ->
                item(key = "confirm") {
                    ConfirmBar("Merge #${p.number}?", p.title, if (merging) "Merging" else "Merge", onCancel = onCancelMerge, onConfirm = { if (!merging) onMerge(p) }) {
                        Column(Modifier.fillMaxWidth().background(C.bg, Sh.well).border(1.dp, C.rule, Sh.well).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("Repo" to p.repo, "Checks" to p.checksText, "Method" to p.method, "Head" to p.headSha.take(7), "Into" to p.base).forEach { (k, v) ->
                                Row { Text(k, Modifier.width(64.dp), style = T.label); Text(v, style = T.mono.copy(fontSize = 12.sp, color = if (k == "Checks") checksColor(p.checks) else C.ink)) }
                            }
                        }
                    }
                }
            }
            item(key = "branch") {
                val ab = if (c.upstream.isEmpty()) "no upstream" else listOf(if (c.ahead > 0) "${c.ahead} ahead" else "", if (c.behind > 0) "${c.behind} behind" else "").filter { it.isNotEmpty() }.joinToString(", ").ifEmpty { "up to date with ${c.upstream}" }
                Panel(pad = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconChip(Ic.branch)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(c.branch.ifEmpty { "detached HEAD" }, style = T.mono.copy(color = C.hi), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(ab, style = T.label)
                        }
                    }
                }
            }
            c.pr?.let { p -> item(key = "pr") { PrCardView(p, canMerge && confirming == null && !merging) { onAskMerge(p) } } }
            if (c.pr == null && c.prUnavailable.isNotEmpty()) item(key = "nopr") { Notice("GitHub: ${c.prUnavailable}", C.dim) }
            if (c.note.isNotEmpty()) item(key = "note") { Notice(c.note, C.accent) }
            item(key = "files") {
                Group("Files", (c.files.size + c.more).toString())
                if (c.files.isEmpty()) Empty("No changed files.")
                else Panel(pad = 6.dp, gap = 0.dp) {
                    c.files.forEachIndexed { i, f ->
                        if (i > 0) RowRule()
                        Row(Modifier.fillMaxWidth().clip(Sh.option).clickable { onFile(f) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(Words.folderName(f.path), style = T.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(listOf(f.path.substringBeforeLast('/', ""), areaWord(f.area)).filter { it.isNotEmpty() }.joinToString(" · "), style = T.label, color = if (areaWord(f.area).isEmpty()) C.dim else C.accentInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (f.binary) Text("binary", style = T.label)
                            else {
                                Text("+${f.add}", style = T.mono.copy(fontSize = 12.sp, color = C.done))
                                Text("-${f.del}", style = T.mono.copy(fontSize = 12.sp, color = C.fail))
                            }
                        }
                    }
                }
                if (c.more > 0) Empty("And ${Words.plural(c.more, "more file")}: open the computer for the rest.")
            }
        }
    }
}

@Composable
private fun PrCardView(p: PrCard, canMerge: Boolean, onMerge: () -> Unit) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${p.repo} · #${p.number}", Modifier.weight(1f), style = T.note, maxLines = 1, overflow = TextOverflow.Ellipsis)
            StateWord(if (p.draft) "DRAFT" else p.state, color = if (p.state == "MERGED") C.done else if (p.state == "OPEN" && !p.draft) C.work else C.dim)
        }
        Text(p.title, style = T.question, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dot(checksColor(p.checks))
            Text(p.checksText, style = T.note, color = C.ink)
        }
        if (p.block.isNotEmpty()) Text(p.block, style = T.note, color = C.accentInk)
        if (p.state == "OPEN") Btn("Merge", Modifier.fillMaxWidth(), primary = true, enabled = canMerge && ReadChanges.allowed(p), onClick = onMerge)
    }
}

// one file's diff: mono, scrolling both ways, added lines green, removed red, hunk heads dim, the cut said
@Composable
fun DiffScreen(f: ChangeFile, d: DiffView?, notice: String = "", onBack: () -> Unit = {}) {
    Column(Modifier.fillMaxSize()) {
        Head(Words.folderName(f.path), onBack = onBack, subtitle = f.path.substringBeforeLast('/', ""))
        Notice(notice)
        if (d == null) { if (notice.isEmpty()) Empty("Reading the diff."); return@Column }
        if (d.text.isEmpty()) { Empty("No difference to show (a binary file, or nothing changed)."); return@Column }
        if (d.truncated) Notice("Showing ${d.shown} of ${d.totalLines} lines.", C.accent)
        val text = remember(d.text) {
            buildAnnotatedString {
                d.text.split('\n').forEachIndexed { i, l ->
                    if (i > 0) append('\n')
                    val color = when (ReadChanges.lineKind(l)) { "add" -> C.done; "del" -> C.fail; "hunk" -> C.dim; "meta" -> C.faint; "note" -> C.accentInk; else -> C.ink }
                    withStyle(SpanStyle(color = color)) { append(l) }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp).clip(Sh.option).background(C.raised, Sh.option).border(1.dp, C.rule, Sh.option).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
            Text(text, Modifier.padding(horizontal = 12.dp, vertical = 10.dp), style = T.mono.copy(fontSize = 12.sp, lineHeight = 17.sp), softWrap = false)
        }
    }
}

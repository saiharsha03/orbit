package dev.hive.mobile

// What the screens show, read from Orbit's JSON, and the small rules around it (ages, clock times, the usage forecast,
// quiet hours, what a notification's buttons do). Nothing here touches Android, so the unit tests run it on the JVM.
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun objs(a: JSONArray?): List<JSONObject> = if (a == null) emptyList() else (0 until a.length()).mapNotNull { a.optJSONObject(it) }
fun strs(a: JSONArray?): List<String> = if (a == null) emptyList() else (0 until a.length()).map { a.optString(it) }.filter { it.isNotEmpty() }

// ---- the rows ----
// wakes / continues: when its next wake-up or auto-continue is due (0 = none)
data class ThreadRow(val id: String, val title: String, val folder: String, val state: String, val note: String, val updated: Long, val section: String, val wakes: Long = 0, val continues: Long = 0)
// one thing that needs the user. askId/options: a question one tap can answer
data class Need(val kind: String, val thread: String, val title: String, val note: String, val at: Long, val askId: String = "", val question: String = "", val options: List<String> = emptyList())
data class Wake(val kind: String, val id: String, val open: String, val title: String, val at: Long, val prompt: String, val every: Int?)
data class Counts(val needs: Int = 0, val threads: Int = 0, val running: Int = 0, val scheduled: Int = 0, val working: Int = 0)
data class Inbox(val needs: List<Need> = emptyList(), val coming: List<Wake> = emptyList(), val counts: Counts = Counts())
data class RunRow(val thread: String, val threadTitle: String, val id: String, val kind: String, val title: String, val now: String, val state: String)
data class Limit(val key: String, val label: String, val pct: Double, val resetsAt: String?)
data class Account(val id: String, val name: String, val detail: String, val note: String, val inUse: Boolean)
data class Member(val open: String, val name: String, val role: String, val state: String)
data class Post(val from: String, val text: String)
data class Team(val id: String, val name: String, val goal: String, val word: String, val members: List<Member>, val posts: List<Post>)
// seen: on your own message, whether Claude has taken it in; null when Orbit does not say
data class Item(val k: String, val t: String, val seen: Boolean? = null, val at: Long = 0)
data class Choice(val label: String, val description: String)
data class Question(val question: String, val header: String, val options: List<Choice>, val multi: Boolean)
data class Ask(val id: String, val questions: List<Question>)
data class Task(val id: String, val kind: String, val title: String, val now: String)
data class ThreadView(val title: String = "", val items: List<Item> = emptyList(), val working: Boolean = false, val asks: List<Ask> = emptyList(), val tasks: List<Task> = emptyList())
// a message sent from this phone that Orbit has not listed back yet. text: what was typed; shown: that plus a line per
// attached file; sentAt: when Orbit took it (0 = still on its way)
data class Outgoing(val mid: String, val text: String, val shown: String, val base: Int, val sentAt: Long = 0)
data class Quiet(val on: Boolean = false, val from: String = "22:00", val to: String = "07:00")

object Read {
    fun threads(j: JSONObject): List<ThreadRow> = objs(j.optJSONArray("threads")).map {
        ThreadRow(it.optString("id"), it.optString("title"), it.optString("folder"), it.optString("state", "idle"), it.optString("note"), it.optLong("updated"), it.optString("section"), it.optLong("wakes"), it.optLong("continues"))
    }.filter { it.id.isNotEmpty() }.distinctBy { it.id }

    fun wake(w: JSONObject) = Wake(w.optString("kind"), w.optString("id"), w.optString("open"), w.optString("title"), w.optLong("at"), w.optString("prompt"), if (w.isNull("every")) null else w.optInt("every"))
    fun wakes(j: JSONObject, name: String = "wakeups"): List<Wake> = objs(j.optJSONArray(name)).map { wake(it) }.distinctBy { it.kind + ":" + it.id }

    fun inbox(j: JSONObject): Inbox {
        val needs = objs(j.optJSONArray("needs")).map {
            val a = it.optJSONObject("ask")
            Need(it.optString("kind"), it.optString("thread"), it.optString("title"), it.optString("note"), it.optLong("at"), a?.optString("id") ?: "", a?.optString("question") ?: "", strs(a?.optJSONArray("options")))
        }.filter { it.thread.isNotEmpty() }
        val c = j.optJSONObject("counts") ?: JSONObject()
        return Inbox(needs, wakes(j, "coming"), Counts(c.optInt("needs", needs.size), c.optInt("threads"), c.optInt("running"), c.optInt("scheduled"), c.optInt("working")))
    }

    fun running(j: JSONObject): List<RunRow> = objs(j.optJSONArray("running")).map {
        RunRow(it.optString("thread"), it.optString("threadTitle"), it.optString("id"), it.optString("kind"), it.optString("title"), it.optString("now"), it.optString("state", "working"))
    }

    fun limits(u: JSONObject?): List<Limit> = objs(u?.optJSONArray("rows")).map { Limit(it.optString("key"), it.optString("label"), it.optDouble("pct", 0.0), if (it.isNull("resetsAt")) null else it.optString("resetsAt")) }

    fun accounts(j: JSONObject): List<Account> = objs(j.optJSONArray("accounts")).map { Account(it.optString("id"), it.optString("name"), it.optString("detail"), it.optString("note"), it.optBoolean("inUse")) }.filter { it.id.isNotEmpty() }

    fun teams(j: JSONObject): List<Team> = objs(j.optJSONArray("teams")).map { t ->
        Team(t.optString("id"), t.optString("name"), t.optString("goal"), t.optString("word"),
            objs(t.optJSONArray("members")).map { Member(it.optString("open"), it.optString("name"), it.optString("role"), it.optString("state")) },
            objs(t.optJSONArray("posts")).map { Post(it.optString("from"), it.optString("text")) })
    }.distinctBy { it.id }

    // Whether a message Orbit lists is the one typed here: the same text, or Orbit's shape of it (cut at 4000
    // characters; with a line per attached file after it).
    private const val ATTACHED = "Attached from the phone: "
    fun sameMessage(listed: String, typed: String): Boolean {
        val l = listed.removeSuffix("…")
        if (typed.isEmpty()) return l.startsWith(ATTACHED)
        return l == typed || (l.length >= 4000 && typed.startsWith(l)) || l.startsWith(typed + "\n" + ATTACHED)
    }

    fun listed(v: ThreadView, text: String): Int = v.items.count { it.k == "you" && sameMessage(it.t, text) }

    // A new send from this phone. base: how many of your messages with this text are already there (listed by Orbit,
    // or sent a moment ago), so "yes" sent twice is two messages and an old "yes" is not taken for the new one.
    fun outgoing(v: ThreadView, pending: List<Outgoing>, mid: String, text: String, shown: String): Outgoing =
        Outgoing(mid, text, shown, listed(v, text) + pending.count { it.text == text })

    // Your sends still on their way, after the thread. Each message is on screen exactly once: as "sending" from the
    // tap until Orbit lists it, from Orbit after that (not seen, then seen).
    fun withOutgoing(v: ThreadView, outgoing: List<Outgoing>): ThreadView {
        val sending = outgoing.filter { listed(v, it.text) <= it.base }
        return if (sending.isEmpty()) v else v.copy(items = v.items + sending.map { Item("sending", it.shown) })
    }

    // The sends to keep after Orbit listed the thread again: the ones it does not list yet. One Orbit took
    // (sentAt > 0) that it still does not list after 20 s is let go, so a bubble never stays "sending" for good.
    const val SENDING_KEEP_MS = 20_000L
    fun settle(v: ThreadView, outgoing: List<Outgoing>, now: Long): List<Outgoing> =
        outgoing.filter { listed(v, it.text) <= it.base && (it.sentAt == 0L || now - it.sentAt < SENDING_KEEP_MS) }

    // Orbit lists what Claude has not read yet (seen = false) after the conversation. For a moment a message can be
    // in both (just written to the conversation, not yet off Orbit's unread list): it is shown once, as read.
    fun once(items: List<Item>): List<Item> {
        val firstUnread = items.indexOfLast { !(it.k == "you" && it.seen == false) && it.k != "note" } + 1
        val last = items.take(firstUnread).lastOrNull() ?: return items
        if (last.k != "you" || last.seen != true) return items
        val dup = items.withIndex().firstOrNull { (i, it) -> i >= firstUnread && it.k == "you" && it.seen == false && sameMessage(last.t, it.t) } ?: return items
        return items.filterIndexed { i, _ -> i != dup.index }
    }

    fun thread(t: JSONObject): ThreadView = ThreadView(
        t.optString("title"),
        once(objs(t.optJSONArray("items")).map { Item(it.optString("k"), it.optString("t"), if (it.has("seen") && !it.isNull("seen")) it.optBoolean("seen") else null, it.optLong("at")) }),
        t.optBoolean("working"),
        objs(t.optJSONArray("asks")).map { a ->
            Ask(a.optString("id"), objs(a.optJSONArray("questions")).map { q ->
                Question(q.optString("question"), q.optString("header"), objs(q.optJSONArray("options")).map { Choice(it.optString("label"), it.optString("description")) }, q.optBoolean("multiSelect"))
            })
        }.filter { it.questions.isNotEmpty() }.distinctBy { it.id },
        objs(t.optJSONArray("tasks")).map { Task(it.optString("id"), it.optString("kind"), it.optString("title"), it.optString("now")) },
    )

    fun quiet(q: JSONObject?): Quiet? = if (q == null) null else Quiet(q.optBoolean("on"), q.optString("from", "22:00"), q.optString("to", "07:00")).takeIf { Words.minutes(it.from) != null && Words.minutes(it.to) != null }
}

// ---- words and numbers for the screens ----
object Words {
    private val hm = DateTimeFormatter.ofPattern("HH:mm")
    private val day = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.ENGLISH)
    private val date = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    // how long ago, short: 38s, 6m, 3h, 2d ("" when Orbit sent no time)
    fun age(ms: Long, now: Long): String {
        if (ms <= 0) return ""
        val s = maxOf(0L, (now - ms) / 1000)
        return when {
            s < 60 -> "${s}s"
            s < 3600 -> "${s / 60}m"
            s < 86_400 -> "${s / 3600}h"
            else -> "${s / 86_400}d"
        }
    }

    // a time to come: 14:00 today, Thu 09:00 within a week, 12 Nov after that
    fun clock(ms: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        if (ms <= 0) return ""
        val at = Instant.ofEpochMilli(ms).atZone(zone)
        val today: LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return when {
            at.toLocalDate() == today -> at.format(hm)
            kotlin.math.abs(ms - now) < 6 * 86_400_000L -> at.format(day)
            else -> at.format(date)
        }
    }

    // "1h 48m", "33m", "2d 4h" from now until then ("now" when it is due)
    fun until(ms: Long, now: Long): String {
        val m = (ms - now) / 60_000
        if (m <= 0) return "now"
        val d = m / 1440
        val h = (m % 1440) / 60
        return if (d > 0) "${d}d ${h}h" else if (h > 0) "${h}h ${m % 60}m" else "${m}m"
    }

    // minutes into the day of "22:00", or null when it is not a time
    fun minutes(hhmm: String): Int? {
        val m = Regex("^(\\d{1,2}):(\\d{2})$").find(hhmm.trim()) ?: return null
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        return if (h in 0..23 && min in 0..59) h * 60 + min else null
    }

    fun hhmm(minutes: Int): String = String.format(Locale.ROOT, "%02d:%02d", (minutes / 60) % 24, minutes % 60)

    // the scheduled row's kind word
    fun wakeWord(w: Wake): String = when {
        w.kind == "continue" -> "AUTO-CONTINUE"
        w.every != null -> "LOOP"
        else -> "WAKE-UP"
    }

    // the same, in the words of a row's grey line: once, every 10 min, continues after the limit
    fun wakeKind(w: Wake): String = when {
        w.kind == "continue" -> "continues after the limit"
        w.every != null -> "every ${w.every} min"
        else -> "once"
    }

    // Home's greeting, by the hour
    fun greeting(now: Long, zone: ZoneId = ZoneId.systemDefault()): String = when (Instant.ofEpochMilli(now).atZone(zone).hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    // Home's one or two sentences: what needs you, then what is working. Counts only, nothing made up.
    fun homeSummary(waiting: Int, failed: Int, working: Int, loaded: Boolean = true): String {
        if (!loaded) return "Checking in with the desk."
        val need = "${plural(waiting, "thread")} ${if (waiting == 1) "needs" else "need"} you"
        val first = when {
            waiting > 0 && failed > 0 -> "$need and ${plural(failed, "turn")} failed."
            waiting > 0 -> "$need."
            failed > 0 -> "${plural(failed, "turn")} failed."
            else -> "Nothing needs you."
        }
        val second = when {
            working > 0 -> " ${plural(working, "thread")} ${if (working == 1) "is" else "are"} working."
            waiting + failed == 0 -> " All quiet on the desk."
            else -> ""
        }
        return first + second
    }

    // a due date ("2026-11-12") beside a to-do: today, tomorrow, Thu within a week, 12 Nov after that ("" when it is no date)
    fun due(due: String, today: LocalDate): String {
        val d = runCatching { LocalDate.parse(due.take(10)) }.getOrNull() ?: return due
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, d)
        return when {
            days < 0 -> "was due ${d.format(date)}"
            days == 0L -> "due today"
            days == 1L -> "due tomorrow"
            days < 7 -> "due ${d.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))}"
            else -> "due ${d.format(date)}"
        }
    }

    // which group an open to-do falls in by its due date: overdue, today, week, later, none
    fun dueGroup(due: String, today: LocalDate): String {
        val d = runCatching { LocalDate.parse(due.take(10)) }.getOrNull() ?: return "none"
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, d)
        return when { days < 0 -> "overdue"; days == 0L -> "today"; days < 7 -> "week"; else -> "later" }
    }

    // a folder path's last part
    fun folderName(path: String): String = path.trimEnd('/', '\\').substringAfterLast('\\').substringAfterLast('/')

    fun plural(n: Int, one: String, many: String = one + "s"): String = "$n ${if (n == 1) one else many}"
}

// ---- usage ----
object Usage {
    // how long a limit's window is, from Orbit's key for it (five_hour, seven_day, seven_day_opus ...)
    fun windowMs(key: String): Long? = when {
        key.startsWith("five_hour") -> 5 * 3_600_000L
        key.startsWith("seven_day") -> 7 * 86_400_000L
        else -> null
    }

    // the session's window and the week's (all models), when Orbit sends them
    fun session(limits: List<Limit>): Limit? = limits.firstOrNull { it.key.startsWith("five_hour") }
    fun week(limits: List<Limit>): Limit? = limits.firstOrNull { it.key == "seven_day" } ?: limits.firstOrNull { it.key.startsWith("seven_day") }

    // the weather beside the session's use on Home
    fun sky(pct: Double): String = when { pct >= GUARD_AT -> "nearly out"; pct >= 60 -> "filling up"; else -> "clear skies" }

    fun resetMs(l: Limit): Long? = l.resetsAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    // "resets 15:00, in 1h 48m" ("" when there is no reset time)
    fun resets(l: Limit, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val at = resetMs(l) ?: return ""
        val soon = at - now < 86_400_000L
        return "resets ${Words.clock(at, now, zone)}" + if (soon) ", in ${Words.until(at, now)}" else ""
    }

    // When this window reaches 100% if it goes on as it has since it opened, when that is before it resets: the time
    // in ms, or null. Not said for a window that is nearly empty or has barely begun (the rate means nothing yet).
    fun fullAt(l: Limit, now: Long): Long? {
        val reset = resetMs(l) ?: return null
        val window = windowMs(l.key) ?: return null
        val elapsed = now - (reset - window)
        if (l.pct < 20 || l.pct >= 100 || elapsed < window / 20) return null
        val full = (reset - window) + (elapsed * 100.0 / l.pct).toLong()
        return full.takeIf { it < reset }
    }

    fun forecast(l: Limit, now: Long, zone: ZoneId = ZoneId.systemDefault()): String = fullAt(l, now)?.let { "At this rate you reach 100% at ${Words.clock(it, now, zone)}" } ?: ""

    // "5-hour window", "7-day window", "7-day Opus window"
    fun windowName(key: String): String {
        if (key.startsWith("five_hour")) return "5-hour window"
        val model = key.removePrefix("seven_day").trimStart('_')
        return if (key.startsWith("seven_day")) "7-day " + (if (model.isEmpty()) "" else model.replaceFirstChar { it.uppercase() } + " ") + "window" else "plan window"
    }

    // The usage guard: the window closest to its limit, once one is at 85% or more and has a reset still to come.
    const val GUARD_AT = 85.0
    fun guard(limits: List<Limit>, now: Long): Limit? = limits.filter { it.pct >= GUARD_AT && (resetMs(it) ?: 0) > now }.maxByOrNull { it.pct }

    // What the guard says: which window, how full, when it fills at this rate, what pauses then and when it carries
    // on, and how many wake-ups fall before the reset. working: threads working now; wakes: every scheduled wake-up.
    fun guardWords(l: Limit, now: Long, working: Int, wakes: List<Wake>, zone: ZoneId = ZoneId.systemDefault()): String {
        val reset = resetMs(l) ?: return ""
        val at = Words.clock(reset, now, zone)
        val head = "The ${windowName(l.key)} is at ${l.pct.toInt()}%."
        val fills = when {
            l.pct >= 100 -> " It is full."
            else -> fullAt(l, now)?.let { " If the rate holds it fills at ${Words.clock(it, now, zone)}." } ?: " At this rate it lasts until it resets at $at."
        }
        val pauses = if (working > 0) " ${Words.plural(working, "working thread")} ${if (working == 1) "pauses" else "pause"} then and ${if (working == 1) "continues" else "continue"} at $at." else if (l.pct >= 100 || fullAt(l, now) != null) " It resets at $at." else ""
        val due = wakes.count { it.kind == "wake" && it.at in now..reset }
        val inside = if (due > 0) " ${Words.plural(due, "wake-up")} ${if (due == 1) "is" else "are"} due before then." else ""
        return head + fills + pauses + inside
    }
}

// ---- quiet hours ----
object QuietHours {
    // whether a time of day (minutes) is inside the quiet hours; they may run over midnight (22:00 to 07:00)
    fun within(q: Quiet, minuteOfDay: Int): Boolean {
        if (!q.on) return false
        val from = Words.minutes(q.from) ?: return false
        val to = Words.minutes(q.to) ?: return false
        if (from == to) return false
        return if (from < to) minuteOfDay in from until to else minuteOfDay >= from || minuteOfDay < to
    }

    // a failure still rings; everything else arrives without sound during quiet hours
    fun silent(q: Quiet, kind: String, minuteOfDay: Int): Boolean = kind != "fail" && within(q, minuteOfDay)
}

// ---- a notification's buttons ----
// op: "answer" (Orbit takes the choice, no need to open the app), "open" (this choice needs the app and its
// fingerprint), "stop"
data class NoteButton(val label: String, val op: String, val ask: String = "")

object NoteButtons {
    // from the notification Orbit sent: its question's choices (Orbit marks the ones that need the app), else Stop.
    // Android shows three at most. buttons: the user's setting (off = none: the notification just opens the app).
    fun of(m: JSONObject, buttons: Boolean = true): List<NoteButton> {
        if (!buttons || m.optString("thread").isEmpty()) return emptyList()
        val ask = m.optJSONObject("ask")
        val opts = objs(ask?.optJSONArray("opts")).filter { it.optString("l").isNotEmpty() }
        val id = ask?.optString("id") ?: ""
        if (id.isNotEmpty() && opts.isNotEmpty()) return opts.take(3).map { NoteButton(it.optString("l"), if (it.optBoolean("open")) "open" else "answer", id) }
        return if (m.optBoolean("stop")) listOf(NoteButton("Stop", "stop")) else emptyList()
    }

    // the request a pressed button sends, sealed with the notification key
    fun request(b: NoteButton, thread: String, seq: Long): JSONObject {
        val j = JSONObject().put("op", b.op).put("id", thread).put("seq", seq).put("rid", seq)
        if (b.op == "answer") j.put("ask", b.ask).put("label", b.label)
        return j
    }
}

package dev.clawdboard.core

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile

enum class Act { WORKING, RUNNING, PERMISSION, QUESTION, FINISHED, ERROR, IDLE }

enum class Doing { THINKING, EDITING, READING, SEARCHING, DELEGATING, TESTS, BUILDING, COMMAND, PLAN }

data class ClaudeSession(
    val id: String,
    val project: String?,
    val branch: String?,
    val model: String?,
    val act: Act,
    val doing: Doing?,
    val file: String?,
    val since: Long,
    val seen: Long,
) {
    val attention: Boolean get() = act == Act.PERMISSION || act == Act.QUESTION

    val key: String get() = "$project|$model|$since"

    fun toJson(withFile: Boolean): JSONObject = JSONObject()
        .put("project", project ?: JSONObject.NULL)
        .put("branch", branch ?: JSONObject.NULL)
        .put("model", model ?: JSONObject.NULL)
        .put("state", act.name.lowercase())
        .put("doing", doing?.name?.lowercase() ?: JSONObject.NULL)
        .put("file", if (withFile) file ?: JSONObject.NULL else JSONObject.NULL)
        .put("since", since)
        .put("seen", seen)
}

const val FINISHED_MS = 3 * 60_000L
const val STALE_MS = 10 * 60_000L
const val WAITING_MS = 30 * 60_000L
const val FORGET_MS = 60 * 60_000L
const val CHEER_MS = 6_000L

private val RANK = listOf(Act.QUESTION, Act.PERMISSION, Act.ERROR, Act.RUNNING, Act.WORKING, Act.FINISHED, Act.IDLE)

fun List<ClaudeSession>.primary(): ClaudeSession? =
    minWithOrNull(compareBy<ClaudeSession> { RANK.indexOf(it.act) }.thenByDescending { it.seen })

fun List<ClaudeSession>.forModel(model: String): ClaudeSession? =
    filter { it.model.equals(model, ignoreCase = true) }.primary()

const val ACTIVITY_FRESH_MS = 15 * 60_000L

fun reactFor(model: String?, list: List<ClaudeSession>?, now: Long): React {
    if (list == null) return React.NONE
    val known = list.any { it.model != null }
    val mine = if (model == null || !known) list.primary() else list.forModel(model)
    if (mine == null && model != null && known && list.any { it.act != Act.IDLE }) return React.NONE
    return reactOf(mine, now, true)
}

fun List<ClaudeSession>.soloModel(): String? =
    filter { it.act != Act.IDLE }.mapNotNull { it.model }.distinct().singleOrNull()
        ?.let { m -> MODELS.firstOrNull { it.equals(m, ignoreCase = true) } }

fun ClaudeSession.describe(withFile: Boolean): String {
    val state = txt.activityState(act, doing, if (withFile) file else null)
    val where = listOfNotNull(project, branch).joinToString(" · ")
    return if (where.isEmpty()) state else "$state · $where"
}

fun activityJson(sessions: List<ClaudeSession>, now: Long, withFile: Boolean): JSONObject =
    JSONObject().put("at", now).put("sessions", JSONArray().also { a -> sessions.forEach { a.put(it.toJson(withFile)) } })

fun parseActivity(o: JSONObject?): List<ClaudeSession>? {
    if (o == null || o.optBoolean("off", false)) return null
    val arr = o.optJSONArray("sessions") ?: return null
    return (0 until arr.length()).mapNotNull { i ->
        val s = arr.optJSONObject(i) ?: return@mapNotNull null
        val act = s.str("state")?.let { v -> Act.entries.firstOrNull { it.name.equals(v, ignoreCase = true) } } ?: return@mapNotNull null
        ClaudeSession(
            id = "$i",
            project = s.str("project"),
            branch = s.str("branch"),
            model = s.str("model"),
            act = act,
            doing = s.str("doing")?.let { v -> Doing.entries.firstOrNull { it.name.equals(v, ignoreCase = true) } },
            file = s.str("file"),
            since = s.optLong("since", 0L),
            seen = s.optLong("seen", 0L),
        )
    }
}

class ActivityTracker(private val repo: (String) -> Pair<String?, String?> = Repo::of) {
    private val sessions = LinkedHashMap<String, ClaudeSession>()
    private val models = HashMap<String, String>()

    @Synchronized
    fun onHook(o: JSONObject, now: Long): Boolean {
        val id = o.str("session_id") ?: return false
        val event = o.str("hook_event_name") ?: return false
        if (event == "SessionEnd") {
            models.remove(id)
            return sessions.remove(id) != null
        }
        val old = sessions[id]
        val next = when (event) {
            "SessionStart" -> old?.let { it.act to it.doing } ?: (Act.IDLE to null)
            "UserPromptSubmit" -> Act.WORKING to Doing.THINKING
            "PreToolUse" -> tool(o)
            "PermissionRequest" -> Act.PERMISSION to doingOf(o.str("tool_name"), o.optJSONObject("tool_input"))
            "PostToolUse", "PostToolUseFailure", "PermissionDenied" ->
                if (old == null || old.act in setOf(Act.RUNNING, Act.PERMISSION, Act.QUESTION)) Act.WORKING to Doing.THINKING else old.act to old.doing
            "Notification" -> when (o.str("notification_type")) {
                "permission_prompt" -> Act.PERMISSION to old?.doing
                "elicitation_dialog", "elicitation_url_dialog" -> Act.QUESTION to null
                "idle_prompt" -> Act.IDLE to null
                else -> return false
            }
            "Stop" -> Act.FINISHED to null
            "StopFailure" -> Act.ERROR to null
            else -> return false
        }
        val (act, doing) = next
        val file = if (event == "PreToolUse" || event == "PermissionRequest") fileOf(o.optJSONObject("tool_input")) ?: old?.file.takeIf { doing == old?.doing } else old?.file
        val fresh = o.str("model")?.let { family(it) }
            ?: if (event in LOOKUPS) transcriptModel(o.str("transcript_path")) else null
        fresh?.let { models[id] = it }
        val model = fresh ?: models[id]
        val (project, branch) = o.str("cwd")?.let(repo) ?: (old?.project to old?.branch)
        sessions[id] = ClaudeSession(
            id = id,
            project = project ?: old?.project,
            branch = branch ?: old?.branch,
            model = model ?: old?.model,
            act = act,
            doing = doing,
            file = file,
            since = if (old != null && old.act == act) old.since else now,
            seen = now,
        )
        return true
    }

    @Synchronized
    fun sessions(now: Long): List<ClaudeSession> {
        sessions.values.removeAll { now - it.seen > FORGET_MS }
        return sessions.values.map { s ->
            when {
                s.act == Act.FINISHED && now - s.since > FINISHED_MS -> s.copy(act = Act.IDLE, doing = null, since = s.since + FINISHED_MS)
                (s.act == Act.WORKING || s.act == Act.RUNNING) && now - s.seen > STALE_MS -> s.copy(act = Act.IDLE, doing = null, since = s.seen + STALE_MS)
                s.attention && now - s.seen > WAITING_MS -> s.copy(act = Act.IDLE, doing = null, since = s.seen + WAITING_MS)
                else -> s
            }
        }
    }

    private fun tool(o: JSONObject): Pair<Act, Doing?> {
        val name = o.str("tool_name")
        return when (name) {
            "AskUserQuestion" -> Act.QUESTION to null
            "ExitPlanMode" -> Act.QUESTION to Doing.PLAN
            "Bash", "PowerShell" -> Act.RUNNING to doingOf(name, o.optJSONObject("tool_input"))
            else -> Act.WORKING to doingOf(name, o.optJSONObject("tool_input"))
        }
    }

    private fun transcriptModel(path: String?): String? = runCatching {
        val f = File(path ?: return null)
        if (!f.isFile) return null
        RandomAccessFile(f, "r").use { r ->
            val len = minOf(r.length(), 262_144L).toInt()
            r.seek(r.length() - len)
            val buf = ByteArray(len)
            r.readFully(buf)
            Regex("\"model\"\\s*:\\s*\"claude-([a-z]+)").findAll(String(buf, Charsets.UTF_8)).lastOrNull()?.groupValues?.get(1)
        }
    }.getOrNull()

    companion object {
        private val LOOKUPS = setOf("SessionStart", "UserPromptSubmit", "Stop")
        private val TESTS = Regex("\\b(tests?|pytest|jest|vitest|mocha|phpunit|rspec|xunit|nunit|unittest|testreleaseunittest|testdebugunittest)\\b")
        private val BUILD = Regex("\\b(build|compile|assemble\\w*|gradlew|msbuild|xcodebuild|tsc|webpack|make|package\\w*)\\b")

        fun family(model: String): String? =
            Regex("(haiku|sonnet|opus|fable)", RegexOption.IGNORE_CASE).find(model)?.value?.lowercase()

        fun doingOf(tool: String?, input: JSONObject?): Doing? = when (tool) {
            null -> null
            "Edit", "Write", "MultiEdit", "NotebookEdit" -> Doing.EDITING
            "Read", "Grep", "Glob", "LS" -> Doing.READING
            "WebFetch", "WebSearch" -> Doing.SEARCHING
            "Task", "Agent" -> Doing.DELEGATING
            "ExitPlanMode" -> Doing.PLAN
            "Bash", "PowerShell" -> {
                val text = listOfNotNull(input?.str("command"), input?.str("description")).joinToString(" ").lowercase()
                when {
                    TESTS.containsMatchIn(text) -> Doing.TESTS
                    BUILD.containsMatchIn(text) -> Doing.BUILDING
                    else -> Doing.COMMAND
                }
            }
            else -> Doing.THINKING
        }

        fun fileOf(input: JSONObject?): String? =
            (input?.str("file_path") ?: input?.str("notebook_path"))?.replace('\\', '/')?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
    }
}

object Repo {
    private class Cached(val at: Long, val value: Pair<String?, String?>)

    private val cache = HashMap<String, Cached>()

    @Synchronized
    fun of(cwd: String): Pair<String?, String?> {
        val now = System.currentTimeMillis()
        cache[cwd]?.takeIf { now - it.at < 20_000L }?.let { return it.value }
        val value = runCatching { read(File(cwd)) }.getOrDefault(File(cwd).name.ifBlank { null } to null)
        cache[cwd] = Cached(now, value)
        return value
    }

    fun read(start: File): Pair<String?, String?> {
        var dir: File? = start.absoluteFile
        var depth = 0
        while (dir != null && depth < 40) {
            val git = File(dir, ".git")
            if (git.exists()) return dir.name to branchOf(git)
            dir = dir.parentFile
            depth++
        }
        return start.name.ifBlank { null } to null
    }

    private fun branchOf(git: File): String? {
        val gitDir = if (git.isDirectory) git else {
            val target = git.readText().trim().removePrefix("gitdir:").trim().takeIf { it.isNotEmpty() } ?: return null
            File(target).let { if (it.isAbsolute) it else File(git.parentFile, target) }
        }
        val head = File(gitDir, "HEAD").takeIf { it.isFile }?.readText()?.trim() ?: return null
        return if (head.startsWith("ref:")) head.removePrefix("ref:").trim().removePrefix("refs/heads/") else head.take(7)
    }
}

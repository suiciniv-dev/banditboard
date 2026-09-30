package dev.clawdboard.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ActivityTest {
    private fun tracker() = ActivityTracker { "Simix.Ponto.Cloud" to "feature/facial" }

    private fun event(name: String, id: String = "s1", extra: String = "") =
        JSONObject("""{"session_id":"$id","hook_event_name":"$name","cwd":"C:/x"$extra}""")

    @Test
    fun promptToolsAndStop() {
        val t = tracker()
        t.onHook(event("UserPromptSubmit"), 1_000)
        assertEquals(Act.WORKING, t.sessions(1_000).single().act)
        assertEquals(Doing.THINKING, t.sessions(1_000).single().doing)

        t.onHook(event("PreToolUse", extra = ""","tool_name":"Edit","tool_input":{"file_path":"C:\\src\\FacialService.cs"}"""), 2_000)
        val editing = t.sessions(2_000).single()
        assertEquals(Doing.EDITING, editing.doing)
        assertEquals("FacialService.cs", editing.file)
        assertEquals("Simix.Ponto.Cloud", editing.project)
        assertEquals("feature/facial", editing.branch)

        t.onHook(event("PreToolUse", extra = ""","tool_name":"Bash","tool_input":{"command":"dotnet test","description":"Run tests"}"""), 3_000)
        assertEquals(Act.RUNNING, t.sessions(3_000).single().act)
        assertEquals(Doing.TESTS, t.sessions(3_000).single().doing)

        t.onHook(event("PostToolUse", extra = ""","tool_name":"Bash""""), 4_000)
        assertEquals(Act.WORKING, t.sessions(4_000).single().act)

        t.onHook(event("Stop"), 5_000)
        assertEquals(Act.FINISHED, t.sessions(5_000).single().act)
        assertEquals(Act.IDLE, t.sessions(5_000 + FINISHED_MS + 1).single().act)

        t.onHook(event("SessionEnd"), 6_000)
        assertTrue(t.sessions(6_000).isEmpty())
    }

    @Test
    fun attentionAndErrors() {
        val t = tracker()
        t.onHook(event("PermissionRequest", extra = ""","tool_name":"Bash","tool_input":{"command":"rm -rf build"}"""), 1_000)
        assertEquals(Act.PERMISSION, t.sessions(1_000).single().act)
        t.onHook(event("PermissionDenied"), 2_000)
        assertEquals(Act.WORKING, t.sessions(2_000).single().act)
        t.onHook(event("PreToolUse", extra = ""","tool_name":"AskUserQuestion","tool_input":{}"""), 3_000)
        assertEquals(Act.QUESTION, t.sessions(3_000).single().act)
        t.onHook(event("StopFailure"), 4_000)
        assertEquals(Act.ERROR, t.sessions(4_000).single().act)
        t.onHook(event("Notification", extra = ""","notification_type":"idle_prompt""""), 5_000)
        assertEquals(Act.IDLE, t.sessions(5_000).single().act)
    }

    @Test
    fun staleWorkGoesIdleAndOldSessionsAreForgotten() {
        val t = tracker()
        t.onHook(event("UserPromptSubmit"), 0)
        assertEquals(Act.IDLE, t.sessions(STALE_MS + 1).single().act)
        assertTrue(t.sessions(FORGET_MS + 1).isEmpty())
    }

    @Test
    fun mostUrgentSessionWins() {
        val t = tracker()
        t.onHook(event("UserPromptSubmit", "a", ""","model":"claude-opus-4""""), 1_000)
        t.onHook(event("PermissionRequest", "b", ""","model":"claude-sonnet-4","tool_name":"Edit""""), 900)
        val all = t.sessions(1_000)
        assertEquals(Act.PERMISSION, all.primary()?.act)
        assertEquals("opus", all.forModel("Opus")?.model)
        assertEquals(Act.PERMISSION, all.forModel("Sonnet")?.act)
        assertNull(all.forModel("Haiku"))
    }

    @Test
    fun ignoresUnknownEvents() {
        val t = tracker()
        assertEquals(false, t.onHook(event("PreCompact"), 1_000))
        assertEquals(false, t.onHook(JSONObject("""{"hook_event_name":"Stop"}"""), 1_000))
        assertTrue(t.sessions(1_000).isEmpty())
    }

    @Test
    fun readsProjectAndBranchFromGit() {
        val root = Files.createTempDirectory("repo").toFile()
        try {
            val project = File(root, "Simix.Ponto.Cloud").apply { mkdirs() }
            File(project, ".git").mkdirs()
            File(project, ".git/HEAD").writeText("ref: refs/heads/feature/facial\n")
            val deep = File(project, "src/Facial").apply { mkdirs() }
            assertEquals("Simix.Ponto.Cloud" to "feature/facial", Repo.read(deep))

            val worktree = File(root, "wt").apply { mkdirs() }
            val gitDir = File(project, ".git/worktrees/wt").apply { mkdirs() }
            File(gitDir, "HEAD").writeText("0123456789abcdef\n")
            File(worktree, ".git").writeText("gitdir: ${gitDir.absolutePath}\n")
            assertEquals("wt" to "0123456", Repo.read(worktree))

            val plain = File(root, "solto").apply { mkdirs() }
            assertEquals("solto", Repo.read(plain).first)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun raccoReactsToClaude() {
        val s = ClaudeSession("a", null, null, "opus", Act.WORKING, Doing.THINKING, null, 0, 0)
        assertEquals(React.WORK, reactOf(s, 0, true))
        assertEquals(React.NONE, reactOf(s, 0, false))
        assertEquals(React.SLEEP, reactOf(null, 0, true))
        assertEquals(React.ALERT, reactOf(s.copy(act = Act.QUESTION), 0, true))
        assertEquals(React.CHEER, reactOf(s.copy(act = Act.FINISHED, since = 0), CHEER_MS - 1, true))
        assertEquals(React.NONE, reactOf(s.copy(act = Act.FINISHED, since = 0), CHEER_MS + 1, true))

        val empty = UsageSnapshot(UsageWindow(0.0, null), UsageWindow(10.0, null), emptyList(), 0)
        assertEquals(Mood.SLEEPY, feelOf(empty, "Opus").mood)
        assertEquals(Mood.NORMAL, feelOf(empty, "Opus", React.WORK).mood)
        val busy = UsageSnapshot(UsageWindow(40.0, null), UsageWindow(10.0, null), emptyList(), 0)
        assertEquals(Mood.SLEEPY, feelOf(busy, "Opus", React.SLEEP).mood)
        assertEquals(React.NONE, feelOf(busy, "Opus", React.SLEEP).react)
        val full = UsageSnapshot(UsageWindow(100.0, null), UsageWindow(10.0, null), emptyList(), 0)
        assertEquals(React.NONE, feelOf(full, "Opus", React.ALERT).react)
    }

    @Test
    fun jsonKeepsFileOnlyWhenAsked() {
        val s = ClaudeSession("a", "P", "main", "opus", Act.WORKING, Doing.EDITING, "A.kt", 1, 2)
        val hidden = activityJson(listOf(s), 3, withFile = false)
        assertTrue(hidden.getJSONArray("sessions").getJSONObject(0).isNull("file"))
        val back = parseActivity(activityJson(listOf(s), 3, withFile = true))!!.single()
        assertEquals("A.kt", back.file)
        assertEquals(Act.WORKING, back.act)
        assertEquals(Doing.EDITING, back.doing)
    }
}

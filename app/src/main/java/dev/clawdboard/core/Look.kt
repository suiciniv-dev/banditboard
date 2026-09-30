package dev.clawdboard.core

import org.json.JSONArray
import org.json.JSONObject

const val LOOK_TOP = 4
const val LOOK_ROWS = LOOK_TOP + 10
const val SPRITE_TOP = 3
const val FEET_ROW = LOOK_ROWS - 1
const val EYE_ROW = 7

val SPRITE = arrayOf(
    "................",
    "................",
    "................",
    "..MM........MM..",
    ".MLLMBBBBBBMLLM.",
    ".BBBBBBBBBBBBBB.",
    ".BLLLLBBBBLLLLB.",
    ".MMMMMBBBBMMMMM.",
    ".MMMMMMBBMMMMMM.",
    ".BMMMMLLLLMMMMB.",
    "BBBBLLLNNLLLBBBB",
    "MbBBBLLLLLLBBBbM",
    "..bBBBBBBBBBBb..",
    "...MM......MM...",
)
val EYE_COLS = intArrayOf(3, 11)

val BANDIT = arrayOf(
    "................",
    "................",
    "................",
    "..BB........BB..",
    "..BBBBBBBBBBBB..",
    "..BBBBBBBBBBBB..",
    ".MMMMMBBBBMMMMM.",
    ".MMMMMBBBBMMMMM.",
    "BBBBBBLNNLBBBBBB",
    "BBBBBBLLLLBBBBBB",
    "..BBBBBBBBBBBB..",
    "..BBBBBBBBBBBB..",
    "...B.B....B.B...",
    "...B.B....B.B...",
)
val BANDIT_EYES = intArrayOf(4, 11)
const val BANDIT_EYE_ROW = 6
val BANDIT_ARMS = 8..9
const val BANDIT_LEGS = 12
val BANDIT_LEG_COLS = intArrayOf(3, 5, 10, 12)
val EAR_ROWS = 3..4
val LEFT_EAR = 1..4
val RIGHT_EAR = 11..14
val ARM_ROWS = 10..11
const val ARM_LIFT = 2

const val FUR = 0xFFEEE7DB
const val MASK = 0xFF4A413B
const val NOSE = 0xFF0C0A08

enum class Accessory { TOP_HAT, GLASSES, HEADPHONES, SPROUT, CROWN, SANTA }

enum class Mood { NORMAL, SLEEPY, SWEATY, EXHAUSTED }

enum class React { NONE, WORK, RUN, ALERT, OOPS, CHEER, SLEEP }

data class Feel(val mood: Mood = Mood.NORMAL, val heat: Float = 0f, val react: React = React.NONE)

fun reactOf(session: ClaudeSession?, now: Long, watching: Boolean): React = when {
    !watching -> React.NONE
    session == null -> React.SLEEP
    else -> when (session.act) {
        Act.WORKING -> React.WORK
        Act.RUNNING -> React.RUN
        Act.PERMISSION, Act.QUESTION -> React.ALERT
        Act.ERROR -> React.OOPS
        Act.FINISHED -> if (now - session.since < CHEER_MS) React.CHEER else React.NONE
        Act.IDLE -> React.SLEEP
    }
}

fun feelOf(usage: UsageSnapshot?, model: String, react: React): Feel {
    val base = feelOf(usage, model)
    val mood = when {
        base.mood == Mood.NORMAL && react == React.SLEEP -> Mood.SLEEPY
        base.mood == Mood.SLEEPY && react != React.NONE && react != React.SLEEP -> Mood.NORMAL
        else -> base.mood
    }
    return base.copy(mood = mood, react = if (mood == Mood.SLEEPY || mood == Mood.EXHAUSTED) React.NONE else react)
}

fun feelOf(usage: UsageSnapshot?, model: String): Feel {
    val session = usage?.fiveHour?.percent
    val own = usage?.scoped?.firstOrNull { it.label.contains(model, ignoreCase = true) }?.percent
    val worst = listOfNotNull(session, usage?.sevenDay?.percent, own).maxOrNull()
    val heat = if (worst == null) 0f else ((worst - 90) / 10).toFloat().coerceIn(0f, 1f)
    val mood = when {
        worst != null && worst >= 99.5 -> Mood.EXHAUSTED
        worst != null && worst >= 85 -> Mood.SWEATY
        session != null && session < 0.5 -> Mood.SLEEPY
        else -> Mood.NORMAL
    }
    return Feel(mood, heat)
}

data class Px(val x: Int, val y: Int, val w: Int, val h: Int, val argb: Long)

private const val GRAY = 0xFFA39B90
private const val CORAL = 0xFFD77757
private const val LAVENDER = 0xFFB9A6F2
private const val MINT = 0xFF7CCBA2
private const val PINK = 0xFFF59AC0

private const val HAT = 0xFF3B3446
private const val HAT_SHINE = 0xFF5A4F6B
private const val GOLD = 0xFFF5D66A
private const val FRAME = 0xFF1B1512
private const val PHONES = 0xFF3F3A48
private const val PHONES_SHINE = 0xFF7D7590
private const val LEAF = 0xFF8FB573
private const val STEM = 0xFF5E8A4A
private const val RED = 0xFFD6455D
private const val WHITE = 0xFFF3EFEA
private const val RUBY = 0xFFE5604D

fun accessoryFor(skin: Skin, model: String?): Accessory? = when (skin) {
    Skin.CLASSIC -> null
    Skin.CROWNS -> Accessory.CROWN
    Skin.XMAS -> Accessory.SANTA
    Skin.MODELS -> when (model) {
        "Fable" -> Accessory.TOP_HAT
        "Opus" -> Accessory.GLASSES
        "Sonnet" -> Accessory.HEADPHONES
        "Haiku" -> Accessory.SPROUT
        else -> null
    }
}

fun bodyArgb(tint: Tint, model: String?, species: Species = Species.RACCOON): Long = when (tint) {
    Tint.NATURAL -> natural(species)
    Tint.LAVENDER -> LAVENDER
    Tint.MINT -> MINT
    Tint.BUBBLEGUM -> PINK
    Tint.RAINBOW -> when (model) {
        "Haiku" -> MINT
        "Sonnet" -> LAVENDER
        "Fable" -> PINK
        else -> natural(species)
    }
}

private fun natural(species: Species) = if (species == Species.CLAWD) CORAL else GRAY

fun accessoryPixels(a: Accessory, species: Species = Species.RACCOON): List<Px> = when (species) {
    Species.CLAWD -> clawdAccessory(a, FRAME) ?: raccoonAccessory(a)
    Species.RACCOON -> clawdAccessory(a, GOLD) ?: raccoonAccessory(a)
    Species.RACCOON_CLASSIC -> raccoonAccessory(a)
}

private fun clawdAccessory(a: Accessory, frame: Long): List<Px>? = when (a) {
    Accessory.GLASSES -> listOf(4, 11).flatMap { e ->
        listOf(Px(e - 1, 5, 3, 1, frame), Px(e - 1, 8, 3, 1, frame), Px(e - 1, 6, 1, 2, frame), Px(e + 1, 6, 1, 2, frame))
    } + Px(6, 6, 4, 1, frame)
    Accessory.HEADPHONES -> listOf(
        Px(3, 2, 10, 1, PHONES), Px(2, 3, 1, 2, PHONES), Px(13, 3, 1, 2, PHONES),
        Px(1, 5, 2, 3, PHONES), Px(13, 5, 2, 3, PHONES), Px(1, 6, 1, 1, PHONES_SHINE), Px(14, 6, 1, 1, PHONES_SHINE),
    )
    Accessory.SANTA -> listOf(
        Px(4, 2, 8, 1, RED), Px(6, 1, 5, 1, RED), Px(9, 0, 3, 1, RED), Px(12, 0, 2, 2, WHITE), Px(3, 3, 10, 1, WHITE),
    )
    else -> null
}

private fun raccoonAccessory(a: Accessory): List<Px> = when (a) {
    Accessory.TOP_HAT -> listOf(
        Px(5, 0, 6, 2, HAT), Px(5, 0, 1, 2, HAT_SHINE), Px(5, 2, 6, 1, GOLD), Px(4, 3, 8, 1, HAT),
    )
    Accessory.GLASSES -> EYE_COLS.flatMap { e ->
        listOf(
            Px(e - 1, EYE_ROW - 1, 4, 1, GOLD), Px(e - 1, EYE_ROW + 2, 4, 1, GOLD),
            Px(e - 1, EYE_ROW, 1, 2, GOLD), Px(e + 2, EYE_ROW, 1, 2, GOLD),
        )
    } + Px(6, EYE_ROW, 4, 1, GOLD)
    Accessory.HEADPHONES -> listOf(
        Px(2, 2, 12, 1, PHONES), Px(1, 3, 1, 3, PHONES), Px(14, 3, 1, 3, PHONES),
        Px(0, 6, 2, 3, PHONES), Px(14, 6, 2, 3, PHONES), Px(0, 7, 1, 1, PHONES_SHINE), Px(15, 7, 1, 1, PHONES_SHINE),
    )
    Accessory.SPROUT -> listOf(
        Px(7, 1, 1, 3, STEM), Px(5, 1, 2, 1, LEAF), Px(6, 2, 1, 1, LEAF), Px(8, 0, 2, 1, LEAF), Px(8, 1, 1, 1, LEAF),
    )
    Accessory.CROWN -> listOf(
        Px(4, 2, 8, 2, GOLD), Px(4, 1, 1, 1, GOLD), Px(7, 0, 2, 2, GOLD), Px(11, 1, 1, 1, GOLD), Px(7, 2, 2, 1, RUBY),
    )
    Accessory.SANTA -> listOf(
        Px(4, 2, 8, 1, RED), Px(6, 1, 5, 1, RED), Px(9, 0, 3, 1, RED), Px(12, 0, 2, 2, WHITE), Px(4, 3, 8, 1, WHITE),
    )
}

private fun hex(argb: Long) = "#%06x".format(argb and 0xFFFFFF)

fun lookJson(p: Prefs): JSONObject {
    val species = p.mascot()
    val models = JSONObject()
    MODELS.forEach { m ->
        val acc = JSONArray()
        val a = accessoryFor(p.skin, m)
        a?.let { accessoryPixels(it, species).forEach { px -> acc.put(JSONArray().put(px.x).put(px.y).put(px.w).put(px.h).put(hex(px.argb))) } }
        models.put(m, JSONObject().put("body", hex(bodyArgb(p.tint, m, species))).put("acc", acc).put("kind", a?.name ?: JSONObject.NULL))
    }
    return JSONObject().put("top", LOOK_TOP).put("rows", LOOK_ROWS).put("species", species.name).put("models", models).put("animations", p.animations)
}

fun mascotsJson(usage: UsageSnapshot?, status: StatusSnapshot?): JSONArray {
    val arr = JSONArray()
    MODELS.forEach { m ->
        val own = usage?.scoped?.firstOrNull { it.label.contains(m, ignoreCase = true) }
        val feel = feelOf(usage, m)
        arr.put(
            JSONObject().put("name", m)
                .put("percent", own?.percent ?: usage?.sevenDay?.percent ?: JSONObject.NULL)
                .put("own", own != null)
                .put("mood", feel.mood.name).put("heat", feel.heat.toDouble())
                .put("down", status?.down?.contains(m) == true)
        )
    }
    return arr
}

package dev.clawdboard.desktop

internal val onMac = System.getProperty("os.name").orEmpty().startsWith("Mac")

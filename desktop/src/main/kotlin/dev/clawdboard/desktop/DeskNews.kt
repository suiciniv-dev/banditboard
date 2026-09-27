package dev.clawdboard.desktop

import dev.clawdboard.core.NewsItem
import dev.clawdboard.core.httpRequest
import org.w3c.dom.Element
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

object DeskNews {
    private const val URL = "https://raw.githubusercontent.com/Olshansk/rss-feeds/main/feeds/feed_anthropic_news.xml"

    fun fetch(): List<NewsItem>? = runCatching {
        val r = httpRequest(URL, timeoutMs = 20_000)
        if (r.code != 200) return null
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(r.body.byteInputStream())
        val items = doc.getElementsByTagName("item")
        (0 until items.length).mapNotNull { i ->
            val e = items.item(i) as Element
            fun tag(n: String) = e.getElementsByTagName(n).item(0)?.textContent?.trim().orEmpty()
            val title = tag("title").ifEmpty { return@mapNotNull null }
            val date = runCatching { ZonedDateTime.parse(tag("pubDate"), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }.getOrNull()
            NewsItem(title, tag("link"), date, tag("category").ifEmpty { null })
        }
    }.getOrNull()
}

import Foundation

struct Incident: Identifiable {
    let id = UUID()
    let name: String
    let link: URL?
}

struct NewsItem: Identifiable {
    let id = UUID()
    let title: String
    let link: URL?
    let date: Date?
}

enum Feeds {
    static func status() async -> [Incident]? {
        guard let url = URL(string: "https://status.claude.com/api/v2/incidents/unresolved.json") else { return nil }
        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 15)
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        guard let (data, resp) = try? await URLSession.shared.data(for: req), (resp as? HTTPURLResponse)?.statusCode == 200,
              let o = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else { return nil }
        return (o["incidents"] as? [[String: Any]] ?? []).map {
            Incident(name: $0["name"] as? String ?? L.incident, link: ($0["shortlink"] as? String).flatMap(URL.init(string:)))
        }
    }

    static func news() async -> [NewsItem]? {
        guard let url = URL(string: "https://raw.githubusercontent.com/Olshansk/rss-feeds/main/feeds/feed_anthropic_news.xml") else { return nil }
        let req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 20)
        guard let (data, resp) = try? await URLSession.shared.data(for: req), (resp as? HTTPURLResponse)?.statusCode == 200 else { return nil }
        let reader = RSS()
        let parser = XMLParser(data: data)
        parser.delegate = reader
        return parser.parse() ? reader.items : nil
    }
}

private final class RSS: NSObject, XMLParserDelegate {
    var items: [NewsItem] = []
    private var current: [String: String]?
    private var text = ""

    private static let format: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "EEE, dd MMM yyyy HH:mm:ss Z"
        return f
    }()

    func parser(_ parser: XMLParser, didStartElement name: String, namespaceURI: String?, qualifiedName: String?, attributes: [String: String] = [:]) {
        if name == "item" { current = [:] }
        text = ""
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        text += string
    }

    func parser(_ parser: XMLParser, foundCDATA block: Data) {
        text += String(decoding: block, as: UTF8.self)
    }

    func parser(_ parser: XMLParser, didEndElement name: String, namespaceURI: String?, qualifiedName: String?) {
        if name == "item", let c = current {
            if let title = c["title"], !title.isEmpty {
                items.append(NewsItem(title: title, link: c["link"].flatMap(URL.init(string:)), date: c["pubDate"].flatMap(RSS.format.date(from:))))
            }
            current = nil
        } else if current != nil, ["title", "link", "pubDate"].contains(name) {
            current?[name] = text.trimmingCharacters(in: .whitespacesAndNewlines)
        }
    }
}

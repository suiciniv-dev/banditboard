import CommonCrypto
import CryptoKit
import Foundation

struct UsageWindow: Codable, Equatable {
    var percent: Double
    var resetsAt: Date?
}

struct ScopedLimit: Codable, Equatable {
    var label: String
    var percent: Double
    var resetsAt: Date?
}

enum AgPool: String, Codable, CaseIterable { case GEMINI, OTHERS }

enum AgFamily: String, Codable, CaseIterable {
    case pro, flash, claude, gpt

    var label: String {
        switch self {
        case .pro: return "Gemini Pro"
        case .flash: return "Gemini Flash"
        case .claude: return "Claude"
        case .gpt: return "GPT-OSS"
        }
    }

    var short: String {
        switch self {
        case .pro: return "Pro"
        case .flash: return "Flash"
        case .claude: return "Claude"
        case .gpt: return "GPT"
        }
    }

    var pool: AgPool { self == .pro || self == .flash ? .GEMINI : .OTHERS }

    static func of(_ label: String) -> AgFamily? {
        let l = label.lowercased()
        if l.contains("gemini") && l.contains("flash") { return .flash }
        if l.contains("gemini") { return .pro }
        if l.contains("claude") { return .claude }
        if l.contains("gpt") { return .gpt }
        return nil
    }
}

struct AgGroup: Codable, Equatable {
    var pool: AgPool
    var session: UsageWindow?
    var week: UsageWindow?

    var worst: UsageWindow? { [session, week].compactMap { $0 }.max { $0.percent < $1.percent } }
}

struct AgSnap: Codable, Equatable {
    var groups: [AgGroup]
    var families: [AgFamily]
    var plan: String?
    var fetchedAt: Date
    var open: Bool

    func group(_ p: AgPool) -> AgGroup? { groups.first { $0.pool == p } }

    func percent(_ f: AgFamily) -> Double? { group(f.pool)?.worst?.percent }

    var shown: [AgFamily] { families.isEmpty ? AgFamily.allCases.filter { group($0.pool) != nil } : families }

    func settled(_ now: Date) -> AgSnap {
        func settle(_ w: UsageWindow?) -> UsageWindow? {
            if let r = w?.resetsAt, r <= now { return UsageWindow(percent: 0, resetsAt: nil) }
            return w
        }
        var s = self
        s.groups = groups.map { AgGroup(pool: $0.pool, session: settle($0.session), week: settle($0.week)) }
        return s
    }

    static func off(_ o: [String: Any]?) -> Bool { (o?["off"] as? Bool) == true }

    static func parse(_ o: [String: Any]?) -> AgSnap? {
        guard let o, !off(o), let pools = o["pools"] as? [[String: Any]] else { return nil }
        func num(_ v: Any?) -> Double? { (v as? NSNumber)?.doubleValue ?? (v as? String).flatMap(Double.init) }
        func window(_ v: Any?) -> UsageWindow? {
            guard let w = v as? [String: Any], let pct = num(w["percent"]) else { return nil }
            let reset = num(w["resetsAt"]).flatMap { $0 > 0 ? Date(timeIntervalSince1970: $0 / 1000) : nil }
            return UsageWindow(percent: min(max(pct, 0), 100), resetsAt: reset)
        }
        let groups = pools.compactMap { g -> AgGroup? in
            guard let pool = (g["pool"] as? String).flatMap(AgPool.init(rawValue:)) else { return nil }
            if g["session"] == nil && g["week"] == nil {
                guard let flat = window(g) else { return nil }
                return (g["kind"] as? String) == "SESSION" ? AgGroup(pool: pool, session: flat) : AgGroup(pool: pool, week: flat)
            }
            let group = AgGroup(pool: pool, session: window(g["session"]), week: window(g["week"]))
            return group.session == nil && group.week == nil ? nil : group
        }.sorted { $0.pool == .GEMINI && $1.pool == .OTHERS }
        if groups.isEmpty { return nil }
        let found = (o["models"] as? [[String: Any]] ?? []).compactMap { ($0["label"] as? String).flatMap(AgFamily.of) }
        let at = num(o["usage_at"]).flatMap { $0 > 0 ? Date(timeIntervalSince1970: $0 / 1000) : nil } ?? .now
        return AgSnap(groups: groups, families: AgFamily.allCases.filter(found.contains), plan: o["plan"] as? String,
                      fetchedAt: at, open: (o["open"] as? Bool) ?? true)
    }

    static func demo(_ now: Date = .now) -> AgSnap {
        AgSnap(
            groups: [
                AgGroup(pool: .GEMINI, session: UsageWindow(percent: 38, resetsAt: now.addingTimeInterval(2 * 3600 + 39 * 60)),
                        week: UsageWindow(percent: 21, resetsAt: now.addingTimeInterval(4 * 86_400 + 4 * 3600))),
                AgGroup(pool: .OTHERS, session: UsageWindow(percent: 12, resetsAt: now.addingTimeInterval(3600)),
                        week: UsageWindow(percent: 88, resetsAt: now.addingTimeInterval(4 * 86_400 + 4 * 3600))),
            ],
            families: AgFamily.allCases, plan: "Pro", fetchedAt: now.addingTimeInterval(-33), open: true
        )
    }
}

struct Snapshot: Codable, Equatable {
    var fiveHour: UsageWindow?
    var sevenDay: UsageWindow?
    var scoped: [ScopedLimit]
    var fetchedAt: Date
    var sessions: [String]?
    var ag: AgSnap? = nil

    var hasClaude: Bool { fiveHour != nil || sevenDay != nil }

    func settled(_ now: Date = .now) -> Snapshot {
        func settle(_ w: UsageWindow?) -> UsageWindow? {
            if let r = w?.resetsAt, r <= now { return UsageWindow(percent: 0, resetsAt: nil) }
            return w
        }
        var s = self
        s.fiveHour = settle(fiveHour)
        s.sevenDay = settle(sevenDay)
        s.scoped = scoped.filter { $0.resetsAt.map { $0 > now } ?? true }
        s.ag = ag?.settled(now)
        return s
    }

    static func demo(_ now: Date = .now) -> Snapshot {
        Snapshot(
            fiveHour: UsageWindow(percent: 37, resetsAt: now.addingTimeInterval(2 * 3600 + 13 * 60)),
            sevenDay: UsageWindow(percent: 64, resetsAt: now.addingTimeInterval(3 * 86_400 + 5 * 3600)),
            scoped: [ScopedLimit(label: "Fable", percent: 22, resetsAt: now.addingTimeInterval(3 * 86_400 + 5 * 3600))],
            fetchedAt: now.addingTimeInterval(-12),
            sessions: ["opus"],
            ag: AgSnap.demo(now)
        )
    }

    func nextRefresh(_ now: Date = .now) -> Date {
        now.addingTimeInterval(now.timeIntervalSince(fetchedAt) < 600 ? 5 * 60 : 30 * 60)
    }

    func own(_ model: String) -> ScopedLimit? {
        scoped.first { $0.label.localizedCaseInsensitiveContains(model) }
    }

    static func parse(_ o: [String: Any], at: Date, previous: Snapshot? = nil) -> Snapshot? {
        func num(_ v: Any?) -> Double? { (v as? NSNumber)?.doubleValue ?? (v as? String).flatMap(Double.init) }
        func date(_ v: Any?) -> Date? { num(v).flatMap { $0 > 0 ? Date(timeIntervalSince1970: $0) : nil } }
        func window(_ name: String) -> UsageWindow? {
            guard let w = o[name] as? [String: Any], let pct = num(w["used_percentage"]) else { return nil }
            return UsageWindow(percent: min(max(pct, 0), 100), resetsAt: date(w["resets_at"]))
        }
        let five = window("five_hour"), seven = window("seven_day")
        let agBody = o["antigravity"] as? [String: Any]
        let ag = AgSnap.parse(agBody) ?? (agBody == nil ? previous?.ag : nil)
        if five == nil && seven == nil && ag == nil { return nil }
        let scoped = (o["scoped"] as? [[String: Any]] ?? []).compactMap { s -> ScopedLimit? in
            guard let label = s["label"] as? String, let pct = num(s["used_percentage"]) else { return nil }
            return ScopedLimit(label: label, percent: min(max(pct, 0), 100), resetsAt: date(s["resets_at"]))
        }
        let usageAt = num(o["usage_at"]).flatMap { $0 > 0 ? Date(timeIntervalSince1970: $0 / 1000) : nil }
        return Snapshot(fiveHour: five, sevenDay: seven, scoped: scoped, fetchedAt: usageAt ?? at, sessions: o["sessions"] as? [String], ag: ag)
    }
}

struct Pairing: Codable, Equatable {
    var urls: [String]
    var key: String
    var seal: String?

    init?(url: URL) {
        guard url.scheme == "banditboard", let items = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems else { return nil }
        func item(_ name: String) -> String? { items.first(where: { $0.name == name })?.value.flatMap { $0.isEmpty ? nil : $0 } }
        switch url.host {
        case "pair":
            guard let key = item("k"), let list = item("u") else { return nil }
            let urls = list.split(separator: ",").map { String($0).trimmingCharacters(in: .whitespaces) }.filter { $0.hasPrefix("http") }
            if urls.isEmpty { return nil }
            self.urls = urls
            self.key = key
            self.seal = nil
        case "box":
            guard let worker = item("w"), worker.hasPrefix("https://"), let box = item("b"), let read = item("r"),
                  let master = item("e"), master.count == 64 else { return nil }
            self.urls = ["\(worker)/v1/box/\(box)"]
            self.key = read
            self.seal = master
        default:
            return nil
        }
    }
}

enum Sealed {
    static func open(_ blob: String, master: String) -> Data? {
        guard let all = Data(base64Encoded: blob), all.count > 48, let raw = hex(master) else { return nil }
        let root = SymmetricKey(data: raw)
        let enc = Data(HMAC<SHA256>.authenticationCode(for: Data("banditboard-enc".utf8), using: root))
        let mac = SymmetricKey(data: Data(HMAC<SHA256>.authenticationCode(for: Data("banditboard-mac".utf8), using: root)))
        let body = Data(all.prefix(all.count - 32))
        let tag = Data(all.suffix(32))
        guard HMAC<SHA256>.isValidAuthenticationCode(tag, authenticating: body, using: mac) else { return nil }
        let iv = Data(body.prefix(16))
        let cipher = Data(body.dropFirst(16))
        var out = Data(count: cipher.count + kCCBlockSizeAES128)
        var moved = 0
        let capacity = out.count
        let status = out.withUnsafeMutableBytes { o in
            cipher.withUnsafeBytes { c in
                iv.withUnsafeBytes { i in
                    enc.withUnsafeBytes { k in
                        CCCrypt(CCOperation(kCCDecrypt), CCAlgorithm(kCCAlgorithmAES), CCOptions(kCCOptionPKCS7Padding),
                                k.baseAddress, enc.count, i.baseAddress, c.baseAddress, cipher.count, o.baseAddress, capacity, &moved)
                    }
                }
            }
        }
        guard status == kCCSuccess else { return nil }
        return out.prefix(moved)
    }

    private static func hex(_ s: String) -> Data? {
        var data = Data(capacity: s.count / 2)
        var index = s.startIndex
        while index < s.endIndex {
            let next = s.index(index, offsetBy: 2, limitedBy: s.endIndex) ?? s.endIndex
            guard let byte = UInt8(s[index..<next], radix: 16) else { return nil }
            data.append(byte)
            index = next
        }
        return data
    }
}

enum Vault {
    private static let defaults = UserDefaults.standard

    private static let group: String? = {
        guard let team = keychainTeam() ?? profileTeam() else { return nil }
        let group = "\(team).dev.clawdboard.banditboard.shared"
        return probe(group) != nil ? group : nil
    }()

    private static func probe(_ group: String?) -> String? {
        var q: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "banditboard.team",
            kSecAttrAccount as String: "team",
            kSecReturnAttributes as String: true,
        ]
        if let group { q[kSecAttrAccessGroup as String] = group }
        var out: AnyObject?
        var status = SecItemCopyMatching(q as CFDictionary, &out)
        if status == errSecItemNotFound {
            q[kSecValueData as String] = Data()
            q[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
            status = SecItemAdd(q as CFDictionary, &out)
        }
        guard status == errSecSuccess else { return nil }
        return (out as? [String: Any])?[kSecAttrAccessGroup as String] as? String ?? group
    }

    private static func keychainTeam() -> String? {
        guard let team = probe(nil)?.split(separator: ".").first, team.count == 10 else { return nil }
        return String(team)
    }

    private static func profileTeam() -> String? {
        guard let url = Bundle.main.url(forResource: "embedded", withExtension: "mobileprovision"),
              let raw = try? Data(contentsOf: url),
              let start = raw.range(of: Data("<?xml".utf8)),
              let end = raw.range(of: Data("</plist>".utf8), in: start.lowerBound..<raw.endIndex),
              let plist = try? PropertyListSerialization.propertyList(from: raw[start.lowerBound..<end.upperBound], format: nil) as? [String: Any],
              let team = (plist["TeamIdentifier"] as? [String])?.first else { return nil }
        return team
    }

    private static func query(_ account: String) -> [String: Any] {
        var q: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "banditboard",
            kSecAttrAccount as String: account,
        ]
        if let group { q[kSecAttrAccessGroup as String] = group }
        return q
    }

    private static func read<T: Decodable>(_ account: String) -> T? {
        var q = query(account)
        q[kSecReturnData as String] = true
        q[kSecMatchLimit as String] = kSecMatchLimitOne
        var out: AnyObject?
        guard SecItemCopyMatching(q as CFDictionary, &out) == errSecSuccess, let data = out as? Data else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }

    private static func write<T: Encodable>(_ account: String, _ value: T?) {
        SecItemDelete(query(account) as CFDictionary)
        guard let value, let data = try? JSONEncoder().encode(value) else { return }
        var q = query(account)
        q[kSecValueData as String] = data
        q[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        SecItemAdd(q as CFDictionary, nil)
    }

    static var pairing: Pairing? {
        get { read("pairing") }
        set { write("pairing", newValue) }
    }

    static var snapshot: Snapshot? {
        get { read("snapshot") }
        set { write("snapshot", newValue) }
    }

    static var demo: Bool {
        get { ProcessInfo.processInfo.arguments.contains("--demo") || (read("demo") ?? false) }
        set { write("demo", newValue ? true : nil) }
    }

    private static var cachedPrefs: Prefs?

    static var prefs: Prefs {
        get {
            if let p = cachedPrefs { return p }
            let p: Prefs = read("prefs") ?? Prefs()
            cachedPrefs = p
            return p
        }
        set {
            cachedPrefs = newValue
            write("prefs", newValue)
        }
    }

    static func mark(_ kind: String) -> Int { defaults.integer(forKey: "mark.\(kind)") }
    static func setMark(_ kind: String, _ level: Int) { defaults.set(level, forKey: "mark.\(kind)") }
    static func freeAt(_ kind: String) -> Date? { defaults.object(forKey: "free.\(kind)") as? Date }
    static func setFreeAt(_ kind: String, _ date: Date?) { defaults.set(date, forKey: "free.\(kind)") }
}

enum FetchError: Error {
    case notPaired, badKey, unreachable, noData
}

enum Client {
    static func fetch() async throws -> Snapshot {
        if Vault.demo {
            var demo = Snapshot.demo()
            if ProcessInfo.processInfo.arguments.contains("--no-ag") { demo.ag = nil }
            return demo
        }
        guard var pairing = Vault.pairing else { throw FetchError.notPaired }
        if let master = pairing.seal { return try await fromBox(pairing, master) }
        for (i, base) in pairing.urls.enumerated() {
            guard let url = URL(string: base + "/api/usage") else { continue }
            var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 5)
            req.setValue(pairing.key, forHTTPHeaderField: "X-Banditboard-Key")
            guard let (data, resp) = try? await URLSession.shared.data(for: req), let http = resp as? HTTPURLResponse else { continue }
            if http.statusCode == 401 { throw FetchError.badKey }
            guard http.statusCode == 200, let o = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else { continue }
            if i > 0 {
                pairing.urls.insert(pairing.urls.remove(at: i), at: 0)
                Vault.pairing = pairing
            }
            let at = (o["at"] as? NSNumber).map { Date(timeIntervalSince1970: $0.doubleValue / 1000) } ?? .now
            guard let usage = o["usage"] as? [String: Any], let snap = Snapshot.parse(usage, at: at, previous: Vault.snapshot) else { throw FetchError.noData }
            Vault.snapshot = snap
            return snap
        }
        throw FetchError.unreachable
    }

    private static func fromBox(_ pairing: Pairing, _ master: String) async throws -> Snapshot {
        guard let url = pairing.urls.first.flatMap(URL.init(string:)) else { throw FetchError.notPaired }
        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 10)
        req.setValue(pairing.key, forHTTPHeaderField: "X-Clawdboard-Key")
        guard let (data, resp) = try? await URLSession.shared.data(for: req), let http = resp as? HTTPURLResponse else { throw FetchError.unreachable }
        if http.statusCode == 401 || http.statusCode == 404 { throw FetchError.badKey }
        guard http.statusCode == 200, let o = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else { throw FetchError.unreachable }
        guard let blob = o["blob"] as? String else { throw FetchError.noData }
        guard let plain = Sealed.open(blob, master: master), let usage = (try? JSONSerialization.jsonObject(with: plain)) as? [String: Any] else {
            throw FetchError.badKey
        }
        let at = (o["at"] as? NSNumber).map { Date(timeIntervalSince1970: $0.doubleValue / 1000) } ?? .now
        guard let snap = Snapshot.parse(usage, at: at, previous: Vault.snapshot) else { throw FetchError.noData }
        Vault.snapshot = snap
        return snap
    }
}

struct Sample: Codable, Equatable {
    var t: Date
    var p5: Double?
    var p7: Double?
}

enum History {
    static let window: TimeInterval = 7 * 86_400
    static let slot: TimeInterval = 30 * 60

    static var samples: [Sample] {
        guard let data = UserDefaults.standard.data(forKey: "history"), let list = try? JSONDecoder().decode([Sample].self, from: data) else { return [] }
        return list
    }

    static func record(_ s: Snapshot, now: Date = .now) {
        guard s.hasClaude else { return }
        let t = Date(timeIntervalSince1970: (s.fetchedAt.timeIntervalSince1970 / slot).rounded(.down) * slot)
        var list = samples.filter { $0.t > now.addingTimeInterval(-window) && $0.t != t }
        list.append(Sample(t: t, p5: s.fiveHour?.percent, p7: s.sevenDay?.percent))
        list.sort { $0.t < $1.t }
        if let data = try? JSONEncoder().encode(list) { UserDefaults.standard.set(data, forKey: "history") }
    }
}

struct Alert {
    let week: Bool
    let level: Int
    let resetsAt: Date?
}

let ALERT_LEVELS = [80, 90, 100]

func nextAlert(week: Bool, _ w: UsageWindow?, sent: Int) -> (Alert?, Int) {
    guard let w else { return (nil, sent) }
    let level = ALERT_LEVELS.last { w.percent >= Double($0) - 0.5 } ?? 0
    if level > sent { return (Alert(week: week, level: level, resetsAt: w.resetsAt), level) }
    if sent > 0 && w.percent < Double(sent - 20) { return (sent >= 90 ? Alert(week: week, level: 0, resetsAt: w.resetsAt) : nil, level) }
    return (nil, sent)
}

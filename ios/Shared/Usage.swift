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

struct Snapshot: Codable, Equatable {
    var fiveHour: UsageWindow?
    var sevenDay: UsageWindow?
    var scoped: [ScopedLimit]
    var fetchedAt: Date
    var sessions: [String]?

    func settled(_ now: Date = .now) -> Snapshot {
        func settle(_ w: UsageWindow?) -> UsageWindow? {
            if let r = w?.resetsAt, r <= now { return UsageWindow(percent: 0, resetsAt: nil) }
            return w
        }
        var s = self
        s.fiveHour = settle(fiveHour)
        s.sevenDay = settle(sevenDay)
        s.scoped = scoped.filter { $0.resetsAt.map { $0 > now } ?? true }
        return s
    }

    func own(_ model: String) -> ScopedLimit? {
        scoped.first { $0.label.localizedCaseInsensitiveContains(model) }
    }

    static func parse(_ o: [String: Any], at: Date) -> Snapshot? {
        func num(_ v: Any?) -> Double? { (v as? NSNumber)?.doubleValue ?? (v as? String).flatMap(Double.init) }
        func date(_ v: Any?) -> Date? { num(v).flatMap { $0 > 0 ? Date(timeIntervalSince1970: $0) : nil } }
        func window(_ name: String) -> UsageWindow? {
            guard let w = o[name] as? [String: Any], let pct = num(w["used_percentage"]) else { return nil }
            return UsageWindow(percent: min(max(pct, 0), 100), resetsAt: date(w["resets_at"]))
        }
        let five = window("five_hour"), seven = window("seven_day")
        if five == nil && seven == nil { return nil }
        let scoped = (o["scoped"] as? [[String: Any]] ?? []).compactMap { s -> ScopedLimit? in
            guard let label = s["label"] as? String, let pct = num(s["used_percentage"]) else { return nil }
            return ScopedLimit(label: label, percent: min(max(pct, 0), 100), resetsAt: date(s["resets_at"]))
        }
        return Snapshot(fiveHour: five, sevenDay: seven, scoped: scoped, fetchedAt: at, sessions: o["sessions"] as? [String])
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
    private static let group = (Bundle.main.object(forInfoDictionaryKey: "AppIdentifierPrefix") as? String ?? "") + "dev.clawdboard.banditboard.shared"
    private static let defaults = UserDefaults.standard

    private static func query(_ account: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "banditboard",
            kSecAttrAccount as String: account,
            kSecAttrAccessGroup as String: group,
        ]
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
            guard let usage = o["usage"] as? [String: Any], let snap = Snapshot.parse(usage, at: at) else { throw FetchError.noData }
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
        guard let snap = Snapshot.parse(usage, at: at) else { throw FetchError.noData }
        Vault.snapshot = snap
        return snap
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

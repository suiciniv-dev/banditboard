import Foundation
import WatchConnectivity

final class WatchSync: NSObject, WCSessionDelegate {
    static let shared = WatchSync()

    func start() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    func send() {
        guard WCSession.isSupported(), WCSession.default.activationState == .activated, WCSession.default.isPaired else { return }
        let encoder = JSONEncoder()
        var context: [String: Any] = ["demo": Vault.demo, "at": Date.now.timeIntervalSince1970]
        if let pairing = Vault.pairing, let data = try? encoder.encode(pairing) { context["pairing"] = data } else { context["pairing"] = "" }
        if let data = try? encoder.encode(Vault.prefs) { context["prefs"] = data }
        try? WCSession.default.updateApplicationContext(context)
    }

    func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        if state == .activated { send() }
    }

    func sessionDidBecomeInactive(_ session: WCSession) {}

    func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }
}

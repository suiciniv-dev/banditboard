import Foundation
import UserNotifications

final class Foreground: NSObject, UNUserNotificationCenterDelegate {
    static let shared = Foreground()

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        [.banner, .sound, .list]
    }
}

enum Notifier {
    static func ask() async {
        UNUserNotificationCenter.current().delegate = Foreground.shared
        _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])
    }

    static func process(_ snap: Snapshot) {
        guard Vault.prefs.alerts else { return }
        let now = Date.now
        let s = snap.settled(now)
        if Vault.prefs.showClaude && s.hasClaude {
            for (kind, week, w) in [("SESSION", false, s.fiveHour), ("WEEK", true, s.sevenDay)] {
                track(kind, week: week, w, now: now, title: { L.alertTitle(week: week, level: $0) }, free: L.alertFree)
            }
        }
        if Vault.prefs.showAg, let ag = s.ag {
            for g in ag.groups {
                for (week, w) in [(false, g.session), (true, g.week)] where w != nil {
                    let kind = "AG.\(g.pool.rawValue).\(week ? "WEEK" : "SESSION")"
                    track(kind, week: week, w, now: now, title: { L.agAlertTitle(g.pool, week: week, level: $0) }, free: L.agAlertFree)
                }
            }
        }
    }

    private static func track(_ kind: String, week: Bool, _ w: UsageWindow?, now: Date, title: (Int) -> String, free: String) {
        let sent = Vault.mark(kind)
        let (alert, level) = nextAlert(week: week, w, sent: sent)
        Vault.setMark(kind, level)
        if let alert {
            if alert.level == 0 {
                let scheduled = Vault.freeAt(kind)
                Vault.setFreeAt(kind, nil)
                if scheduled.map({ $0 > now }) ?? true {
                    cancel("free.\(kind)")
                    post("free.\(kind)", title(0), free, at: nil)
                }
            } else {
                let body = alert.resetsAt.map { L.resets(formatAt($0, now: now)) } ?? ""
                post("alert.\(kind)", title(alert.level), body, at: nil)
            }
        }
        if level >= 80, let reset = w?.resetsAt, reset > now {
            if Vault.freeAt(kind) != reset {
                Vault.setFreeAt(kind, reset)
                post("free.\(kind)", title(0), free, at: reset)
            }
        }
    }

    static func sample() {
        let now = Date.now
        post("sample.session", L.alertTitle(week: false, level: 90), L.resets(formatAt(now.addingTimeInterval(2 * 3600 + 13 * 60), now: now)), at: now.addingTimeInterval(6))
        post("sample.week", L.alertTitle(week: true, level: 80), L.resets(formatAt(now.addingTimeInterval(3 * 86_400 + 5 * 3600), now: now)), at: now.addingTimeInterval(9))
    }

    private static func cancel(_ id: String) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [id])
    }

    private static func post(_ id: String, _ title: String, _ body: String, at: Date?) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let trigger = at.map { UNTimeIntervalNotificationTrigger(timeInterval: max($0.timeIntervalSinceNow, 1), repeats: false) }
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: id, content: content, trigger: trigger))
    }
}

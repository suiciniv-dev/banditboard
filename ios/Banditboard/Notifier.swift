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
        let now = Date.now
        let s = snap.settled(now)
        for (kind, week, w) in [("SESSION", false, s.fiveHour), ("WEEK", true, s.sevenDay)] {
            let sent = Vault.mark(kind)
            let (alert, level) = nextAlert(week: week, w, sent: sent)
            Vault.setMark(kind, level)
            if let alert {
                if alert.level == 0 {
                    let scheduled = Vault.freeAt(kind)
                    Vault.setFreeAt(kind, nil)
                    if scheduled.map({ $0 > now }) ?? true {
                        cancel("free.\(kind)")
                        post("free.\(kind)", L.alertTitle(week: week, level: 0), L.alertFree, at: nil)
                    }
                } else {
                    let body = alert.resetsAt.map { L.resets(formatAt($0, now: now)) } ?? ""
                    post("alert.\(kind)", L.alertTitle(week: week, level: alert.level), body, at: nil)
                }
            }
            if level >= 90, let reset = w?.resetsAt, reset > now {
                if Vault.freeAt(kind) != reset {
                    Vault.setFreeAt(kind, reset)
                    post("free.\(kind)", L.alertTitle(week: week, level: 0), L.alertFree, at: reset)
                }
            }
        }
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

import BackgroundTasks
import SwiftUI
import WidgetKit

@main
struct BanditboardApp: App {
    @Environment(\.scenePhase) private var phase
    @StateObject private var model = DashboardModel()

    var body: some Scene {
        WindowGroup {
            DashboardView(model: model)
                .preferredColorScheme(.dark)
                .environment(\.locale, L.locale)
                .onOpenURL { url in
                    guard let pairing = Pairing(url: url) else { return }
                    Vault.pairing = pairing
                    Task { await model.refresh() }
                }
                .task { await Notifier.ask() }
        }
        .onChange(of: phase) { _, now in
            if now == .active { Task { await model.refresh() } }
            if now == .background { Refresher.schedule() }
        }
        .backgroundTask(.appRefresh(Refresher.id)) {
            await Refresher.run()
        }
    }
}

enum Refresher {
    static let id = "dev.clawdboard.banditboard.refresh"

    static func schedule() {
        let request = BGAppRefreshTaskRequest(identifier: id)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        try? BGTaskScheduler.shared.submit(request)
    }

    static func run() async {
        schedule()
        guard let snap = try? await Client.fetch() else { return }
        Notifier.process(snap)
        WidgetCenter.shared.reloadAllTimelines()
    }
}

@MainActor
final class DashboardModel: ObservableObject {
    @Published var snapshot: Snapshot? = Vault.snapshot
    @Published var problem: FetchError?
    @Published var busy = false
    @Published var incidents: [Incident]?
    @Published var news: [NewsItem]?
    private var newsAt = Date.distantPast

    func feeds() async {
        if let list = await Feeds.status() { incidents = list }
        if news == nil || Date.now.timeIntervalSince(newsAt) > 3600, let list = await Feeds.news() {
            news = list
            newsAt = .now
        }
    }

    var paired: Bool { Vault.pairing != nil }

    func refresh() async {
        guard paired, !busy else { return }
        busy = true
        defer { busy = false }
        do {
            let snap = try await Client.fetch()
            snapshot = snap
            problem = nil
            Notifier.process(snap)
            WidgetCenter.shared.reloadAllTimelines()
        } catch let e as FetchError {
            problem = e
        } catch {
            problem = .unreachable
        }
    }

    func unpair() {
        Vault.pairing = nil
        problem = nil
        objectWillChange.send()
    }
}

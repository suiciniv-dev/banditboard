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
                    if url.scheme == "banditboard", url.host == "test-alert" {
                        Task {
                            await Notifier.ask()
                            Notifier.sample()
                        }
                        return
                    }
                    if url.scheme == "banditboard", url.host == "demo" {
                        Vault.demo = url.query != "off=1"
                        WatchSync.shared.send()
                        model.objectWillChange.send()
                        Task { await model.refresh() }
                        return
                    }
                    guard let pairing = Pairing(url: url) else { return }
                    Vault.pairing = pairing
                    Vault.demo = false
                    WatchSync.shared.send()
                    Task { await model.refresh() }
                }
                .task { if !ProcessInfo.processInfo.arguments.contains("--demo") { await Notifier.ask() } }
                .onAppear { WatchSync.shared.start() }
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
        History.record(snap)
        Notifier.process(snap)
        WidgetCenter.shared.reloadAllTimelines()
        WatchSync.shared.send()
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

    var paired: Bool { Vault.pairing != nil || Vault.demo }

    func refresh() async {
        guard paired, !busy else { return }
        busy = true
        defer { busy = false }
        do {
            let snap = try await Client.fetch()
            snapshot = snap
            problem = nil
            History.record(snap)
            Notifier.process(snap)
            WidgetCenter.shared.reloadAllTimelines()
            WatchSync.shared.send()
        } catch let e as FetchError {
            problem = e
        } catch {
            problem = .unreachable
        }
    }

    func unpair() {
        Vault.pairing = nil
        WatchSync.shared.send()
        problem = nil
        objectWillChange.send()
    }
}

import SwiftUI
import WatchConnectivity
import WidgetKit

@main
struct BanditboardWatchApp: App {
    @StateObject private var model = WatchModel()

    var body: some Scene {
        WindowGroup {
            WatchDashboard(model: model)
                .environment(\.locale, L.locale)
        }
    }
}

@MainActor
final class WatchModel: NSObject, ObservableObject, WCSessionDelegate {
    @Published var snapshot: Snapshot? = Vault.snapshot
    @Published var problem: FetchError?

    var paired: Bool { Vault.pairing?.seal != nil || Vault.demo }

    override init() {
        super.init()
        if WCSession.isSupported() {
            WCSession.default.delegate = self
            WCSession.default.activate()
        }
    }

    func refresh() async {
        guard paired else { return }
        do {
            snapshot = try await Client.fetch()
            problem = nil
            WidgetCenter.shared.reloadAllTimelines()
        } catch let e as FetchError {
            problem = e
        } catch {
            problem = .unreachable
        }
    }

    nonisolated func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        let context = session.receivedApplicationContext
        Task { @MainActor in self.apply(context) }
    }

    nonisolated func session(_ session: WCSession, didReceiveApplicationContext context: [String: Any]) {
        Task { @MainActor in self.apply(context) }
    }

    private func apply(_ context: [String: Any]) {
        guard !context.isEmpty else { return }
        let decoder = JSONDecoder()
        if let data = context["pairing"] as? Data {
            Vault.pairing = try? decoder.decode(Pairing.self, from: data)
        } else if context["pairing"] != nil {
            Vault.pairing = nil
        }
        if let data = context["prefs"] as? Data, let prefs = try? decoder.decode(Prefs.self, from: data) { Vault.prefs = prefs }
        if let demo = context["demo"] as? Bool { Vault.demo = demo }
        objectWillChange.send()
        WidgetCenter.shared.reloadAllTimelines()
        Task { await refresh() }
    }
}

struct WatchDashboard: View {
    @ObservedObject var model: WatchModel
    @State private var page = ProcessInfo.processInfo.arguments.first { $0.hasPrefix("--page=") }.flatMap { Int($0.dropFirst(7)) } ?? 0

    var body: some View {
        Group {
            if model.paired {
                TimelineView(.periodic(from: .now, by: 1)) { ctx in
                    pages(model.snapshot?.settled(ctx.date), now: ctx.date)
                }
            } else {
                ScrollView {
                    VStack(spacing: 8) {
                        MascotView(reserveTop: false).frame(width: 70)
                        Text(L.watchPair).font(.footnote).multilineTextAlignment(.center).foregroundStyle(Palette.muted)
                    }
                }
            }
        }
        .containerBackground(Palette.bg.gradient, for: .tabView)
        .task {
            while !Task.isCancelled {
                await model.refresh()
                try? await Task.sleep(for: .seconds(60))
            }
        }
    }

    private func pages(_ u: Snapshot?, now: Date) -> some View {
        TabView(selection: $page) {
            WindowPage(title: L.session, usage: u?.fiveHour, now: now, mascot: MascotView(feel: feelOf(u, "Opus"), reserveTop: false)).tag(0)
            WindowPage(title: L.week, usage: u?.sevenDay, now: now, mascot: nil).tag(1)
            ModelsPage(snapshot: u).tag(2)
        }
        .tabViewStyle(.verticalPage)
    }
}

struct WindowPage: View {
    let title: String
    let usage: UsageWindow?
    let now: Date
    let mascot: MascotView?

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title).font(.system(.headline, design: .rounded)).foregroundStyle(Palette.text)
                Spacer()
                if let mascot { mascot.frame(width: 38) }
            }
            Text(formatPct(usage?.percent))
                .font(.system(size: 44, weight: .bold, design: .rounded))
                .foregroundStyle(usage.map { Palette.level($0.percent) } ?? Palette.muted)
                .minimumScaleFactor(0.6)
            Gauge(value: min(usage?.percent ?? 0, 100), in: 0...100) { EmptyView() }
                .gaugeStyle(.linearCapacity)
                .tint(usage.map { Palette.level($0.percent) } ?? Palette.dim)
            if let reset = usage?.resetsAt, reset > now {
                Text("\(L.resetsIn) \(formatLeft(reset.timeIntervalSince(now)))").font(.footnote).foregroundStyle(Palette.muted)
                Text(formatAt(reset, now: now)).font(.caption2).foregroundStyle(Palette.dim)
            }
        }
        .padding(.horizontal, 4)
    }
}

struct ModelsPage: View {
    let snapshot: Snapshot?

    var body: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
            ForEach(MODELS, id: \.self) { m in
                let own = snapshot?.own(m)
                let pct = own?.percent ?? snapshot?.sevenDay?.percent
                VStack(spacing: 2) {
                    MascotView(model: m, feel: feelOf(snapshot, m)).frame(width: 44)
                    Text(m).font(.caption2).foregroundStyle(Palette.muted)
                    Text(formatPct(pct)).font(.system(.footnote, design: .rounded).weight(.semibold))
                        .foregroundStyle(own != nil && pct != nil ? Palette.level(pct!) : Palette.muted)
                }
            }
        }
    }
}

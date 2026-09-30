import SwiftUI
import WidgetKit

struct WatchEntry: TimelineEntry {
    let date: Date
    let snapshot: Snapshot?
}

struct WatchProvider: TimelineProvider {
    func placeholder(in context: Context) -> WatchEntry {
        WatchEntry(date: .now, snapshot: Snapshot.demo())
    }

    func getSnapshot(in context: Context, completion: @escaping (WatchEntry) -> Void) {
        completion(WatchEntry(date: .now, snapshot: context.isPreview ? Snapshot.demo() : (Vault.snapshot ?? Snapshot.demo())))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WatchEntry>) -> Void) {
        Task {
            let paired = Vault.pairing?.seal != nil || Vault.demo
            let snap = paired ? ((try? await Client.fetch()) ?? Vault.snapshot) : nil
            let now = Date.now
            var entries = [WatchEntry(date: now, snapshot: snap)]
            if let reset = snap?.fiveHour?.resetsAt, reset > now, reset < now.addingTimeInterval(6 * 3600) {
                entries.append(WatchEntry(date: reset, snapshot: snap))
            }
            completion(Timeline(entries: entries, policy: .after(snap?.nextRefresh(now) ?? now.addingTimeInterval(30 * 60))))
        }
    }
}

struct WatchWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: WatchEntry

    private var u: Snapshot? { entry.snapshot?.settled(entry.date) }
    private var session: Double? { u?.fiveHour?.percent }

    var body: some View {
        switch family {
        case .accessoryRectangular:
            HStack(spacing: 6) {
                MascotView(reserveTop: false).frame(width: 34)
                VStack(alignment: .leading, spacing: 0) {
                    Text("\(L.session) \(formatPct(session))").font(.headline)
                    Text("\(L.week) \(formatPct(u?.sevenDay?.percent))").font(.caption)
                    if let reset = u?.fiveHour?.resetsAt, reset > entry.date {
                        Text(reset, style: .relative).font(.caption2).foregroundStyle(.secondary)
                    }
                }
            }
        case .accessoryInline:
            Text("\(L.session) \(formatPct(session)) · \(L.week) \(formatPct(u?.sevenDay?.percent))")
        case .accessoryCorner:
            Text(formatPct(session))
                .font(.system(.body, design: .rounded).weight(.semibold))
                .widgetLabel {
                    Gauge(value: min(session ?? 0, 100), in: 0...100) { Text("5h") }
                        .tint(session.map { Palette.level($0) } ?? Palette.dim)
                }
        default:
            RaccoRing(percent: session, feel: feelOf(u, "Opus"), tint: session.map { Palette.level($0) } ?? Palette.dim)
        }
    }
}

struct WatchUsageWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "watch-usage", provider: WatchProvider()) { entry in
            WatchWidgetView(entry: entry)
                .environment(\.locale, L.locale)
                .containerBackground(Palette.bg, for: .widget)
        }
        .configurationDisplayName("Banditboard")
        .description(L.pt ? "Uso do Claude Code com o Racco." : "Claude Code usage with Racco.")
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline, .accessoryCorner])
    }
}

@main
struct BanditboardWatchWidgets: WidgetBundle {
    var body: some Widget {
        WatchUsageWidget()
    }
}

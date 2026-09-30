import SwiftUI
import WidgetKit

struct UsageEntry: TimelineEntry {
    let date: Date
    let snapshot: Snapshot?
    var paired = true
}

private let sample = Snapshot(
    fiveHour: UsageWindow(percent: 37, resetsAt: .now.addingTimeInterval(2 * 3600 + 13 * 60)),
    sevenDay: UsageWindow(percent: 64, resetsAt: .now.addingTimeInterval(3 * 86_400 + 5 * 3600)),
    scoped: [ScopedLimit(label: "Fable", percent: 22, resetsAt: nil)],
    fetchedAt: .now
)

struct UsageProvider: TimelineProvider {
    func placeholder(in context: Context) -> UsageEntry {
        UsageEntry(date: .now, snapshot: sample)
    }

    func getSnapshot(in context: Context, completion: @escaping (UsageEntry) -> Void) {
        completion(UsageEntry(date: .now, snapshot: context.isPreview ? sample : (Vault.snapshot ?? sample)))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<UsageEntry>) -> Void) {
        Task {
            let paired = Vault.pairing != nil || Vault.demo
            let snap = paired ? ((try? await Client.fetch()) ?? Vault.snapshot) : nil
            let now = Date.now
            var entries = [UsageEntry(date: now, snapshot: snap, paired: paired)]
            if let reset = snap?.fiveHour?.resetsAt, reset > now, reset < now.addingTimeInterval(6 * 3600) {
                entries.append(UsageEntry(date: reset, snapshot: snap, paired: paired))
            }
            completion(Timeline(entries: entries, policy: .after(now.addingTimeInterval(15 * 60))))
        }
    }
}

struct UsageWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: UsageEntry

    private var u: Snapshot? { entry.snapshot?.settled(entry.date) }

    var body: some View {
        if !entry.paired {
            VStack(spacing: 6) {
                MascotView(reserveTop: false)
                Text(L.pairTitle).font(.caption2).foregroundStyle(Palette.muted).multilineTextAlignment(.center)
            }
        } else {
            switch family {
            case .accessoryCircular: circular
            case .accessoryRectangular: rectangular
            case .systemMedium: medium
            default: small
            }
        }
    }

    private var small: some View {
        VStack(spacing: 4) {
            MascotView(model: nil, feel: feelOf(u, "Opus"), reserveTop: false).frame(maxWidth: 96)
            Text(formatPct(u?.fiveHour?.percent))
                .font(.system(size: 34, weight: .bold, design: .rounded))
                .foregroundStyle(u?.fiveHour.map { Palette.level($0.percent) } ?? Palette.muted)
                .minimumScaleFactor(0.6)
            resetLine(L.session, u?.fiveHour?.resetsAt)
        }
    }

    private var medium: some View {
        HStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 12) {
                meter(L.session, u?.fiveHour)
                meter(L.week, u?.sevenDay)
            }
            MascotView(model: nil, feel: feelOf(u, "Opus"), reserveTop: false).frame(width: 104)
        }
    }

    private var rectangular: some View {
        HStack(spacing: 8) {
            MascotView(reserveTop: false).frame(width: 38)
            VStack(alignment: .leading, spacing: 1) {
                Text("\(L.session) \(formatPct(u?.fiveHour?.percent))").font(.headline)
                Text("\(L.week) \(formatPct(u?.sevenDay?.percent))").font(.caption)
            }
        }
    }

    private var circular: some View {
        Gauge(value: min(u?.fiveHour?.percent ?? 0, 100), in: 0...100) {
            Text("5h")
        } currentValueLabel: {
            Text(formatPct(u?.fiveHour?.percent))
        }
        .gaugeStyle(.accessoryCircular)
    }

    private func resetLine(_ label: String, _ reset: Date?) -> some View {
        HStack(spacing: 3) {
            Text(label)
            if let reset, reset > entry.date {
                Text("·")
                Text(reset, style: .relative)
            }
        }
        .font(.caption2)
        .foregroundStyle(Palette.muted)
        .lineLimit(1)
        .minimumScaleFactor(0.7)
    }

    private func meter(_ label: String, _ w: UsageWindow?) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label).font(.system(size: 14, weight: .semibold, design: .rounded)).foregroundStyle(Palette.text)
                Spacer()
                Text(formatPct(w?.percent)).font(.system(size: 18, weight: .bold, design: .rounded))
                    .foregroundStyle(w.map { Palette.level($0.percent) } ?? Palette.muted)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Palette.track)
                    if let pct = w?.percent {
                        Capsule().fill(Palette.level(pct)).frame(width: max(6, geo.size.width * min(pct, 100) / 100))
                    }
                }
            }
            .frame(height: 6)
            if let reset = w?.resetsAt, reset > entry.date {
                HStack(spacing: 3) {
                    Text(L.resetsIn)
                    Text(reset, style: .relative)
                }
                .font(.caption2)
                .foregroundStyle(Palette.muted)
                .lineLimit(1)
            }
        }
    }
}

struct UsageWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "usage", provider: UsageProvider()) { entry in
            UsageWidgetView(entry: entry)
                .environment(\.locale, L.locale)
                .containerBackground(Palette.bg, for: .widget)
                .widgetURL(URL(string: "banditboard://dashboard"))
        }
        .configurationDisplayName("Banditboard")
        .description(L.pt ? "Uso do Claude Code com o Racco." : "Claude Code usage with Racco.")
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryRectangular, .accessoryCircular])
    }
}

@main
struct BanditboardWidgets: WidgetBundle {
    var body: some Widget {
        UsageWidget()
    }
}

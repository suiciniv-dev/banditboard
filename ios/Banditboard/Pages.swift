import Charts
import SwiftUI

enum Page: String, CaseIterable, Identifiable {
    case dash, mascots, chart, clock, ag, both

    var id: String { rawValue }
    var isAg: Bool { self == .ag || self == .both }
}

private let CLOCK_DIGITS: [Color] = [Color(hex: 0xF6A5C0), Color(hex: 0xF9C3D6), Color(hex: 0xF5D66A), Color(hex: 0xC8B4F5)]
private let LAV = Color(hex: 0xB9A6F2)

struct ResetInfo: View {
    let reset: Date?
    let now: Date
    var tone = Tone.claude
    var compact = false
    var waiting = false

    var body: some View {
        VStack(alignment: .trailing, spacing: 1) {
            if let reset {
                if !compact { Text(L.resetsIn).font(.caption).foregroundStyle(tone.muted) }
                Text(formatLeft(reset.timeIntervalSince(now)))
                    .font(.system(size: compact ? 18 : 22, weight: .semibold, design: .rounded))
                    .foregroundStyle(tone.text)
                    .monospacedDigit()
                Text(formatAt(reset, now: now)).font(.caption2).foregroundStyle(tone.muted)
            } else {
                Text(waiting ? L.toolNoData : L.noReset).font(.caption).foregroundStyle(tone.dim)
            }
        }
    }
}

struct UsageBlock: View {
    let title: String
    let window: String
    let usage: UsageWindow?
    let now: Date
    var tone = Tone.claude
    var big: CGFloat = 60

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .lastTextBaseline, spacing: 8) {
                Text(title).font(.system(size: 19, weight: .semibold, design: .rounded)).foregroundStyle(tone.text).lineLimit(1)
                Text(window).font(.caption).foregroundStyle(tone.dim).lineLimit(1)
            }
            HStack(alignment: .bottom) {
                Text(formatPct(usage?.percent))
                    .font(.system(size: big, weight: .bold, design: .rounded))
                    .foregroundStyle(usage.map { Palette.level($0.percent) } ?? tone.dim)
                    .contentTransition(.numericText())
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                Spacer(minLength: 8)
                ResetInfo(reset: usage?.resetsAt, now: now, tone: tone, waiting: usage == nil).padding(.bottom, big * 0.12)
            }
            Bar(pct: usage?.percent, height: 12, track: tone.track)
        }
    }
}

struct WideRow: View {
    let title: String
    let window: String
    let usage: UsageWindow?
    let now: Date
    var tone = Tone.claude
    var compact = false

    var body: some View {
        if compact {
            HStack(spacing: 12) {
                Text(title).font(.system(size: 15, weight: .semibold, design: .rounded)).foregroundStyle(tone.text)
                    .lineLimit(1).minimumScaleFactor(0.7).frame(width: 150, alignment: .leading)
                Text(formatPct(usage?.percent)).font(.system(size: 26, weight: .bold, design: .rounded))
                    .foregroundStyle(usage.map { Palette.level($0.percent) } ?? tone.dim)
                    .contentTransition(.numericText()).frame(width: 70, alignment: .leading)
                Bar(pct: usage?.percent, height: 10, track: tone.track)
                Text(usage?.resetsAt.map { formatLeft($0.timeIntervalSince(now)) } ?? "--")
                    .font(.system(size: 16, weight: .medium, design: .rounded)).foregroundStyle(tone.text)
                    .monospacedDigit().frame(width: 84, alignment: .trailing)
            }
        } else {
            HStack(spacing: 18) {
                VStack(alignment: .leading, spacing: 0) {
                    HStack(alignment: .lastTextBaseline, spacing: 6) {
                        Text(title).font(.system(size: 18, weight: .semibold, design: .rounded)).foregroundStyle(tone.text)
                        Text(window).font(.caption).foregroundStyle(tone.dim)
                    }
                    Text(formatPct(usage?.percent)).font(.system(size: 48, weight: .bold, design: .rounded))
                        .foregroundStyle(usage.map { Palette.level($0.percent) } ?? tone.dim)
                        .contentTransition(.numericText())
                }
                .frame(width: 180, alignment: .leading)
                Bar(pct: usage?.percent, height: 16, track: tone.track)
                ResetInfo(reset: usage?.resetsAt, now: now, tone: tone, waiting: usage == nil).frame(width: 130, alignment: .trailing)
            }
        }
    }
}

struct RaccoCell: View {
    let name: String
    let pct: Double?
    var own = true
    let feel: Feel
    var model: String? = nil
    var wear: Accessory? = nil
    var seed = 0
    var tone = Tone.claude
    var note: String? = nil
    var size: CGFloat = 96

    var body: some View {
        VStack(spacing: 5) {
            Text(formatPct(pct))
                .font(.system(size: 16, weight: .semibold, design: .rounded))
                .foregroundStyle(pct.map { own ? Palette.level($0) : tone.muted } ?? tone.dim)
                .contentTransition(.numericText())
            Bar(pct: pct, color: own ? nil : tone.muted.opacity(0.55), height: 6, track: tone.track).frame(width: size * 0.75)
            MascotView(model: model, feel: feel, wear: wear, seed: seed, alive: true).frame(width: size)
            Text(name).font(.system(size: 15, weight: .medium, design: .rounded))
                .foregroundStyle(feel.mood == .exhausted ? Palette.bad : tone.text)
                .lineLimit(1).minimumScaleFactor(0.7)
            if let note { Text(note).font(.caption2).foregroundStyle(Palette.bad).lineLimit(1) }
        }
        .frame(maxWidth: .infinity)
    }
}

struct ToolHeader: View {
    let name: String
    let tone: Tone
    let right: String

    var body: some View {
        HStack(spacing: 8) {
            Circle().fill(tone.accent).frame(width: 8, height: 8)
            Text(name).font(.system(size: 18, weight: .semibold, design: .rounded)).foregroundStyle(tone.text)
            Spacer(minLength: 12)
            Text(right).font(.caption).foregroundStyle(tone.dim).lineLimit(1)
        }
    }
}

struct ClockLine: View {
    let now: Date
    var size: CGFloat = 34

    var body: some View {
        HStack(alignment: .lastTextBaseline, spacing: 10) {
            Text(now.formatted(.dateTime.hour(.twoDigits(amPM: .omitted)).minute(.twoDigits).locale(L.locale)))
                .font(.system(size: size, weight: .semibold, design: .rounded)).foregroundStyle(Palette.text)
            Text(now.formatted(.dateTime.weekday(.wide).day().month(.wide).locale(L.locale))).font(.footnote).foregroundStyle(Palette.muted)
            Spacer()
        }
    }
}

private struct Rule: View {
    var color = Palette.line

    var body: some View { Rectangle().fill(color).frame(height: 1) }
}

private func claudeCells(_ u: Snapshot?, size: CGFloat, notes: Bool) -> [RaccoCell] {
    MODELS.enumerated().map { i, m in
        let own = u?.own(m)
        let feel = feelOf(u, m)
        return RaccoCell(name: m, pct: own?.percent ?? u?.sevenDay?.percent, own: own != nil, feel: feel, model: m, seed: i + 11,
                         note: notes ? (feel.mood == .exhausted ? L.exhausted : " ") : nil, size: size)
    }
}

private func agCells(_ s: AgSnap?, tone: Tone, size: CGFloat, notes: Bool) -> [RaccoCell] {
    (s?.shown ?? AgFamily.allCases).enumerated().map { i, f in
        let pct = s?.percent(f)
        let note: String? = notes ? (pct.map { $0 >= 99.5 ? L.exhausted : ($0 >= 85 ? L.agNearLimit : " ") } ?? " ") : nil
        return RaccoCell(name: f.label, pct: pct, feel: agFeel(pct), wear: f.wear, seed: i + 31, tone: tone, note: note, size: size)
    }
}

private struct Grid2: View {
    let cells: [RaccoCell]

    var body: some View {
        VStack(spacing: 14) {
            ForEach(Array(stride(from: 0, to: cells.count, by: 2)), id: \.self) { r in
                HStack(alignment: .bottom, spacing: 12) {
                    ForEach(r..<min(r + 2, cells.count), id: \.self) { cells[$0] }
                }
            }
        }
    }
}

private struct Row4: View {
    let cells: [RaccoCell]

    var body: some View {
        HStack(alignment: .bottom, spacing: 10) {
            ForEach(cells.indices, id: \.self) { cells[$0] }
        }
    }
}

struct MascotsPage: View {
    let u: Snapshot?
    let now: Date
    let landscape: Bool

    var body: some View {
        if landscape {
            VStack(spacing: 10) {
                WideRow(title: L.session, window: L.hours5, usage: u?.fiveHour, now: now)
                WideRow(title: L.week, window: L.days7, usage: u?.sevenDay, now: now)
                Rule()
                Row4(cells: claudeCells(u, size: 78, notes: false))
            }
            .padding(.horizontal, 24).padding(.top, 10).padding(.bottom, 30)
        } else {
            VStack(spacing: 0) {
                ClockLine(now: now)
                Spacer(minLength: 10)
                UsageBlock(title: L.session, window: L.sessionWindow, usage: u?.fiveHour, now: now, big: 58)
                Spacer(minLength: 10)
                UsageBlock(title: L.week, window: L.weekWindow, usage: u?.sevenDay, now: now, big: 58)
                Spacer(minLength: 14)
                Rule()
                Spacer(minLength: 10)
                Grid2(cells: claudeCells(u, size: 92, notes: true))
            }
            .padding(.horizontal, 22).padding(.top, 8).padding(.bottom, 40)
        }
    }
}

struct ClockPage: View {
    let u: Snapshot?
    let now: Date
    let landscape: Bool

    private var digits: [String] {
        let c = Calendar.current.dateComponents([.hour, .minute], from: now)
        return String(format: "%02d%02d", c.hour ?? 0, c.minute ?? 0).map(String.init)
    }

    var body: some View {
        GeometryReader { g in
            let fs = landscape ? min(g.size.width / 3.4, g.size.height / 2.2) : min(g.size.width / 1.6, g.size.height / 4.4)
            VStack(spacing: 10) {
                Spacer(minLength: 0)
                if landscape {
                    HStack(spacing: 0) {
                        ForEach(0..<4, id: \.self) { i in
                            if i == 2 {
                                Text(":").foregroundStyle(Palette.muted.opacity(Int(now.timeIntervalSince1970) % 2 == 0 ? 0.85 : 0.3))
                                    .font(.system(size: fs * 0.8, weight: .bold, design: .rounded))
                            }
                            digit(i, fs)
                        }
                    }
                } else {
                    VStack(spacing: -fs * 0.18) {
                        HStack(spacing: 0) { digit(0, fs); digit(1, fs) }
                        HStack(spacing: 0) { digit(2, fs); digit(3, fs) }
                    }
                }
                Text(now.formatted(.dateTime.weekday(.wide).day().month(.wide).locale(L.locale))).font(.title3).foregroundStyle(Palette.muted)
                HStack(spacing: 18) {
                    mini("5h", u?.fiveHour?.percent)
                    mini("7d", u?.sevenDay?.percent)
                }
                .padding(.top, 6)
                HStack(alignment: .bottom, spacing: 14) {
                    ForEach(Array(MODELS.enumerated()), id: \.offset) { i, m in
                        MascotView(model: m, feel: feelOf(u, m), seed: i + 21, alive: true).frame(width: 40)
                    }
                }
                .padding(.top, 4)
                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, 20).padding(.bottom, 30)
    }

    private func digit(_ i: Int, _ fs: CGFloat) -> some View {
        Text(digits[i])
            .font(.system(size: fs, weight: .bold, design: .rounded))
            .foregroundStyle(CLOCK_DIGITS[i])
            .contentTransition(.numericText())
            .animation(.snappy, value: digits[i])
    }

    private func mini(_ label: String, _ p: Double?) -> some View {
        HStack(spacing: 8) {
            Text(label).font(.subheadline).foregroundStyle(Palette.muted)
            Bar(pct: p, height: 8).frame(width: 90)
            Text(formatPct(p)).font(.subheadline.weight(.semibold)).foregroundStyle(p.map(Palette.level) ?? Palette.dim)
        }
    }
}

struct ChartPage: View {
    let samples: [Sample]
    let u: Snapshot?
    let now: Date
    let landscape: Bool

    private struct Point: Identifiable {
        let id: String
        let t: Date
        let v: Double
        let series: String
    }

    private func points(_ pick: (Sample) -> Double?, _ name: String) -> [Point] {
        var out: [Point] = []
        var segment = 0
        var prev: Date?
        for (i, s) in samples.enumerated() {
            guard let v = pick(s) else { segment += 1; prev = nil; continue }
            if let prev, s.t.timeIntervalSince(prev) > History.slot * 1.5 { segment += 1 }
            out.append(Point(id: "\(name)\(i)", t: s.t, v: v, series: "\(name)\(segment)"))
            prev = s.t
        }
        return out
    }

    private func lonely(_ list: [Point]) -> [Point] {
        let counts = Dictionary(grouping: list, by: \.series).mapValues(\.count)
        return list.filter { counts[$0.series] == 1 || $0.id == list.last?.id }
    }

    var body: some View {
        let start = now.addingTimeInterval(-History.window)
        let week = points({ $0.p7 }, "w")
        let session = points({ $0.p5 }, "s")
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(L.last7Days).font(.system(size: 24, weight: .semibold, design: .rounded)).foregroundStyle(Palette.text)
                Spacer()
                legend(Palette.clawd, L.legendSession)
                legend(LAV, L.legendWeek)
            }
            Chart {
                ForEach(week) { p in
                    LineMark(x: .value("t", p.t), y: .value("%", p.v), series: .value("s", p.series))
                        .foregroundStyle(LAV).lineStyle(StrokeStyle(lineWidth: 2.6, lineCap: .round, lineJoin: .round))
                }
                ForEach(session) { p in
                    LineMark(x: .value("t", p.t), y: .value("%", p.v), series: .value("s", p.series))
                        .foregroundStyle(Palette.clawd).lineStyle(StrokeStyle(lineWidth: 2.6, lineCap: .round, lineJoin: .round))
                }
                ForEach(lonely(week) + lonely(session)) { p in
                    PointMark(x: .value("t", p.t), y: .value("%", p.v))
                        .foregroundStyle(p.series.hasPrefix("w") ? LAV : Palette.clawd).symbolSize(40)
                }
            }
            .chartXScale(domain: start...now)
            .chartYScale(domain: 0...100)
            .chartYAxis {
                AxisMarks(position: .leading, values: [0, 25, 50, 75, 100]) { v in
                    AxisGridLine().foregroundStyle(Palette.line)
                    AxisValueLabel { Text("\(v.as(Int.self) ?? 0)%").foregroundStyle(Palette.dim) }
                }
            }
            .chartXAxis {
                AxisMarks(values: .stride(by: .day)) { _ in
                    AxisGridLine().foregroundStyle(Palette.line.opacity(0.55))
                    AxisValueLabel(format: .dateTime.weekday(.abbreviated).day().locale(L.locale)).foregroundStyle(Palette.dim)
                }
            }
            let stats = Group {
                Text(L.peak5h(formatPct(samples.compactMap(\.p5).max()))).foregroundStyle(Palette.muted)
                Text(L.weekNow(formatPct(u?.sevenDay?.percent))).foregroundStyle(Palette.muted)
                Text(samples.isEmpty ? L.noSamples : L.samples(samples.count)).foregroundStyle(Palette.dim)
            }
            .font(.subheadline)
            if landscape { HStack(spacing: 24) { stats } } else { VStack(alignment: .leading, spacing: 2) { stats } }
        }
        .padding(.horizontal, 22).padding(.top, 10).padding(.bottom, 36)
    }

    private func legend(_ c: Color, _ text: String) -> some View {
        HStack(spacing: 6) {
            Capsule().fill(c).frame(width: 16, height: 4)
            Text(text).font(.caption).foregroundStyle(Palette.muted)
        }
        .padding(.leading, 10)
    }
}

private struct AgRow: Identifiable {
    let id: String
    let title: String
    let window: String
    let usage: UsageWindow
}

private func agRows(_ s: AgSnap) -> [AgRow] {
    s.groups.flatMap { g -> [AgRow] in
        var out: [AgRow] = []
        if let w = g.session { out.append(AgRow(id: "\(g.pool)s", title: "\(L.session) (\(L.label(g.pool)))", window: L.hours5, usage: w)) }
        if let w = g.week { out.append(AgRow(id: "\(g.pool)w", title: "\(L.week) (\(L.label(g.pool)))", window: L.days7, usage: w)) }
        return out
    }
}

private func agStatus(_ s: AgSnap?, now: Date) -> String {
    guard let s else { return L.toolNoData }
    let ago = formatAgo(now.timeIntervalSince(s.fetchedAt))
    if !s.open { return "\(L.agClosed) · \(ago)" }
    return [s.plan.map(L.agPlan), ago].compactMap { $0 }.joined(separator: " · ")
}

struct AgPage: View {
    let s: AgSnap?
    let tone: Tone
    let now: Date
    let landscape: Bool

    var body: some View {
        if let s {
            let rows = agRows(s)
            if landscape {
                VStack(spacing: 8) {
                    ToolHeader(name: L.toolAntigravity, tone: tone, right: agStatus(s, now: now))
                    ForEach(rows) { r in
                        WideRow(title: r.title, window: r.window, usage: r.usage, now: now, tone: tone, compact: rows.count > 2)
                    }
                    Rule(color: tone.line)
                    Row4(cells: agCells(s, tone: tone, size: rows.count > 2 ? 64 : 78, notes: false))
                }
                .padding(.horizontal, 24).padding(.top, 10).padding(.bottom, 30)
            } else {
                VStack(spacing: 0) {
                    ToolHeader(name: L.toolAntigravity, tone: tone, right: agStatus(s, now: now))
                    Spacer(minLength: 8)
                    VStack(spacing: rows.count > 2 ? 12 : 22) {
                        ForEach(rows) { r in
                            UsageBlock(title: r.title, window: r.window, usage: r.usage, now: now, tone: tone, big: rows.count > 2 ? 38 : 56)
                        }
                    }
                    Spacer(minLength: 12)
                    Rule(color: tone.line)
                    Spacer(minLength: 10)
                    Grid2(cells: agCells(s, tone: tone, size: rows.count > 2 ? 70 : 90, notes: true))
                }
                .padding(.horizontal, 22).padding(.top, 10).padding(.bottom, 40)
            }
        } else {
            VStack(spacing: 14) {
                MascotView(feel: agFeel(nil), reserveTop: false, wear: .star, alive: true).frame(width: 120)
                Text(L.toolAntigravity).font(.system(size: 24, weight: .semibold, design: .rounded)).foregroundStyle(tone.text)
                Text(L.agWaitingPhone).font(.callout).foregroundStyle(tone.muted).multilineTextAlignment(.center)
            }
            .padding(32)
        }
    }
}

struct BothPage: View {
    let u: Snapshot?
    let tone: Tone
    let now: Date
    let landscape: Bool

    var body: some View {
        let s = u?.ag
        let claudeAge = u.flatMap { $0.hasClaude ? formatAgo(now.timeIntervalSince($0.fetchedAt)) : nil } ?? L.toolNoData
        let claudeRows: [(String, UsageWindow?)] = [(L.session, u?.fiveHour), (L.week, u?.sevenDay)]
        let agLines: [(String, UsageWindow?)] = s.map { agRows($0).map { ($0.title, Optional($0.usage)) } } ?? [(L.session, nil), (L.week, nil)]
        let left = Half(name: L.toolClaude, tone: .claude, age: claudeAge, rows: claudeRows, cells: claudeCells(u, size: 50, notes: false), now: now)
        let right = Half(name: L.toolAntigravity, tone: tone, age: agStatus(s, now: now), rows: agLines, cells: agCells(s, tone: tone, size: 50, notes: false), now: now)
        if landscape {
            HStack(spacing: 0) { left; right }
        } else {
            VStack(spacing: 0) { left; right }
        }
    }
}

private struct Half: View {
    let name: String
    let tone: Tone
    let age: String
    let rows: [(String, UsageWindow?)]
    let cells: [RaccoCell]
    let now: Date

    var body: some View {
        let many = rows.count > 2
        VStack(alignment: .leading, spacing: many ? 6 : 10) {
            ToolHeader(name: name, tone: tone, right: age)
            ForEach(rows.indices, id: \.self) { i in
                let (label, w) = rows[i]
                VStack(spacing: 3) {
                    HStack(alignment: .lastTextBaseline, spacing: 8) {
                        Text(label).font(.system(size: many ? 13 : 15, weight: .semibold, design: .rounded)).foregroundStyle(tone.text).lineLimit(1)
                        Text(formatPct(w?.percent)).font(.system(size: many ? 20 : 26, weight: .bold, design: .rounded))
                            .foregroundStyle(w.map { Palette.level($0.percent) } ?? tone.dim).contentTransition(.numericText())
                        Spacer(minLength: 4)
                        Text(w?.resetsAt.map { "\(L.resetsIn) \(formatLeft($0.timeIntervalSince(now)))" } ?? " ")
                            .font(.caption2).foregroundStyle(tone.muted).lineLimit(1)
                    }
                    Bar(pct: w?.percent, height: many ? 6 : 9, track: tone.track)
                }
            }
            Spacer(minLength: 4)
            Row4(cells: cells)
        }
        .padding(.horizontal, 20).padding(.top, 14).padding(.bottom, 30)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(tone.bg)
    }
}

struct ToolPicker: View {
    var done: () -> Void
    @State private var claude = Vault.prefs.showClaude
    @State private var ag = Vault.prefs.showAg

    var body: some View {
        GeometryReader { g in
            ScrollView {
                VStack(spacing: 20) {
                    VStack(spacing: 6) {
                        Text(L.pickTitle).font(.system(size: 26, weight: .semibold, design: .rounded)).foregroundStyle(Palette.text)
                        Text(L.pickSubtitle).font(.subheadline).foregroundStyle(Palette.muted)
                    }
                    .multilineTextAlignment(.center)
                    let cards = Group {
                        card(L.toolClaude, L.pickClaudeHint, .claude, $claude) { MascotView(model: "Fable", alive: true) }
                        card(L.toolAntigravity, L.pickAgHint, Tone.ag(Vault.prefs.toolTheme), $ag) { MascotView(wear: .star, alive: true) }
                    }
                    if g.size.width > 560 { HStack(spacing: 20) { cards } } else { VStack(spacing: 14) { cards } }
                    Button {
                        var p = Vault.prefs
                        p.showClaude = claude
                        p.showAg = ag
                        p.toolsChosen = true
                        Vault.prefs = p
                        done()
                    } label: {
                        Text(L.pickContinue).font(.system(size: 17, weight: .semibold, design: .rounded))
                            .padding(.horizontal, 28).padding(.vertical, 12)
                            .background(claude || ag ? Palette.clawd : Palette.line, in: Capsule())
                            .foregroundStyle(Palette.bg)
                    }
                    .disabled(!(claude || ag))
                }
                .padding(24)
                .frame(maxWidth: 760)
                .frame(minHeight: g.size.height)
                .frame(maxWidth: .infinity)
            }
        }
    }

    private func card(_ name: String, _ hint: String, _ tone: Tone, _ on: Binding<Bool>, @ViewBuilder mascot: () -> some View) -> some View {
        Button { on.wrappedValue.toggle() } label: {
            VStack(spacing: 10) {
                HStack(spacing: 10) {
                    Image(systemName: on.wrappedValue ? "checkmark.square.fill" : "square").font(.title3).foregroundStyle(on.wrappedValue ? tone.accent : tone.muted)
                    Text(name).font(.system(size: 20, weight: .semibold, design: .rounded)).foregroundStyle(tone.text)
                    Spacer()
                }
                mascot().frame(width: 90)
                Text(hint).font(.footnote).foregroundStyle(tone.muted).multilineTextAlignment(.center)
            }
            .padding(18)
            .frame(maxWidth: .infinity)
            .background(tone.bg, in: RoundedRectangle(cornerRadius: 22))
            .overlay(RoundedRectangle(cornerRadius: 22).stroke(on.wrappedValue ? tone.accent : tone.line, lineWidth: 2))
        }
        .buttonStyle(.plain)
    }
}

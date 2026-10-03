import SwiftUI

struct DashboardView: View {
    @ObservedObject var model: DashboardModel
    @State private var settings = false
    @State private var page: Page?
    @State private var samples = History.samples

    private var prefs: Prefs { Vault.prefs }

    private func pages(_ u: Snapshot?) -> [Page] {
        let p = prefs
        let agOn = p.showAg && (u?.ag != nil || !p.showClaude)
        return Page.allCases.filter {
            switch $0 {
            case .ag: return agOn
            case .both: return agOn && p.showClaude
            default: return p.showClaude
            }
        }
    }

    private var home: Page {
        guard prefs.showClaude else { return .ag }
        switch prefs.home {
        case .dash: return .dash
        case .mascots: return .mascots
        case .clock: return .clock
        }
    }

    private var carouselKey: String { "\(prefs.carousel)-\(prefs.dwell)-\(page?.rawValue ?? "")" }

    var body: some View {
        GeometryReader { geo in
            let landscape = geo.size.width > geo.size.height
            ZStack {
                if model.paired {
                    if (!prefs.toolsChosen && !Vault.demo) || ProcessInfo.processInfo.arguments.contains("--picker") {
                        Palette.bg.ignoresSafeArea()
                        ToolPicker { model.objectWillChange.send() }
                    } else {
                        TimelineView(.periodic(from: .now, by: 1)) { ctx in
                            pager(now: ctx.date, landscape: landscape)
                        }
                    }
                } else {
                    Palette.bg.ignoresSafeArea()
                    PairingView()
                }
            }
        }
        .task {
            while !Task.isCancelled {
                await model.refresh()
                samples = History.samples
                try? await Task.sleep(for: .seconds(prefs.showAg ? 30 : 60))
            }
        }
        .task(id: carouselKey) {
            guard prefs.carousel else { return }
            try? await Task.sleep(for: .seconds(prefs.dwell))
            guard !Task.isCancelled else { return }
            let list = pages(model.snapshot)
            guard !list.isEmpty else { return }
            let current = page.flatMap(list.firstIndex(of:)) ?? 0
            withAnimation(.easeInOut(duration: 0.6)) { page = list[(current + 1) % list.count] }
        }
        .sheet(isPresented: $settings, onDismiss: { UIApplication.shared.isIdleTimerDisabled = prefs.keepAwake }) { SettingsView(model: model) }
        .onAppear {
            UIApplication.shared.isIdleTimerDisabled = prefs.keepAwake
            if ProcessInfo.processInfo.arguments.contains("--settings") { settings = true }
            if ProcessInfo.processInfo.arguments.contains("--landscape"), let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
                scene.requestGeometryUpdate(.iOS(interfaceOrientations: .landscapeRight))
            }
            if let arg = ProcessInfo.processInfo.arguments.first(where: { $0.hasPrefix("--page=") }) { page = Page(rawValue: String(arg.dropFirst(7))) }
        }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .task {
            while !Task.isCancelled {
                await model.feeds()
                try? await Task.sleep(for: .seconds(300))
            }
        }
    }

    private func pager(now: Date, landscape: Bool) -> some View {
        let u = model.snapshot?.settled(now)
        let list = pages(u)
        let current = page.flatMap { list.contains($0) ? $0 : nil } ?? (list.contains(home) ? home : list.first ?? .dash)
        let agTone = Tone.ag(prefs.toolTheme)
        let tone = current == .ag ? agTone : Tone.claude
        return ZStack(alignment: .bottom) {
            tone.bg.ignoresSafeArea()
            RadialGradient(colors: [tone.glow, .clear], center: UnitPoint(x: 0.5, y: 1.05), startRadius: 0, endRadius: 520).ignoresSafeArea()
            TabView(selection: Binding(get: { current }, set: { page = $0 })) {
                ForEach(list) { p in
                    view(p, u: u, now: now, landscape: landscape, agTone: agTone).tag(p)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            ZStack {
                dots(list, current: current)
                HStack {
                    Spacer()
                    Button { settings = true } label: {
                        Image(systemName: "gearshape.fill").font(.system(size: 18)).foregroundStyle(tone.dim.opacity(0.8)).frame(width: 40, height: 40)
                    }
                }
                .padding(.trailing, 6)
            }
            .frame(height: 30)
        }
        .animation(.easeInOut(duration: 0.35), value: current)
    }

    @ViewBuilder
    private func view(_ p: Page, u: Snapshot?, now: Date, landscape: Bool, agTone: Tone) -> some View {
        switch p {
        case .dash: dash(u: u, now: now, landscape: landscape)
        case .mascots: MascotsPage(u: u, now: now, landscape: landscape)
        case .chart: ChartPage(samples: samples, u: u, now: now, landscape: landscape)
        case .clock: ClockPage(u: u, now: now, landscape: landscape)
        case .ag: AgPage(s: u?.ag, tone: agTone, now: now, landscape: landscape)
        case .both: BothPage(u: u, tone: agTone, now: now, landscape: landscape)
        }
    }

    private func dots(_ list: [Page], current: Page) -> some View {
        HStack(spacing: 4) {
            ForEach(list) { p in
                let on = p == current
                let color = p.isAg ? Tone.agAccent : Palette.text
                Circle()
                    .fill(on ? color : color.opacity(p.isAg ? 0.35 : 0.25))
                    .frame(width: on ? 8 : 6, height: on ? 8 : 6)
                    .frame(width: 18, height: 30)
                    .contentShape(Rectangle())
                    .onTapGesture { withAnimation(.easeInOut(duration: 0.35)) { page = p } }
            }
        }
    }

    private func dash(u: Snapshot?, now: Date, landscape: Bool) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                header(now: now)
                if let problem = model.problem, problem != .noData {
                    Text(problem == .badKey ? L.badKey : L.unreachable)
                        .font(.footnote)
                        .foregroundStyle(Palette.warn)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                let cards = Group {
                    UsageCard(title: L.session, window: L.sessionWindow, usage: u?.fiveHour, scoped: [], now: now)
                    UsageCard(title: L.week, window: L.weekWindow, usage: u?.sevenDay, scoped: u?.scoped ?? [], now: now)
                    ModelsCard(snapshot: u)
                    if prefs.showAg, let ag = u?.ag { AgCard(s: ag, now: now) }
                    StatusCard(incidents: model.incidents)
                    NewsCard(news: model.news)
                }
                if landscape {
                    LazyVGrid(columns: [GridItem(.flexible(), spacing: 16, alignment: .top), GridItem(.flexible(), spacing: 16, alignment: .top)], spacing: 16) { cards }
                } else {
                    cards
                }
                footer(u: u, now: now)
            }
            .padding(.horizontal, 18).padding(.top, 8).padding(.bottom, 40)
        }
        .refreshable { await model.refresh() }
    }

    private func header(now: Date) -> some View {
        HStack(alignment: .center, spacing: 12) {
            MascotView(reserveTop: false, alive: true).frame(width: 52)
            VStack(alignment: .leading, spacing: 2) {
                Text("Banditboard").font(.system(size: 26, weight: .semibold, design: .rounded)).foregroundStyle(Palette.text)
                Text(L.subtitle).font(.footnote).foregroundStyle(Palette.muted)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(now.formatted(.dateTime.hour(.twoDigits(amPM: .omitted)).minute(.twoDigits).locale(L.locale)))
                    .font(.system(size: 28, weight: .semibold, design: .rounded)).foregroundStyle(Palette.text)
                Text(now.formatted(.dateTime.weekday(.wide).day().month(.wide).locale(L.locale))).font(.caption2).foregroundStyle(Palette.muted)
            }
        }
    }

    private func footer(u: Snapshot?, now: Date) -> some View {
        VStack(spacing: 10) {
            if let u, u.hasClaude {
                Text(L.updatedAgo(formatAgo(now.timeIntervalSince(u.fetchedAt)))).font(.footnote).foregroundStyle(Palette.ok)
            } else if prefs.showClaude {
                Text(L.waiting).font(.footnote).foregroundStyle(Palette.muted)
            }
            HStack(spacing: 24) {
                Button(model.busy ? "…" : L.refresh) { Task { await model.refresh() } }
                Button(L.settings) { settings = true }
            }
            .font(.footnote.weight(.semibold))
            .foregroundStyle(Palette.clawd)
        }
        .padding(.top, 4)
    }
}

struct AgCard: View {
    let s: AgSnap
    let now: Date

    var body: some View {
        let accent = Vault.prefs.toolTheme == .claude ? Palette.clawd : Tone.agAccent
        Panel {
            HStack(alignment: .lastTextBaseline, spacing: 8) {
                Text(L.toolAntigravity).font(.system(size: 18, weight: .semibold, design: .rounded)).foregroundStyle(accent)
                Text(s.plan.map(L.agPlan) ?? "").font(.caption).foregroundStyle(Palette.dim)
            }
            ForEach(s.groups, id: \.pool) { g in
                VStack(alignment: .leading, spacing: 4) {
                    Text(L.label(g.pool)).font(.subheadline.weight(.semibold)).foregroundStyle(Palette.text)
                    line(L.session, g.session)
                    line(L.week, g.week)
                }
            }
            HStack(alignment: .bottom, spacing: 12) {
                ForEach(Array(s.shown.enumerated()), id: \.offset) { i, f in
                    VStack(spacing: 4) {
                        MascotView(feel: agFeel(s.percent(f)), wear: f.wear, seed: i + 61, alive: true)
                        Text(f.short).font(.caption.weight(.semibold)).foregroundStyle(Palette.text)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            Text(L.agModelsHint).font(.caption2).foregroundStyle(Palette.dim)
            Text(L.agUpdatedAgo(formatAgo(now.timeIntervalSince(s.fetchedAt)))).font(.caption2).foregroundStyle(s.open ? Palette.ok : Palette.muted)
        }
    }

    @ViewBuilder
    private func line(_ name: String, _ w: UsageWindow?) -> some View {
        if let w {
            HStack(spacing: 10) {
                Text(name).font(.caption).foregroundStyle(Palette.muted).frame(width: 64, alignment: .leading)
                Bar(pct: w.percent, height: 7)
                Text(formatPct(w.percent)).font(.caption.weight(.semibold)).foregroundStyle(Palette.level(w.percent)).frame(width: 40, alignment: .trailing)
            }
        }
    }
}

struct Bar: View {
    let pct: Double?
    var color: Color? = nil
    var height: CGFloat = 8
    var track: Color = Palette.track

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(track)
                if let pct {
                    Capsule().fill(color ?? Palette.level(pct)).frame(width: max(height, geo.size.width * min(pct, 100) / 100))
                }
            }
        }
        .frame(height: height)
        .animation(.easeOut(duration: 0.9), value: pct)
    }
}

struct Panel<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) { content }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Palette.card, in: RoundedRectangle(cornerRadius: 18))
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(Palette.line, lineWidth: 1))
    }
}

struct UsageCard: View {
    let title: String
    let window: String
    let usage: UsageWindow?
    let scoped: [ScopedLimit]
    let now: Date

    var body: some View {
        Panel {
            HStack(alignment: .lastTextBaseline, spacing: 8) {
                Text(title).font(.system(size: 20, weight: .semibold, design: .rounded)).foregroundStyle(Palette.text)
                Text(window).font(.caption).foregroundStyle(Palette.dim)
            }
            HStack(alignment: .center) {
                Text(formatPct(usage?.percent))
                    .font(.system(size: 56, weight: .bold, design: .rounded))
                    .foregroundStyle(usage.map { Palette.level($0.percent) } ?? Palette.dim)
                Spacer()
                if let reset = usage?.resetsAt {
                    VStack(alignment: .trailing, spacing: 2) {
                        Text(L.resetsIn).font(.caption).foregroundStyle(Palette.muted)
                        Text(formatLeft(reset.timeIntervalSince(now))).font(.system(size: 24, weight: .semibold, design: .rounded)).foregroundStyle(Palette.text)
                        Text(formatAt(reset, now: now)).font(.caption).foregroundStyle(Palette.muted)
                    }
                }
            }
            Bar(pct: usage?.percent, height: 12)
            ForEach(scoped, id: \.label) { s in
                Text("● \(s.label) \(formatPct(s.percent))").font(.footnote).foregroundStyle(Palette.level(s.percent))
            }
        }
    }
}

struct ModelsCard: View {
    let snapshot: Snapshot?

    var body: some View {
        Panel {
            Text(L.models).font(.system(size: 18, weight: .semibold, design: .rounded)).foregroundStyle(Palette.clawd)
            HStack(alignment: .bottom, spacing: 12) {
                ForEach(MODELS, id: \.self) { m in
                    let own = snapshot?.own(m)
                    let pct = own?.percent ?? snapshot?.sevenDay?.percent
                    let color = (own != nil && pct != nil) ? Palette.level(pct!) : Palette.dim
                    VStack(spacing: 6) {
                        Text(formatPct(pct)).font(.system(size: 16, weight: .semibold, design: .rounded)).foregroundStyle(own != nil ? color : Palette.muted)
                        Bar(pct: pct, color: color, height: 5)
                        MascotView(model: m, feel: feelOf(snapshot, m), seed: MODELS.firstIndex(of: m) ?? 0, alive: true)
                        Text(m).font(.system(size: 14, weight: .semibold, design: .rounded)).foregroundStyle(Palette.text)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            Text(L.modelsHint).font(.caption2).foregroundStyle(Palette.dim)
        }
    }
}

private struct CardTitle: View {
    let title: String
    let sub: String

    var body: some View {
        HStack(alignment: .lastTextBaseline, spacing: 8) {
            Text(title).font(.system(size: 18, weight: .semibold, design: .rounded)).foregroundStyle(Palette.clawd)
            Text(sub).font(.caption).foregroundStyle(Palette.dim)
        }
    }
}

struct StatusCard: View {
    let incidents: [Incident]?

    var body: some View {
        Panel {
            CardTitle(title: "Status", sub: "status.claude.com")
            if let incidents {
                if incidents.isEmpty {
                    Text("● \(L.allOperational)").font(.subheadline).foregroundStyle(Palette.ok)
                } else {
                    ForEach(incidents.prefix(4)) { i in
                        if let link = i.link {
                            Link(destination: link) { Text("● \(i.name)").font(.subheadline).foregroundStyle(Palette.warn).multilineTextAlignment(.leading) }
                        } else {
                            Text("● \(i.name)").font(.subheadline).foregroundStyle(Palette.warn)
                        }
                    }
                }
            } else {
                Text(L.checkingStatus).font(.subheadline).foregroundStyle(Palette.muted)
            }
        }
    }
}

struct NewsCard: View {
    let news: [NewsItem]?

    var body: some View {
        Panel {
            CardTitle(title: L.news, sub: "anthropic.com/news")
            if let news {
                ForEach(news.prefix(4)) { n in
                    let row = HStack(alignment: .top, spacing: 10) {
                        Text(n.title).font(.subheadline).foregroundStyle(Palette.text).multilineTextAlignment(.leading)
                        Spacer(minLength: 0)
                        if let d = n.date {
                            Text(d.formatted(.dateTime.day().month(.abbreviated).locale(L.locale))).font(.caption2).foregroundStyle(Palette.dim)
                        }
                    }
                    if let link = n.link { Link(destination: link) { row } } else { row }
                }
            } else {
                Text(L.loadingNews).font(.subheadline).foregroundStyle(Palette.muted)
            }
        }
    }
}

struct PairingView: View {
    var body: some View {
        VStack(spacing: 18) {
            MascotView(model: "Fable", alive: true).frame(width: 150)
            Text(L.pairTitle).font(.system(size: 24, weight: .semibold, design: .rounded)).foregroundStyle(Palette.text)
            Text(L.pairBody).font(.callout).foregroundStyle(Palette.muted).multilineTextAlignment(.center).padding(.horizontal, 28)
        }
    }
}

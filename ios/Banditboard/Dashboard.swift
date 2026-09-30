import SwiftUI

struct DashboardView: View {
    @ObservedObject var model: DashboardModel

    var body: some View {
        ZStack {
            Palette.bg.ignoresSafeArea()
            if model.paired {
                TimelineView(.periodic(from: .now, by: 1)) { ctx in
                    content(now: ctx.date)
                }
            } else {
                PairingView()
            }
        }
        .task {
            while !Task.isCancelled {
                await model.refresh()
                try? await Task.sleep(for: .seconds(60))
            }
        }
        .task {
            while !Task.isCancelled {
                await model.feeds()
                try? await Task.sleep(for: .seconds(300))
            }
        }
    }

    private func content(now: Date) -> some View {
        let u = model.snapshot?.settled(now)
        return ScrollView {
            VStack(spacing: 16) {
                header(now: now)
                if let problem = model.problem, problem != .noData {
                    Text(problem == .badKey ? L.badKey : L.unreachable)
                        .font(.footnote)
                        .foregroundStyle(Palette.warn)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                UsageCard(title: L.session, window: L.sessionWindow, usage: u?.fiveHour, scoped: [], now: now)
                UsageCard(title: L.week, window: L.weekWindow, usage: u?.sevenDay, scoped: u?.scoped ?? [], now: now)
                ModelsCard(snapshot: u)
                StatusCard(incidents: model.incidents)
                NewsCard(news: model.news)
                footer(u: u, now: now)
            }
            .padding(18)
        }
        .refreshable { await model.refresh() }
    }

    private func header(now: Date) -> some View {
        HStack(alignment: .center, spacing: 12) {
            MascotView(reserveTop: false).frame(width: 52)
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
            if let u {
                Text(L.updatedAgo(formatAgo(now.timeIntervalSince(u.fetchedAt)))).font(.footnote).foregroundStyle(Palette.ok)
            } else {
                Text(L.waiting).font(.footnote).foregroundStyle(Palette.muted)
            }
            HStack(spacing: 24) {
                Button(model.busy ? "…" : L.refresh) { Task { await model.refresh() } }
                Button(L.repair) { model.unpair() }
            }
            .font(.footnote.weight(.semibold))
            .foregroundStyle(Palette.clawd)
        }
        .padding(.top, 4)
    }
}

struct Bar: View {
    let pct: Double?
    var color: Color? = nil
    var height: CGFloat = 8

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Palette.track)
                if let pct {
                    Capsule().fill(color ?? Palette.level(pct)).frame(width: max(height, geo.size.width * min(pct, 100) / 100))
                }
            }
        }
        .frame(height: height)
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
                        MascotView(model: m, feel: feelOf(snapshot, m))
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
            MascotView(model: "Fable").frame(width: 150)
            Text(L.pairTitle).font(.system(size: 24, weight: .semibold, design: .rounded)).foregroundStyle(Palette.text)
            Text(L.pairBody).font(.callout).foregroundStyle(Palette.muted).multilineTextAlignment(.center).padding(.horizontal, 28)
        }
    }
}

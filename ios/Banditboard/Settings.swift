import SwiftUI
import WidgetKit

struct SettingsView: View {
    @ObservedObject var model: DashboardModel
    @Environment(\.dismiss) private var dismiss
    @State private var prefs = Vault.prefs

    private var version: String { Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "" }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    HStack(alignment: .bottom, spacing: 12) {
                        ForEach(MODELS, id: \.self) { m in
                            VStack(spacing: 4) {
                                MascotView(model: m, look: prefs, seed: MODELS.firstIndex(of: m) ?? 0, alive: true)
                                Text(m).font(.caption).foregroundStyle(Palette.muted)
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                    choice(L.mascot, Species.allCases, prefs.species, { L.label($0) }) { prefs.species = $0 }
                    choice(L.accessories, Skin.allCases, prefs.skin, { L.label($0) }) { prefs.skin = $0 }
                    choice(L.color, Tint.allCases, prefs.tint, { L.label($0) }) { prefs.tint = $0 }
                    choice(L.language, Language.allCases, prefs.language, { L.label($0) }) { prefs.language = $0 }
                    Toggle(L.alertsToggle, isOn: $prefs.alerts)
                        .tint(Palette.clawd)
                        .foregroundStyle(Palette.text)
                    Panel {
                        Text(L.tools).font(.system(size: 18, weight: .semibold, design: .rounded)).foregroundStyle(Palette.clawd)
                        Toggle(L.toolClaude, isOn: Binding(get: { prefs.showClaude }, set: { prefs.showClaude = $0 || !prefs.showAg }))
                            .tint(Palette.clawd)
                            .foregroundStyle(Palette.text)
                        Toggle(L.toolAntigravity, isOn: Binding(get: { prefs.showAg }, set: { prefs.showAg = $0 || !prefs.showClaude }))
                            .tint(Tone.agAccent)
                            .foregroundStyle(Palette.text)
                        if prefs.showAg {
                            if let ag = model.snapshot?.ag {
                                let ago = formatAgo(Date.now.timeIntervalSince(ag.fetchedAt))
                                Text(ag.open ? "● \(L.agReceiving) · \(ago)" : "● \(L.agClosed) · \(ago)")
                                    .font(.footnote).foregroundStyle(ag.open ? Palette.ok : Palette.muted)
                            } else {
                                Text(L.agNoDataYet).font(.footnote).foregroundStyle(Palette.warn)
                            }
                            choice(L.theme, ToolTheme.allCases, prefs.toolTheme, { L.label($0) }) { prefs.toolTheme = $0 }
                        }
                    }
                    Panel {
                        Text(L.screen).font(.system(size: 18, weight: .semibold, design: .rounded)).foregroundStyle(Palette.clawd)
                        if prefs.showClaude {
                            choice(L.homePage, Home.allCases, prefs.home, { L.label($0) }) { prefs.home = $0 }
                        }
                        Toggle(L.carousel, isOn: $prefs.carousel).tint(Palette.clawd).foregroundStyle(Palette.text)
                        if prefs.carousel {
                            choice(L.dwell, Prefs.DWELL, prefs.dwell, { L.seconds($0) }) { prefs.dwell = $0 }
                        }
                        Toggle(L.keepAwake, isOn: $prefs.keepAwake).tint(Palette.clawd).foregroundStyle(Palette.text)
                        Toggle(L.animations, isOn: $prefs.animations).tint(Palette.clawd).foregroundStyle(Palette.text)
                        Text(L.rotateHint).font(.caption).foregroundStyle(Palette.dim)
                    }
                    Panel {
                        Text(L.pairing).font(.system(size: 18, weight: .semibold, design: .rounded)).foregroundStyle(Palette.clawd)
                        Text(Vault.pairing?.seal != nil || Vault.demo ? L.pairedBox : L.pairedLan).font(.subheadline).foregroundStyle(Palette.muted)
                        Button(L.repair) {
                            model.unpair()
                            dismiss()
                        }
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Palette.clawd)
                    }
                    Panel {
                        Text(L.credits).font(.system(size: 18, weight: .semibold, design: .rounded)).foregroundStyle(Palette.clawd)
                        Text("Banditboard \(version)").font(.system(size: 16, weight: .semibold, design: .rounded)).foregroundStyle(Palette.text)
                        Text(L.createdBy).font(.subheadline.weight(.semibold)).foregroundStyle(Palette.clawd)
                        Text(L.mascotRacco).font(.footnote).foregroundStyle(Palette.muted)
                        Text(L.promoBody).font(.footnote).foregroundStyle(Palette.muted)
                        Text(L.fanProject).font(.caption).foregroundStyle(Palette.dim)
                    }
                }
                .padding(18)
            }
            .background(Palette.bg)
            .navigationTitle(L.settings)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L.close) { dismiss() }.foregroundStyle(Palette.clawd)
                }
            }
        }
        .preferredColorScheme(.dark)
        .environment(\.locale, L.locale)
        .onChange(of: prefs) { _, new in
            Vault.prefs = new
            UIApplication.shared.isIdleTimerDisabled = new.keepAwake
            WidgetCenter.shared.reloadAllTimelines()
            WatchSync.shared.send()
            model.objectWillChange.send()
        }
    }

    private func choice<T: Hashable>(_ title: String, _ options: [T], _ selected: T, _ label: @escaping (T) -> String, _ pick: @escaping (T) -> Void) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.subheadline).foregroundStyle(Palette.muted)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(options, id: \.self) { option in
                        Button { pick(option) } label: {
                            Text(label(option))
                                .font(.subheadline)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .foregroundStyle(option == selected ? Palette.bg : Palette.muted)
                                .background(option == selected ? Palette.text : Palette.card, in: Capsule())
                        }
                    }
                }
            }
        }
    }
}

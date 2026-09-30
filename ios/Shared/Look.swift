import SwiftUI

extension Color {
    init(hex: UInt32) {
        self.init(red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255)
    }

    func mixed(with other: Color, _ t: Double) -> Color {
        let a = UIColor(self).rgba, b = UIColor(other).rgba
        return Color(red: a.0 + (b.0 - a.0) * t, green: a.1 + (b.1 - a.1) * t, blue: a.2 + (b.2 - a.2) * t)
    }
}

private extension UIColor {
    var rgba: (Double, Double, Double) {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        getRed(&r, green: &g, blue: &b, alpha: &a)
        return (Double(r), Double(g), Double(b))
    }
}

enum Palette {
    static let bg = Color(hex: 0x16130F)
    static let card = Color(hex: 0x1F1A14)
    static let line = Color(hex: 0x383024)
    static let track = Color(hex: 0x2B2419)
    static let text = Color(hex: 0xECE3D6)
    static let muted = Color(hex: 0xA89A86)
    static let dim = Color(hex: 0x7D715F)
    static let ok = Color(hex: 0x8FB573)
    static let warn = Color(hex: 0xF0A83C)
    static let bad = Color(hex: 0xE5604D)
    static let clawd = Color(hex: 0xD77757)

    static func level(_ pct: Double) -> Color {
        if pct >= 85 { return bad }
        if pct >= 60 { return warn }
        return ok
    }
}

let MODELS = ["Haiku", "Sonnet", "Opus", "Fable"]

enum Mood { case normal, sleepy, sweaty, exhausted }

struct Feel {
    var mood: Mood = .normal
    var heat: Double = 0
}

func feelOf(_ u: Snapshot?, _ model: String) -> Feel {
    let session = u?.fiveHour?.percent
    let values = [session, u?.sevenDay?.percent, u?.own(model)?.percent].compactMap { $0 }
    guard let worst = values.max() else { return Feel() }
    let heat = min(max((worst - 90) / 10, 0), 1)
    if worst >= 99.5 { return Feel(mood: .exhausted, heat: heat) }
    if worst >= 85 { return Feel(mood: .sweaty, heat: heat) }
    if let session, session < 0.5 { return Feel(mood: .sleepy, heat: heat) }
    return Feel(mood: .normal, heat: heat)
}

private let BANDIT = [
    "................",
    "................",
    "................",
    "..BB........BB..",
    "..BBBBBBBBBBBB..",
    "..BBBBBBBBBBBB..",
    ".MMMMMBBBBMMMMM.",
    ".MMMMMBBBBMMMMM.",
    "BBBBBBLNNLBBBBBB",
    "BBBBBBLLLLBBBBBB",
    "..BBBBBBBBBBBB..",
    "..BBBBBBBBBBBB..",
    "...B.B....B.B...",
    "...B.B....B.B...",
]

private struct Px {
    let x: Double, y: Double, w: Double, h: Double
    let color: UInt32
}

private let GOLD: UInt32 = 0xF5D66A
private let HAT: UInt32 = 0x3B3446
private let PHONES: UInt32 = 0x3F3A48

private func accessory(_ model: String?) -> [Px] {
    switch model {
    case "Fable":
        return [Px(x: 5, y: 0, w: 6, h: 2, color: HAT), Px(x: 5, y: 0, w: 1, h: 2, color: 0x5A4F6B), Px(x: 5, y: 2, w: 6, h: 1, color: GOLD), Px(x: 4, y: 3, w: 8, h: 1, color: HAT)]
    case "Opus":
        var glasses: [Px] = [Px(x: 6, y: 6, w: 4, h: 1, color: GOLD)]
        for e: Double in [4, 11] {
            glasses.append(Px(x: e - 1, y: 5, w: 3, h: 1, color: GOLD))
            glasses.append(Px(x: e - 1, y: 8, w: 3, h: 1, color: GOLD))
            glasses.append(Px(x: e - 1, y: 6, w: 1, h: 2, color: GOLD))
            glasses.append(Px(x: e + 1, y: 6, w: 1, h: 2, color: GOLD))
        }
        return glasses
    case "Sonnet":
        return [
            Px(x: 3, y: 2, w: 10, h: 1, color: PHONES), Px(x: 2, y: 3, w: 1, h: 2, color: PHONES), Px(x: 13, y: 3, w: 1, h: 2, color: PHONES),
            Px(x: 1, y: 5, w: 2, h: 3, color: PHONES), Px(x: 13, y: 5, w: 2, h: 3, color: PHONES),
            Px(x: 1, y: 6, w: 1, h: 1, color: 0x7D7590), Px(x: 14, y: 6, w: 1, h: 1, color: 0x7D7590),
        ]
    case "Haiku":
        return [
            Px(x: 7, y: 1, w: 1, h: 3, color: 0x5E8A4A), Px(x: 5, y: 1, w: 2, h: 1, color: 0x8FB573), Px(x: 6, y: 2, w: 1, h: 1, color: 0x8FB573),
            Px(x: 8, y: 0, w: 2, h: 1, color: 0x8FB573), Px(x: 8, y: 1, w: 1, h: 1, color: 0x8FB573),
        ]
    default:
        return []
    }
}

struct MascotView: View {
    var model: String? = nil
    var feel = Feel()
    var reserveTop = true

    var body: some View {
        Canvas { ctx, size in
            let top = reserveTop ? 0.0 : 3.0
            let u = size.width / 16
            let oy = -top * u
            let out = feel.mood == .exhausted
            let base = Color(hex: 0xA39B90)
            let warm = feel.heat * 0.72
            let body = out ? Color(hex: 0x4E3530) : (feel.heat > 0 ? base.mixed(with: Color(hex: 0xFF3B2F), warm) : base)
            let fur = out ? Color(hex: 0x6E5A52) : (feel.heat > 0 ? Color(hex: 0xEEE7DB).mixed(with: Color(hex: 0xFFB0A3), warm) : Color(hex: 0xEEE7DB))
            let mask = out ? Color(hex: 0x2A1F1C) : Color(hex: 0x4A413B)
            func rect(_ x: Double, _ y: Double, _ w: Double, _ h: Double, _ c: Color) {
                ctx.fill(Path(CGRect(x: x * u, y: oy + y * u, width: w * u + 0.6, height: h * u + 0.6)), with: .color(c))
            }
            for (r, row) in BANDIT.enumerated() {
                for (c, ch) in row.enumerated() where ch != "." {
                    let color: Color
                    switch ch {
                    case "B": color = body
                    case "L": color = fur
                    case "M": color = mask
                    default: color = Color(hex: 0x0C0A08)
                    }
                    rect(Double(c), Double(r), 1, 1, color)
                }
            }
            let shine = Color(hex: 0xF3EFEA)
            for e in [4.0, 11.0] {
                if out {
                    let cx = (e + 0.5) * u, cy = oy + 7 * u, hw = u * 0.95
                    var x = Path()
                    x.move(to: CGPoint(x: cx - hw, y: cy - hw)); x.addLine(to: CGPoint(x: cx + hw, y: cy + hw))
                    x.move(to: CGPoint(x: cx - hw, y: cy + hw)); x.addLine(to: CGPoint(x: cx + hw, y: cy - hw))
                    ctx.stroke(x, with: .color(Color(hex: 0xD8CFC2)), lineWidth: u * 0.45)
                } else if feel.mood == .sleepy {
                    rect(e, 7.6, 1, 0.4, shine)
                } else {
                    rect(e, 6, 1, 2, shine)
                }
            }
            for p in accessory(model) { rect(p.x, p.y, p.w, p.h, Color(hex: p.color)) }
            if feel.mood == .sweaty { rect(15, 4, 1, 2, Color(hex: 0x8FD3F4)) }
        }
        .aspectRatio(16.0 / (reserveTop ? 14 : 11), contentMode: .fit)
    }
}

enum L {
    static let pt = Locale.preferredLanguages.first?.hasPrefix("pt") ?? true
    static let locale = Locale(identifier: Locale.preferredLanguages.first ?? "pt-BR")

    static var session: String { pt ? "Sessão" : "Session" }
    static var week: String { pt ? "Semana" : "Week" }
    static var sessionWindow: String { pt ? "janela de 5 horas" : "5-hour window" }
    static var weekWindow: String { pt ? "janela de 7 dias" : "7-day window" }
    static var resetsIn: String { pt ? "libera em" : "resets in" }
    static var models: String { pt ? "Modelos" : "Models" }
    static var modelsHint: String {
        pt ? "Barra colorida é o limite próprio do modelo. Cinza é o limite semanal geral, que vale para todos."
            : "A colored bar is the model's own limit. Gray is the general weekly limit, shared by all."
    }
    static var subtitle: String { pt ? "iPhone · uso do Claude" : "iPhone · Claude usage" }
    static var refresh: String { pt ? "Atualizar" : "Refresh" }
    static var repair: String { pt ? "Parear de novo" : "Pair again" }
    static var now: String { pt ? "agora" : "now" }
    static var pairTitle: String { pt ? "Pareie com o PC ou o Mac" : "Pair with your PC or Mac" }
    static var pairBody: String {
        pt ? "No navegador do PC, abra banditboard.pages.dev/conectar, aponte a câmera do iPhone para o código e rode o comando que aparece. Funciona em qualquer rede.\n\nSe já usa o Banditboard no Windows ou no Mac, também dá para ligar \"Compartilhar o uso com o iPhone\" no painel e ler o código de lá."
            : "In your PC browser, open banditboard.pages.dev/conectar, point the iPhone camera at the code and run the command shown. Works on any network.\n\nIf you already use Banditboard on Windows or Mac, you can also turn on \"Share usage with the iPhone\" in its dashboard and scan the code there."
    }
    static var unreachable: String {
        pt ? "Não achei o Banditboard do PC. Veja se o iPhone está no mesmo Wi-Fi e se o compartilhamento está ligado."
            : "Could not reach Banditboard on your PC. Check that the iPhone is on the same Wi-Fi and sharing is on."
    }
    static var badKey: String { pt ? "A chave mudou. Pareie de novo pelo código." : "The key changed. Pair again with the code." }
    static var waiting: String { pt ? "Esperando o primeiro envio do Claude Code" : "Waiting for the first update from Claude Code" }
    static var alertFree: String { pt ? "Pode voltar a usar o Claude." : "You can use Claude again." }
    static var incident: String { pt ? "Incidente" : "Incident" }
    static var checkingStatus: String { pt ? "Verificando status.claude.com..." : "Checking status.claude.com..." }
    static var allOperational: String { pt ? "Todos os sistemas operacionais" : "All systems operational" }
    static var news: String { pt ? "Notícias da Anthropic" : "Anthropic news" }
    static var loadingNews: String { pt ? "Buscando as notícias..." : "Loading news..." }

    static func updatedAgo(_ ago: String) -> String { pt ? "atualizado \(ago) · Claude Code" : "updated \(ago) · Claude Code" }
    static func resets(_ at: String) -> String { pt ? "Libera \(at)" : "Resets \(at)" }

    static func alertTitle(week: Bool, level: Int) -> String {
        if level == 0 { return week ? (pt ? "Limite da semana liberado" : "Weekly limit reset") : (pt ? "Sessão liberada" : "Session reset") }
        if level >= 100 { return week ? (pt ? "Limite da semana atingido" : "Weekly limit reached") : (pt ? "Limite da sessão atingido" : "Session limit reached") }
        return week ? (pt ? "Semana em \(level)%" : "Week at \(level)%") : (pt ? "Sessão em \(level)%" : "Session at \(level)%")
    }
}

func formatPct(_ pct: Double?) -> String {
    guard let pct else { return "--" }
    return "\(Int(pct.rounded()))%"
}

func formatLeft(_ seconds: TimeInterval) -> String {
    let s = Int(seconds)
    if s <= 0 { return L.now }
    let d = s / 86_400, h = (s % 86_400) / 3600, m = (s % 3600) / 60
    if d > 0 { return "\(d)d \(h)h" }
    if h > 0 { return String(format: "%dh %02dm", h, m) }
    return String(format: "%dm %02ds", m, s % 60)
}

func formatAgo(_ seconds: TimeInterval) -> String {
    let s = max(Int(seconds), 0)
    if s < 5 { return L.now }
    if s < 60 { return L.pt ? "há \(s)s" : "\(s)s ago" }
    if s < 3600 { return L.pt ? "há \(s / 60) min" : "\(s / 60) min ago" }
    return L.pt ? "há \(s / 3600)h" : "\(s / 3600)h ago"
}

func formatAt(_ date: Date, now: Date = .now) -> String {
    let cal = Calendar.current
    let hm = date.formatted(.dateTime.hour(.twoDigits(amPM: .omitted)).minute(.twoDigits).locale(L.locale))
    if cal.isDate(date, inSameDayAs: now) { return L.pt ? "hoje às \(hm)" : "today at \(hm)" }
    if let tomorrow = cal.date(byAdding: .day, value: 1, to: now), cal.isDate(date, inSameDayAs: tomorrow) {
        return L.pt ? "amanhã às \(hm)" : "tomorrow at \(hm)"
    }
    let day = date.formatted(.dateTime.weekday(.abbreviated).day().month(.defaultDigits).locale(L.locale))
    return L.pt ? "\(day) às \(hm)" : "\(day) at \(hm)"
}

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

enum Species: String, Codable, CaseIterable { case raccoon, classic }
enum Skin: String, Codable, CaseIterable { case classic, models, crowns, xmas }
enum Tint: String, Codable, CaseIterable { case natural, rainbow, lavender, mint, bubblegum }
enum Language: String, Codable, CaseIterable { case auto, pt, en }

struct Prefs: Codable, Equatable {
    var species: Species = .raccoon
    var skin: Skin = .models
    var tint: Tint = .natural
    var language: Language = .auto
    var alerts = true

    init() {}

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        species = (try? c.decodeIfPresent(Species.self, forKey: .species)) ?? .raccoon
        skin = (try? c.decodeIfPresent(Skin.self, forKey: .skin)) ?? .models
        tint = (try? c.decodeIfPresent(Tint.self, forKey: .tint)) ?? .natural
        language = (try? c.decodeIfPresent(Language.self, forKey: .language)) ?? .auto
        alerts = (try? c.decodeIfPresent(Bool.self, forKey: .alerts)) ?? true
    }
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

private let SPRITE = [
    "................",
    "................",
    "................",
    "..MM........MM..",
    ".MLLMBBBBBBMLLM.",
    ".BBBBBBBBBBBBBB.",
    ".BLLLLBBBBLLLLB.",
    ".MMMMMBBBBMMMMM.",
    ".MMMMMMBBMMMMMM.",
    ".BMMMMLLLLMMMMB.",
    "BBBBLLLNNLLLBBBB",
    "MbBBBLLLLLLBBBbM",
    "..bBBBBBBBBBBb..",
    "...MM......MM...",
]

private struct Px {
    let x: Double, y: Double, w: Double, h: Double
    let color: UInt32

    init(_ x: Double, _ y: Double, _ w: Double, _ h: Double, _ color: UInt32) {
        self.x = x
        self.y = y
        self.w = w
        self.h = h
        self.color = color
    }
}

private enum Accessory { case topHat, glasses, headphones, sprout, crown, santa }

private let GOLD: UInt32 = 0xF5D66A
private let HAT: UInt32 = 0x3B3446
private let HAT_SHINE: UInt32 = 0x5A4F6B
private let PHONES: UInt32 = 0x3F3A48
private let PHONES_SHINE: UInt32 = 0x7D7590
private let LEAF: UInt32 = 0x8FB573
private let STEM: UInt32 = 0x5E8A4A
private let RED: UInt32 = 0xD6455D
private let WHITE: UInt32 = 0xF3EFEA
private let RUBY: UInt32 = 0xE5604D

private func accessoryFor(_ skin: Skin, _ model: String?) -> Accessory? {
    switch skin {
    case .classic: return nil
    case .crowns: return .crown
    case .xmas: return .santa
    case .models:
        switch model {
        case "Fable": return .topHat
        case "Opus": return .glasses
        case "Sonnet": return .headphones
        case "Haiku": return .sprout
        default: return nil
        }
    }
}

func bodyHex(_ tint: Tint, _ model: String?) -> UInt32 {
    switch tint {
    case .natural: return 0xA39B90
    case .lavender: return 0xB9A6F2
    case .mint: return 0x7CCBA2
    case .bubblegum: return 0xF59AC0
    case .rainbow:
        switch model {
        case "Haiku": return 0x7CCBA2
        case "Sonnet": return 0xB9A6F2
        case "Fable": return 0xF59AC0
        default: return 0xA39B90
        }
    }
}

private func pixels(_ a: Accessory, _ species: Species) -> [Px] {
    if species == .raccoon {
        switch a {
        case .glasses:
            var out = [Px(6, 6, 4, 1, GOLD)]
            for e: Double in [4, 11] {
                out.append(Px(e - 1, 5, 3, 1, GOLD))
                out.append(Px(e - 1, 8, 3, 1, GOLD))
                out.append(Px(e - 1, 6, 1, 2, GOLD))
                out.append(Px(e + 1, 6, 1, 2, GOLD))
            }
            return out
        case .headphones:
            return [
                Px(3, 2, 10, 1, PHONES), Px(2, 3, 1, 2, PHONES), Px(13, 3, 1, 2, PHONES),
                Px(1, 5, 2, 3, PHONES), Px(13, 5, 2, 3, PHONES), Px(1, 6, 1, 1, PHONES_SHINE), Px(14, 6, 1, 1, PHONES_SHINE),
            ]
        case .santa:
            return [Px(4, 2, 8, 1, RED), Px(6, 1, 5, 1, RED), Px(9, 0, 3, 1, RED), Px(12, 0, 2, 2, WHITE), Px(3, 3, 10, 1, WHITE)]
        default:
            break
        }
    }
    switch a {
    case .topHat:
        return [Px(5, 0, 6, 2, HAT), Px(5, 0, 1, 2, HAT_SHINE), Px(5, 2, 6, 1, GOLD), Px(4, 3, 8, 1, HAT)]
    case .glasses:
        var out = [Px(6, 7, 4, 1, GOLD)]
        for e: Double in [3, 11] {
            out.append(Px(e - 1, 6, 4, 1, GOLD))
            out.append(Px(e - 1, 9, 4, 1, GOLD))
            out.append(Px(e - 1, 7, 1, 2, GOLD))
            out.append(Px(e + 2, 7, 1, 2, GOLD))
        }
        return out
    case .headphones:
        return [
            Px(2, 2, 12, 1, PHONES), Px(1, 3, 1, 3, PHONES), Px(14, 3, 1, 3, PHONES),
            Px(0, 6, 2, 3, PHONES), Px(14, 6, 2, 3, PHONES), Px(0, 7, 1, 1, PHONES_SHINE), Px(15, 7, 1, 1, PHONES_SHINE),
        ]
    case .sprout:
        return [Px(7, 1, 1, 3, STEM), Px(5, 1, 2, 1, LEAF), Px(6, 2, 1, 1, LEAF), Px(8, 0, 2, 1, LEAF), Px(8, 1, 1, 1, LEAF)]
    case .crown:
        return [Px(4, 2, 8, 2, GOLD), Px(4, 1, 1, 1, GOLD), Px(7, 0, 2, 2, GOLD), Px(11, 1, 1, 1, GOLD), Px(7, 2, 2, 1, RUBY)]
    case .santa:
        return [Px(4, 2, 8, 1, RED), Px(6, 1, 5, 1, RED), Px(9, 0, 3, 1, RED), Px(12, 0, 2, 2, WHITE), Px(4, 3, 8, 1, WHITE)]
    }
}

struct MascotView: View {
    var model: String? = nil
    var feel = Feel()
    var reserveTop = true
    var look = Vault.prefs

    var body: some View {
        Canvas { ctx, size in
            let top = reserveTop ? 0.0 : 3.0
            let u = size.width / 16
            let oy = -top * u
            let out = feel.mood == .exhausted
            let base = Color(hex: bodyHex(look.tint, model))
            let warm = feel.heat * 0.72
            let body = out ? Color(hex: 0x4E3530) : (feel.heat > 0 ? base.mixed(with: Color(hex: 0xFF3B2F), warm) : base)
            let shade = body.mixed(with: .black, 0.24)
            let plainFur = Color(hex: 0xEEE7DB)
            let fur = out ? Color(hex: 0x6E5A52) : (feel.heat > 0 ? plainFur.mixed(with: Color(hex: 0xFFB0A3), warm) : plainFur)
            let mask = out ? Color(hex: 0x2A1F1C) : Color(hex: 0x4A413B)
            let ink = Color(hex: 0x0C0A08)
            let shine = Color(hex: 0xF3EFEA)
            func rect(_ x: Double, _ y: Double, _ w: Double, _ h: Double, _ c: Color) {
                ctx.fill(Path(CGRect(x: x * u, y: oy + y * u, width: w * u + 0.6, height: h * u + 0.6)), with: .color(c))
            }
            func cross(_ cx: Double, _ cy: Double) {
                let hw = u * 0.95
                var x = Path()
                x.move(to: CGPoint(x: cx - hw, y: cy - hw))
                x.addLine(to: CGPoint(x: cx + hw, y: cy + hw))
                x.move(to: CGPoint(x: cx - hw, y: cy + hw))
                x.addLine(to: CGPoint(x: cx + hw, y: cy - hw))
                ctx.stroke(x, with: .color(Color(hex: 0xD8CFC2)), lineWidth: u * 0.45)
            }
            let classic = look.species == .classic
            for (r, row) in (classic ? SPRITE : BANDIT).enumerated() {
                for (c, ch) in row.enumerated() where ch != "." {
                    let color: Color
                    switch ch {
                    case "B": color = body
                    case "b": color = shade
                    case "L": color = fur
                    case "M": color = mask
                    default: color = ink
                    }
                    rect(Double(c), Double(r), 1, 1, color)
                }
            }
            if classic {
                for e in [3.0, 11.0] {
                    if out {
                        cross((e + 1) * u, oy + 8 * u)
                    } else if feel.mood == .sleepy {
                        rect(e, 8.6, 2, 0.4, ink)
                    } else {
                        rect(e, 7, 2, 2, ink)
                        rect(e, 7, 1, 1, shine)
                    }
                }
            } else {
                for e in [4.0, 11.0] {
                    if out {
                        cross((e + 0.5) * u, oy + 7 * u)
                    } else if feel.mood == .sleepy {
                        rect(e, 7.6, 1, 0.4, shine)
                    } else {
                        rect(e, 6, 1, 2, shine)
                    }
                }
            }
            if let a = accessoryFor(look.skin, model) {
                for p in pixels(a, look.species) { rect(p.x, p.y, p.w, p.h, Color(hex: p.color)) }
            }
            if feel.mood == .sweaty { rect(15, 4, 1, 2, Color(hex: 0x8FD3F4)) }
        }
        .aspectRatio(16.0 / (reserveTop ? 14 : 11), contentMode: .fit)
    }
}

enum L {
    private static var forced: Language? {
        ProcessInfo.processInfo.arguments.first { $0.hasPrefix("--lang=") }.flatMap { Language(rawValue: String($0.dropFirst(7))) }
    }

    static var pt: Bool {
        switch forced ?? Vault.prefs.language {
        case .pt: return true
        case .en: return false
        case .auto: return Locale.preferredLanguages.first?.hasPrefix("pt") ?? true
        }
    }

    static var locale: Locale {
        switch forced ?? Vault.prefs.language {
        case .pt: return Locale(identifier: "pt-BR")
        case .en: return Locale(identifier: "en-US")
        case .auto: return Locale(identifier: Locale.preferredLanguages.first ?? "pt-BR")
        }
    }

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
    static var settings: String { pt ? "Ajustes" : "Settings" }
    static var watchPair: String {
        pt ? "Abra o Banditboard no iPhone e pareie pela página banditboard.pages.dev/conectar. O relógio recebe o pareamento sozinho."
            : "Open Banditboard on the iPhone and pair through banditboard.pages.dev/conectar. The watch gets the pairing on its own."
    }
    static var close: String { pt ? "Fechar" : "Close" }
    static var mascot: String { pt ? "Mascote" : "Mascot" }
    static var accessories: String { pt ? "Acessórios" : "Accessories" }
    static var color: String { pt ? "Cor" : "Color" }
    static var language: String { pt ? "Idioma" : "Language" }
    static var alertsToggle: String { pt ? "Avisar quando chegar perto do limite" : "Warn when getting close to the limit" }
    static var pairing: String { pt ? "Pareamento" : "Pairing" }
    static var pairedBox: String { pt ? "Recebendo pelo servidor do Banditboard, cifrado de ponta a ponta." : "Receiving through the Banditboard server, encrypted end to end." }
    static var pairedLan: String { pt ? "Recebendo do Banditboard do PC ou do Mac pela rede de casa." : "Receiving from Banditboard on your PC or Mac over your home network." }
    static var credits: String { pt ? "Créditos" : "Credits" }
    static var createdBy: String { pt ? "Criado por Vinícius Pires da Silva" : "Created by Vinícius Pires da Silva" }
    static var mascotRacco: String { pt ? "Racco, o guaxinim do Banditboard" : "Racco, the Banditboard raccoon" }
    static var promoBody: String {
        pt ? "Use o Banditboard em qualquer dispositivo: celular, tablet ou computador. O painel fica sempre à vista e os avisos de limite chegam onde você estiver."
            : "Use Banditboard on any device: phone, tablet or computer. The dashboard stays in sight and the limit alerts reach you wherever you are."
    }
    static var fanProject: String { pt ? "Projeto pessoal de fã, sem vínculo com a Anthropic." : "Personal fan project, not affiliated with Anthropic." }

    static func label(_ s: Species) -> String {
        switch s {
        case .raccoon: return "Racco"
        case .classic: return pt ? "Racco clássico" : "Classic Racco"
        }
    }

    static func label(_ s: Skin) -> String {
        switch s {
        case .classic: return pt ? "Clássico" : "Classic"
        case .models: return pt ? "Por modelo" : "Per model"
        case .crowns: return pt ? "Coroas" : "Crowns"
        case .xmas: return pt ? "Natal" : "Christmas"
        }
    }

    static func label(_ t: Tint) -> String {
        switch t {
        case .natural: return "Natural"
        case .rainbow: return pt ? "Arco-íris" : "Rainbow"
        case .lavender: return pt ? "Lavanda" : "Lavender"
        case .mint: return pt ? "Menta" : "Mint"
        case .bubblegum: return pt ? "Chiclete" : "Bubblegum"
        }
    }

    static func label(_ l: Language) -> String {
        switch l {
        case .auto: return pt ? "Automático" : "Automatic"
        case .pt: return "Português"
        case .en: return "English"
        }
    }

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
    let c = cal.dateComponents([.weekday, .day, .month, .hour, .minute], from: date)
    let hm = String(format: "%02d:%02d", c.hour ?? 0, c.minute ?? 0)
    if cal.isDate(date, inSameDayAs: now) { return L.pt ? "hoje às \(hm)" : "today at \(hm)" }
    if let tomorrow = cal.date(byAdding: .day, value: 1, to: now), cal.isDate(date, inSameDayAs: tomorrow) {
        return L.pt ? "amanhã às \(hm)" : "tomorrow at \(hm)"
    }
    let weekday = ((c.weekday ?? 2) + 5) % 7
    let day = c.day ?? 1
    let month = c.month ?? 1
    if L.pt {
        let days = ["seg", "ter", "qua", "qui", "sex", "sáb", "dom"]
        return "\(days[weekday]) \(day)/\(String(format: "%02d", month)) às \(hm)"
    }
    let days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
    let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    return "\(days[weekday]), \(months[month - 1]) \(day) at \(hm)"
}


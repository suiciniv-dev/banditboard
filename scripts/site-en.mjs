import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const site = join(dirname(fileURLToPath(import.meta.url)), "..", "site");
let html = readFileSync(join(site, "index.html"), "utf8");

const start = html.indexOf("const T = {");
let depth = 0, end = -1;
for (let i = html.indexOf("{", start); i < html.length; i++) {
  if (html[i] === "{") depth++;
  else if (html[i] === "}" && --depth === 0) { end = i + 1; break; }
}
const T = new Function(`return (${html.slice(html.indexOf("{", start), end)});`)();
const L = T.en;
const text = v => v.replace(/<[^>]+>/g, "");
const attr = v => text(v).replace(/&/g, "&amp;").replace(/"/g, "&quot;");

function closeOf(src, tag, from) {
  const re = new RegExp(`<${tag}\\b|</${tag}>`, "gi");
  re.lastIndex = from;
  let d = 1, m;
  while ((m = re.exec(src))) {
    if (m[0][1] === "/") { if (--d === 0) return m.index; }
    else d++;
  }
  throw new Error(`sem </${tag}>`);
}

let out = "", pos = 0;
const open = /<([a-z0-9]+)\b[^>]*\sdata-t="([^"]+)"[^>]*>/gi;
let m;
while ((m = open.exec(html))) {
  const [tagText, tag, key] = m;
  const v = L[key];
  if (typeof v !== "string") continue;
  const inner = m.index + tagText.length;
  const close = closeOf(html, tag, inner);
  out += html.slice(pos, inner) + (tag.toLowerCase() === "title" ? text(v) : v);
  pos = close;
  open.lastIndex = close;
}
html = out + html.slice(pos);

html = html
  .replace(/(<[^>]*\sdata-tc="([^"]+)"[^>]*\scontent=")[^"]*"/g, (s, a, k) => L[k] ? `${a}${attr(L[k])}"` : s)
  .replace(/(<[^>]*\sdata-th="([^"]+)"[^>]*\shref=")[^"]*"/g, (s, a, k) => L[k] ? `${a}${attr(L[k])}"` : s)
  .replace(/(<img\b[^>]*\sdata-alt="([^"]+)"[^>]*\salt=")[^"]*"/g, (s, a, k) => L[k] ? `${a}${attr(L[k])}"` : s)
  .replace(/(<(?!img)[a-z]+\b[^>]*\sdata-alt="([^"]+)"[^>]*\saria-label=")[^"]*"/g, (s, a, k) => L[k] ? `${a}${attr(L[k])}"` : s)
  .replace(/(<img\b[^>]*\sdata-img="[^"]+"[^>]*\ssrc="\/img\/)pt\//g, "$1en/")
  .replace(/<html lang="pt-BR">/, '<html lang="en">')
  .replace(/(id="lang" type="button" aria-label=")[^"]*">[^<]*</, `$1${L.otherName}">${L.other}<`)
  .replace(/(id="sugGo" href=")[^"]*"/, `$1${T.pt.home}"`);

html = html.replace(/(<script type="application\/ld\+json" id="ld">)([\s\S]*?)(<\/script>)/, (s, a, json, c) => {
  const ld = JSON.parse(json);
  Object.assign(ld, { description: text(L.desc), inLanguage: "en", url: L.url, screenshot: ld.screenshot.replace("/img/pt/", "/img/en/") });
  ld.offers.priceCurrency = "USD";
  return `${a}\n${JSON.stringify(ld, null, 2)}\n${c}`;
});

mkdirSync(join(site, "en"), { recursive: true });
writeFileSync(join(site, "en", "index.html"), html);
console.log("site/en/index.html");

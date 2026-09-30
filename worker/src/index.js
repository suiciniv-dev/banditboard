import installPs1 from '../../app/src/main/assets/pc/install.ps1'
import usagePs1 from '../../app/src/main/assets/pc/usage.ps1'
import installSh from '../../app/src/main/assets/pc/install.sh'
import usageSh from '../../app/src/main/assets/pc/usage.sh'

const CORS = {
  'access-control-allow-origin': '*',
  'access-control-allow-headers': 'content-type, x-clawdboard, x-clawdboard-key',
  'access-control-allow-methods': 'GET, POST, DELETE, OPTIONS',
}

const TEXTS = {
  pt: {
    connected: 'Banditboard conectado ao Claude Code.',
    backup: 'Backup dos seus ajustes: ',
    every: 'A cada resposta do Claude Code (VS Code ou terminal) o uso vai cifrado para os seus celulares, no maximo a cada 2 minutos.',
  },
  en: {
    connected: 'Banditboard connected to Claude Code.',
    backup: 'Backup of your settings: ',
    every: 'After each Claude Code response (VS Code or terminal) the usage goes encrypted to your phones, at most every 2 minutes.',
  },
}

const HEX32 = /^[0-9a-f]{32}$/
const HEX64 = /^[0-9a-f]{64}$/
const KEEP_DAYS = 45

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json', 'cache-control': 'no-store', ...CORS },
  })
}

function text(body) {
  return new Response(body, { headers: { 'content-type': 'text/plain; charset=utf-8', 'cache-control': 'no-store', ...CORS } })
}

async function sha256(value) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value))
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('')
}

function same(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string' || a.length !== b.length) return false
  let diff = 0
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i)
  return diff === 0
}

function boxPs1(t) {
  return 'param([string]$BoxUrl, [string]$BoxKey, [string]$BoxEnc)\n' + installPs1
    .replace('__USAGE__', () => usagePs1.trimEnd())
    .replaceAll("'__URL__'", () => '$BoxUrl')
    .replaceAll("'__KEY__'", () => '$BoxKey')
    .replaceAll("'__ENC__'", () => '$BoxEnc')
    .replaceAll("'__ID__'", () => "'box'")
    .replace('__T_CONNECTED__', () => t.connected)
    .replace('__T_BACKUP__', () => t.backup)
    .replace('__T_EVERY__', () => t.every)
}

function boxSh(t) {
  return installSh
    .replace('__USAGE__', () => usageSh.trimEnd())
    .replaceAll("'__URL__'", () => '"$BOX_URL"')
    .replaceAll("'__KEY__'", () => '"$BOX_KEY"')
    .replaceAll("'__ENC__'", () => '"$BOX_ENC"')
    .replaceAll("'__ID__'", () => "'box'")
    .replace('__T_CONNECTED__', () => t.connected)
    .replace('__T_BACKUP__', () => t.backup)
    .replace('__T_EVERY__', () => t.every)
    .replace('#!/bin/sh\n', () => '#!/bin/sh\nBOX_URL="$1"\nBOX_KEY="$2"\nBOX_ENC="$3"\n')
}

let google = null

function pem(text) {
  const b64 = text.replace(/-----[^-]+-----/g, '').replace(/\s+/g, '')
  return Uint8Array.from(atob(b64), (c) => c.charCodeAt(0))
}

function b64url(bytes) {
  return btoa(String.fromCharCode(...new Uint8Array(bytes))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

async function accessToken(sa) {
  const now = Math.floor(Date.now() / 1000)
  if (google && google.exp > now + 60) return google.token
  const enc = (o) => b64url(new TextEncoder().encode(JSON.stringify(o)))
  const unsigned = `${enc({ alg: 'RS256', typ: 'JWT' })}.${enc({
    iss: sa.client_email,
    scope: 'https://www.googleapis.com/auth/firebase.messaging',
    aud: 'https://oauth2.googleapis.com/token',
    iat: now,
    exp: now + 3600,
  })}`
  const key = await crypto.subtle.importKey('pkcs8', pem(sa.private_key), { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['sign'])
  const signature = await crypto.subtle.sign('RSASSA-PKCS1-v1_5', key, new TextEncoder().encode(unsigned))
  const res = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${unsigned}.${b64url(signature)}`,
  })
  if (!res.ok) throw new Error(`oauth ${res.status}`)
  const out = await res.json()
  google = { token: out.access_token, exp: now + (out.expires_in || 3600) }
  return google.token
}

async function notify(env, id, blob, at) {
  if (!env.FCM_SERVICE_ACCOUNT) return
  const { results } = await env.DB.prepare("SELECT token FROM device WHERE box = ? AND platform = 'android'").bind(id).all()
  if (!results.length) return
  const sa = JSON.parse(env.FCM_SERVICE_ACCOUNT)
  const token = await accessToken(sa)
  for (const { token: device } of results) {
    const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
      method: 'POST',
      headers: { authorization: `Bearer ${token}`, 'content-type': 'application/json' },
      body: JSON.stringify({ message: { token: device, data: { box: id, blob, at: String(at) }, android: { priority: 'HIGH', ttl: '3600s' } } }),
    })
    if (res.status === 404 || res.status === 400) {
      const body = await res.text()
      if (body.includes('UNREGISTERED') || body.includes('INVALID_ARGUMENT')) {
        await env.DB.prepare('DELETE FROM device WHERE box = ? AND token = ?').bind(id, device).run()
      }
    }
  }
}

async function readBody(req, limit) {
  const size = Number(req.headers.get('content-length') || '0')
  if (size > limit) return null
  const raw = await req.text()
  if (raw.length > limit) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

async function route(req, env, ctx) {
  const url = new URL(req.url)
  if (req.method === 'OPTIONS') return new Response(null, { headers: CORS })
  const t = TEXTS[url.searchParams.get('lang') === 'en' ? 'en' : 'pt']
  if (req.method === 'GET' && url.pathname === '/') return json(200, { ok: true })
  if (req.method === 'GET' && url.pathname === '/pc/box.ps1') return text(boxPs1(t))
  if (req.method === 'GET' && url.pathname === '/pc/box.sh') return text(boxSh(t))

  const parts = url.pathname.split('/').filter(Boolean)
  if (parts[0] !== 'v1' || parts[1] !== 'box') return json(404, { error: 'not found' })

  if (req.method === 'POST' && parts.length === 2) {
    const body = await readBody(req, 1024)
    if (!body || !HEX32.test(body.id) || !HEX64.test(body.write) || !HEX64.test(body.read)) return json(400, { error: 'bad box' })
    const { meta } = await env.DB.prepare('INSERT OR IGNORE INTO box (id, write, read, created) VALUES (?, ?, ?, ?)')
      .bind(body.id, body.write, body.read, Date.now()).run()
    return meta.changes ? json(201, { ok: true }) : json(409, { error: 'exists' })
  }

  const id = parts[2]
  if (!HEX32.test(id || '')) return json(404, { error: 'not found' })
  const row = await env.DB.prepare('SELECT * FROM box WHERE id = ?').bind(id).first()
  if (!row) return json(404, { error: 'not found' })
  const hash = await sha256((req.headers.get('x-clawdboard-key') || '').trim())

  if (req.method === 'POST' && parts[3] === 'push' && parts.length === 4) {
    if (!same(hash, row.write)) return json(401, { error: 'bad key' })
    const body = await readBody(req, 16384)
    if (!body || typeof body.blob !== 'string' || body.blob.length > 12000 || !/^[A-Za-z0-9+/=]+$/.test(body.blob)) {
      return json(400, { error: 'bad blob' })
    }
    const lvl = typeof body.lvl === 'string' && /^[0-9,]{0,16}$/.test(body.lvl) ? body.lvl : ''
    const at = Date.now()
    await env.DB.prepare('UPDATE box SET blob = ?, at = ?, lvl = ? WHERE id = ?').bind(body.blob, at, lvl, id).run()
    if (lvl !== row.lvl) ctx.waitUntil(notify(env, id, body.blob, at).catch(() => {}))
    return json(200, { ok: true })
  }

  if (!same(hash, row.read)) return json(401, { error: 'bad key' })

  if (req.method === 'GET' && parts.length === 3) return json(200, { blob: row.blob, at: row.at })

  if (parts[3] === 'device' && parts.length === 4) {
    const body = await readBody(req, 4096)
    if (!body || typeof body.token !== 'string' || body.token.length < 20 || body.token.length > 4096) return json(400, { error: 'bad device' })
    if (req.method === 'POST') {
      const platform = body.platform === 'ios' ? 'ios' : 'android'
      await env.DB.prepare('INSERT OR REPLACE INTO device (box, token, platform, added) VALUES (?, ?, ?, ?)').bind(id, body.token, platform, Date.now()).run()
      return json(200, { ok: true })
    }
    if (req.method === 'DELETE') {
      await env.DB.prepare('DELETE FROM device WHERE box = ? AND token = ?').bind(id, body.token).run()
      return json(200, { ok: true })
    }
  }
  return json(404, { error: 'not found' })
}

export default {
  async fetch(req, env, ctx) {
    try {
      return await route(req, env, ctx)
    } catch {
      return json(500, { error: 'internal' })
    }
  },

  async scheduled(event, env) {
    const cutoff = Date.now() - KEEP_DAYS * 86_400_000
    await env.DB.prepare('DELETE FROM box WHERE COALESCE(at, created) < ?').bind(cutoff).run()
    await env.DB.prepare('DELETE FROM device WHERE box NOT IN (SELECT id FROM box)').run()
  },
}

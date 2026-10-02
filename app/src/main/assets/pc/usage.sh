#!/bin/sh
export LC_ALL=C
dir="$HOME/.claude"
targets="$dir/clawdboard-targets.json"
tmp="${TMPDIR:-/tmp}"
tmp="${tmp%/}"
mark="$tmp/clawdboard-uso.txt"
seen="$tmp/clawdboard-sessoes.txt"
now=$(date +%s)

if [ "$1" != "--work" ]; then
    input=$(cat)
    field() { printf '%s' "$input" | sed -n "s/.*\"$1\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p" | head -n 1; }
    session=$(field session_id)
    transcript=$(field transcript_path)
    model=""
    if [ -n "$transcript" ] && [ -f "$transcript" ]; then
        model=$(tail -c 262144 "$transcript" | grep -o '"model"[[:space:]]*:[[:space:]]*"claude-[a-z]*' | tail -n 1 | sed 's/.*claude-//')
    fi
    if [ -n "$session" ]; then
        {
            [ -f "$seen" ] && awk -F '\t' -v s="$session" -v n="$now" 'NF == 3 && $1 != s && n - $3 < 900' "$seen"
            printf '%s\t%s\t%s\n' "$session" "$model" "$now"
        } > "$seen.$$" && mv "$seen.$$" "$seen"
    fi
    last=$(cat "$mark" 2>/dev/null)
    case "$last" in '' | *[!0-9]*) last=0 ;; esac
    [ $((now - last)) -lt 120 ] && exit 0
    printf '%s\n' "$now" > "$mark"
    nohup /bin/sh "$0" --work < /dev/null > /dev/null 2>&1 &
    exit 0
fi

find_claude() {
    command -v claude 2>/dev/null && return
    for c in "$HOME/.local/bin/claude" "$HOME/.claude/local/claude" /opt/homebrew/bin/claude /usr/local/bin/claude; do
        [ -x "$c" ] && { printf '%s\n' "$c"; return; }
    done
    ls -t "$HOME"/.vscode/extensions/anthropic.claude-code-*/resources/native-binary/claude 2> /dev/null | head -n 1
}
claude=$(find_claude)
[ -n "$claude" ] || exit 0
cd "$tmp" || exit 0
out="$tmp/clawdboard-usage.$$"
"$claude" -p "/usage" --output-format text --no-session-persistence --settings '{"disableAllHooks":true}' > "$out" 2> /dev/null &
pid=$!
(sleep 60; kill "$pid") < /dev/null > /dev/null 2>&1 &
watch=$!
wait "$pid"
kill "$watch" 2> /dev/null
text=$(cat "$out" 2> /dev/null)
rm -f "$out"
[ -n "$text" ] || exit 0

to_epoch() {
    stamp=$(printf '%04d-%02d-%02d %02d:%02d:00' "$1" "$2" "$3" "$4" "$5")
    date -j -f '%Y-%m-%d %H:%M:%S' "$stamp" +%s 2> /dev/null || date -d "$stamp" +%s 2> /dev/null
}

epoch_of() {
    set -- $(printf '%s\n' "$1" | awk '{
        s = tolower($0); gsub(/^[ \t]+|[ \t]+$/, "", s)
        if (s ~ /^in /) {
            t = 0
            if (match(s, /[0-9]+ *d/)) t += substr(s, RSTART, RLENGTH) * 86400
            if (match(s, /[0-9]+ *h/)) t += substr(s, RSTART, RLENGTH) * 3600
            if (match(s, /[0-9]+ *m/)) t += substr(s, RSTART, RLENGTH) * 60
            print "rel", t; exit
        }
        mon = 0; day = 0
        if (match(s, /^[a-z]+ +[0-9]+,/)) {
            split(substr(s, RSTART, RLENGTH), a, /[ ,]+/)
            i = index("janfebmaraprmayjunjulaugsepoctnovdec", substr(a[1], 1, 3))
            if (i == 0 || i % 3 != 1) exit
            mon = (i + 2) / 3; day = a[2] + 0
            s = substr(s, RLENGTH + 1)
        }
        if (!match(s, /[0-9]+(:[0-9]+)? *(am|pm)/)) exit
        t = substr(s, RSTART, RLENGTH)
        pm = (t ~ /pm/); gsub(/ *(am|pm)/, "", t)
        n = split(t, b, ":"); h = b[1] % 12; mi = (n > 1) ? b[2] + 0 : 0
        if (pm) h += 12
        print "abs", mon, day, h, mi
    }')
    case "$1" in
        rel) printf '%s\n' $((now + $2)) ;;
        abs)
            y=$(date +%Y)
            m=$2
            d=$3
            if [ "$m" = 0 ]; then
                m=$(date +%m); m=${m#0}
                d=$(date +%d); d=${d#0}
            fi
            e=$(to_epoch "$y" "$m" "$d" "$4" "$5")
            [ -n "$e" ] || return
            if [ "$e" -lt $((now - 3600)) ]; then
                if [ "$2" = 0 ]; then e=$((e + 86400)); else e=$(to_epoch $((y + 1)) "$m" "$d" "$4" "$5"); fi
            fi
            printf '%s\n' "$e"
            ;;
    esac
}

escape() { printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'; }

window() {
    rest=${1#*"$2"}
    pct=$(printf '%s\n' "$rest" | sed -n 's/^[[:space:]]*\([0-9][0-9.]*\)%.*/\1/p')
    [ -n "$pct" ] || return
    reset=$(printf '%s\n' "$rest" | sed -n 's/.*resets[[:space:]][[:space:]]*\([^(]*\).*/\1/p')
    e=$(epoch_of "$reset")
    j="{\"used_percentage\":$pct"
    [ -n "$e" ] && j="$j,\"resets_at\":$e"
    [ -n "$3" ] && j="$j,\"label\":\"$(escape "$3")\""
    printf '%s}' "$j"
}

payload=""
line=$(printf '%s\n' "$text" | grep -F -m 1 'Current session:')
[ -n "$line" ] && w=$(window "$line" 'Current session:') && [ -n "$w" ] && payload="$payload,\"five_hour\":$w"
line=$(printf '%s\n' "$text" | grep -F -m 1 'Current week (all models):')
[ -n "$line" ] && w=$(window "$line" 'Current week (all models):') && [ -n "$w" ] && payload="$payload,\"seven_day\":$w"
scoped=""
lines=$(printf '%s\n' "$text" | grep '^Current week (' | grep -v -F 'Current week (all models)')
if [ -n "$lines" ]; then
    while IFS= read -r line; do
        label=$(printf '%s\n' "$line" | sed -n 's/^Current week (\([^)]*\)):.*/\1/p')
        [ -n "$label" ] || continue
        w=$(window "$line" "($label):" "$label")
        [ -n "$w" ] && scoped="$scoped,$w"
    done <<EOF
$lines
EOF
fi
[ -n "$scoped" ] && payload="$payload,\"scoped\":[${scoped#,}]"
[ -n "$payload" ] || exit 0

active=$(awk -F '\t' -v n="$now" 'NF == 3 && $3 ~ /^[0-9]+$/ && n - $3 < 600 { gsub(/["\\]/, "", $2); printf ",\"%s\"", $2 }' "$seen" 2> /dev/null)
[ -n "$active" ] && payload="$payload,\"sessions\":[${active#,}]"
json="{${payload#,}}"

level() { awk -v p="$1" 'BEGIN { l = 0; if (p != "") { if (p + 0 >= 79.5) l = 80; if (p + 0 >= 89.5) l = 90; if (p + 0 >= 99.5) l = 100 } print l }'; }
pct_of() { printf '%s\n' "$text" | grep -F -m 1 "$1" | sed -n "s/^.*$2[[:space:]]*\([0-9][0-9.]*\)%.*/\1/p"; }
lvl="$(level "$(pct_of 'Current session:' 'session:')"),$(level "$(pct_of 'Current week (all models):' 'models):')")"
if [ -f "$dir/clawdboard-ag.txt" ]; then
    agl="$(tr -dc '0-9' < "$dir/clawdboard-ag.txt")"
    [ "${#agl}" -eq 4 ] && lvl="$lvl,$agl"
fi

hmac_hex() { openssl dgst -sha256 -mac HMAC -macopt "hexkey:$1" -binary | od -An -v -tx1 | tr -d ' \n'; }
seal() {
    ek=$(printf 'banditboard-enc' | hmac_hex "$1")
    mk=$(printf 'banditboard-mac' | hmac_hex "$1")
    iv=$(openssl rand -hex 16)
    box=$(mktemp "$tmp/clawdboard-box.XXXXXX") || return 1
    { printf '%s' "$iv" | xxd -r -p; printf '%s' "$json" | openssl enc -aes-256-cbc -K "$ek" -iv "$iv"; } > "$box"
    openssl dgst -sha256 -mac HMAC -macopt "hexkey:$mk" -binary < "$box" > "$box.tag"
    cat "$box" "$box.tag" | base64 | tr -d '\n'
    rm -f "$box" "$box.tag"
}

[ -f "$targets" ] || exit 0
tr -d '\r\n' < "$targets" | tr '}' '\n' | while IFS= read -r obj; do
    url=$(printf '%s\n' "$obj" | sed -n 's/.*"url"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
    key=$(printf '%s\n' "$obj" | sed -n 's/.*"key"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
    enc=$(printf '%s\n' "$obj" | sed -n 's/.*"enc"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
    [ -n "$url" ] && [ -n "$key" ] || continue
    if [ -n "$enc" ]; then
        blob=$(seal "$enc") || continue
        curl -s -m 8 -o /dev/null -X POST -H 'Content-Type: application/json' -H 'X-Clawdboard: 1' \
            -H "X-Clawdboard-Key: $key" --data-binary "{\"blob\":\"$blob\",\"lvl\":\"$lvl\"}" "$url/push"
    else
        curl -s -m 5 --noproxy '*' -o /dev/null -X POST -H 'Content-Type: application/json' -H 'X-Clawdboard: 1' \
            -H "X-Clawdboard-Key: $key" --data-binary "$json" "$url/api/push"
    fi
done
exit 0

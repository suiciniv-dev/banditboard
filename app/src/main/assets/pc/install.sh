#!/bin/sh
dir="$HOME/.claude"
script="$dir/clawdboard-usage.sh"
settings="$dir/settings.json"
targets="$dir/clawdboard-targets.json"
backup="$settings.antes-do-clawdboard"

mkdir -p "$dir" 2> /dev/null
[ -w "$dir" ] || exit 4
[ ! -f "$settings" ] || [ -w "$settings" ] || exit 4
if [ -f "$settings" ] && [ ! -f "$backup" ]; then cp "$settings" "$backup" || exit 4; fi

cat > "$script" << '__FIM_DO_USAGE__' || exit 4
__USAGE__
__FIM_DO_USAGE__
chmod 755 "$script"

entries=""
if [ -f "$targets" ]; then
    entries=$(tr -d '\r\n' < "$targets" | tr '}' '\n' | while IFS= read -r obj; do
        id=$(printf '%s\n' "$obj" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
        url=$(printf '%s\n' "$obj" | sed -n 's/.*"url"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
        key=$(printf '%s\n' "$obj" | sed -n 's/.*"key"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
        enc=$(printf '%s\n' "$obj" | sed -n 's/.*"enc"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
        [ -n "$url" ] && [ -n "$key" ] || continue
        if [ "$id" = '__ID__' ] || [ "$url" = '__URL__' ]; then continue; fi
        printf '  {"id": "%s", "url": "%s", "key": "%s", "enc": "%s"},\n' "$id" "$url" "$key" "$enc"
    done)
fi
{
    printf '[\n'
    [ -n "$entries" ] && printf '%s\n' "$entries"
    printf '  {"id": "%s", "url": "%s", "key": "%s", "enc": "%s"}\n]\n' '__ID__' '__URL__' '__KEY__' '__ENC__'
} > "$targets.tmp" && mv "$targets.tmp" "$targets" || exit 4

err="$dir/clawdboard-install.err"
if ! /usr/bin/osascript -l JavaScript - "$settings" "/bin/sh \"$script\"" > /dev/null 2> "$err" << '__FIM_DO_JS__'
function run(argv) {
    ObjC.import('Foundation')
    const path = argv[0]
    const command = argv[1]
    let settings = {}
    if ($.NSFileManager.defaultManager.fileExistsAtPath(path)) {
        const text = $.NSString.stringWithContentsOfFileEncodingError(path, $.NSUTF8StringEncoding, null)
        if (text.isNil()) throw new Error('read')
        if (text.js.trim()) settings = JSON.parse(text.js)
    }
    if (typeof settings.hooks !== 'object' || settings.hooks === null || Array.isArray(settings.hooks)) settings.hooks = {}
    for (const event of ['Stop', 'SessionStart']) {
        const old = Array.isArray(settings.hooks[event]) ? settings.hooks[event] : []
        const groups = old.filter(g => !JSON.stringify(g).includes('clawdboard-usage'))
        groups.push({ hooks: [{ type: 'command', command: command, timeout: 30 }] })
        settings.hooks[event] = groups
    }
    const out = $(JSON.stringify(settings, null, 2) + '\n')
    if (!out.writeToFileAtomicallyEncodingError(path, true, $.NSUTF8StringEncoding, null)) throw new Error('write')
}
__FIM_DO_JS__
then
    cat "$err" >&2
    if grep -q 'SyntaxError' "$err"; then rm -f "$err"; exit 3; fi
    rm -f "$err"
    exit 5
fi
rm -f "$err"

printf '\033[32m%s\033[0m\n' '__T_CONNECTED__'
[ -f "$backup" ] && printf '%s%s\n' '__T_BACKUP__' "$backup"
printf '%s\n' '__T_EVERY__'
exit 0

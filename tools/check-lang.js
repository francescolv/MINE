// Checks that the English texts written in MINE's code, as fallbacks of its translation keys
// (Component.translatableWithFallback("key", "text", ...)), are the same as en_us.json: the
// fallback is what a game without the translation shows, en_us.json what translators start from.
//
//   node tools/check-lang.js   fails if a key is missing from en_us.json or its text differs (CI)
'use strict';

const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const english = JSON.parse(fs.readFileSync(path.join(root, 'src/main/resources/assets/mine/lang/en_us.json'), 'utf8'));

function javaFiles(dir) {
	return fs.readdirSync(dir, {withFileTypes: true}).flatMap(entry => {
		const file = path.join(dir, entry.name);
		return entry.isDirectory() ? javaFiles(file) : entry.name.endsWith('.java') ? [file] : [];
	});
}

// A Java string literal's text.
function unescape(literal) {
	return literal.slice(1, -1).replace(/\\(u[0-9a-fA-F]{4}|.)/g, (all, c) => {
		if (c.length === 5) {
			return String.fromCharCode(parseInt(c.slice(1), 16));
		}
		return {n: '\n', t: '\t', r: '\r', b: '\b', f: '\f', '0': '\0'}[c] ?? c;
	});
}

const STRING = '"(?:[^"\\\\\\n]|\\\\.)*"';
// The key and the fallback, which may be made of literals joined with +.
const CALL = new RegExp('translatableWithFallback\\(\\s*(' + STRING + ')\\s*,\\s*(' + STRING + '(?:\\s*\\+\\s*' + STRING + ')*)', 'g');

let problems = 0;
for (const dir of ['src/main/java', 'src/client/java']) {
	for (const file of javaFiles(path.join(root, dir))) {
		const source = fs.readFileSync(file, 'utf8');
		for (const match of source.matchAll(CALL)) {
			const key = unescape(match[1]);
			const fallback = match[2].match(new RegExp(STRING, 'g')).map(unescape).join('');
			const where = path.relative(root, file) + ':' + source.slice(0, match.index).split('\n').length;
			if (!(key in english)) {
				console.error(where + ': ' + key + ' is not in en_us.json');
				problems++;
			} else if (english[key] !== fallback) {
				console.error(where + ': ' + key + '\n  code:       ' + JSON.stringify(fallback) + '\n  en_us.json: ' + JSON.stringify(english[key]));
				problems++;
			}
		}
	}
}
if (problems > 0) {
	console.error(problems + ' texts differ: make the code and en_us.json say the same.');
	process.exit(1);
}

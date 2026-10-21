// MINE's Python comes in two files: web/python/mine.py runs in the browser (MicroPython), and
// web/python/thonny/mine.py on the student's PC (Thonny). How they reach the server differs; what
// students call (byte, redstone, mine.say...) must not. This checks the part they share is the same.
//
//   node tools/check-mine-py.js   fails if the shared parts differ (CI)
'use strict';

const fs = require('fs');
const path = require('path');

const python = path.resolve(__dirname, '../src/main/resources/web/python');
const BEGIN = '# --- The same in';
const END = '# --- End of the part they share ---';

function shared(file) {
	const text = fs.readFileSync(path.join(python, file), 'utf8');
	const begin = text.indexOf(BEGIN);
	const end = text.indexOf(END);
	if (begin < 0 || end < begin) {
		console.error(file + ': the shared part is not marked (' + BEGIN + ' ... ' + END + ')');
		process.exit(1);
	}
	return text.slice(begin, end);
}

const browser = shared('mine.py').split('\n');
const thonny = shared('thonny/mine.py').split('\n');
for (let i = 0; i < Math.max(browser.length, thonny.length); i++) {
	if (browser[i] !== thonny[i]) {
		console.error('The shared part of the two mine.py differs at its line ' + (i + 1) + ':\n  mine.py:        '
			+ JSON.stringify(browser[i]) + '\n  thonny/mine.py: ' + JSON.stringify(thonny[i]));
		process.exit(1);
	}
}

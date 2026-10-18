// Runs a student's Python program with MicroPython, off the page: each call to Byte waits for the
// server's answer (a synchronous request, allowed in workers), so Python reads like Lua.
import { loadMicroPython } from './micropython.mjs';

let base = '';
let code = '';

function request(path, body) {
	const xhr = new XMLHttpRequest();
	xhr.open('POST', base + path + '?code=' + encodeURIComponent(code), false);
	xhr.setRequestHeader('Content-Type', 'application/json');
	xhr.send(JSON.stringify(body));
	return xhr.responseText;
}

self.onmessage = async event => {
	const { source, name, origin, session } = event.data;
	base = origin;
	code = session;
	const say = (kind, text) => self.postMessage({ kind, text });
	let calls = 0;
	let waited = 0;
	// The first call waits for Byte to start (the student goes back to the game, 3-2-1): told apart.
	let started = 0;
	const mp = await loadMicroPython({
		url: new URL('./micropython.wasm', import.meta.url).href,
		stdout: line => say('out', line),
		stderr: line => say('err', line),
		linebuffer: true,
	});
	const mine = await (await fetch(new URL('./mine.py', import.meta.url))).text();
	mp.FS.writeFile('/mine.py', mine);
	mp.registerJsModule('mine_js', {
		call: (fn, args) => {
			const start = performance.now();
			const answer = request('/api/py/call', { fn, args: JSON.parse(args) });
			const ms = performance.now() - start;
			if (!started) {
				started = ms;
			} else {
				calls++;
				waited += ms;
			}
			self.postMessage({ kind: 'call', fn, ms: Math.round(ms), calls, started: Math.round(started / 100) / 10,
				average: calls ? Math.round(waited / calls) : 0 });
			return answer;
		},
	});
	let error = '';
	try {
		mp.runPython('import mine\n');
		mp.runPython(source);
	} catch (e) {
		const message = String(e.message || e);
		// SystemExit: Byte was stopped (Ctrl+T, Stop, or MINE after a lesson's test). Not an error.
		if (!/SystemExit/.test(message)) {
			// The whole traceback: the page finds the line of the student's program in it.
			error = message.trim().split('\n').filter(line => line.trim()).join('\n');
		} else {
			say('stopped', '');
		}
	}
	request('/api/py/end', { error: error ? name + '.py: ' + error.split('\n').pop() : '' });
	self.postMessage({ kind: 'done', error, calls, average: calls ? Math.round(waited / calls) : 0 });
};

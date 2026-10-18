// Runs a Python program on a student's Byte the way MINE's editor does, without a browser:
// MicroPython (src/main/resources/web/python) in Node, each call to Byte a request to the server.
// The GameTests use it to try Python programs on the test server.
//
//   node tools/python-run.mjs <server> <student's code> <name> <program.py>
//
// Exit code: 0 the program ended, 1 a Python error, 2 Byte stopped it, 3 Run failed.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { Worker } from 'node:worker_threads';

const [server, code, name, file] = process.argv.slice(2);
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const web = path.join(root, 'src/main/resources/web/python');
const url = p => server + p + '?code=' + encodeURIComponent(code);

// The editor's server closes connections idle for a while: a request on one of them fails at
// once ("other side closed") and is made again, on a new one, as browsers do.
async function post(p, body) {
	for (let attempt = 1; ; attempt++) {
		try {
			const answer = await fetch(url(p), {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)});
			return await answer.json();
		} catch (e) {
			if (attempt >= 3) {
				throw e;
			}
		}
	}
}

// The calls wait for Byte: a worker makes the request, this thread sleeps until the answer.
const shared = new SharedArrayBuffer(8 + (1 << 20));
const flag = new Int32Array(shared, 0, 1);
const length = new Int32Array(shared, 4, 1);
const bytes = new Uint8Array(shared, 8);
const fetcher = new Worker(`
	const { parentPort, workerData } = require('node:worker_threads');
	const flag = new Int32Array(workerData, 0, 1), length = new Int32Array(workerData, 4, 1), bytes = new Uint8Array(workerData, 8);
	parentPort.on('message', async ({ to, body }) => {
		let text;
		for (let attempt = 1; text === undefined; attempt++) {
			try {
				text = await (await fetch(to, {method: 'POST', headers: {'Content-Type': 'application/json'}, body})).text();
			} catch (e) {
				// A connection the server had closed: again, on a new one.
				if (attempt >= 3) {
					text = JSON.stringify({ok: false, error: String(e)});
				}
			}
		}
		const encoded = new TextEncoder().encode(text);
		bytes.set(encoded);
		length[0] = encoded.length;
		Atomics.store(flag, 0, 1);
		Atomics.notify(flag, 0);
	});`, {eval: true, workerData: shared});

function call(fn, args) {
	Atomics.store(flag, 0, 0);
	fetcher.postMessage({to: url('/api/py/call'), body: JSON.stringify({fn, args: JSON.parse(args)})});
	Atomics.wait(flag, 0, 0);
	return new TextDecoder().decode(bytes.slice(0, length[0]));
}

// Run, again while Byte is starting up.
let run;
for (let i = 0; i < 60; i++) {
	run = await post('/api/py/run', {name});
	if (run.ok) break;
	await new Promise(resolve => setTimeout(resolve, 500));
}
if (!run.ok) {
	console.log('RUN FAILED ' + JSON.stringify(run));
	process.exit(3);
}

const { loadMicroPython } = await import(path.join(web, 'micropython.mjs'));
const mp = await loadMicroPython({url: path.join(web, 'micropython.wasm'), stdout: line => console.log(line), linebuffer: true});
mp.FS.writeFile('/mine.py', fs.readFileSync(path.join(web, 'mine.py'), 'utf8'));
mp.registerJsModule('mine_js', {call});
let error = '';
let exit = 0;
try {
	mp.runPython('import mine\n');
	mp.runPython(fs.readFileSync(file, 'utf8'));
} catch (e) {
	const message = String(e.message || e).trim();
	if (/SystemExit/.test(message)) {
		exit = 2;
	} else {
		error = message.split('\n').filter(line => line.trim()).pop();
		console.log('PYTHON ERROR ' + error);
		exit = 1;
	}
}
await post('/api/py/end', {error: error ? name + '.py: ' + error : ''});
await fetcher.terminate();
process.exit(exit);

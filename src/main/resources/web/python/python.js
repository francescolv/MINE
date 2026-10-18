// MINE's Python editor: write Python, Run it on Byte. The program runs in the browser with
// MicroPython (python-worker.js); each call (byte.forward()...) runs on Byte through the bridge.
// Programs are kept on the server like the blocks' ones; teachers can open their students'.
'use strict';

const TEXTS = {
	en: {
		title: 'MINE · Python', name: 'Program:', defaultName: 'python', save: 'Save', run: 'Run ▶', stop: 'Stop ■',
		blocks: 'Blocks', thonny: 'Thonny…', output: 'What the program prints',
		myPrograms: 'My programs', newProgram: '+ New program', students: 'Students\' programs…',
		viewing: '«%1» by %2: you can look at it, or copy it to your programs.', copy: 'Copy to my programs', backToMine: 'Back to mine',
		unsaved: 'Your program hasn\'t been saved: if you go on, the changes are lost. Go on?',
		saved: 'Saved.', sending: 'Starting…', starting: 'Go back to Minecraft: the program starts as soon as you are there.',
		running: 'Byte is running the program…', done: 'Done.', stopped: 'Byte stopped.', failed: 'The program stopped with an error.',
		noServer: 'The server didn\'t answer: is the game still open?',
		expired: 'This link has expired. In the game, type /mine blocks to open the editor again.',
		calls: (s, n, ms) => 'started after ' + s + ' s · then ' + n + ' calls to Byte, ' + ms + ' ms on average',
		step: 'step %1 of %2', hint: 'Hint', completed: 'Mission completed! In the game, answer the question with /mine lesson answer:',
		line: 'line %1',
		explain: {
			syntax: 'Python doesn\'t understand this line: check the brackets, the colon (:) at the end of for, if, while and def, and the quotes.',
			indent: 'The indentation (the spaces at the start of the line) doesn\'t match: the lines inside a for, if, while or def have 4 more spaces.',
			name: 'Python doesn\'t know «%1»: is it spelt right? Did you create it before using it, or is an import missing?',
			notByte: 'Byte has no function «%1»: check how it\'s written (help byte, in Byte\'s terminal).',
			type: 'A value of the wrong kind: for example a number where text is needed. Look at the line.',
			zero: 'A division by zero.',
			index: 'You asked for an element the list doesn\'t have: lists start at 0, and the last one is len(list) - 1.',
			notStarted: 'Byte didn\'t start the program: run it again, and go back to Minecraft.',
			silent: 'The program stopped talking to Byte: keep this page open while it runs.'
		},
		thonnyTitle: 'Python on your PC, with Thonny',
		thonnySteps: [
			'Make a folder for your Python programs on your PC, e.g. <code>MINE-Python</code> in your home.',
			'Download <code>mine.py</code> (the button below) and put it in that folder. Once is enough.',
			'In Thonny, save your programs in the same folder (e.g. <code>scale.py</code>), and press F5.',
			'The first time, MINE finds the school\'s server by itself and asks for your code: type it. It remembers it, in <code>mine.txt</code> in the folder.',
			'Go back to Minecraft: after 3-2-1 Byte starts. The program is also kept in My programs here.'
		],
		thonnyCode: 'Your code (it\'s yours for good; in the game: /mine python):', download: 'Download mine.py', close: 'Close',
		example: 'from mine import byte\nimport mine\n\nmine.say("Hello from Python!")\nfor i in range(3):\n    byte.up()\n    byte.placeDown()\n    byte.forward()\n'
	},
	it: {
		title: 'MINE · Python', name: 'Programma:', defaultName: 'python', save: 'Salva', run: 'Esegui ▶', stop: 'Ferma ■',
		blocks: 'Blocchi', thonny: 'Thonny…', output: 'Quello che scrive il programma',
		myPrograms: 'I miei programmi', newProgram: '+ Nuovo programma', students: 'Programmi degli studenti…',
		viewing: '«%1» di %2: puoi guardarlo, o copiarlo tra i tuoi programmi.', copy: 'Copia nei miei', backToMine: 'Torna ai miei',
		unsaved: 'Il programma non è salvato: se continui, perdi le modifiche. Continuo?',
		saved: 'Salvato.', sending: 'Avvio…', starting: 'Torna in Minecraft: il programma parte appena ci sei.',
		running: 'Byte sta eseguendo il programma…', done: 'Finito.', stopped: 'Byte si è fermato.', failed: 'Il programma si è fermato per un errore.',
		noServer: 'Il server non risponde: il gioco è ancora aperto?',
		expired: 'Questo collegamento è scaduto. Nel gioco scrivi /mine blocks per riaprire l\'editor.',
		calls: (s, n, ms) => 'partito dopo ' + s + ' s · poi ' + n + ' chiamate a Byte, in media ' + ms + ' ms',
		step: 'tappa %1 di %2', hint: 'Suggerimento', completed: 'Missione completata! Nel gioco rispondi alla domanda con /mine lesson answer:',
		line: 'riga %1',
		explain: {
			syntax: 'Python non capisce questa riga: controlla le parentesi, i due punti (:) alla fine di for, if, while e def, e le virgolette.',
			indent: 'Il rientro (gli spazi all\'inizio della riga) non torna: le righe dentro un for, if, while o def hanno 4 spazi in più.',
			name: 'Python non conosce «%1»: è scritto giusto? L\'hai creato prima di usarlo, o manca un import?',
			notByte: 'Byte non ha la funzione «%1»: controlla come è scritta (help byte, nel terminale di Byte).',
			type: 'Un valore del tipo sbagliato: per esempio un numero dove serve un testo. Guarda la riga.',
			zero: 'Una divisione per zero.',
			index: 'Hai chiesto un elemento che la lista non ha: le liste partono da 0, e l\'ultimo è len(lista) - 1.',
			notStarted: 'Byte non ha avviato il programma: eseguilo di nuovo, e torna in Minecraft.',
			silent: 'Il programma ha smesso di parlare con Byte: tieni aperta questa pagina mentre gira.'
		},
		thonnyTitle: 'Python sul tuo PC, con Thonny',
		thonnySteps: [
			'Crea sul PC una cartella per i tuoi programmi Python, per esempio <code>MINE-Python</code> nella tua home.',
			'Scarica <code>mine.py</code> (il pulsante qui sotto) e mettilo in quella cartella. Basta una volta.',
			'In Thonny salva i tuoi programmi nella stessa cartella (per esempio <code>scale.py</code>), e premi F5.',
			'La prima volta MINE trova da solo il server della scuola e ti chiede il codice: scrivilo. Se lo ricorda, in <code>mine.txt</code> nella cartella.',
			'Torna in Minecraft: dopo 3-2-1 Byte parte. Il programma resta anche tra I miei programmi, qui.'
		],
		thonnyCode: 'Il tuo codice (è tuo per sempre; nel gioco: /mine python):', download: 'Scarica mine.py', close: 'Chiudi',
		example: 'from mine import byte\nimport mine\n\nmine.say("Ciao da Python!")\nfor i in range(3):\n    byte.up()\n    byte.placeDown()\n    byte.forward()\n'
	}
};

const code = new URLSearchParams(location.search).get('code') || '';
const fromBlocks = new URLSearchParams(location.search).get('from') === 'blocks';
let t = TEXTS[(navigator.language || '').startsWith('it') ? 'it' : 'en'];
let session = null;
let editor;
/** The source as last loaded or saved: leaving it asks first. */
let clean = '';
/** Looking at a student's program (a teacher): {owner, name}; null for one's own. */
let viewing = null;
let worker = null;
let errorLine = null;

function $(id) {
	return document.getElementById(id);
}

function api(path, extra) {
	return path + '?kind=python&code=' + encodeURIComponent(code) + (extra || '');
}

function expired() {
	$('expired').textContent = t.expired;
	$('expired').hidden = false;
}

function status(text, error) {
	$('status').className = error ? 'error' : '';
	$('status').textContent = text;
}

function load(source) {
	editor.setValue(source || '');
	clean = editor.getValue();
	markError(null);
}

function canLeave() {
	return viewing !== null || editor.getValue() === clean || confirm(t.unsaved);
}

function fillPrograms(selected) {
	const list = $('programs');
	list.replaceChildren(new Option(t.myPrograms, ''), new Option(t.newProgram, '+'));
	for (const name of session.programs || []) {
		list.add(new Option(name, name));
	}
	list.value = (session.programs || []).includes(selected) ? selected : '';
}

function fillStudents() {
	if (!session.students || session.students.length === 0) {
		return;
	}
	const list = $('students');
	list.replaceChildren(new Option(t.students, ''));
	for (const student of session.students) {
		for (const name of student.programs) {
			list.add(new Option(student.name + ': ' + name, student.id + '/' + name));
		}
	}
	list.hidden = false;
}

function setViewing(what) {
	viewing = what;
	$('viewing').hidden = what === null;
	$('save').disabled = $('run').disabled = what !== null;
	editor.setOption('readOnly', what !== null);
	if (what !== null) {
		$('viewing-text').textContent = t.viewing.replace('%1', what.name).replace('%2', what.owner);
	}
}

async function fetchProgram(student, name) {
	const answer = await fetch(api('api/program', '&student=' + encodeURIComponent(student) + '&name=' + encodeURIComponent(name)));
	if (answer.status === 403) {
		expired();
		return null;
	}
	const program = await answer.json();
	return program.workspace === undefined ? null : program;
}

async function openOwn(name) {
	if (!canLeave()) {
		fillPrograms($('name').value);
		return;
	}
	setViewing(null);
	if (name === '+') {
		$('name').value = '';
		load('');
		fillPrograms('');
		$('name').focus();
		return;
	}
	const program = await fetchProgram(session.id, name);
	if (program) {
		$('name').value = program.name;
		load(program.workspace);
	}
}

async function openStudent(value) {
	$('students').value = '';
	const slash = value.indexOf('/');
	if (slash < 0 || !canLeave()) {
		return;
	}
	const program = await fetchProgram(value.substring(0, slash), value.substring(slash + 1));
	if (program) {
		load(program.workspace);
		setViewing({owner: program.owner, name: program.name});
	}
}

function showLesson(lesson) {
	$('lesson').hidden = !lesson;
	if (lesson) {
		$('lesson-title').textContent = lesson.title;
		$('lesson-step').textContent = lesson.completed ? '' : t.step.replace('%1', lesson.step).replace('%2', lesson.steps);
		$('lesson-goal').textContent = lesson.completed ? t.completed + ' ' + lesson.reflection : lesson.goal;
		$('hint').hidden = lesson.completed || !lesson.more;
		$('lesson-hints').replaceChildren(...(lesson.hints || []).map(hint => {
			const item = document.createElement('li');
			item.textContent = hint;
			return item;
		}));
	}
	if (editor) {
		editor.refresh();
	}
}

async function askHint() {
	$('hint').disabled = true;
	try {
		const answer = await fetch(api('api/hint'), {method: 'POST'});
		if (answer.status === 403) {
			expired();
			return;
		}
		showLesson((await answer.json()).lesson);
	} catch (e) {
		status(t.noServer, true);
	} finally {
		$('hint').disabled = false;
	}
}

/** Back from the game: the lesson may be at another step. The program being edited stays. */
async function refresh() {
	if (!session) {
		return;
	}
	try {
		const answer = await fetch(api('api/session'));
		if (answer.status === 403) {
			expired();
			return;
		}
		const fresh = await answer.json();
		session.programs = fresh.programs;
		session.students = fresh.students;
		fillPrograms(viewing === null ? $('name').value.trim() : '');
		fillStudents();
		showLesson(fresh.lesson);
	} catch (e) {
		// The server may be restarting: the next time will do.
	}
}

function out(text, kind) {
	const span = document.createElement('span');
	span.className = kind || '';
	span.textContent = text + '\n';
	$('output').append(span);
	$('output').scrollTop = $('output').scrollHeight;
}

function markError(line) {
	if (errorLine !== null) {
		editor.removeLineClass(errorLine, 'background', 'error-line');
		errorLine = null;
	}
	if (line !== null && line >= 0 && line < editor.lineCount()) {
		errorLine = line;
		editor.addLineClass(line, 'background', 'error-line');
		editor.scrollIntoView({line, ch: 0}, 80);
	}
}

/** What went wrong, in the student's words: Python's message, its line, and what it usually means. */
function explain(error) {
	const lines = error.split('\n');
	// The student's program is "<stdin>": its last line in the traceback (not mine.py's).
	const found = [...error.matchAll(/File "<stdin>", line (\d+)/g)];
	const line = found.length ? parseInt(found[found.length - 1][1], 10) : null;
	const last = lines[lines.length - 1];
	out((line ? t.line.replace('%1', line) + ' · ' : '') + last, 'err');
	const e = t.explain;
	let help = null;
	let match;
	if ((match = last.match(/^RuntimeError: (?:byte|redstone|mine|os)\.([\w.]+) isn't one of Byte's functions/))) {
		help = e.notByte.replace('%1', match[1]);
	} else if (/IndentationError|unindent|indent/.test(last)) {
		help = e.indent;
	} else if (/SyntaxError/.test(last)) {
		help = e.syntax;
	} else if ((match = last.match(/NameError: name '(\w+)'/))) {
		help = e.name.replace('%1', match[1]);
	} else if (/TypeError/.test(last)) {
		help = e.type;
	} else if (/ZeroDivisionError/.test(last)) {
		help = e.zero;
	} else if (/IndexError/.test(last)) {
		help = e.index;
	} else if (/didn't start the program/.test(last)) {
		help = e.notStarted;
	}
	if (help) {
		out(help, 'explain');
	}
	markError(line ? line - 1 : null);
}

function finished(text, error) {
	status(text, error);
	$('run').disabled = viewing !== null;
	$('save').disabled = viewing !== null;
	$('stop').disabled = true;
	worker = null;
}

async function save() {
	const name = $('name').value.trim().toLowerCase();
	try {
		const answer = await fetch(api('api/py/save'), {
			method: 'POST', headers: {'Content-Type': 'application/json'},
			body: JSON.stringify({name, source: editor.getValue()}),
		});
		if (answer.status === 403) {
			expired();
			return false;
		}
		const result = await answer.json();
		status(result.ok ? t.saved : result.message, !result.ok);
		if (result.ok) {
			clean = editor.getValue();
			session.programs = [name].concat((session.programs || []).filter(other => other !== name));
			fillPrograms(name);
		}
		return result.ok;
	} catch (e) {
		status(t.noServer, true);
		return false;
	}
}

async function run() {
	const name = $('name').value.trim().toLowerCase();
	const source = editor.getValue();
	$('output').textContent = '';
	$('timing').textContent = '';
	markError(null);
	$('run').disabled = $('save').disabled = true;
	status(t.sending);
	let answer;
	try {
		const reply = await fetch(api('api/py/run'), {
			method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({name, source}),
		});
		if (reply.status === 403) {
			expired();
			return;
		}
		answer = await reply.json();
	} catch (e) {
		finished(t.noServer, true);
		return;
	}
	if (!answer.ok) {
		finished(answer.message, true);
		return;
	}
	clean = source;
	session.programs = [name].concat((session.programs || []).filter(other => other !== name));
	fillPrograms(name);
	status(answer.message || t.starting);
	$('stop').disabled = false;
	worker = new Worker('python/python-worker.js', {type: 'module'});
	worker.onmessage = event => {
		const m = event.data;
		if (m.kind === 'out') {
			out(m.text);
		} else if (m.kind === 'call') {
			status(t.running);
			$('timing').textContent = t.calls(m.started, m.calls, m.average);
		} else if (m.kind === 'stopped') {
			out(t.stopped, 'note');
		} else if (m.kind === 'done') {
			worker.terminate();
			if (m.error) {
				explain(m.error);
			}
			finished(m.error ? t.failed : t.done, !!m.error);
		}
	};
	worker.postMessage({source, name, origin: location.origin, session: code});
}

function stop() {
	if (worker) {
		worker.terminate();
	}
	fetch(api('api/py/stop'), {method: 'POST', body: '{}'});
	out(t.stopped, 'note');
	finished(t.stopped);
}

async function start() {
	editor = CodeMirror($('editor'), {
		mode: {name: 'python', version: 3}, lineNumbers: true, indentUnit: 4, tabSize: 4, indentWithTabs: false,
		matchBrackets: true, autoCloseBrackets: true, viewportMargin: Infinity, lineWrapping: true,
		// Tab writes four spaces, as Python wants.
		extraKeys: {Tab: cm => cm.execCommand(cm.somethingSelected() ? 'indentMore' : 'insertSoftTab')},
	});
	try {
		const answer = await fetch(api('api/session'));
		session = answer.ok ? await answer.json() : null;
	} catch (e) {
		session = null;
	}
	// The student's language in the game wins; the browser's only when the session is gone.
	if (session) {
		t = TEXTS[session.language === 'it' ? 'it' : 'en'];
	}
	document.title = t.title;
	document.documentElement.lang = session ? session.language : 'en';
	$('name-label').textContent = t.name;
	$('save').textContent = t.save;
	$('run').textContent = t.run;
	$('stop').textContent = t.stop;
	$('to-blocks').textContent = t.blocks;
	$('to-blocks').href = './?code=' + encodeURIComponent(code);
	$('thonny').textContent = t.thonny;
	$('thonny-title').textContent = t.thonnyTitle;
	$('thonny-steps').innerHTML = t.thonnySteps.map(step => '<li>' + step + '</li>').join('');
	$('thonny-code-label').textContent = t.thonnyCode;
	$('thonny-download').textContent = t.download;
	$('thonny-close').textContent = t.close;
	$('thonny').onclick = () => $('thonny-dialog').showModal();
	$('thonny-close').onclick = () => $('thonny-dialog').close();
	$('output-title').textContent = t.output;
	$('copy').textContent = t.copy;
	$('back').textContent = t.backToMine;
	$('hint').textContent = t.hint;
	$('programs').setAttribute('aria-label', t.myPrograms);
	$('students').setAttribute('aria-label', t.students);
	if (!session) {
		expired();
		return;
	}
	$('who').textContent = session.player;
	$('thonny-code').textContent = session.thonny || '';
	let source = session.workspace;
	let name = session.name || t.defaultName;
	if (fromBlocks) {
		// "Continue in Python" in the block editor: the blocks, as Python, become a new program.
		try {
			const handed = JSON.parse(localStorage.getItem('mine.fromBlocks') || 'null');
			localStorage.removeItem('mine.fromBlocks');
			if (handed) {
				source = handed.source;
				name = (handed.name || t.defaultName) + '_py';
			}
		} catch (e) {
			// No local storage: the last program opens.
		}
		history.replaceState(null, '', 'python?code=' + encodeURIComponent(code));
	}
	$('name').value = name.substring(0, 24);
	load(source || t.example);
	if (fromBlocks) {
		clean = '';
	}
	fillPrograms(session.name);
	fillStudents();
	showLesson(session.lesson);
	window.addEventListener('focus', refresh);
	$('programs').onchange = () => openOwn($('programs').value);
	$('students').onchange = () => openStudent($('students').value);
	$('copy').onclick = () => {
		$('name').value = viewing.name;
		setViewing(null);
		clean = '';
	};
	$('back').onclick = () => {
		setViewing(null);
		openOwn($('name').value || '+');
	};
	$('save').onclick = save;
	$('run').onclick = run;
	$('stop').onclick = stop;
	$('hint').onclick = askHint;
	editor.on('change', () => markError(null));
}

start();

// MINE's block editor: blocks for the student's Byte, the Lua they make, and sending it to the
// Byte through the class server. The page is opened by /mine blocks with the student's code.
'use strict';

const TEXTS = {
	en: {
		title: 'MINE · Blocks',
		agent: 'Byte', logic: 'Logic', loops: 'Loops', math: 'Math', text: 'Text', variables: 'Variables', functions: 'Functions',
		name: 'Program:', defaultName: 'blocks', send: 'Send to Byte', run: 'Run ▶', showCode: 'Lua', codeTitle: 'The Lua your blocks make',
		sending: 'Sending…', expired: 'This link has expired. In the game, type /mine blocks to open the editor again.',
		noServer: 'The server didn\'t answer: is the game still open?',
		forward: 'forward', back: 'back', up: 'up', down: 'down', left: 'left', right: 'right',
		front: 'in front', above: 'above', below: 'below',
		move: 'move %1', turn: 'turn %1', place: 'place a block %1', dig: 'dig %1', detect: 'block %1?',
		select: 'use slot %1', say: 'say %1', hello: 'Hello!', challenge: 'the challenge\'s number',
		challengeAt: 'the challenge\'s number %1', ripe: 'ripe crop below?',
		compare: 'block %1 same as the slot\'s?', count: 'blocks in the slot',
		redstone: 'Redstone', setSignal: 'redstone signal %1 set to %2', getSignal: 'signal from %1?',
		waitSignal: 'wait for a redstone change', wait: 'wait %1 seconds', back2: 'behind', top: 'on top', bottom: 'underneath',
		device: 'Board', deviceRead: 'board value %1', deviceSend: 'send to the board %1 %2', deviceWait: 'wait for %1 from the board',
		deviceConnected: 'board connected?',
		openFile: 'Open file', saveFile: 'Save file', badFile: 'This file isn\'t a program made with MINE\'s blocks.',
		myPrograms: 'My programs', newProgram: '+ New program', students: 'Students\' programs…',
		viewing: '«%1» by %2: you can look at it, or copy it to your programs.', copy: 'Copy to my programs', backToMine: 'Back to mine',
		unsaved: 'Your blocks haven\'t been sent yet: if you go on, they are lost. Go on?',
		step: 'step %1 of %2', hint: 'Hint', completed: 'Mission completed! In the game, answer the question with /mine lesson answer:',
		codeTitlePython: 'Your blocks in Python', toPython: 'Continue in Python ▸'
	},
	it: {
		title: 'MINE · Blocchi',
		agent: 'Byte', logic: 'Logica', loops: 'Cicli', math: 'Matematica', text: 'Testo', variables: 'Variabili', functions: 'Funzioni',
		name: 'Programma:', defaultName: 'blocchi', send: 'Invia a Byte', run: 'Esegui ▶', showCode: 'Lua', codeTitle: 'Il Lua dei tuoi blocchi',
		sending: 'Invio…', expired: 'Questo collegamento è scaduto. Nel gioco scrivi /mine blocks per riaprire l\'editor.',
		noServer: 'Il server non risponde: il gioco è ancora aperto?',
		forward: 'avanti', back: 'indietro', up: 'su', down: 'giù', left: 'sinistra', right: 'destra',
		front: 'davanti', above: 'sopra', below: 'sotto',
		move: 'vai %1', turn: 'gira a %1', place: 'piazza un blocco %1', dig: 'scava %1', detect: 'blocco %1?',
		select: 'usa lo slot %1', say: 'di\' %1', hello: 'Ciao!', challenge: 'il numero della sfida',
		challengeAt: 'il numero %1 della sfida', ripe: 'raccolto maturo sotto?',
		compare: 'blocco %1 uguale a quello dello slot?', count: 'blocchi nello slot',
		redstone: 'Redstone', setSignal: 'segnale redstone %1 a %2', getSignal: 'segnale da %1?',
		waitSignal: 'aspetta un cambio di redstone', wait: 'aspetta %1 secondi', back2: 'dietro', top: 'sopra', bottom: 'sotto',
		device: 'Scheda', deviceRead: 'valore della scheda %1', deviceSend: 'manda alla scheda %1 %2', deviceWait: 'aspetta %1 dalla scheda',
		deviceConnected: 'scheda collegata?',
		openFile: 'Apri file', saveFile: 'Salva file', badFile: 'Questo file non è un programma fatto con i blocchi di MINE.',
		myPrograms: 'I miei programmi', newProgram: '+ Nuovo programma', students: 'Programmi degli studenti…',
		viewing: '«%1» di %2: puoi guardarlo, o copiarlo tra i tuoi programmi.', copy: 'Copia nei miei', backToMine: 'Torna ai miei',
		unsaved: 'I tuoi blocchi non sono ancora stati inviati: se continui, li perdi. Continuo?',
		step: 'tappa %1 di %2', hint: 'Suggerimento', completed: 'Missione completata! Nel gioco rispondi alla domanda con /mine lesson answer:',
		codeTitlePython: 'I tuoi blocchi in Python', toPython: 'Continua in Python ▸'
	}
};

const code = new URLSearchParams(location.search).get('code') || '';
/** For the server: the block editor's programs and words (the default). */
const KIND = '';
// Python: its own page, with the same session.
document.getElementById('python').href = 'python?code=' + encodeURIComponent(code);
const AGENT_COLOUR = 30;
const REDSTONE_COLOUR = 0;
const DEVICE_COLOUR = 180;
let t = TEXTS.en;
let workspace;
let session;
/** The blocks as they were last loaded or sent: leaving others asks first. */
let clean = '';
/** Looking at a student's program (a teacher): {owner, name}; null for one's own. */
let viewing = null;

function loadScript(src) {
	return new Promise((resolve, reject) => {
		const script = document.createElement('script');
		script.src = src;
		script.onload = resolve;
		script.onerror = reject;
		document.head.appendChild(script);
	});
}

// Byte's blocks: each one is a line of Lua with byte (CC: Tweaked's turtle API, under Byte's name) or MINE's mine API.
function defineBlocks() {
	const where = [[t.front, ''], [t.above, 'Up'], [t.below, 'Down']];
	// Byte's sides, as CC: Tweaked's redstone API names them.
	const sides = [[t.back2, 'back'], [t.front, 'front'], [t.left, 'left'], [t.right, 'right'], [t.top, 'top'], [t.bottom, 'bottom']];
	Blockly.defineBlocksWithJsonArray([
		{type: 'mine_move', message0: t.move, args0: [{type: 'field_dropdown', name: 'WAY',
			options: [[t.forward, 'forward'], [t.back, 'back'], [t.up, 'up'], [t.down, 'down']]}],
		previousStatement: null, nextStatement: null, colour: AGENT_COLOUR},
		{type: 'mine_turn', message0: t.turn, args0: [{type: 'field_dropdown', name: 'WAY',
			options: [[t.left, 'Left'], [t.right, 'Right']]}],
		previousStatement: null, nextStatement: null, colour: AGENT_COLOUR},
		{type: 'mine_place', message0: t.place, args0: [{type: 'field_dropdown', name: 'WHERE', options: where}],
			previousStatement: null, nextStatement: null, colour: AGENT_COLOUR},
		{type: 'mine_dig', message0: t.dig, args0: [{type: 'field_dropdown', name: 'WHERE', options: where}],
			previousStatement: null, nextStatement: null, colour: AGENT_COLOUR},
		{type: 'mine_detect', message0: t.detect, args0: [{type: 'field_dropdown', name: 'WHERE', options: where}],
			output: 'Boolean', colour: AGENT_COLOUR},
		{type: 'mine_compare', message0: t.compare, args0: [{type: 'field_dropdown', name: 'WHERE', options: where}],
			output: 'Boolean', colour: AGENT_COLOUR},
		{type: 'mine_count', message0: t.count, output: 'Number', colour: AGENT_COLOUR},
		{type: 'mine_select', message0: t.select, args0: [{type: 'field_number', name: 'SLOT', value: 1, min: 1, max: 16, precision: 1}],
			previousStatement: null, nextStatement: null, colour: AGENT_COLOUR},
		{type: 'mine_say', message0: t.say, args0: [{type: 'input_value', name: 'TEXT'}],
			previousStatement: null, nextStatement: null, colour: AGENT_COLOUR},
		{type: 'mine_challenge', message0: t.challenge, output: 'Number', colour: AGENT_COLOUR},
		{type: 'mine_challenge_at', message0: t.challengeAt,
			args0: [{type: 'field_number', name: 'N', value: 1, min: 1, max: 16, precision: 1}], output: 'Number', colour: AGENT_COLOUR},
		{type: 'mine_ripe', message0: t.ripe, output: 'Boolean', colour: AGENT_COLOUR},
		{type: 'mine_set_signal', message0: t.setSignal, args0: [{type: 'field_dropdown', name: 'SIDE', options: sides},
			{type: 'input_value', name: 'ON', check: 'Boolean'}], inputsInline: true,
		previousStatement: null, nextStatement: null, colour: REDSTONE_COLOUR},
		{type: 'mine_get_signal', message0: t.getSignal, args0: [{type: 'field_dropdown', name: 'SIDE', options: sides}],
			output: 'Boolean', colour: REDSTONE_COLOUR},
		{type: 'mine_wait_signal', message0: t.waitSignal, previousStatement: null, nextStatement: null, colour: REDSTONE_COLOUR},
		{type: 'mine_device_read', message0: t.deviceRead, args0: [{type: 'field_input', name: 'KEY', text: 'pulsante_a'}],
			output: null, colour: DEVICE_COLOUR},
		{type: 'mine_device_send', message0: t.deviceSend, args0: [{type: 'field_input', name: 'KEY', text: 'cuore'},
			{type: 'input_value', name: 'VALUE'}], inputsInline: true,
		previousStatement: null, nextStatement: null, colour: DEVICE_COLOUR},
		{type: 'mine_device_wait', message0: t.deviceWait, args0: [{type: 'field_input', name: 'KEY', text: 'pulsante_a'}],
			previousStatement: null, nextStatement: null, colour: DEVICE_COLOUR},
		{type: 'mine_device_connected', message0: t.deviceConnected, output: 'Boolean', colour: DEVICE_COLOUR},
		// Not only for redstone: with the loops, as Scratch keeps "wait" with them.
		{type: 'mine_wait', message0: t.wait, args0: [{type: 'input_value', name: 'SECONDS', check: 'Number'}], inputsInline: true,
			previousStatement: null, nextStatement: null, style: 'loop_blocks'}
	]);
	generate(window.lua.luaGenerator, window.lua.Order.HIGH, window.lua.Order.NONE, {no: 'false', sleep: 'os.sleep'});
	if (window.python) {
		const py = window.python.pythonGenerator;
		// Four spaces, as Python programs are written (Blockly's default is two).
		py.INDENT = '    ';
		// The same names as in Lua (see web/python/mine.py).
		generate(py, window.python.Order.FUNCTION_CALL, window.python.Order.NONE, {no: 'False', sleep: 'time.sleep'});
	}
}

// A board's keys: lower-case letters, digits and _ (as the board writes them, key:value).
function boardKey(block) {
	return JSON.stringify(String(block.getFieldValue('KEY')).trim().toLowerCase());
}

/**
 * What each of Byte's blocks writes, in Lua and in Python: a statement (a line) or an expression
 * (a value). {@code field} reads one of the block's fields, {@code input} the code of one of its
 * inputs (or a default), and {@code words} what the two languages say differently.
 */
const BLOCK_CODE = {
	mine_move: {statement: (field) => 'byte.' + field('WAY') + '()'},
	mine_turn: {statement: (field) => 'byte.turn' + field('WAY') + '()'},
	mine_place: {statement: (field) => 'byte.place' + field('WHERE') + '()'},
	mine_dig: {statement: (field) => 'byte.dig' + field('WHERE') + '()'},
	mine_detect: {expression: (field) => 'byte.detect' + field('WHERE') + '()'},
	mine_compare: {expression: (field) => 'byte.compare' + field('WHERE') + '()'},
	mine_count: {expression: () => 'byte.getItemCount()'},
	mine_select: {statement: (field) => 'byte.select(' + field('SLOT') + ')'},
	mine_challenge: {expression: () => 'mine.challenge()'},
	mine_challenge_at: {expression: (field) => 'mine.challenge(' + field('N') + ')'},
	mine_ripe: {expression: () => 'byte.ripeDown()'},
	mine_set_signal: {statement: (field, input, words) => 'redstone.setOutput("' + field('SIDE') + '", ' + input('ON', words.no) + ')'},
	mine_get_signal: {expression: (field) => 'redstone.getInput("' + field('SIDE') + '")'},
	mine_wait_signal: {statement: () => 'os.pullEvent("redstone")'},
	mine_wait: {statement: (field, input, words) => words.sleep + '(' + input('SECONDS', '1') + ')'},
	mine_device_read: {expression: (field, input, words, block) => 'mine.device.read(' + boardKey(block) + ')'},
	mine_device_send: {statement: (field, input, words, block) => 'mine.device.send(' + boardKey(block) + ', ' + input('VALUE', '0') + ')'},
	mine_device_wait: {statement: (field, input, words, block) => 'mine.device.wait(' + boardKey(block) + ')'},
	mine_device_connected: {expression: () => 'mine.device.connected()'},
	mine_say: {statement: (field, input) => 'mine.say(' + input('TEXT', '""') + ')'}
};

/** Teaches {@code generator} (Lua's or Python's) Byte's blocks: calls bind as {@code call}, inputs are read at {@code none}. */
function generate(generator, call, none, words) {
	for (const [type, code] of Object.entries(BLOCK_CODE)) {
		generator.forBlock[type] = (block, gen) => {
			const field = name => block.getFieldValue(name);
			const input = (name, otherwise) => gen.valueToCode(block, name, none) || otherwise;
			return code.expression ? [code.expression(field, input, words, block), call]
				: code.statement(field, input, words, block) + '\n';
		};
	}
}

/** The blocks as a Python program, with the imports it needs. */
function pythonCode() {
	const body = window.python.pythonGenerator.workspaceToCode(workspace);
	const names = ['byte', 'redstone'].filter(name => body.includes(name + '.'));
	const imports = [];
	if (names.length) {
		imports.push('from mine import ' + names.join(', '));
	}
	for (const module of ['mine', 'time', 'os']) {
		if (body.includes(module + '.')) {
			imports.push('import ' + module);
		}
	}
	return (imports.length ? imports.join('\n') + '\n\n' : '') + body;
}

function toolbox() {
	const number = n => ({shadow: {type: 'math_number', fields: {NUM: n}}});
	return {kind: 'categoryToolbox', contents: [
		{kind: 'category', name: t.agent, colour: AGENT_COLOUR, contents: [
			{kind: 'block', type: 'mine_move'},
			{kind: 'block', type: 'mine_turn'},
			{kind: 'block', type: 'mine_place'},
			{kind: 'block', type: 'mine_dig'},
			{kind: 'block', type: 'mine_detect'},
			{kind: 'block', type: 'mine_compare', fields: {WHERE: 'Down'}},
			{kind: 'block', type: 'mine_select'},
			{kind: 'block', type: 'mine_count'},
			{kind: 'block', type: 'mine_say', inputs: {TEXT: {shadow: {type: 'text', fields: {TEXT: t.hello}}}}},
			{kind: 'block', type: 'mine_challenge'},
			{kind: 'block', type: 'mine_challenge_at'},
			{kind: 'block', type: 'mine_ripe'}
		]},
		{kind: 'category', name: t.redstone, colour: REDSTONE_COLOUR, contents: [
			{kind: 'block', type: 'mine_set_signal', inputs: {ON: {shadow: {type: 'logic_boolean', fields: {BOOL: 'TRUE'}}}}},
			{kind: 'block', type: 'mine_get_signal'},
			{kind: 'block', type: 'mine_wait_signal'}
		]},
		{kind: 'category', name: t.device, colour: DEVICE_COLOUR, contents: [
			{kind: 'block', type: 'mine_device_wait'},
			{kind: 'block', type: 'mine_device_read', fields: {KEY: 'luce'}},
			{kind: 'block', type: 'mine_device_send', inputs: {VALUE: number(1)}},
			{kind: 'block', type: 'mine_device_connected'}
		]},
		{kind: 'category', name: t.loops, categorystyle: 'loop_category', contents: [
			{kind: 'block', type: 'controls_repeat_ext', inputs: {TIMES: number(4)}},
			{kind: 'block', type: 'controls_whileUntil'},
			{kind: 'block', type: 'controls_for', inputs: {FROM: number(1), TO: number(10), BY: number(1)}},
			{kind: 'block', type: 'mine_wait', inputs: {SECONDS: number(1)}}
		]},
		{kind: 'category', name: t.logic, categorystyle: 'logic_category', contents: [
			{kind: 'block', type: 'controls_if'},
			{kind: 'block', type: 'logic_compare'},
			{kind: 'block', type: 'logic_operation'},
			{kind: 'block', type: 'logic_negate'},
			{kind: 'block', type: 'logic_boolean'}
		]},
		{kind: 'category', name: t.math, categorystyle: 'math_category', contents: [
			{kind: 'block', type: 'math_number'},
			{kind: 'block', type: 'math_arithmetic'},
			{kind: 'block', type: 'math_random_int', inputs: {FROM: number(1), TO: number(6)}}
		]},
		{kind: 'category', name: t.text, categorystyle: 'text_category', contents: [
			{kind: 'block', type: 'text'},
			{kind: 'block', type: 'text_join'}
		]},
		{kind: 'category', name: t.variables, categorystyle: 'variable_category', custom: 'VARIABLE'},
		{kind: 'category', name: t.functions, categorystyle: 'procedure_category', custom: 'PROCEDURE'}
	]};
}

/** The language the code pane shows: the Lua Byte runs, or the same blocks in Python. */
let codeLanguage = 'lua';

function showCode() {
	$('code').textContent = codeLanguage === 'python' ? pythonCode() : window.lua.luaGenerator.workspaceToCode(workspace);
	$('code-title').textContent = codeLanguage === 'python' ? t.codeTitlePython : t.codeTitle;
	$('code-lua').classList.toggle('chosen', codeLanguage === 'lua');
	$('code-python').classList.toggle('chosen', codeLanguage === 'python');
	$('to-python').hidden = codeLanguage !== 'python';
}

function current() {
	return JSON.stringify(Blockly.serialization.workspaces.save(workspace));
}

function lockEditing(locked) {
	$('send').disabled = $('run').disabled = locked;
}

function relayout() {
	if (workspace) {
		Blockly.svgResize(workspace);
	}
}

function load(json) {
	workspace.clear();
	if (json) {
		try {
			Blockly.serialization.workspaces.load(JSON.parse(json), workspace);
		} catch (e) {
			console.warn('Could not load the program', e);
		}
	}
	clean = current();
	showCode();
}

function fillPrograms(selected) {
	const list = $('programs');
	list.replaceChildren();
	list.add(new Option(t.myPrograms, '', true, false));
	list.options[0].disabled = true;
	for (const name of session.programs || []) {
		list.add(new Option(name, name, false, name === selected));
	}
	list.add(new Option(t.newProgram, '+'));
	if (!selected) {
		list.selectedIndex = 0;
	}
}

async function send(run) {
	const name = $('name').value.trim().toLowerCase();
	$('send').disabled = $('run').disabled = true;
	status(t.sending);
	try {
		const answer = await fetch(api('api/send'), {
			method: 'POST',
			headers: {'Content-Type': 'application/json'},
			body: JSON.stringify({
				name: name,
				lua: window.lua.luaGenerator.workspaceToCode(workspace),
				workspace: JSON.stringify(Blockly.serialization.workspaces.save(workspace)),
				run: run
			})
		});
		if (answer.status === 403) {
			expired();
			return;
		}
		const result = await answer.json();
		status(result.message, !result.ok);
		if (result.ok) {
			clean = current();
			session.programs = [name].concat((session.programs || []).filter(other => other !== name));
			fillPrograms(name);
		}
	} catch (e) {
		status(t.noServer, true);
	} finally {
		$('send').disabled = $('run').disabled = false;
	}
}

async function start() {
	session = null;
	try {
		const answer = await fetch(api('api/session'));
		session = answer.ok ? await answer.json() : null;
	} catch (e) {
		session = null;
	}
	// Blockly's own words (repeat, if, ...) come in a file per language.
	const language = session && session.language === 'it' ? 'it' : 'en';
	t = TEXTS[language];
	await loadScript('blockly/msg/' + language + '.js');
	document.title = t.title;
	$('programs').setAttribute('aria-label', t.myPrograms);
	$('students').setAttribute('aria-label', t.students);
	document.documentElement.lang = session ? session.language : 'en';
	$('name-label').textContent = t.name;
	$('send').textContent = t.send;
	$('run').textContent = t.run;
	$('toggle-code').textContent = t.showCode;
	$('code-title').textContent = t.codeTitle;
	if (!session) {
		expired();
		return;
	}
	$('who').textContent = session.player;
	$('name').value = session.name || t.defaultName;
	$('copy').textContent = t.copy;
	$('open-file').textContent = t.openFile;
	$('save-file').textContent = t.saveFile;
	$('back').textContent = t.backToMine;
	$('hint').textContent = t.hint;
	$('hint').onclick = askHint;
	fillPrograms(session.name);
	fillStudents();

	defineBlocks();
	workspace = Blockly.inject('blocks', {
		toolbox: toolbox(),
		media: 'blockly/media/',
		trashcan: true,
		zoom: {controls: true, wheel: true, startScale: 1.0},
		grid: {spacing: 24, length: 3, colour: '#dddddd', snap: true}
	});
	load(session.workspace);
	showLesson(session.lesson);
	window.addEventListener('focus', refresh);
	// The code pane follows the blocks, once they stop moving for a moment (a drag changes them at every step).
	let codeTimer = null;
	workspace.addChangeListener(event => {
		if (!event.isUiEvent) {
			clearTimeout(codeTimer);
			codeTimer = setTimeout(showCode, 150);
		}
	});
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
	// Programs as files: the lessons' solutions, or a program to share.
	$('open-file').onclick = () => {
		if (canLeave()) {
			$('file').click();
		}
	};
	$('file').onchange = async () => {
		const file = $('file').files[0];
		$('file').value = '';
		if (!file) {
			return;
		}
		try {
			const json = await file.text();
			JSON.parse(json);
			setViewing(null);
			$('name').value = file.name.replace(/\.json$/, '').toLowerCase().replace(/[^a-z0-9_]/g, '_').substring(0, 24);
			load(json);
			clean = '';
		} catch (e) {
			$('status').className = 'error';
			$('status').textContent = t.badFile;
		}
	};
	$('save-file').onclick = () => {
		const link = document.createElement('a');
		link.href = URL.createObjectURL(new Blob([JSON.stringify(Blockly.serialization.workspaces.save(workspace), null, '\t')],
			{type: 'application/json'}));
		link.download = ($('name').value.trim() || t.defaultName) + '.json';
		link.click();
		URL.revokeObjectURL(link.href);
	};
	window.addEventListener('beforeunload', event => {
		if (viewing === null && current() !== clean) {
			event.preventDefault();
		}
	});
	$('send').onclick = () => send(false);
	$('run').onclick = () => send(true);
	$('code-lua').onclick = () => {
		codeLanguage = 'lua';
		showCode();
	};
	$('code-python').onclick = () => {
		codeLanguage = 'python';
		showCode();
	};
	// The blocks as a new Python program: the Python page finds it here and opens it.
	$('to-python').textContent = t.toPython;
	$('to-python').onclick = () => {
		try {
			localStorage.setItem('mine.fromBlocks', JSON.stringify({name: $('name').value, source: pythonCode()}));
		} catch (e) {
			console.warn('No local storage', e);
		}
		location.href = 'python?code=' + encodeURIComponent(code) + '&from=blocks';
	};
	$('toggle-code').onclick = () => {
		$('code-pane').hidden = !$('code-pane').hidden;
		Blockly.svgResize(workspace);
	};
}

start();

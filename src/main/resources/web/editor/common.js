// What the block editor (editor.js) and the Python editor (python.js) share: the session with the
// server, the student's lesson, their programs and, for a teacher, the students' ones. Loaded
// before the page's own script, which declares the state (code, t, session, clean, viewing, KIND)
// and these functions for its editor:
//   current()           the program being edited, as it is saved
//   load(program)       shows a program in the editor
//   fillPrograms(name)  the list of the student's programs, with name selected
//   lockEditing(locked) a student's program is read only: nothing to save or run
//   relayout()          the lesson box changed size: the editor fits the page again
'use strict';

function $(id) {
	return document.getElementById(id);
}

/** A path of the server's API, for this student (and this kind of editor), with more parameters in {@code extra}. */
function api(path, extra) {
	return path + '?' + (KIND ? 'kind=' + KIND + '&' : '') + 'code=' + encodeURIComponent(code) + (extra || '');
}

function expired() {
	$('expired').textContent = t.expired;
	$('expired').hidden = false;
}

function status(text, error) {
	$('status').className = error ? 'error' : '';
	$('status').textContent = text;
}

function canLeave() {
	return viewing !== null || current() === clean || confirm(t.unsaved);
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

/** Looking at a student's program ({owner, name}), or at one's own (null). */
function setViewing(what) {
	viewing = what;
	$('viewing').hidden = what === null;
	lockEditing(what !== null);
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

/** The student's lesson: its step, the goal, the hints asked for so far and a button for the next. */
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
	relayout();
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

/**
 * Back from the game: the lesson may be at another step, and new programs may be there (a lesson
 * gives some). The program being edited stays as it is.
 */
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

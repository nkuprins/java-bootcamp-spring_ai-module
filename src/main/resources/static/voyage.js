const request = async (url, options = {}) => {
    const response = await fetch(url, options);
    if (!response.ok) {
        const problem = await response.json().catch(() => ({}));
        throw new Error(problem.detail || `Request failed (${response.status})`);
    }
    return response.json();
};

const postJson = (url, body) => request(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
});

const api = {
    create: () => request('/api/voyages', { method: 'POST' }),
    get: (id) => request(`/api/voyages/${id}`),
    log: (id) => request(`/api/voyages/${id}/log`),
    turn: (id) => request(`/api/voyages/${id}/turns`, { method: 'POST' }),
    event: (id, type, location) => postJson(`/api/voyages/${id}/events`, { type, location: location || null }),
    transmissions: (id) => request(`/api/voyages/${id}/transmissions`),
    transmit: (id, message) => postJson(`/api/voyages/${id}/transmissions`, { message }),
    interview: (id, crewId, question) => postJson(`/api/voyages/${id}/crew/${crewId}/interview`, { question })
};

const els = {
    lamp: document.getElementById('lamp'),
    hour: document.getElementById('hour'),
    status: document.getElementById('status-line'),
    nextTurn: document.getElementById('next-turn'),
    auto: document.getElementById('auto'),
    newVoyage: document.getElementById('new-voyage'),
    gauges: document.getElementById('gauges'),
    lost: document.getElementById('lost-banner'),
    crew: document.getElementById('crew'),
    log: document.getElementById('log'),
    eventForm: document.getElementById('event-form'),
    eventType: document.getElementById('event-type'),
    eventLocation: document.getElementById('event-location'),
    transmissionForm: document.getElementById('transmission-form'),
    transmissionMessage: document.getElementById('transmission-message'),
    transmissions: document.getElementById('transmissions'),
    dialog: document.getElementById('interview'),
    dialogTitle: document.getElementById('interview-title'),
    dialogClose: document.getElementById('interview-close'),
    qa: document.getElementById('qa'),
    interviewForm: document.getElementById('interview-form'),
    interviewQuestion: document.getElementById('interview-question')
};

const ROOMS = {
    BRIDGE: 'Bridge', ENGINEERING: 'Engineering', HYDROPONICS: 'Hydroponics', MEDBAY: 'Medbay',
    COMMS_ARRAY: 'Comms array', MESS_HALL: 'Mess hall', QUARTERS: 'Quarters'
};
const SYSTEMS = [['oxygen', 'OXYGEN'], ['power', 'POWER'], ['hull', 'HULL'], ['food', 'FOOD']];
const STORAGE_KEY = 'long-voyage-id';

let voyageId = null;
let voyage = null;
let busy = false;
let autoPlay = false;
let interviewee = null;

function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    // Everything from the server goes in as text, never as HTML: model output can contain anything
    if (text !== undefined) node.textContent = text;
    return node;
}

function setStatus(message, kind = '') {
    els.status.textContent = message || '';
    els.status.className = kind;
}

function rememberVoyage(id) {
    voyageId = id;
    history.replaceState(null, '', `#${id}`);
    try { localStorage.setItem(STORAGE_KEY, id); } catch { /* private mode: the URL still works */ }
}

function savedVoyageId() {
    if (location.hash.length > 1) return location.hash.slice(1);
    try { return localStorage.getItem(STORAGE_KEY); } catch { return null; }
}

function updateControls() {
    const usable = voyage && !voyage.lost;
    els.nextTurn.disabled = busy || !usable;
    els.auto.disabled = !usable;
    els.newVoyage.disabled = busy;
    els.eventForm.querySelector('button').disabled = busy || !usable;
    els.transmissionForm.querySelector('button').disabled = !usable;
    els.auto.classList.toggle('on', autoPlay);
    els.auto.textContent = autoPlay ? 'Auto ■' : 'Auto ▶';
    els.lamp.className = 'lamp ' + (!voyage ? 'idle' : voyage.lost ? 'lost' : busy ? 'busy' : '');
}

function renderGauges() {
    els.gauges.replaceChildren(...SYSTEMS.map(([key, label]) => {
        const level = voyage[key];
        const gauge = element('div', 'gauge ' + (level < 30 ? 'critical' : level < 60 ? 'warning' : 'good'));
        const row = element('div', 'row');
        row.append(element('span', '', label), element('span', 'num', String(level)));
        const track = element('div', 'track');
        const fill = element('div', 'fill');
        fill.style.width = `${level}%`;
        track.append(fill);
        gauge.append(row, track);
        return gauge;
    }));
    els.lost.hidden = !voyage.lost;
    els.hour.textContent = `HOUR ${hourNumber(voyage.turn)}`;
}

// The latest private thought tells the most about someone; if they have none yet, show what they last did
function latestAbout(member, entries) {
    const thoughtPrefix = member.name + ' thinks: ';
    for (let i = entries.length - 1; i >= 0; i--) {
        const entry = entries[i];
        if (entry.kind === 'THOUGHT' && entry.text.startsWith(thoughtPrefix)) {
            return { text: '“' + entry.text.slice(thoughtPrefix.length) + '”', thought: true };
        }
    }
    for (let i = entries.length - 1; i >= 0; i--) {
        if (entries[i].kind === 'ACTION' && entries[i].text.startsWith(member.name)) {
            return { text: entries[i].text, thought: false };
        }
    }
    return { text: 'No word yet.', thought: false };
}

function renderCrew(entries) {
    els.crew.replaceChildren(...voyage.crew.map(member => {
        const card = element('article', 'member');
        const who = element('div', 'who');
        who.append(element('span', 'name', member.name), element('span', 'role', member.role));
        const top = element('div', 'top');
        top.append(who, element('span', 'room', ROOMS[member.location] || member.location));
        const latest = latestAbout(member, entries);
        const ask = element('button', 'ghost', 'Interview');
        ask.disabled = voyage.lost;
        ask.onclick = () => openInterview(member);
        card.append(top, element('p', 'latest' + (latest.thought ? ' thought' : ''), latest.text), ask);
        return card;
    }));
}

const hourNumber = (turn) => String(turn).padStart(2, '0');

// Events are triggered by Mission Control after an hour has ended: whoever is in that room sees it at once,
// but everyone reacts when the next hour begins. So they are shown between the two hours, not inside one
function logSectionOf(entry) {
    if (entry.kind === 'EVENT' && entry.turn > 0) {
        return { order: entry.turn + 0.5, turn: entry.turn, between: true };
    }
    return { order: entry.turn, turn: entry.turn, between: false };
}

function logSectionLabel(section) {
    if (section.between) return `BETWEEN HOURS ${hourNumber(section.turn)} → ${hourNumber(section.turn + 1)}`;
    return section.turn === 0 ? 'BEFORE DEPARTURE' : `HOUR ${hourNumber(section.turn)}`;
}

function renderLog(entries) {
    if (entries.length === 0) {
        els.log.replaceChildren(element('div', 'empty', 'The log is empty. Let the first hour pass.'));
        return;
    }
    const sections = new Map();
    entries.forEach(entry => {
        const section = logSectionOf(entry);
        if (!sections.has(section.order)) sections.set(section.order, { ...section, entries: [] });
        sections.get(section.order).entries.push(entry);
    });
    // Newest on top, but inside a section things read in the order they happened
    const groups = [...sections.values()].sort((a, b) => b.order - a.order).map(section => {
        const group = element('div', 'hour-group');
        group.append(element('div', 'hour-label' + (section.between ? ' between' : ''), logSectionLabel(section)));
        if (section.between && section.turn === voyage.turn) {
            group.append(element('div', 'hour-note', 'The crew will react when the next hour begins.'));
        }
        section.entries.forEach(entry => group.append(element('div', 'entry ' + entry.kind, entry.text)));
        return group;
    });
    els.log.replaceChildren(...groups);
}

// In transit: how long until it gets there. Delivered: the hour, so it can be found in the log
function transmissionStatus(transmission) {
    if (transmission.delivered) return `DELIVERED · HOUR ${transmission.arrivalTurn}`;
    if (voyage.lost) return 'NEVER ARRIVED';
    const hoursLeft = transmission.arrivalTurn - voyage.turn;
    return `IN TRANSIT · ARRIVES IN ${hoursLeft} ${hoursLeft === 1 ? 'HOUR' : 'HOURS'}`;
}

function renderTransmissions(transmissions) {
    els.transmissions.replaceChildren(...transmissions.slice().reverse().map(transmission => {
        const box = element('div', 'transmission' + (transmission.delivered ? ' delivered' : ''));
        box.append(element('div', '', transmission.message), element('div', 'meta', transmissionStatus(transmission)));
        return box;
    }));
}

async function refresh() {
    const [loaded, entries, transmissions] = await Promise.all([
        api.get(voyageId), api.log(voyageId), api.transmissions(voyageId)
    ]);
    voyage = loaded;
    renderGauges();
    renderCrew(entries);
    renderLog(entries);
    renderTransmissions(transmissions);
    updateControls();
}

async function runTurn() {
    busy = true;
    updateControls();
    const started = Date.now();
    const timer = setInterval(() => {
        setStatus(`Hour ${voyage.turn + 1} in progress… the crew is deciding (${Math.round((Date.now() - started) / 1000)}s)`, 'busy');
    }, 1000);
    setStatus(`Hour ${voyage.turn + 1} in progress… the crew is deciding`, 'busy');
    try {
        await api.turn(voyageId);
        await refresh();
        setStatus(voyage.lost ? 'Contact lost.' : `Hour ${voyage.turn} logged.`);
    } finally {
        clearInterval(timer);
        busy = false;
        updateControls();
    }
}

async function autoLoop() {
    try {
        while (autoPlay && voyage && !voyage.lost) {
            await runTurn();
        }
    } catch (error) {
        setStatus(error.message, 'error');
    } finally {
        autoPlay = false;
        updateControls();
    }
}

function openInterview(member) {
    interviewee = member;
    els.dialogTitle.textContent = `PRIVATE CHANNEL · ${member.name.toUpperCase()}`;
    els.qa.replaceChildren();
    els.interviewQuestion.value = '';
    els.dialog.showModal();
    els.interviewQuestion.focus();
}

els.newVoyage.onclick = async () => {
    try {
        autoPlay = false;
        const created = await api.create();
        rememberVoyage(created.id);
        await refresh();
        setStatus('A new voyage has begun. Six crew, decades from any planet.');
    } catch (error) {
        setStatus(error.message, 'error');
    }
};

els.nextTurn.onclick = () => runTurn().catch(error => setStatus(error.message, 'error'));

els.auto.onclick = () => {
    autoPlay = !autoPlay;
    updateControls();
    if (autoPlay && !busy) autoLoop();
};

els.eventForm.onsubmit = async (event) => {
    event.preventDefault();
    try {
        const update = await api.event(voyageId, els.eventType.value, els.eventLocation.value);
        await refresh();
        setStatus(`${update.entries[0]?.text || 'Event triggered'}. The crew will react when the next hour begins.`, 'error');
    } catch (error) {
        setStatus(error.message, 'error');
    }
};

els.transmissionForm.onsubmit = async (event) => {
    event.preventDefault();
    const message = els.transmissionMessage.value.trim();
    if (!message) return;
    try {
        const sent = await api.transmit(voyageId, message);
        els.transmissionMessage.value = '';
        await refresh();
        const hours = sent.arrivalTurn - sent.sentTurn;
        setStatus(`Transmission sent. It will reach the ship in ${hours} ${hours === 1 ? 'hour' : 'hours'}.`);
    } catch (error) {
        setStatus(error.message, 'error');
    }
};

els.interviewForm.onsubmit = async (event) => {
    event.preventDefault();
    const question = els.interviewQuestion.value.trim();
    if (!question || !interviewee) return;
    const button = els.interviewForm.querySelector('button');
    const answer = element('div', 'a', '…');
    els.qa.append(element('div', 'q', '› ' + question), answer);
    els.interviewQuestion.value = '';
    button.disabled = true;
    try {
        const reply = await api.interview(voyageId, interviewee.id, question);
        answer.textContent = reply.answer;
    } catch (error) {
        answer.textContent = error.message;
    } finally {
        button.disabled = false;
        els.qa.scrollTop = els.qa.scrollHeight;
    }
};

els.interviewQuestion.addEventListener('keydown', event => {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        els.interviewForm.requestSubmit();
    }
});

els.dialogClose.onclick = () => els.dialog.close();

(async () => {
    const id = savedVoyageId();
    if (!id) {
        setStatus('No voyage yet. Press "New voyage" to launch one.');
        updateControls();
        return;
    }
    try {
        rememberVoyage(id);
        await refresh();
        setStatus(voyage.lost ? 'This voyage is lost. Start a new one.' : 'Contact established.');
    } catch (error) {
        voyageId = null;
        setStatus('Saved voyage not found. Press "New voyage" to launch one.', 'error');
        updateControls();
    }
})();

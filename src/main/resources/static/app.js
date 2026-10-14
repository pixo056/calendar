'use strict';
const $ = selector => document.querySelector(selector);
const types = { TEST: 'Контрольная', CREDIT: 'Зачёт', EXAM: 'Экзамен', ACTIVITY: 'Мероприятие', DEADLINE: 'Дедлайн' };
const now = new Date();
const dateKey = date => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
const todayKey = dateKey(now);
const dateOf = value => new Date(`${value}T12:00:00`);
const dateText = value => dateOf(value).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' });
let visibleMonth = new Date(now.getFullYear(), now.getMonth(), 1);
let events = [];
let view = window.matchMedia('(max-width: 600px)').matches ? 'list' : 'calendar';
let busy = false;
let loaded = false;
let selectedGroup = 'ТОП-105Б';
let groups = [];
let loadVersion = 0;

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function message(text, error = false) {
  $('#status').textContent = text;
  $('#status').classList.toggle('error', error);
}

async function api(path = '', options = {}) {
  const response = await fetch(`/api/events${path}?group=${encodeURIComponent(selectedGroup)}`, {
    ...options, headers: { 'Content-Type': 'application/json' }
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || `Не удалось выполнить запрос (${response.status}). Попробуйте ещё раз.`);
  }
  return response.status === 204 ? null : response.json();
}

async function load() {
  const version = ++loadVersion;
  loaded = false;
  $('#add-event').disabled = true;
  events = [];
  $('#month-count').textContent = '—';
  $('#week-count').textContent = '—';
  $('#deadline-title').textContent = 'Загрузка…';
  $('#deadline-date').textContent = '';
  $('#calendar-content').replaceChildren(element('p', 'empty', 'Загружаем календарь группы…'));
  $('#upcoming-content').replaceChildren();
  message('Загружаем события…');
  try {
    const received = await api();
    if (version !== loadVersion) return;
    events = received;
    loaded = true;
    $('#add-event').disabled = false;
    render();
    message('');
  } catch (error) {
    if (version !== loadVersion) return;
    message('Не удалось загрузить события. Проверьте, что сервер запущен, и обновите страницу.', true);
    $('#calendar-content').replaceChildren(element('p', 'empty', 'Сервер недоступен. Обновите страницу после запуска приложения.'));
    $('#upcoming-content').replaceChildren(element('p', 'empty', 'Нет соединения с сервером'));
  }
}

function displayGroup() {
  $('#selected-group').textContent = selectedGroup;
  $('#breadcrumb-group').textContent = `Группа ${selectedGroup}`;
  document.title = `Студенческий календарь · ${selectedGroup}`;
  $('#choose-group').setAttribute('aria-label', `Выбрать группу. Сейчас ${selectedGroup}`);
}

function renderGroups() {
  const query = $('#group-search').value.trim().toLocaleLowerCase('ru');
  const matches = groups.filter(group => group.toLocaleLowerCase('ru').includes(query));
  // Keep the selected group easy to find without changing the official names.
  if (!query) matches.sort((a, b) => Number(b === selectedGroup) - Number(a === selectedGroup));
  const results = $('#group-results');
  results.replaceChildren();
  $('#group-results-count').textContent = `Найдено: ${matches.length}${matches.length > 100 ? '. Показаны первые 100 — уточните поиск.' : ''}`;
  if (!matches.length) results.append(element('p', 'empty', 'Группа не найдена. Проверьте название.'));
  matches.slice(0, 100).forEach(group => {
    const button = element('button', `group-option${group === selectedGroup ? ' selected' : ''}`, group);
    button.type = 'button';
    button.setAttribute('aria-pressed', String(group === selectedGroup));
    button.addEventListener('click', () => {
      if (busy) return;
      const changed = selectedGroup !== group;
      selectedGroup = group;
      try { localStorage.setItem('calendar.selectedGroup', group); } catch { /* storage may be disabled */ }
      displayGroup();
      $('#group-dialog').close();
      if (changed || !loaded) load();
    });
    results.append(button);
  });
}

async function initialize() {
  try {
    const response = await fetch('/api/groups');
    if (!response.ok) throw new Error('Не удалось загрузить справочник групп.');
    const catalog = await response.json();
    groups = catalog.groups;
    let saved;
    try { saved = localStorage.getItem('calendar.selectedGroup'); } catch { /* use default */ }
    selectedGroup = groups.includes(saved) ? saved : catalog.defaultGroup;
    displayGroup();
    $('#choose-group').disabled = false;
    await load();
  } catch (error) {
    $('#add-event').disabled = true;
    message('Не удалось загрузить справочник групп. Перезапустите приложение и обновите страницу.', true);
  }
}

function monthEvents() {
  const start = dateKey(visibleMonth);
  const end = dateKey(new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + 1, 1));
  const search = $('#search').value.trim().toLocaleLowerCase('ru');
  return events.filter(event => event.date >= start && event.date < end &&
    (!$('#type-filter').value || event.type === $('#type-filter').value) &&
    [event.title, event.subject, event.location, event.description].join(' ').toLocaleLowerCase('ru').includes(search));
}

function render() {
  if (!loaded) return;
  const monthName = visibleMonth.toLocaleDateString('ru-RU', { month: 'long', year: 'numeric' });
  $('#month-title').textContent = monthName[0].toUpperCase() + monthName.slice(1).replace(' г.', '');
  $('#month-count').textContent = events.filter(event => dateOf(event.date).getFullYear() === visibleMonth.getFullYear() && dateOf(event.date).getMonth() === visibleMonth.getMonth()).length;
  const weekEnd = dateKey(new Date(now.getFullYear(), now.getMonth(), now.getDate() + 7));
  const upcoming = events.filter(event => event.date >= todayKey && event.date < weekEnd);
  $('#week-count').textContent = upcoming.length;
  const deadline = events.find(event => event.type === 'DEADLINE' && event.date >= todayKey);
  $('#deadline-title').textContent = deadline?.title || 'Пока нет';
  $('#deadline-date').textContent = deadline ? `${dateText(deadline.date)}${deadline.time ? ` · ${deadline.time.slice(0, 5)}` : ''}` : 'Можно спокойно выдохнуть';
  $('#calendar-view').classList.toggle('active', view === 'calendar');
  $('#list-view').classList.toggle('active', view === 'list');
  $('#calendar-view').setAttribute('aria-pressed', view === 'calendar');
  $('#list-view').setAttribute('aria-pressed', view === 'list');
  const content = $('#calendar-content');
  content.replaceChildren();
  const filtered = monthEvents();
  if (view === 'list') renderList(content, filtered);
  else renderMonth(content, filtered);
  const side = $('#upcoming-content');
  side.replaceChildren();
  if (!upcoming.length) {
    const empty = element('div', 'empty');
    empty.append(element('strong', '', 'Свободный горизонт'), element('span', '', 'В ближайшие 7 дней событий нет.'));
    side.append(empty);
  }
  upcoming.slice(0, 6).forEach(event => {
    const button = element('button', `upcoming-item ${event.type}`);
    button.append(element('small', '', `${dateText(event.date)}${event.time ? ` · ${event.time.slice(0, 5)}` : ''}`), element('strong', '', event.title), element('span', '', `${event.subject} · ${types[event.type]}`));
    button.addEventListener('click', () => editEvent(event));
    side.append(button);
  });
  if (upcoming.length > 6) side.append(element('p', 'storage-note', `Ещё событий: ${upcoming.length - 6}. Откройте нужный месяц в календаре.`));
}

function renderMonth(content, filtered) {
  const weekdays = element('div', 'weekdays');
  ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'].forEach(day => weekdays.append(element('span', '', day)));
  const grid = element('div', 'month-grid');
  const offset = (visibleMonth.getDay() + 6) % 7;
  const totalDays = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + 1, 0).getDate();
  const cells = Math.ceil((offset + totalDays) / 7) * 7;
  for (let index = 0; index < cells; index++) {
    const date = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth(), index - offset + 1);
    const key = dateKey(date);
    const outside = date.getMonth() !== visibleMonth.getMonth();
    const day = element('div', `day${outside ? ' outside' : ''}${key === todayKey ? ' is-today' : ''}`);
    const button = element('button', 'day-number', date.getDate());
    button.setAttribute('aria-label', `Добавить событие: ${date.toLocaleDateString('ru-RU')}`);
    if (key === todayKey) button.setAttribute('aria-current', 'date');
    button.addEventListener('click', () => editEvent(null, key));
    day.append(button);
    if (!outside) filtered.filter(event => event.date === key).forEach(event => {
      const chip = element('button', `event-chip ${event.type}`);
      chip.title = `${types[event.type]}: ${event.title}${event.time ? `, ${event.time.slice(0, 5)}` : ''}`;
      chip.setAttribute('aria-label', chip.title);
      if (event.time) chip.append(element('time', '', event.time.slice(0, 5)));
      chip.append(document.createTextNode(event.title));
      chip.addEventListener('click', () => editEvent(event));
      day.append(chip);
    });
    grid.append(day);
  }
  const scroll = element('div', 'month-scroll');
  const month = element('div', 'month-body');
  month.append(weekdays, grid);
  scroll.append(month);
  content.append(scroll);
}

function renderList(content, filtered) {
  const list = element('div', 'event-list');
  if (!filtered.length) list.append(element('p', 'empty', 'В этом месяце подходящих событий нет. Добавьте первое или измените фильтры.'));
  filtered.forEach(event => {
    const button = element('button', `list-event ${event.type}`);
    const details = element('span');
    details.append(element('strong', '', event.title), element('small', '', `${event.subject} · ${types[event.type]}${event.location ? ` · ${event.location}` : ''}`));
    button.append(element('span', 'date-label', `${dateText(event.date)}${event.time ? ` · ${event.time.slice(0, 5)}` : ''}`), details, element('i', 'dot'));
    button.addEventListener('click', () => editEvent(event));
    list.append(button);
  });
  content.append(list);
}

function editEvent(event = null, date = todayKey) {
  if (busy || !loaded) return;
  const form = $('#event-form');
  form.reset();
  $('#form-error').textContent = '';
  $('#event-id').value = event?.id || '';
  $('#dialog-title').textContent = event ? 'Редактировать событие' : 'Новое событие';
  $('#event-dialog .eyebrow').textContent = `ГРУППА ${selectedGroup}`;
  $('#delete-event').hidden = !event;
  form.elements.date.value = event?.date || date;
  if (event) ['title', 'subject', 'type', 'time', 'location', 'description'].forEach(key => { form.elements[key].value = event[key] || ''; });
  $('#event-dialog').showModal();
  form.elements.title.focus();
}

function setBusy(value) {
  busy = value;
  ['save-event', 'delete-event', 'confirm-delete', 'cancel-delete', 'close-dialog', 'cancel-dialog'].forEach(id => { $(`#${id}`).disabled = value; });
  $('#choose-group').disabled = value;
}

$('#event-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (busy) return;
  const form = event.currentTarget;
  const payload = Object.fromEntries(new FormData(form));
  payload.time = payload.time || null;
  const id = $('#event-id').value;
  setBusy(true);
  $('#form-error').textContent = '';
  try {
    const saved = await api(id ? `/${id}` : '', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
    events = events.filter(item => item.id !== saved.id);
    events.push(saved);
    events.sort((a, b) => a.date.localeCompare(b.date) || (a.time || '').localeCompare(b.time || '') || a.id - b.id);
    visibleMonth = new Date(dateOf(saved.date).getFullYear(), dateOf(saved.date).getMonth(), 1);
    $('#search').value = '';
    $('#type-filter').value = '';
    $('#event-dialog').close();
    render();
    message(id ? 'Изменения сохранены.' : 'Событие добавлено.');
  } catch (error) {
    $('#form-error').textContent = error.message || 'Нет связи с сервером. Попробуйте ещё раз.';
  } finally { setBusy(false); }
});

$('#delete-event').addEventListener('click', () => {
  $('#delete-description').textContent = $('#event-form').elements.title.value;
  $('#delete-dialog').showModal();
});
$('#confirm-delete').addEventListener('click', async () => {
  if (busy) return;
  const id = Number($('#event-id').value);
  setBusy(true);
  try {
    await api(`/${id}`, { method: 'DELETE' });
    events = events.filter(event => event.id !== id);
    $('#delete-dialog').close();
    $('#event-dialog').close();
    render();
    message('Событие удалено.');
  } catch (error) {
    $('#delete-dialog').close();
    $('#form-error').textContent = error.message || 'Нет связи с сервером.';
  } finally { setBusy(false); }
});
$('#cancel-delete').addEventListener('click', () => $('#delete-dialog').close());
['close-dialog', 'cancel-dialog'].forEach(id => $(`#${id}`).addEventListener('click', () => $('#event-dialog').close()));
['event-dialog', 'delete-dialog'].forEach(id => $(`#${id}`).addEventListener('cancel', event => { if (busy) event.preventDefault(); }));
$('#add-event').addEventListener('click', () => editEvent());
$('#search').addEventListener('input', render);
$('#type-filter').addEventListener('change', render);
$('#previous-month').addEventListener('click', () => { visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() - 1, 1); render(); });
$('#next-month').addEventListener('click', () => { visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + 1, 1); render(); });
$('#today').addEventListener('click', () => { visibleMonth = new Date(now.getFullYear(), now.getMonth(), 1); render(); });
$('#calendar-view').addEventListener('click', () => { view = 'calendar'; render(); });
$('#list-view').addEventListener('click', () => { view = 'list'; render(); });
$('#choose-group').addEventListener('click', () => {
  $('#group-search').value = '';
  renderGroups();
  $('#group-dialog').showModal();
  // Leave the keyboard closed on phones until the user taps search.
  $('#close-groups').focus();
});
$('#close-groups').addEventListener('click', () => $('#group-dialog').close());
$('#group-search').addEventListener('input', renderGroups);
$('#current-date').textContent = now.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' });
$('#add-event').disabled = true;
initialize();

const api = '/api/accounts';
const state = { selected: null, stream: null };
const $ = selector => document.querySelector(selector);

function money(amount, currency) {
  return new Intl.NumberFormat('fr-MA', { style: 'currency', currency }).format(amount);
}

function notice(message, error = false) {
  $('#notice').textContent = message;
  $('#notice').classList.toggle('error', error);
}

async function request(path, options) {
  const response = await fetch(api + path, options);
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || `Erreur HTTP ${response.status}`);
  return data;
}

async function waitForAccount(id) {
  for (let attempt = 0; attempt < 30; attempt++) {
    const response = await fetch(api + '/' + encodeURIComponent(id));
    if (response.ok) return;
    if (response.status !== 404) throw new Error(`Erreur HTTP ${response.status}`);
    await new Promise(resolve => setTimeout(resolve, 300));
  }
  throw new Error('La projection de lecture tarde à se mettre à jour. Actualisez la page.');
}

async function refresh() {
  const { accounts } = await request('');
  const container = $('#accounts');
  container.replaceChildren();
  if (!accounts.length) {
    const empty = document.createElement('p');
    empty.textContent = 'Aucun compte pour le moment.';
    container.append(empty);
  }
  for (const account of accounts) {
    const row = document.createElement('button');
    row.className = 'account-row' + (account.id === state.selected ? ' selected' : '');
    const name = document.createElement('span');
    name.textContent = account.id.slice(0, 8);
    const suffix = document.createElement('small');
    suffix.textContent = account.status === 'ACTIVE' ? 'Actif' : 'Suspendu';
    name.append(suffix);
    const balance = document.createElement('b');
    balance.textContent = money(account.balance, account.currency);
    row.append(name, balance);
    row.addEventListener('click', () => select(account.id));
    container.append(row);
  }
  if (state.selected) await refreshDetail();
}

async function refreshDetail() {
  const id = state.selected;
  if (!id) return;
  const [account, { operations }, events] = await Promise.all([
    request('/' + encodeURIComponent(id)),
    request('/' + encodeURIComponent(id) + '/operations'),
    request('/' + encodeURIComponent(id) + '/events')
  ]);
  if (state.selected !== id) return;
  $('#detail').hidden = false;
  $('#account-id').textContent = account.id;
  $('#account-status').textContent = account.status === 'ACTIVE' ? 'ACTIF' : 'SUSPENDU';
  $('#balance').textContent = money(account.balance, account.currency);
  $('#status-button').textContent = account.status === 'ACTIVE' ? 'Suspendre le compte' : 'Réactiver le compte';
  const history = $('#operations');
  history.replaceChildren();
  for (const operation of [...operations].reverse()) {
    const line = document.createElement('div');
    line.className = 'operation';
    const label = document.createElement('span');
    label.textContent = operation.type === 'CREDIT' ? 'Crédit' : 'Débit';
    const date = document.createElement('small');
    date.textContent = new Date(operation.occurredAt).toLocaleString('fr-MA');
    label.append(date);
    const amount = document.createElement('strong');
    amount.className = operation.type.toLowerCase();
    amount.textContent = (operation.type === 'CREDIT' ? '+ ' : '− ') + money(operation.amount, operation.currency);
    line.append(label, amount);
    history.append(line);
  }
  if (!operations.length) history.textContent = 'Aucune opération.';
  renderEvents(events);
}

function renderEvents(events) {
  const container = $('#events');
  container.replaceChildren();
  for (const event of events) {
    const line = document.createElement('div');
    line.className = 'operation';
    const label = document.createElement('span');
    label.textContent = describeEvent(event);
    const date = document.createElement('small');
    date.textContent = '#' + event.sequence + ' · ' + new Date(event.occurredAt).toLocaleString('fr-MA');
    label.append(date);
    const type = document.createElement('strong');
    type.className = 'credit';
    type.textContent = event.type;
    line.append(label, type);
    container.append(line);
  }
}

function describeEvent(event) {
  const payload = event.payload || {};
  switch (event.type) {
    case 'Created': return `Ouverture du compte (${payload.currency})`;
    case 'Activated': return 'Activation du compte';
    case 'Credited': return `Crédit de ${money(payload.amount, payload.currency)}`;
    case 'Debited': return `Débit de ${money(payload.amount, payload.currency)}`;
    case 'StatusChanged': return `Statut changé en ${payload.status}`;
    default: return event.type;
  }
}

async function select(id) {
  state.stream?.close();
  state.selected = id;
  await refresh();
  if (state.selected !== id) return;
  state.stream = new EventSource(api + '/' + encodeURIComponent(id) + '/stream');
  state.stream.addEventListener('operation', () => {
    refresh().catch(error => notice(error.message, true));
  });
}

function json(body) {
  return { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) };
}

$('#create-form').addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const form = new FormData(event.currentTarget);
    const result = await request('', json({ initialBalance: Number(form.get('initialBalance')), currency: form.get('currency') }));
    notice('Compte créé. La projection de lecture se met à jour.');
    await waitForAccount(result.accountId);
    await select(result.accountId);
  } catch (error) { notice(error.message, true); }
});

$('#amount-form').addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const account = await request('/' + encodeURIComponent(state.selected));
    const form = new FormData(event.currentTarget);
    const action = event.submitter.dataset.action;
    await request('/' + encodeURIComponent(state.selected) + '/' + action,
      json({ amount: Number(form.get('amount')), currency: account.currency }));
    notice(action === 'credits' ? 'Crédit enregistré.' : 'Débit enregistré.');
    event.currentTarget.reset();
  } catch (error) { notice(error.message, true); }
});

$('#status-button').addEventListener('click', async () => {
  try {
    const account = await request('/' + encodeURIComponent(state.selected));
    const status = account.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    await request('/' + encodeURIComponent(state.selected) + '/status', { ...json({ status }), method: 'PATCH' });
    notice('Changement de statut enregistré.');
    for (let attempt = 0; attempt < 30; attempt++) {
      const current = await request('/' + encodeURIComponent(state.selected));
      if (current.status === status) break;
      await new Promise(resolve => setTimeout(resolve, 300));
    }
    await refresh();
  } catch (error) { notice(error.message, true); }
});

refresh().catch(error => notice(error.message, true));

'use strict';

const state = { products: [], cart: { items: [], count: 0, total: 0 }, busy: false };
const $ = (selector) => document.querySelector(selector);
const money = (value) => new Intl.NumberFormat('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
const productIcon = (name) => {
  const s = name.toLowerCase();
  if (/кофе|капуч|эспресс|латте|матча|чай/.test(s)) return 'DRK';
  if (/круас|печень|сэндвич|хлеб/.test(s)) return 'FOD';
  if (/вода|сок|напиток/.test(s)) return 'H2O';
  return 'SKU';
};

async function request(path, data) {
  const options = data === undefined ? {} : {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
    body: new URLSearchParams(data).toString()
  };
  let response;
  try { response = await fetch(path, options); }
  catch { throw new Error('Нет связи с Java-сервером. Проверь запуск приложения.'); }
  let payload;
  try { payload = await response.json(); }
  catch { throw new Error(`Некорректный ответ сервера (${response.status})`); }
  if (!response.ok) throw new Error(payload.error || `Ошибка HTTP ${response.status}`);
  return payload;
}

function node(tag, className, content) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (content !== undefined) element.textContent = content;
  return element;
}

function notify(message, error = false) {
  const stack = $('#toast-stack');
  const toast = node('div', `toast${error ? ' error' : ''}`, `${error ? '✕' : '✓'}  ${message}`);
  stack.append(toast);
  window.setTimeout(() => toast.remove(), 4200);
  if (error && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    document.body.classList.remove('shake-once');
    void document.body.offsetWidth;
    document.body.classList.add('shake-once');
    window.setTimeout(() => document.body.classList.remove('shake-once'), 260);
  }
}

function renderStats() {
  $('#stat-items').textContent = String(state.products.length).padStart(2, '0');
  $('#stat-stock').textContent = state.products.reduce((acc, product) => acc + product.quantity, 0);
  $('#stat-cart').textContent = String(state.cart.count).padStart(2, '0');
}

function renderProducts() {
  const query = $('#search').value.trim().toLocaleLowerCase('ru');
  const filtered = state.products.filter(product =>
    product.name.toLocaleLowerCase('ru').includes(query) || product.barcode.toLocaleLowerCase('ru').includes(query)
  );
  $('#result-count').textContent = `${String(filtered.length).padStart(2, '0')} ПОЗИЦИЙ НАЙДЕНО`;
  const grid = $('#product-grid');
  grid.replaceChildren();
  if (filtered.length === 0) {
    grid.append(node('div', 'no-results', query ? 'НЕТ СОВПАДЕНИЙ / TRY AGAIN_' : 'КАТАЛОГ ПУСТ'));
    return;
  }
  const fragment = document.createDocumentFragment();
  for (const product of filtered) {
    const card = node('article', `product-card${product.quantity === 0 ? ' soldout' : ''}`);
    const top = node('div', 'product-top');
    top.append(node('div', 'product-icon', productIcon(product.name)));
    const status = node('span', `stock-indicator${product.quantity === 0 ? ' out' : product.quantity < 5 ? ' low' : ''}`,
      product.quantity === 0 ? 'НЕТ В НАЛИЧИИ' : `● ${product.quantity} ШТ.`);
    top.append(status);
    card.append(top);
    card.append(node('div', 'product-title', product.name));
    card.append(node('div', 'product-barcode', `# ${product.barcode}`));
    const bottom = node('div', 'product-bottom');
    const price = node('div', 'product-price');
    price.append(document.createTextNode(money(product.price)));
    price.append(node('small', '', 'BYN'));
    bottom.append(price);
    const add = node('button', 'add-button', '+');
    add.type = 'button'; add.dataset.add = product.barcode;
    add.disabled = state.busy || product.quantity === 0;
    add.title = product.quantity === 0 ? 'Нет в наличии' : `Добавить ${product.name}`;
    add.setAttribute('aria-label', add.title);
    bottom.append(add);
    card.append(bottom);
    fragment.append(card);
  }
  grid.append(fragment);
}

function renderCart() {
  const list = $('#cart-list');
  list.replaceChildren();
  $('#receipt-count').textContent = String(state.cart.items.length).padStart(2, '0');
  $('#summary-count').textContent = `${state.cart.count} ЕД.`;
  $('#total-price').textContent = money(state.cart.total);
  $('#checkout').disabled = state.cart.count === 0 || state.busy;
  $('#clear-cart').disabled = state.cart.count === 0 || state.busy;
  if (state.cart.items.length === 0) {
    const empty = node('div', 'empty-cart');
    empty.append(node('div', 'empty-glyph', '[ _ ]'));
    empty.append(node('strong', '', 'КОРЗИНА ПУСТА'));
    const msg = node('span', '', 'Выбери товар из каталога, чтобы начать покупку.');
    empty.append(msg); list.append(empty);
    return;
  }
  for (const item of state.cart.items) {
    const row = node('article', 'cart-item');
    const top = node('div', 'cart-item-top');
    top.append(node('div', 'cart-item-name', item.name));
    const remove = node('button', 'remove-item', '×');
    remove.type = 'button'; remove.dataset.remove = item.barcode;
    remove.title = `Убрать ${item.name}`; remove.disabled = state.busy;
    remove.setAttribute('aria-label', remove.title);
    top.append(remove); row.append(top);
    row.append(node('div', 'cart-item-code', `# ${item.barcode}  •  ${money(item.price)} BYN`));
    const bottom = node('div', 'cart-item-bottom');
    const controls = node('div', 'quantity-widget');
    const decrease = node('button', '', '−');
    decrease.type = 'button'; decrease.dataset.set = item.barcode;
    decrease.dataset.quantity = String(item.cartQuantity - 1); decrease.disabled = state.busy;
    decrease.setAttribute('aria-label', `Уменьшить количество ${item.name}`);
    const amount = node('span', '', item.cartQuantity);
    const increase = node('button', '', '+');
    increase.type = 'button'; increase.dataset.set = item.barcode;
    increase.dataset.quantity = String(item.cartQuantity + 1);
    increase.disabled = state.busy || item.cartQuantity >= item.quantity;
    increase.setAttribute('aria-label', `Увеличить количество ${item.name}`);
    controls.append(decrease, amount, increase);
    bottom.append(controls);
    bottom.append(node('div', 'cart-subtotal', `${money(item.subtotal)} BYN`));
    row.append(bottom); list.append(row);
  }
}

function render() { renderStats(); renderProducts(); renderCart(); }

async function reload() {
  const [products, cart] = await Promise.all([
    request('/api/products'), request('/api/cart')
  ]);
  state.products = products;
  state.cart = cart;
  render();
}

async function mutate(path, data, successMessage) {
  if (state.busy) return;
  state.busy = true; render();
  try {
    await request(path, data);
    await reload();
    if (successMessage) notify(successMessage);
  } catch (error) { notify(error.message, true); }
  finally { state.busy = false; render(); }
}

$('#product-grid').addEventListener('click', (event) => {
  const button = event.target.closest('button[data-add]');
  if (!button) return;
  mutate('/api/cart/add', { barcode: button.dataset.add, quantity: 1 });
});

$('#cart-list').addEventListener('click', (event) => {
  const set = event.target.closest('button[data-set]');
  const remove = event.target.closest('button[data-remove]');
  if (set) mutate('/api/cart/set', { barcode: set.dataset.set, quantity: set.dataset.quantity });
  else if (remove) mutate('/api/cart/remove', { barcode: remove.dataset.remove });
});

$('#barcode-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const barcode = $('#barcode').value.trim();
  if (!barcode) return;
  await mutate('/api/cart/add', { barcode, quantity: 1 });
  $('#barcode').value = '';
  $('#barcode').focus();
});

$('#clear-cart').addEventListener('click', () => mutate('/api/cart/clear', {}, 'Чек очищен'));
$('#refresh').addEventListener('click', async () => {
  try { await reload(); notify('Каталог обновлён'); }
  catch (error) { notify(error.message, true); }
});
$('#search').addEventListener('input', renderProducts);

$('#checkout').addEventListener('click', async () => {
  if (state.busy || state.cart.count === 0) return;
  state.busy = true; render();
  try {
    const receipt = await request('/api/checkout', {});
    await reload();
    $('#success-count').textContent = String(receipt.count);
    $('#success-total').textContent = `${money(receipt.total)} BYN`;
    $('#success-dialog').showModal();
  } catch (error) { notify(error.message, true); }
  finally { state.busy = false; render(); }
});
$('#close-dialog').addEventListener('click', () => $('#success-dialog').close());
$('#success-dialog').addEventListener('click', (event) => {
  if (event.target === $('#success-dialog')) $('#success-dialog').close();
});

document.addEventListener('keydown', (event) => {
  if (event.key === '/' && !/INPUT|TEXTAREA/.test(document.activeElement.tagName) && !$('#success-dialog').open) {
    event.preventDefault(); $('#search').focus();
  }
  if (event.key === 'Escape' && document.activeElement === $('#search')) $('#search').blur();
});

function updateClock() { $('#clock').textContent = new Date().toLocaleTimeString('ru-RU', { hour12: false }); }
updateClock(); window.setInterval(updateClock, 1000);
reload().catch(error => {
  notify(error.message, true);
  $('#product-grid').replaceChildren(node('div', 'no-results', 'СЕРВЕР НЕДОСТУПЕН / CONNECTION LOST'));
});

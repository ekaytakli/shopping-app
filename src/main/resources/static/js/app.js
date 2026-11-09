import { request, loadProducts, stockStatus, ApiError } from './api.mjs';
import { CART_KEY, restoreCart, changeQuantity, cartSummary, categoryTheme } from './cart.mjs';

const $ = selector => document.querySelector(selector);
const number = new Intl.NumberFormat('tr-TR');
const price = new Intl.NumberFormat('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const state = { pageNo: 1, pageSize: 10, search: '', categoryId: '', page: null, categories: [], loading: false };
let listController;
let detailController;
let toastTimer;

// API values are always inserted as text; product names cannot inject markup.
function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function toast(message) {
  clearTimeout(toastTimer);
  $('#toast').textContent = message;
  $('#toast').hidden = false;
  toastTimer = setTimeout(() => { $('#toast').hidden = true; }, 6000);
}

function showListState(title, description) {
  $('#list-state').hidden = false;
  $('#state-title').textContent = title;
  $('#state-description').textContent = description;
}

function updatePagination() {
  const page = state.page;
  $('#previous').disabled = state.loading || !page || page.pageNo <= 1;
  $('#next').disabled = state.loading || !page || page.last;
  $('#page-label').textContent = `${page?.pageNo || 1} / ${Math.max(1, page?.totalPages || 0)}`;
}

function illustration(name, className) {
  const theme = categoryTheme(name);
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('class', className);
  svg.setAttribute('aria-hidden', 'true');
  svg.setAttribute('viewBox', '0 0 200 180');
  const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
  use.setAttribute('href', '/images/groceries.svg#' + theme.illustration);
  svg.append(use);
  return svg;
}

function renderCategoryTiles() {
  const fragment = document.createDocumentFragment();
  for (const category of [{ categoryId: '', categoryName: 'Tümü' }, ...state.categories]) {
    const id = String(category.categoryId);
    const button = element('button', 'category-tile');
    button.type = 'button';
    button.classList.toggle('active', id === state.categoryId);
    button.setAttribute('aria-pressed', String(id === state.categoryId));
    const picture = element('span', 'category-picture');
    picture.style.setProperty('--tile-bg', categoryTheme(category.categoryName).background);
    picture.append(illustration(category.categoryName, 'category-art'));
    button.append(picture, element('span', '', category.categoryName));
    button.addEventListener('click', () => {
      $('#category-filter').value = id;
      $('#filters').requestSubmit();
      $('#catalog').scrollIntoView({ block: 'start' });
    });
    fragment.append(button);
  }
  $('#category-tiles').replaceChildren(fragment);
}

function renderPage(page) {
  const fragment = document.createDocumentFragment();
  for (const product of page.content) {
    const card = element('article', 'product-card');
    card.classList.toggle('is-sold-out', product.unitsInStock === 0);
    const visual = element('div', 'product-visual');
    visual.style.setProperty('--tile-bg', categoryTheme(product.category?.categoryName).background);
    const picture = element('button', 'product-art');
    picture.type = 'button';
    picture.setAttribute('aria-label', product.productName + ' detaylarını aç');
    picture.append(illustration(product.category?.categoryName, ''));
    picture.addEventListener('click', () => openDetail(product.id));
    const add = element('button', 'add-button', '+');
    add.type = 'button';
    add.setAttribute('aria-label', product.productName + ' alışveriş listesine ekle');
    add.disabled = product.unitsInStock <= 0;
    add.addEventListener('click', () => addToCart(product));
    visual.append(picture, add);
    if (!product.unitsInStock) visual.append(element('span', 'sold-out-label', 'Şimdilik tükendi'));
    const info = element('div', 'product-info');
    const name = element('button', 'product-name', product.productName);
    name.type = 'button';
    name.addEventListener('click', () => openDetail(product.id));
    const bottom = element('div', 'product-card-bottom');
    const status = stockStatus(product.unitsInStock);
    bottom.append(element('strong', 'product-price', price.format(product.unitPrice)),
      element('span', 'stock-badge ' + status.className, status.label));
    info.append(element('p', 'product-category', product.category?.categoryName || 'Market'), name,
      element('p', 'product-quantity', product.quantityPerUnit || 'Paket bilgisi belirtilmemiş'), bottom);
    card.append(visual, info);
    fragment.append(card);
  }
  $('#products-body').replaceChildren(fragment);
  $('#list-state').hidden = page.content.length > 0;
  if (!page.content.length) showListState('Burada henüz ürün yok', state.search || state.categoryId
    ? 'Bu aramayla eşleşen ürün bulunamadı. Farklı bir ürün adı veya kategori deneyebilirsin.'
    : 'Kataloğa ilk ürününü ekleyerek başlayabilirsin.');
  $('#catalog-count').textContent = number.format(page.totalElements);
  $('#catalog-title').firstChild.textContent = state.search ? 'Arama sonuçları ' : state.categoryId
    ? (state.categories.find(category => String(category.categoryId) === state.categoryId)?.categoryName || 'Seçilen kategori') + ' ' : 'Marketten seçtiklerin ';
  $('#catalog-description').textContent = state.search ? '“' + state.search + '” için bulunan ürünler.' : 'Kataloğu keşfet, favori lezzetlerini listene ekle.';
  const start = page.content.length ? (page.pageNo - 1) * page.pageSize + 1 : 0;
  const end = page.content.length ? start + page.content.length - 1 : 0;
  $('#page-summary').textContent = number.format(page.totalElements) + ' üründen ' + start + '–' + end + ' arası gösteriliyor';
}

async function refreshProducts() {
  listController?.abort();
  const controller = new AbortController();
  listController = controller;
  state.loading = true;
  state.page = null;
  $('#products-body').replaceChildren();
  $('#list-error').hidden = true;
  $('#table-region').setAttribute('aria-busy', 'true');
  $('#refresh').disabled = true;
  $('#page-summary').textContent = 'Ürünler yükleniyor…';
  $('#catalog-count').textContent = '—';
  showListState('Ürünler yükleniyor', 'Kataloğunuz hazırlanıyor…');
  updatePagination();
  try {
    let page = await loadProducts(state, controller.signal);
    // A refreshed database may contain fewer pages than the previous result.
    if (page.totalPages > 0 && page.pageNo > page.totalPages) {
      state.pageNo = page.totalPages;
      page = await loadProducts(state, controller.signal);
    }
    if (controller.signal.aborted) return;
    state.page = page;
    state.pageNo = page.pageNo;
    renderPage(page);
  } catch (error) {
    if (error.name === 'AbortError') return;
    $('#list-error').textContent = error.message;
    $('#list-error').hidden = false;
    showListState('Katalog yüklenemedi', 'Tekrar denemek için Yenile düğmesini kullanın.');
    $('#page-summary').textContent = 'Ürün bilgileri alınamadı.';
  } finally {
    if (listController === controller) {
      state.loading = false;
      $('#table-region').setAttribute('aria-busy', 'false');
      $('#refresh').disabled = false;
      updatePagination();
    }
  }
}

async function refreshCategories() {
  try {
    const categories = await request('/api/categories');
    state.categories = categories;
    const filter = $('#category-filter');
    const selected = filter.value;
    filter.replaceChildren(new Option('Tüm kategoriler', ''));
    const formSelect = $('#product-category');
    const formSelected = formSelect.value;
    formSelect.replaceChildren(new Option(categories.length ? 'Kategori seçin' : 'Önce veritabanına kategori ekleyin', ''));
    for (const category of categories) {
      filter.add(new Option(category.categoryName, String(category.categoryId)));
      formSelect.add(new Option(category.categoryName, String(category.categoryId)));
    }
    filter.value = selected;
    formSelect.value = formSelected;
    renderCategoryTiles();
    $('#category-filter').disabled = false;
    $('#product-form button[type=submit]').disabled = !categories.length;
    return true;
  } catch (error) {
    $('#category-tiles').replaceChildren(element('p', 'muted', 'Kategoriler alınamadı. Yenile düğmesiyle tekrar deneyebilirsin.'));
    $('#category-filter').disabled = true;
    $('#product-form button[type=submit]').disabled = true;
    toast(`Kategoriler alınamadı. ${error.message}`);
    return false;
  }
}

function clearFormErrors(form) {
  form.querySelectorAll('.field-error').forEach(node => { node.textContent = ''; });
  form.querySelectorAll('[aria-invalid]').forEach(node => node.removeAttribute('aria-invalid'));
  form.querySelector('.form-message').hidden = true;
}

function showFormError(form, error) {
  const banner = form.querySelector('.form-message');
  banner.textContent = error.message;
  banner.hidden = false;
  let firstInvalid;
  if (error.fields && typeof error.fields === 'object' && !Array.isArray(error.fields)) {
    for (const [name, messages] of Object.entries(error.fields)) {
      const field = form.elements.namedItem(name);
      const target = [...form.querySelectorAll('[data-error]')].find(node => node.dataset.error === name);
      if (field && target) {
        target.textContent = Array.isArray(messages) ? messages.join(' · ') : String(messages);
        field.setAttribute('aria-invalid', 'true');
        if (!target.id) target.id = `${form.id}-${name}-error`;
        field.setAttribute('aria-describedby', name === 'password' ? `password-help ${target.id}` : target.id);
        firstInvalid ||= field;
      }
    }
  }
  firstInvalid?.focus();
}

async function submitForm(event, path, buildBody, onSuccess) {
  event.preventDefault();
  const form = event.currentTarget;
  if (form.dataset.busy) return;
  clearFormErrors(form);
  const button = form.querySelector('[type=submit]');
  const label = button.textContent;
  try {
    const body = buildBody(new FormData(form));
    form.dataset.busy = 'true';
    form.closest('dialog').querySelectorAll('button').forEach(node => { node.disabled = true; });
    button.textContent = 'Kaydediliyor…';
    const result = await request(path, { body });
    form.reset();
    form.closest('dialog').close();
    await onSuccess(result);
  } catch (error) {
    showFormError(form, error);
  } finally {
    delete form.dataset.busy;
    form.closest('dialog').querySelectorAll('button').forEach(node => { node.disabled = false; });
    if (form.id === 'product-form') button.disabled = !state.categories.length;
    button.textContent = label;
  }
}

async function openDetail(id) {
  detailController?.abort();
  const controller = new AbortController();
  detailController = controller;
  $('#detail-title').textContent = 'Ürün bilgileri';
  $('#detail-content').replaceChildren(element('p', 'dialog-description', 'Ürün yükleniyor…'));
  $('#detail-dialog').showModal();
  try {
    const product = await request(`/api/products/${id}`, { signal: controller.signal });
    if (controller.signal.aborted) return;
    $('#detail-title').textContent = product.productName;
    const list = element('dl');
    for (const [label, value] of [
      ['Ürün numarası', product.id], ['Kategori', product.category?.categoryName || 'Kategorisiz'],
      ['Birim fiyat', price.format(product.unitPrice)], ['Stok adedi', number.format(product.unitsInStock)],
      ['Stok durumu', stockStatus(product.unitsInStock).label], ['Paket bilgisi', product.quantityPerUnit || 'Belirtilmedi'],
    ]) {
      const row = element('div');
      row.append(element('dt', '', label), element('dd', '', value));
      list.append(row);
    }
    const add = element('button', 'button primary detail-add', product.unitsInStock > 0 ? '＋ Alışveriş listeme ekle' : 'Şimdilik tükendi');
    add.type = 'button';
    add.disabled = product.unitsInStock <= 0;
    add.addEventListener('click', () => addToCart(product));
    $('#detail-content').replaceChildren(illustration(product.category?.categoryName, 'detail-art'), list, add);
  } catch (error) {
    if (error.name !== 'AbortError') $('#detail-content').replaceChildren(element('p', 'banner error', error.message));
  }
}

$('#filters').addEventListener('submit', event => {
  event.preventDefault();
  state.search = $('#search').value.trim();
  state.categoryId = $('#category-filter').value;
  state.pageNo = 1;
  $('#clear-filters').hidden = !state.search && !state.categoryId;
  renderCategoryTiles();
  refreshProducts();
  $('#catalog').scrollIntoView({ block: 'start' });
});
$('#category-filter').addEventListener('change', () => $('#filters').requestSubmit());
$('#clear-filters').addEventListener('click', () => {
  $('#search').value = '';
  $('#category-filter').value = '';
  $('#filters').requestSubmit();
});
$('#page-size').addEventListener('change', event => {
  state.pageSize = Number(event.target.value);
  state.pageNo = 1;
  refreshProducts();
});
$('#previous').addEventListener('click', () => { state.pageNo--; refreshProducts(); });
$('#next').addEventListener('click', () => { state.pageNo++; refreshProducts(); });
$('#refresh').addEventListener('click', () => { refreshProducts(); refreshCategories(); });
$('#open-product').addEventListener('click', async () => {
  const form = $('#product-form');
  clearFormErrors(form);
  $('#product-dialog').showModal();
  const ok = await refreshCategories();
  if (!ok) showFormError(form, new ApiError('Kategoriler yüklenemedi. Pencereyi kapatıp yeniden deneyin.'));
  else if (!state.categories.length) showFormError(form, new ApiError('Ürün eklemek için veritabanında en az bir kategori bulunmalı.'));
});
$('#open-user').addEventListener('click', () => {
  clearFormErrors($('#user-form'));
  $('#user-dialog').showModal();
});
document.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click', () => button.closest('dialog').close()));
document.querySelectorAll('dialog').forEach(dialog => {
  dialog.addEventListener('cancel', event => { if (dialog.querySelector('form')?.dataset.busy) event.preventDefault(); });
  dialog.addEventListener('close', () => {
    if (dialog.id === 'user-dialog') $('#user-password').value = '';
    if (dialog.id === 'detail-dialog') detailController?.abort();
  });
});
$('#product-form').addEventListener('submit', event => submitForm(event, '/api/products', values => {
  const productName = values.get('productName').trim();
  if (!productName) throw new ApiError('Ürün adı boş olamaz.', { productName: ['Bir ürün adı girin.'] });
  return { productName, categoryId: Number(values.get('categoryId')), unitPrice: Number(values.get('unitPrice')),
    unitsInStock: Number(values.get('unitsInStock')), quantityPerUnit: values.get('quantityPerUnit').trim() || null };
}, async product => {
  toast(`${product.productName} kataloğa eklendi.`);
  state.pageNo = 1;
  state.search = '';
  state.categoryId = '';
  $('#search').value = '';
  $('#category-filter').value = '';
  $('#clear-filters').hidden = true;
  renderCategoryTiles();
  await refreshProducts();
}));
$('#user-form').addEventListener('submit', event => submitForm(event, '/api/users', values => {
  const password = values.get('password');
  if (new TextEncoder().encode(password).length > 72) {
    throw new ApiError('Şifre UTF-8 olarak 72 baytı aşamaz.', { password: ['Daha kısa bir şifre seçin.'] });
  }
  return { email: values.get('email').trim(), password };
}, user => toast(`${user.email} için kullanıcı kaydı oluşturuldu.`)));

let cart = [];
let storageNoticeShown = false;
try { cart = restoreCart(localStorage.getItem(CART_KEY)); } catch { /* Storage may be disabled. */ }

function saveCart() {
  try { localStorage.setItem(CART_KEY, JSON.stringify(cart)); }
  catch {
    if (!storageNoticeShown) {
      storageNoticeShown = true;
      toast('Tarayıcı depolaması kapalı. Listen bu sayfa açıkken korunacak.');
    }
  }
}

function addToCart(product) {
  const before = cart.find(line => line.product.id === product.id)?.quantity || 0;
  cart = changeQuantity(cart, product, 1);
  const after = cart.find(line => line.product.id === product.id)?.quantity || 0;
  renderCart();
  saveCart();
  if (after > before) toast(product.productName + ' listene eklendi.');
  else toast('Bu ürün için katalogdaki stok sınırına ulaştın.');
}

function renderCart() {
  const summary = cartSummary(cart);
  $('#cart-count').textContent = number.format(summary.count);
  $('#cart-total').textContent = price.format(summary.total);
  const fragment = document.createDocumentFragment();
  for (const line of cart) {
    const row = element('div', 'cart-item');
    const info = element('div', 'cart-item-info');
    info.append(element('h3', '', line.product.productName), element('p', '', price.format(line.product.unitPrice)));
    const controls = element('div', 'quantity-control');
    for (const delta of [-1, 1]) {
      if (delta === 1) controls.append(element('span', '', number.format(line.quantity)));
      const button = element('button', '', delta < 0 ? '−' : '+');
      button.type = 'button';
      button.setAttribute('aria-label', line.product.productName + (delta < 0 ? ' adet azalt' : ' adet artır'));
      button.disabled = delta > 0 && line.quantity >= line.product.unitsInStock;
      button.addEventListener('click', () => {
        cart = changeQuantity(cart, line.product, delta);
        renderCart();
        saveCart();
        // Keep keyboard focus on the changed row after rebuilding the list.
        const updated = [...$('#cart-items').querySelectorAll('.cart-item')]
          .find(node => node.dataset.productId === String(line.product.id));
        (updated?.querySelector('button:not(:disabled)') || $('#cart-dialog [data-close]')).focus();
      });
      controls.append(button);
    }
    row.dataset.productId = String(line.product.id);
    row.append(illustration(line.product.category?.categoryName, 'cart-item-art'), info, controls);
    fragment.append(row);
  }
  if (!cart.length) {
    const empty = element('div', 'empty-state');
    empty.append(illustration('', 'empty-icon'), element('h3', '', 'Listen güzel şeyler bekliyor.'),
      element('p', '', 'Ürünlerdeki + düğmesine dokun, beğendiklerini burada bir araya getir.'));
    fragment.append(empty);
  }
  $('#cart-items').replaceChildren(fragment);
}

$('#open-cart').addEventListener('click', () => {
  renderCart();
  $('#cart-dialog').showModal();
});
window.addEventListener('storage', event => {
  if (event.key === CART_KEY || event.key === null) {
    try { cart = restoreCart(localStorage.getItem(CART_KEY)); renderCart(); } catch { /* Keep the in-memory list. */ }
  }
});
renderCart();

refreshProducts();
refreshCategories();

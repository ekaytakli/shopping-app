import { test } from 'node:test';
import assert from 'node:assert/strict';
import { changeQuantity, restoreCart, cartSummary, categoryTheme } from '../../main/resources/static/js/cart.mjs';

const product = { id: 1, productName: 'Chai', unitPrice: 12.5, unitsInStock: 2, category: { categoryName: 'Beverages' } };

test('adding twice combines quantities without changing the original list', () => {
  const first = changeQuantity([], product, 1);
  const second = changeQuantity(first, product, 1);
  assert.equal(first[0].quantity, 1);
  assert.equal(second.length, 1);
  assert.equal(second[0].quantity, 2);
});

test('quantities stay within available stock and reaching zero removes the item', () => {
  const cart = changeQuantity([], product, 10);
  assert.equal(cart[0].quantity, 2);
  assert.deepEqual(changeQuantity(cart, product, -2), []);
  assert.deepEqual(changeQuantity([], { ...product, unitsInStock: 0 }, 1), []);
});

test('restoring rejects malformed or manipulated storage and deduplicates products', () => {
  assert.deepEqual(restoreCart('{bad-json'), []);
  assert.deepEqual(restoreCart('{}'), []);
  assert.deepEqual(restoreCart(JSON.stringify([{ product: { ...product, unitPrice: -5 }, quantity: 1 }])), []);
  const restored = restoreCart(JSON.stringify([{ product, quantity: 99 }, { product, quantity: 1 }, { product, quantity: -1 }]));
  assert.equal(restored.length, 1);
  assert.equal(restored[0].quantity, 2);
});

test('totals count units and use catalog prices', () => {
  const cart = [{ product, quantity: 2 }, { product: { ...product, id: 2, unitPrice: 3.75 }, quantity: 3 }];
  assert.deepEqual(cartSummary(cart), { count: 5, total: 36.25 });
  assert.deepEqual(cartSummary([]), { count: 0, total: 0 });
});

test('catalog updates supply the current price and stock when adding again', () => {
  const previous = changeQuantity([], product, 2);
  const updated = changeQuantity(previous, { ...product, unitPrice: 20, unitsInStock: 1 }, 1);
  assert.equal(updated[0].quantity, 1);
  assert.equal(updated[0].product.unitPrice, 20);
});

test('Turkish and English category names use matching representative artwork', () => {
  assert.equal(categoryTheme('Beverages').illustration, 'beverages');
  assert.equal(categoryTheme('İÇECEKLER').illustration, 'beverages');
  assert.equal(categoryTheme('Dairy Products').illustration, 'dairy');
  assert.equal(categoryTheme('Süt Ürünleri').illustration, 'dairy');
  assert.equal(categoryTheme('Seafood').illustration, 'seafood');
  assert.equal(categoryTheme('Yeni kategori').illustration, 'all');
  assert.equal(categoryTheme(null).illustration, 'all');
});

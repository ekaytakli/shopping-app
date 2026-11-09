import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ApiError, request, paginate, loadProducts, stockStatus } from '../../main/resources/static/js/api.mjs';

test('pagination clamps pages and handles an empty catalog', () => {
  assert.deepEqual(paginate([], 3, 10), { content: [], pageNo: 1, pageSize: 10, totalElements: 0, totalPages: 0, last: true });
  assert.deepEqual(paginate([1, 2, 3], 5, 2), { content: [3], pageNo: 2, pageSize: 2, totalElements: 3, totalPages: 2, last: true });
});

test('stock labels distinguish zero, low and sufficient inventory', () => {
  assert.equal(stockStatus(0).label, 'Tükendi');
  assert.equal(stockStatus(9).label, 'Düşük stok');
  assert.equal(stockStatus(10).label, 'Stokta');
});

test('unfiltered listing uses server pagination', async t => {
  t.mock.method(globalThis, 'fetch', async path => {
    assert.equal(path, '/api/products?pageNo=2&pageSize=10');
    return Response.json({ success: true, data: { pageNo: 2, content: [] } });
  });
  assert.equal((await loadProducts({ pageNo: 2, pageSize: 10, search: '', categoryId: '' })).pageNo, 2);
});

test('search safely encodes text and combines category filtering before pagination', async t => {
  t.mock.method(globalThis, 'fetch', async path => {
    assert.equal(new URL(path, 'http://localhost').searchParams.get('productName'), 'Chai & tea');
    return Response.json({ success: true, data: [
      { id: 3, category: { categoryId: 2 } }, { id: 2, category: { categoryId: 1 } },
      { id: 1, category: { categoryId: 2 } },
    ] });
  });
  const result = await loadProducts({ pageNo: 2, pageSize: 1, search: 'Chai & tea', categoryId: '2' });
  assert.equal(result.totalElements, 2);
  assert.equal(result.content[0].id, 3);
});

test('category-only filter uses the existing backend category endpoint', async t => {
  t.mock.method(globalThis, 'fetch', async path => {
    assert.equal(path, '/api/products/getByCategory_CategoryIdIn?categories=2');
    return Response.json({ success: true, data: [] });
  });
  assert.equal((await loadProducts({ pageNo: 1, pageSize: 10, search: '', categoryId: '2' })).totalElements, 0);
});

test('POST sends JSON and surfaces validation details', async t => {
  t.mock.method(globalThis, 'fetch', async (path, options) => {
    assert.equal(options.method, 'POST');
    assert.deepEqual(JSON.parse(options.body), { productName: '' });
    return Response.json({ success: false, message: 'Doğrulama hataları', data: { productName: ['Boş olamaz'] } }, { status: 400 });
  });
  await assert.rejects(request('/api/products', { body: { productName: '' } }), error => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.status, 400);
    assert.deepEqual(error.fields.productName, ['Boş olamaz']);
    return true;
  });
});

test('network errors, invalid JSON and cancellation remain distinguishable', async t => {
  const mock = t.mock.method(globalThis, 'fetch', async () => { throw new TypeError('offline'); });
  await assert.rejects(request('/api/categories'), /Sunucuya ulaşılamadı/);
  mock.mock.mockImplementation(async () => new Response('<html>Error</html>', { status: 502 }));
  await assert.rejects(request('/api/categories'), /HTTP 502/);
  mock.mock.mockImplementation(async () => { throw new DOMException('Cancelled', 'AbortError'); });
  await assert.rejects(request('/api/categories'), { name: 'AbortError' });
});

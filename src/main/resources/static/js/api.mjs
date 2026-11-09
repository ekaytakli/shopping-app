export class ApiError extends Error {
  constructor(message, fields = {}, status = 0) {
    super(message);
    this.fields = fields;
    this.status = status;
  }
}

export async function request(path, { body, signal } = {}) {
  let response;
  try {
    response = await fetch(path, {
      method: body === undefined ? 'GET' : 'POST',
      headers: { Accept: 'application/json', ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      signal,
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new ApiError('Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.');
  }
  const result = await response.json().catch(() => null);
  if (!response.ok || result?.success !== true) {
    throw new ApiError(result?.message || `İstek tamamlanamadı (HTTP ${response.status}).`, result?.data, response.status);
  }
  return result.data;
}

export function paginate(items, pageNo, pageSize) {
  const totalElements = items.length;
  const totalPages = Math.ceil(totalElements / pageSize);
  const current = Math.min(Math.max(1, pageNo), Math.max(1, totalPages));
  return { content: items.slice((current - 1) * pageSize, current * pageSize), pageNo: current,
    pageSize, totalElements, totalPages, last: current >= totalPages };
}

export async function loadProducts({ pageNo, pageSize, search, categoryId }, signal) {
  if (!search && !categoryId) {
    return request(`/api/products?${new URLSearchParams({ pageNo, pageSize })}`, { signal });
  }
  const path = search
    ? `/api/products/getByProductNameContains?${new URLSearchParams({ productName: search })}`
    : `/api/products/getByCategory_CategoryIdIn?${new URLSearchParams({ categories: categoryId })}`;
  let items = await request(path, { signal });
  if (categoryId) items = items.filter(item => String(item.category?.categoryId) === String(categoryId));
  items.sort((a, b) => a.id - b.id);
  return paginate(items, pageNo, pageSize);
}

export function stockStatus(stock) {
  if (stock <= 0) return { label: 'Tükendi', className: 'stock-out' };
  if (stock < 10) return { label: 'Düşük stok', className: 'stock-low' };
  return { label: 'Stokta', className: 'stock-ok' };
}

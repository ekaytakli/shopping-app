export const CART_KEY = 'northwind-market-list-v1';

function safeProduct(product) {
  if (!product || !Number.isSafeInteger(product.id) || product.id < 1 ||
      typeof product.productName !== 'string' || !product.productName.trim() ||
      !Number.isFinite(product.unitPrice) || product.unitPrice < 0 || product.unitPrice > 9999999999.99 ||
      !Number.isInteger(product.unitsInStock) || product.unitsInStock < 0 || product.unitsInStock > 32767) return null;
  return { id: product.id, productName: product.productName.slice(0, 40),
    unitPrice: product.unitPrice, unitsInStock: product.unitsInStock,
    category: typeof product.category?.categoryName === 'string'
      ? { categoryName: product.category.categoryName.slice(0, 100) } : null };
}

export function restoreCart(serialized) {
  try {
    const saved = JSON.parse(serialized);
    if (!Array.isArray(saved)) return [];
    const ids = new Set();
    return saved.slice(0, 500).flatMap(item => {
      const product = safeProduct(item?.product);
      if (!product || ids.has(product.id) || !Number.isInteger(item.quantity) || item.quantity < 1 || !product.unitsInStock) return [];
      ids.add(product.id);
      return [{ product, quantity: Math.min(item.quantity, product.unitsInStock) }];
    });
  } catch { return []; }
}

export function changeQuantity(lines, source, delta) {
  const product = safeProduct(source);
  if (!product || !Number.isInteger(delta)) return lines;
  const current = lines.find(line => line.product.id === product.id);
  const quantity = Math.min(product.unitsInStock, Math.max(0, (current?.quantity || 0) + delta));
  if (quantity === 0) return lines.filter(line => line.product.id !== product.id);
  if (current) return lines.map(line => line.product.id === product.id ? { product, quantity } : line);
  return [...lines, { product, quantity }];
}

export function cartSummary(lines) {
  return lines.reduce((summary, line) => ({
    count: summary.count + line.quantity,
    total: summary.total + Math.round(line.product.unitPrice * 100) * line.quantity / 100,
  }), { count: 0, total: 0 });
}

export function categoryTheme(name = '') {
  const normalized = typeof name === 'string' ? name.toLocaleLowerCase('tr-TR') : '';
  const themes = [
    [/beverage|içecek|icecek/, 'beverages', '#fff5e8'],
    [/condiment|sos|baharat/, 'condiments', '#fff0e9'],
    [/confection|tatlı|tatli|atıştır|atistir|şeker|seker/, 'sweets', '#f7f0ff'],
    [/dairy|süt|sut|peynir/, 'dairy', '#eff5ff'],
    [/grain|cereal|tahıl|tahil|ekmek|bakliyat/, 'grains', '#fff7e8'],
    [/meat|poultry|^et$|et ve|tavuk/, 'meat', '#fff0f0'],
    [/produce|sebze|meyve/, 'produce', '#eff8ec'],
    [/seafood|balık|balik|deniz/, 'seafood', '#edf8fa'],
  ];
  const match = themes.find(([pattern]) => pattern.test(normalized));
  return { illustration: match?.[1] || 'all', background: match?.[2] || '#fff2e8' };
}

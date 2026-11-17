const API = '/api';

let currentCustomerId = localStorage.getItem('shop-customer-id') || null;
let latestProducts = [];

function showToast(message, type) {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.className = 'toast ' + (type || '');
  toast.classList.remove('hidden');
  clearTimeout(showToast._t);
  showToast._t = setTimeout(() => toast.classList.add('hidden'), 2500);
}

async function api(path, options) {
  const res = await fetch(API + path, Object.assign({ headers: { 'Content-Type': 'application/json' } }, options));
  const body = await res.json();
  if (!res.ok || body.success === false) {
    throw new Error(body.message || ('요청 실패 (' + res.status + ')'));
  }
  return body.data;
}

function won(n) { return Number(n).toLocaleString() + '원'; }

function productIcon(name) {
  const table = [
    [/마우스/, '🖱️'], [/키보드/, '⌨️'], [/허브|usb/i, '🔌'],
    [/모니터/, '🖥️'], [/파우치|가방/, '🎒'], [/헤드셋|이어폰/, '🎧'],
    [/노트북/, '💻'], [/스피커/, '🔊'], [/충전/, '🔋'],
  ];
  for (const [re, icon] of table) if (re.test(name)) return icon;
  return '📦';
}

function stockBadge(stock) {
  if (stock <= 0) return '<span class="stock-badge stock-out">품절</span>';
  if (stock <= 10) return '<span class="stock-badge stock-low">재고 ' + stock + '개</span>';
  return '<span class="stock-badge stock-ok">재고 ' + stock + '개</span>';
}

/* ---------- Auth / Modal ---------- */

function openModal(id) {
  document.getElementById('modal-backdrop').classList.remove('hidden');
  document.getElementById(id).classList.remove('hidden');
}

function closeModal(id) {
  document.getElementById(id).classList.add('hidden');
  const anyOpen = ['login-panel', 'signup-panel'].some(p => !document.getElementById(p).classList.contains('hidden'));
  if (!anyOpen) document.getElementById('modal-backdrop').classList.add('hidden');
}

function closeAllModals() {
  ['login-panel', 'signup-panel'].forEach(id => document.getElementById(id).classList.add('hidden'));
  document.getElementById('modal-backdrop').classList.add('hidden');
}

function setLoggedIn(customerId) {
  currentCustomerId = customerId;
  localStorage.setItem('shop-customer-id', customerId);
  refreshAuthView();
  refreshMyPage();
}

function setLoggedOut() {
  currentCustomerId = null;
  localStorage.removeItem('shop-customer-id');
  refreshAuthView();
}

function refreshAuthView() {
  const guest = document.getElementById('guest-view');
  const user = document.getElementById('user-view');
  const mypage = document.getElementById('mypage-panel');
  if (currentCustomerId) {
    guest.classList.add('hidden');
    user.classList.remove('hidden');
    document.getElementById('user-info').textContent = currentCustomerId + '님';
    mypage.classList.remove('hidden');
  } else {
    guest.classList.remove('hidden');
    user.classList.add('hidden');
    mypage.classList.add('hidden');
  }
}

/* ---------- Products ---------- */

async function loadProducts(keyword) {
  const query = keyword ? ('?count=50&keyword=' + encodeURIComponent(keyword)) : '?count=50';
  const paged = await api('/products' + query);
  latestProducts = paged.content;
  renderProducts(paged.content);
}

function renderProducts(products) {
  const grid = document.getElementById('product-grid');
  if (products.length === 0) {
    grid.innerHTML = '<div class="empty-state">검색 결과가 없어요.</div>';
    return;
  }
  grid.innerHTML = products.map(p => `
    <div class="product-card">
      <div class="product-icon">${productIcon(p.productName)}</div>
      <div class="product-name">${p.productName}</div>
      <div class="product-price">${won(p.productPrice)}</div>
      ${stockBadge(p.productStock)}
      <div class="qty-row">
        <div class="stepper">
          <button type="button" data-step="-1" data-id="${p.id}">−</button>
          <input type="number" min="1" max="${p.productStock}" value="1" data-qty="${p.id}">
          <button type="button" data-step="1" data-id="${p.id}">+</button>
        </div>
        <button class="btn btn-primary btn-sm" data-order-id="${p.id}" ${p.productStock <= 0 ? 'disabled' : ''}>주문</button>
      </div>
    </div>
  `).join('');

  grid.querySelectorAll('[data-step]').forEach(btn => {
    btn.addEventListener('click', () => {
      const input = grid.querySelector('input[data-qty="' + btn.dataset.id + '"]');
      const next = parseInt(input.value || '1', 10) + parseInt(btn.dataset.step, 10);
      const max = parseInt(input.max || '999', 10);
      input.value = Math.min(Math.max(next, 1), max);
    });
  });

  grid.querySelectorAll('[data-order-id]').forEach(btn => {
    btn.addEventListener('click', async () => {
      if (!currentCustomerId) {
        showToast('로그인 후 주문할 수 있습니다.', 'error');
        openModal('login-panel');
        return;
      }
      const id = btn.getAttribute('data-order-id');
      const qtyInput = grid.querySelector('input[data-qty="' + id + '"]');
      const quantity = parseInt(qtyInput.value, 10);
      try {
        await api('/customers/order', { method: 'POST', body: JSON.stringify({ productId: Number(id), quantity }) });
        showToast('주문이 완료되었습니다.', 'success');
        loadProducts(document.getElementById('search-keyword').value);
        refreshMyPage();
      } catch (e) {
        showToast(e.message, 'error');
      }
    });
  });
}

/* ---------- My page ---------- */

async function refreshMyPage() {
  if (!currentCustomerId) return;
  try {
    const detail = await api('/customers/' + encodeURIComponent(currentCustomerId));
    document.getElementById('my-point').textContent = detail.customerPoint.toLocaleString();

    const orderList = document.getElementById('order-list');
    if (detail.products.length === 0) {
      orderList.innerHTML = '<div class="empty-state">아직 주문한 상품이 없어요.</div>';
    } else {
      orderList.innerHTML = detail.products.map(item => `
        <div class="order-row">
          <div class="order-row-main">
            <div class="order-row-name">${productIcon(item.productName)} ${item.productName}</div>
            <div class="order-row-meta">${won(item.productPrice)} · ${item.quantity}개</div>
          </div>
          <div class="order-subtotal">${won(item.subtotal)}</div>
          <div class="order-row-actions">
            <input type="number" class="qty-input" style="width:56px" min="1" max="${item.quantity}" value="1" data-cancel-qty="${item.productId}">
            <button class="btn btn-danger btn-sm" data-cancel-id="${item.productId}">취소</button>
          </div>
        </div>
      `).join('');

      orderList.querySelectorAll('[data-cancel-id]').forEach(btn => {
        btn.addEventListener('click', async () => {
          const id = btn.getAttribute('data-cancel-id');
          const qtyInput = orderList.querySelector('input[data-cancel-qty="' + id + '"]');
          const quantity = parseInt(qtyInput.value, 10);
          try {
            await api('/customers/cancel', { method: 'POST', body: JSON.stringify({ productId: Number(id), quantity }) });
            showToast('주문이 취소되었습니다.', 'success');
            loadProducts(document.getElementById('search-keyword').value);
            refreshMyPage();
          } catch (e) {
            showToast(e.message, 'error');
          }
        });
      });
    }

    const history = await api('/customers/' + encodeURIComponent(currentCustomerId) + '/points');
    const historyBody = document.querySelector('#point-history-table tbody');
    historyBody.innerHTML = history.map(h => {
      const sign = h.changeAmount >= 0 ? '+' : '';
      const cls = h.changeAmount >= 0 ? 'amount-pos' : 'amount-neg';
      return `
        <tr>
          <td><span class="reason-pill reason-${h.reason}">${h.reason}</span></td>
          <td class="${cls}">${sign}${h.changeAmount.toLocaleString()}</td>
          <td>${h.balanceAfter.toLocaleString()}</td>
          <td>${h.createdAt.replace('T', ' ').substring(0, 19)}</td>
        </tr>`;
    }).join('');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

/* ---------- Event wiring ---------- */

document.getElementById('btn-show-login').addEventListener('click', () => openModal('login-panel'));
document.getElementById('btn-show-signup').addEventListener('click', () => openModal('signup-panel'));
document.getElementById('modal-backdrop').addEventListener('click', closeAllModals);
document.querySelectorAll('.modal-close').forEach(btn => {
  btn.addEventListener('click', () => closeModal(btn.dataset.close));
});

document.getElementById('login-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const customerId = document.getElementById('login-id').value;
  const customerPassword = document.getElementById('login-pw').value;
  try {
    const data = await api('/customers/login', { method: 'POST', body: JSON.stringify({ customerId, customerPassword }) });
    showToast('로그인 되었습니다.', 'success');
    closeAllModals();
    setLoggedIn(data.customerId);
  } catch (e2) {
    showToast(e2.message, 'error');
  }
});

document.getElementById('signup-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const customerId = document.getElementById('signup-id').value;
  const customerPassword = document.getElementById('signup-pw').value;
  try {
    await api('/customers', { method: 'POST', body: JSON.stringify({ customerId, customerPassword }) });
    showToast('회원가입이 완료되었습니다. 로그인해주세요.', 'success');
    closeModal('signup-panel');
    openModal('login-panel');
  } catch (e2) {
    showToast(e2.message, 'error');
  }
});

document.getElementById('btn-logout').addEventListener('click', async () => {
  try {
    await api('/customers/logout', { method: 'POST' });
  } catch (e) {
    // ignore
  }
  setLoggedOut();
  showToast('로그아웃 되었습니다.', 'success');
});

document.getElementById('btn-search').addEventListener('click', () => {
  loadProducts(document.getElementById('search-keyword').value);
});
document.getElementById('search-keyword').addEventListener('keydown', (e) => {
  if (e.key === 'Enter') { e.preventDefault(); loadProducts(e.target.value); }
});

document.getElementById('btn-refresh').addEventListener('click', () => {
  document.getElementById('search-keyword').value = '';
  loadProducts();
});

refreshAuthView();
loadProducts();
refreshMyPage();

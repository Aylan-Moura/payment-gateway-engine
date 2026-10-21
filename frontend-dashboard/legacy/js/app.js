'use strict';

// Configuração -> apontar para onde o api-gateway estiver hospedado
const GATEWAY_BASE_URL = 'http://localhost:8080';
// Base URL pública dos comprovantes no S3 (ajustar conforme o bucket/publicação)
const RECEIPT_BASE_URL = 'https://receipts.payment-gateway.local';

const LS_TOKEN = 'pgw_token';
const LS_REFRESH = 'pgw_refresh';
const LS_MERCHANT_ID = 'pgw_merchant_id';
const LS_API_KEY = 'pgw_api_key';
const LS_EMAIL = 'pgw_email';
const LS_TRANSACTIONS = 'pgw_transactions';

const $ = (id) => document.getElementById(id);

function showAlert(el, message, type) {
    el.textContent = message;
    el.className = 'alert alert-' + type + ' mt-3 mb-0';
}

function hideAlert(el) {
    el.className = 'alert d-none mt-3 mb-0';
}

async function request(path, options) {
    const resp = await fetch(GATEWAY_BASE_URL + path, options);
    if (!resp.ok) {
        let detail = 'HTTP ' + resp.status;
        try {
            const body = await resp.json();
            if (body.message) detail = body.message;
            else if (body.error) detail = body.error;
        } catch (e) { /* corpo não-JSON */ }
        throw new Error(detail);
    }
    return resp.json();
}

function setSession(payload) {
    localStorage.setItem(LS_TOKEN, payload.token);
    localStorage.setItem(LS_REFRESH, payload.refreshToken);
    localStorage.setItem(LS_MERCHANT_ID, payload.merchantId);
    localStorage.setItem(LS_API_KEY, payload.apiKey);
    localStorage.setItem(LS_EMAIL, getEmailFromJwt(payload.token));
}

function getEmailFromJwt(token) {
    try {
        const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        return JSON.parse(atob(base64)).sub;
    } catch (e) {
        return 'merchant';
    }
}

function formatDate(iso) {
    return new Date(iso).toLocaleString('pt-BR');
}

function formatBRL(value) {
    return Number(value).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function isAuthenticated() {
    return !!localStorage.getItem(LS_TOKEN);
}

// ---------- Transações locais ----------
function loadTransactions() {
    try {
        return JSON.parse(localStorage.getItem(LS_TRANSACTIONS)) || [];
    } catch (e) {
        return [];
    }
}

function saveTransactions(txs) {
    localStorage.setItem(LS_TRANSACTIONS, JSON.stringify(txs));
}

// ---------- Renderização ----------
function renderAuth() {
    $('authSection').classList.remove('d-none');
    $('dashboardSection').classList.add('d-none');
    $('navMerchant').classList.add('d-none');
    $('logoutBtn').classList.add('d-none');
}

function renderDashboard() {
    $('authSection').classList.add('d-none');
    $('dashboardSection').classList.remove('d-none');
    $('navMerchant').classList.remove('d-none');
    $('logoutBtn').classList.remove('d-none');
    $('navMerchant').textContent = localStorage.getItem(LS_EMAIL);
    $('payMerchantId').value = localStorage.getItem(LS_API_KEY) || '-';
    renderTransactions();
}

const BADGE_CLASSES = {
    PENDING: 'bg-warning text-dark',
    APPROVED: 'bg-success',
    REJECTED: 'bg-danger'
};

function renderTransactions() {
    const txs = loadTransactions();
    const tbody = $('transactionsBody');
    tbody.innerHTML = '';
    $('emptyState').classList.toggle('d-none', txs.length > 0);

    txs.forEach((tx, index) => {
        const badge = BADGE_CLASSES[tx.status] || 'bg-secondary';
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><code class="small">${tx.transactionId}</code></td>
            <td>${formatBRL(tx.amount)}</td>
            <td>${tx.paymentMethod}</td>
            <td><span class="badge ${badge}">${tx.status}</span></td>
            <td>${formatDate(tx.createdAt)}</td>
            <td class="text-end">
                <button class="btn btn-sm btn-outline-primary" data-download="${index}">Comprovante</button>
            </td>`;
        tbody.appendChild(tr);
    });

    tbody.querySelectorAll('[data-download]').forEach((btn) => {
        btn.addEventListener('click', () => downloadReceipt(btn.dataset.download));
    });
}

function downloadReceipt(index) {
    const tx = loadTransactions()[index];
    if (!tx) return;
    const url = RECEIPT_BASE_URL + '/' + tx.merchantId + '/' + tx.transactionId + '/receipt.pdf';
    window.open(url, '_blank');
}

// ---------- Fluxos principais ----------
async function onLogin(event) {
    event.preventDefault();
    const alertEl = $('authAlert');
    hideAlert(alertEl);
    try {
        const payload = await request('/api/v1/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                email: $('loginEmail').value.trim(),
                password: $('loginPassword').value
            })
        });
        setSession(payload);
        renderDashboard();
    } catch (err) {
        showAlert(alertEl, 'Falha no login: ' + err.message, 'danger');
    }
}

async function onRegister(event) {
    event.preventDefault();
    const alertEl = $('authAlert');
    hideAlert(alertEl);
    try {
        const payload = await request('/api/v1/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                companyName: $('regCompany').value.trim(),
                email: $('regEmail').value.trim(),
                password: $('regPassword').value
            })
        });
        setSession(payload);
        renderDashboard();
    } catch (err) {
        showAlert(alertEl, 'Falha no cadastro: ' + err.message, 'danger');
    }
}

async function onPayment(event) {
    event.preventDefault();
    const alertEl = $('payAlert');
    hideAlert(alertEl);
    const btn = $('payBtn');
    btn.disabled = true;
    try {
        const payload = await request('/api/v1/payments', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + localStorage.getItem(LS_TOKEN),
                'X-API-Key': localStorage.getItem(LS_API_KEY)
            },
            body: JSON.stringify({
                merchantId: localStorage.getItem(LS_MERCHANT_ID),
                amount: $('payAmount').value,
                paymentMethod: $('payMethod').value
            })
        });

        const txs = loadTransactions();
        txs.unshift({
            transactionId: payload.transactionId,
            merchantId: localStorage.getItem(LS_MERCHANT_ID),
            amount: $('payAmount').value,
            paymentMethod: $('payMethod').value,
            status: payload.status,
            createdAt: new Date().toISOString()
        });
        saveTransactions(txs);
        renderTransactions();
        showAlert(alertEl, 'Transação criada: ' + payload.transactionId + ' (status ' + payload.status + ')', 'success');
        $('payAmount').value = '';
    } catch (err) {
        showAlert(alertEl, 'Falha ao processar pagamento: ' + err.message, 'danger');
    } finally {
        btn.disabled = false;
    }
}

function logout() {
    [
        LS_TOKEN, LS_REFRESH, LS_MERCHANT_ID, LS_API_KEY, LS_EMAIL
    ].forEach((k) => localStorage.removeItem(k));
    renderAuth();
}

// ---------- Bootstrap ----------
document.addEventListener('DOMContentLoaded', () => {
    $('loginForm').addEventListener('submit', onLogin);
    $('registerForm').addEventListener('submit', onRegister);
    $('paymentForm').addEventListener('submit', onPayment);
    $('logoutBtn').addEventListener('click', logout);

    if (isAuthenticated()) {
        renderDashboard();
    } else {
        renderAuth();
    }
});
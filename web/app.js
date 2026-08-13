// Chadet Web Application Engine
const API_BASE = 'http://localhost:8080';

const state = {
  token: localStorage.getItem('chadet_jwt') || null,
  user: null,
  wallet: null,
  conversations: [],
  activeConversation: null,
  messages: [],
  stompClient: null,
  tradeStompClient: null,
  activeSubscription: null,
  tradesCache: {} // tradeId -> Trade object
};

// --- DOM Elements ---
const authScreen = document.getElementById('authScreen');
const loginForm = document.getElementById('loginForm');
const registerForm = document.getElementById('registerForm');
const authTitle = document.getElementById('authTitle');
const switchAuthLink = document.getElementById('switchAuthLink');
const switchText = document.getElementById('switchText');

const userNameDisplay = document.getElementById('userNameDisplay');
const userAvatar = document.getElementById('userAvatar');
const headerBalance = document.getElementById('headerBalance');
const headerHeld = document.getElementById('headerHeld');
const btnLogout = document.getElementById('btnLogout');
const btnOpenDeposit = document.getElementById('btnOpenDeposit');

const conversationList = document.getElementById('conversationList');
const chatHeaderTitle = document.getElementById('chatHeaderTitle');
const chatHeaderSubtitle = document.getElementById('chatHeaderSubtitle');
const chatHeaderActions = document.getElementById('chatHeaderActions');
const messagesContainer = document.getElementById('messagesContainer');
const chatInputBar = document.getElementById('chatInputBar');
const messageInput = document.getElementById('messageInput');
const btnSendMessage = document.getElementById('btnSendMessage');
const btnCreateTrade = document.getElementById('btnCreateTrade');

const btnNewDirect = document.getElementById('btnNewDirect');
const btnNewGroup = document.getElementById('btnNewGroup');
const modalDirectChat = document.getElementById('modalDirectChat');
const modalGroupChat = document.getElementById('modalGroupChat');
const modalNewTrade = document.getElementById('modalNewTrade');
const modalDeposit = document.getElementById('modalDeposit');

const toastContainer = document.getElementById('toastContainer');

// --- Helper Functions ---
function showToast(msg) {
  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.textContent = msg;
  toastContainer.appendChild(toast);
  setTimeout(() => toast.remove(), 4000);
}

function openModal(modal) {
  modal.classList.remove('hidden');
}

function closeModal(modal) {
  modal.classList.add('hidden');
}

document.querySelectorAll('[data-close]').forEach(btn => {
  btn.addEventListener('click', () => {
    const modalId = btn.getAttribute('data-close');
    document.getElementById(modalId)?.classList.add('hidden');
  });
});

// --- API Client ---
async function apiCall(endpoint, method = 'GET', body = null) {
  const headers = { 'Content-Type': 'application/json' };
  if (state.token) {
    console.log(state.token)
    headers['Authorization'] = `Bearer ${state.token}`;
  }

  try {
    const res = await fetch(`${API_BASE}${endpoint}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : null
    });

    if (res.status === 401) {
      handleLogout();
      throw new Error('Session expired, please login again');
    }

    if (res.status === 204) return null;

    const text = await res.text();
    if (!res.ok) {
      throw new Error(text || `Error ${res.status}`);
    }

    return text ? JSON.parse(text) : null;
  } catch (err) {
    showToast(`Error: ${err.message}`);
    throw err;
  }
}

// --- Auth Handling ---
switchAuthLink.addEventListener('click', () => {
  const isLoginVisible = !loginForm.classList.contains('hidden');
  if (isLoginVisible) {
    loginForm.classList.add('hidden');
    registerForm.classList.remove('hidden');
    authTitle.textContent = 'Create an Account';
    switchText.textContent = 'Already have an account?';
    switchAuthLink.textContent = 'Sign In';
  } else {
    registerForm.classList.add('hidden');
    loginForm.classList.remove('hidden');
    authTitle.textContent = 'Welcome Back';
    switchText.textContent = "Don't have an account?";
    switchAuthLink.textContent = 'Register';
  }
});

loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const gmail = document.getElementById('loginGmail').value;
  const password = document.getElementById('loginPassword').value;

  try {
    const data = await apiCall('/api/auth/login', 'POST', { gmail, password });
    handleAuthSuccess(data.token);
  } catch (e) { }
});

registerForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const gmail = document.getElementById('regGmail').value;
  const username = document.getElementById('regUsername').value;
  const password = document.getElementById('regPassword').value;

  try {
    const data = await apiCall('/api/auth/register', 'POST', { gmail, username, password });
    handleAuthSuccess(data.token);
  } catch (e) { }
});

function handleAuthSuccess(token) {
  state.token = token;
  localStorage.setItem('chadet_jwt', token);
  authScreen.classList.add('hidden');
  initApp();
}

function handleLogout() {
  state.token = null;
  state.user = null;
  localStorage.removeItem('chadet_jwt');
  if (state.stompClient) {
    state.stompClient.deactivate();
  }
  if (state.tradeStompClient) {
    state.tradeStompClient.deactivate();
  }
  authScreen.classList.remove('hidden');
}

btnLogout.addEventListener('click', handleLogout);

// --- Initialization ---
async function initApp() {
  try {
    state.user = await apiCall('/api/user/me');
    userNameDisplay.textContent = `${state.user.username}`;
    userAvatar.textContent = (state.user.username || 'U')[0].toUpperCase();

    await refreshWallet();
    await loadConversations();
    initWebSocket();
    initTradeWebSocket();
  } catch (e) {
    console.error('Failed to init app', e);
  }
}

// --- Wallet ---
async function refreshWallet() {
  try {
    state.wallet = await apiCall('/api/wallet');
    headerBalance.textContent = `$${Number(state.wallet.balance).toFixed(2)}`;
    headerHeld.textContent = `($${Number(state.wallet.heldBalance).toFixed(2)} in escrow)`;
  } catch (e) { }
}

btnOpenDeposit.addEventListener('click', () => openModal(modalDeposit));

document.getElementById('formDeposit').addEventListener('submit', async (e) => {
  e.preventDefault();
  const amount = Number(document.getElementById('depositAmount').value);
  try {
    await apiCall('/api/wallet/deposit', 'POST', { amount });
    showToast(`Successfully deposited $${amount.toFixed(2)}`);
    closeModal(modalDeposit);
    await refreshWallet();
  } catch (e) { }
});

// --- Conversations & Messages ---
async function loadConversations() {
  state.conversations = await apiCall('/api/conversations') || [];
  renderConversationList();
}

function renderConversationList() {
  conversationList.innerHTML = '';
  if (state.conversations.length === 0) {
    conversationList.innerHTML = '<li style="padding: 20px; text-align: center; color: var(--text-muted); font-size: 0.85rem;">No chats yet. Start one!</li>';
    return;
  }

  state.conversations.forEach(conv => {
    const li = document.createElement('li');
    const isActive = state.activeConversation && state.activeConversation.id === conv.id;
    li.className = `conversation-item ${isActive ? 'active' : ''}`;

    const displayName = conv.isGroup
      ? (conv.name || 'Group Chat')
      : `Chat with ${conv.memberIds.find(id => id !== state.user?.id) || 'User'}`;

    const initial = conv.isGroup ? '👥' : '💬';
    const lastMsg = conv.lastMessage ? conv.lastMessage.content : 'No messages yet';

    li.innerHTML = `
      <div class="conv-avatar ${conv.isGroup ? 'group' : ''}">${initial}</div>
      <div class="conv-meta">
        <div class="conv-name">${displayName}</div>
        <div class="conv-last-msg">${lastMsg}</div>
      </div>
    `;

    li.addEventListener('click', () => selectConversation(conv));
    conversationList.appendChild(li);
  });
}

async function selectConversation(conv) {
  state.activeConversation = conv;
  renderConversationList();

  chatHeaderTitle.textContent = conv.isGroup ? conv.name : `1-on-1 Chat`;
  chatHeaderSubtitle.textContent = `ID: ${conv.id} • ${conv.memberIds.length} members`;
  chatHeaderActions.classList.remove('hidden');
  chatInputBar.classList.remove('hidden');

  // Load message history
  const page = await apiCall(`/api/conversations/${conv.id}/messages?page=0&size=50`);
  state.messages = (page?.content || []).reverse();
  renderMessages();

  // Load trades for this conversation
  await loadConversationTrades(conv.id);

  // Subscribe to STOMP WebSocket channel for this conversation
  subscribeToConversation(conv.id);
}

async function loadConversationTrades(convId) {
  try {
    const trades = await apiCall(`/api/trades/conversation/${convId}`) || [];
    trades.forEach(t => {
      state.tradesCache[t.id] = t;
    });
    renderMessages();
  } catch (e) { }
}

function renderMessages() {
  messagesContainer.innerHTML = '';

  if (state.messages.length === 0 && Object.keys(state.tradesCache).length === 0) {
    messagesContainer.innerHTML = '<div class="empty-chat"><p>Start the conversation or propose a trade!</p></div>';
    return;
  }

  // Render combined messages and in-chat trade widgets
  state.messages.forEach(msg => {
    const row = document.createElement('div');
    const isMe = state.user && msg.senderId === state.user.id;
    row.className = `message-row ${isMe ? 'me' : 'other'}`;

    const timeStr = new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    row.innerHTML = `
      <div class="msg-bubble">
        ${!isMe ? `<div class="msg-sender">${msg.senderId.slice(0, 8)}...</div>` : ''}
        <div>${escapeHtml(msg.content)}</div>
        <div class="msg-time">${timeStr}</div>
      </div>
    `;
    messagesContainer.appendChild(row);
  });

  // Render active trade cards
  Object.values(state.tradesCache)
    .filter(t => t.conversationId === state.activeConversation?.id)
    .forEach(trade => {
      messagesContainer.appendChild(createTradeCardElement(trade));
    });

  messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

function createTradeCardElement(trade) {
  const card = document.createElement('div');
  card.className = 'trade-card';

  const isSender = state.user && trade.senderId === state.user.id;
  const isReceiver = state.user && trade.receiverId === state.user.id;
  const isFinalized = trade.status === 'COMPLETED' || trade.status === 'CANCELLED';

  let statusClass = trade.status.toLowerCase();

  let actionsHtml = '';
  if (!isFinalized) {
    if (isSender && !trade.senderConfirmed) {
      actionsHtml += `<button class="btn btn-primary btn-sm btn-confirm" data-trade-id="${trade.id}">Confirm (1/2)</button>`;
    }
    if (isReceiver && !trade.receiverConfirmed) {
      actionsHtml += `<button class="btn btn-success btn-sm btn-confirm" data-trade-id="${trade.id}">Accept & Release Funds</button>`;
    }
    if (isSender || isReceiver) {
      actionsHtml += `<button class="btn btn-danger btn-sm btn-cancel" data-trade-id="${trade.id}">Cancel</button>`;
    }
  }

  card.innerHTML = `
    <div class="trade-card-header">
      <span style="font-weight: 700; font-size: 0.9rem;">⚡ Escrow Trade</span>
      <span class="trade-badge ${statusClass}">${trade.status.replace(/_/g, ' ')}</span>
    </div>
    <div class="trade-amount">$${Number(trade.amount).toFixed(2)} USD</div>
    <div class="trade-parties">
      <div>Sender: ${trade.senderId.slice(0, 8)}... ${trade.senderConfirmed ? '✓' : '⏳'}</div>
      <div>Receiver: ${trade.receiverId.slice(0, 8)}... ${trade.receiverConfirmed ? '✓' : '⏳'}</div>
    </div>
    ${actionsHtml ? `<div class="trade-actions">${actionsHtml}</div>` : ''}
  `;

  card.querySelectorAll('.btn-confirm').forEach(btn => {
    btn.addEventListener('click', () => confirmTrade(trade.id));
  });

  card.querySelectorAll('.btn-cancel').forEach(btn => {
    btn.addEventListener('click', () => cancelTrade(trade.id));
  });

  return card;
}

async function confirmTrade(tradeId) {
  try {
    const updated = await apiCall(`/api/trades/${tradeId}/confirm`, 'POST');
    state.tradesCache[tradeId] = updated;
    showToast('Trade confirmation submitted!');
    await refreshWallet();
    renderMessages();
  } catch (e) { }
}

async function cancelTrade(tradeId) {
  try {
    const updated = await apiCall(`/api/trades/${tradeId}/cancel`, 'POST');
    state.tradesCache[tradeId] = updated;
    showToast('Trade cancelled and funds released!');
    await refreshWallet();
    renderMessages();
  } catch (e) { }
}

// --- Sending Messages ---
async function handleSendMessage() {
  const content = messageInput.value.trim();
  if (!content || !state.activeConversation) return;

  try {
    messageInput.value = '';
    const sent = await apiCall(`/api/conversations/${state.activeConversation.id}/messages`, 'POST', { content });
    if (sent && !state.messages.some(m => m.id === sent.id)) {
      state.messages.push(sent);
      renderMessages();
      loadConversations();
    }
  } catch (e) { }
}

btnSendMessage.addEventListener('click', handleSendMessage);
messageInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') handleSendMessage();
});

// --- User Search & Instant Chat ---
const userSearchInput = document.getElementById('userSearchInput');
const userSearchResults = document.getElementById('userSearchResults');
const modalUserSearchInput = document.getElementById('modalUserSearchInput');
const modalUserSearchResults = document.getElementById('modalUserSearchResults');

let searchDebounceTimeout = null;

function renderSearchResults(users, container, onSelect, currentQuery = '') {
  container.innerHTML = '';
  if (!users || users.length === 0) {
    container.innerHTML = `<div style="padding: 12px; font-size: 0.85rem; color: var(--text-muted); text-align: center;">No user found for "${escapeHtml(currentQuery)}"</div>`;
    container.classList.remove('hidden');
    return;
  }

  users.forEach(u => {
    const item = document.createElement('div');
    item.className = 'search-item';
    const isMe = state.user && u.id === state.user.id;

    item.innerHTML = `
      <div class="search-item-info">
        <div class="search-item-name">${escapeHtml(u.username)} ${isMe ? '<span style="font-size:0.75rem; color:var(--text-muted);">(You)</span>' : ''}</div>
        <div class="search-item-email">${escapeHtml(u.gmail)}</div>
      </div>
      ${isMe
        ? '<span style="font-size: 0.75rem; color: var(--text-muted); padding: 4px 8px;">Current User</span>'
        : '<button class="btn btn-primary btn-sm">Chat 💬</button>'}
    `;
    if (!isMe) {
      item.addEventListener('click', () => onSelect(u));
    }
    container.appendChild(item);
  });
  container.classList.remove('hidden');
}

const sidebarSearchForm = document.getElementById('sidebarSearchForm');

async function performSearch(query, container, onSelect) {
  if (!query) {
    container.classList.add('hidden');
    container.innerHTML = '';
    return;
  }
  try {
    console.log('[Search] Executing search for:', query);
    const users = await apiCall(`/api/user/search?query=${encodeURIComponent(query)}`) || [];
    console.log('[Search] Result users:', users);
    renderSearchResults(users, container, onSelect, query);
  } catch (e) {
    console.error('[Search] Error executing search:', e);
  }
}

if (sidebarSearchForm) {
  sidebarSearchForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const query = userSearchInput.value.trim();
    await performSearch(query, userSearchResults, async (selectedUser) => {
      userSearchResults.classList.add('hidden');
      userSearchInput.value = '';
      await openDirectChatWithUser(selectedUser.id);
    });
  });
}

userSearchInput.addEventListener('input', (e) => {
  clearTimeout(searchDebounceTimeout);
  const query = e.target.value.trim();
  searchDebounceTimeout = setTimeout(() => {
    performSearch(query, userSearchResults, async (selectedUser) => {
      userSearchResults.classList.add('hidden');
      userSearchInput.value = '';
      await openDirectChatWithUser(selectedUser.id);
    });
  }, 150);
});

modalUserSearchInput.addEventListener('input', (e) => {
  clearTimeout(searchDebounceTimeout);
  const query = e.target.value.trim();
  searchDebounceTimeout = setTimeout(() => {
    performSearch(query, modalUserSearchResults, async (selectedUser) => {
      closeModal(modalDirectChat);
      await openDirectChatWithUser(selectedUser.id);
    });
  }, 150);
});

// Close search dropdown when clicking outside
document.addEventListener('click', (e) => {
  if (!userSearchInput.contains(e.target) && !userSearchResults.contains(e.target)) {
    userSearchResults.classList.add('hidden');
  }
});

async function openDirectChatWithUser(recipientId) {
  try {
    const conv = await apiCall('/api/conversations/direct', 'POST', { recipientId });
    await loadConversations();
    selectConversation(conv);
  } catch (e) { }
}

// --- Modal Handlers ---
btnNewDirect.addEventListener('click', () => {
  modalUserSearchInput.value = '';
  modalUserSearchResults.innerHTML = '';
  openModal(modalDirectChat);
});

btnNewGroup.addEventListener('click', () => {
  selectedGroupMembers = [];
  renderGroupSelectedMembers();
  if (groupMemberSearchInput) groupMemberSearchInput.value = '';
  if (groupSearchResults) groupSearchResults.innerHTML = '';
  document.getElementById('groupName').value = '';
  openModal(modalGroupChat);
});

btnCreateTrade.addEventListener('click', () => {
  if (state.activeConversation) {
    const otherId = state.activeConversation.memberIds.find(id => id !== state.user?.id) || '';
    document.getElementById('tradeReceiverId').value = otherId;
    const displayName = state.activeConversation.isGroup
      ? `Group Trade`
      : `Chat Partner (${otherId.slice(0, 8)}...)`;
    document.getElementById('tradeReceiverDisplay').textContent = displayName;
    openModal(modalNewTrade);
  }
});

document.getElementById('formGroupChat').addEventListener('submit', async (e) => {
  e.preventDefault();
  const name = document.getElementById('groupName').value.trim();
  const memberIds = selectedGroupMembers.map(m => m.id);

  try {
    const conv = await apiCall('/api/conversations/group', 'POST', { name, memberIds });
    closeModal(modalGroupChat);
    await loadConversations();
    selectConversation(conv);
  } catch (e) { }
});

document.getElementById('formNewTrade').addEventListener('submit', async (e) => {
  e.preventDefault();
  const receiverId = document.getElementById('tradeReceiverId').value.trim();
  const amount = Number(document.getElementById('tradeAmount').value);

  try {
    const trade = await apiCall('/api/trades', 'POST', {
      conversationId: state.activeConversation.id,
      receiverId,
      amount
    });
    state.tradesCache[trade.id] = trade;
    closeModal(modalNewTrade);
    showToast(`Escrow trade for $${amount.toFixed(2)} created!`);
    await refreshWallet();
    renderMessages();
  } catch (e) { }
});

// --- STOMP WebSocket Realtime ---
function initWebSocket() {
  if (state.stompClient) {
    state.stompClient.deactivate();
  }

  const socketFactory = () => new SockJS(`${API_BASE}/ws`);

  state.stompClient = new StompJs.Client({
    webSocketFactory: socketFactory,
    connectHeaders: {
      Authorization: `Bearer ${state.token}`
    },
    debug: (str) => {
      console.log('[STOMP Debug]', str);
    },
    reconnectDelay: 3000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000
  });

  state.stompClient.onConnect = (frame) => {
    console.log('[STOMP] Connected to WebSocket broker:', frame);
    showToast('Realtime WebSocket Connected 🟢');
    if (state.activeConversation) {
      subscribeToConversation(state.activeConversation.id);
    }
  };

  state.stompClient.onStompError = (frame) => {
    console.error('[STOMP] Stomp broker error:', frame.headers['message'], frame.body);
    showToast(`WebSocket Error: ${frame.headers['message'] || 'Broker error'}`);
  };

  state.stompClient.onWebSocketError = (event) => {
    console.error('[WebSocket Error]', event);
  };

  state.stompClient.activate();
}

function subscribeToConversation(convId) {
  if (!convId) return;
  if (!state.stompClient || !state.stompClient.connected) {
    console.log('[STOMP] Deferring subscription until connected for conv:', convId);
    return;
  }

  if (state.activeSubscription) {
    try {
      state.activeSubscription.unsubscribe();
    } catch (e) {}
  }

  const topic = `/topic/conversations/${convId}`;
  console.log('[STOMP] Subscribing to topic:', topic);
  state.activeSubscription = state.stompClient.subscribe(topic, (message) => {
    try {
      const payload = JSON.parse(message.body);
      console.log('[STOMP] Inbound payload on', topic, payload);

      if (payload.type === 'TRADE_EVENT') {
        showToast(`Trade Notification: ${payload.message || payload.status}`);
        loadConversationTrades(convId);
        refreshWallet();
      } else {
        // Chat message (matches active conversation)
        if (!state.messages.some(m => m.id === payload.id)) {
          state.messages.push(payload);
          renderMessages();
        }
        loadConversations();
      }
    } catch (err) {
      console.error('[STOMP] Error parsing inbound message body:', err);
    }
  });
}

// --- Trade Service STOMP WebSocket Realtime ---
function initTradeWebSocket() {
  if (state.tradeStompClient) {
    state.tradeStompClient.deactivate();
  }

  const socketFactory = () => new SockJS(`${API_BASE}/trade-ws`);

  state.tradeStompClient = new StompJs.Client({
    webSocketFactory: socketFactory,
    connectHeaders: {
      Authorization: `Bearer ${state.token}`
    },
    debug: (str) => {
      console.log('[Trade STOMP Debug]', str);
    },
    reconnectDelay: 3000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000
  });

  state.tradeStompClient.onConnect = (frame) => {
    console.log('[Trade STOMP] Connected to Trade WebSocket broker:', frame);
    showToast('Trade Realtime Connected ⚡');

    // Subscribe to personal trade notifications
    state.tradeStompClient.subscribe('/user/queue/trades', (message) => {
      try {
        const payload = JSON.parse(message.body);
        console.log('[Trade STOMP] Inbound trade update:', payload);
        if (payload.tradeId) {
          state.tradesCache[payload.tradeId] = payload;
        }
        showToast(`[Trade] ${payload.message || payload.status}`);
        if (state.activeConversation) {
          loadConversationTrades(state.activeConversation.id);
        }
        refreshWallet();
      } catch (err) {
        console.error('[Trade STOMP] Error parsing trade message:', err);
      }
    });

    // Subscribe to personal wallet balance updates
    state.tradeStompClient.subscribe('/user/queue/wallet', (message) => {
      try {
        const wallet = JSON.parse(message.body);
        console.log('[Trade STOMP] Inbound wallet update:', wallet);
        state.wallet = wallet;
        headerBalance.textContent = `$${Number(wallet.balance).toFixed(2)}`;
        headerHeld.textContent = `($${Number(wallet.heldBalance).toFixed(2)} in escrow)`;
      } catch (err) {
        console.error('[Trade STOMP] Error parsing wallet update:', err);
      }
    });
  };

  state.tradeStompClient.onStompError = (frame) => {
    console.error('[Trade STOMP] Broker error:', frame.headers['message'], frame.body);
  };

  state.tradeStompClient.onWebSocketError = (event) => {
    console.error('[Trade WebSocket Error]', event);
  };

  state.tradeStompClient.activate();
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

// Auto-login on load if token exists
if (state.token) {
  authScreen.classList.add('hidden');
  initApp();
}

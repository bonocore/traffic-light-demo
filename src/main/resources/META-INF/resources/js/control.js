// Operator Console Controller
document.addEventListener('DOMContentLoaded', () => {
  // Sidebar Preview Elements
  const previewRed = document.getElementById('preview-red');
  const previewAmber = document.getElementById('preview-amber');
  const previewGreen = document.getElementById('preview-green');
  const previewState = document.getElementById('preview-state');
  const previewMode = document.getElementById('preview-mode');
  const previewTimer = document.getElementById('preview-timer');

  // Connection Elements
  const statusDot = document.getElementById('status-dot');
  const statusText = document.getElementById('status-text');

  // Key Manager Elements
  const activeKeySelect = document.getElementById('active-key-select');
  const keysTableBody = document.getElementById('keys-table-body');
  const createKeyForm = document.getElementById('create-key-form');
  const newKeyName = document.getElementById('new-key-name');
  const newKeyRole = document.getElementById('new-key-role');



  let activeApiKey = localStorage.getItem('traffic_active_key') || 'admin-key-2026';
  let keysList = [];
  let eventSource = null;

  // ------------------------------------------------------------
  // Visual Preview Sync
  // ------------------------------------------------------------
  function updatePreview(data) {
    if (!data) return;

    previewRed.classList.remove('lit');
    previewAmber.classList.remove('lit', 'blinking');
    previewGreen.classList.remove('lit');

    const state = data.state || 'OFF';
    const mode = data.mode || 'AUTO';
    const remaining = data.remainingSeconds || 0;

    switch (state) {
      case 'RED':
        previewRed.classList.add('lit');
        previewState.innerHTML = '<span style="color:#ff4d5a;">●</span> RED';
        break;
      case 'AMBER':
        previewAmber.classList.add('lit');
        previewState.innerHTML = '<span style="color:#ffb01f;">●</span> AMBER';
        break;
      case 'GREEN':
        previewGreen.classList.add('lit');
        previewState.innerHTML = '<span style="color:#10b981;">●</span> GREEN';
        break;
      case 'FLASHING_AMBER':
        previewAmber.classList.add('blinking');
        previewState.innerHTML = '<span style="color:#ffb01f;">⚠️</span> FLASHING';
        break;
      case 'OFF':
      default:
        previewState.innerHTML = '<span style="color:#64748b;">○</span> OFF';
        break;
    }

    previewMode.textContent = mode;
    previewMode.className = `hud-mode ${mode.toLowerCase()}`;

    if (mode === 'AUTO' && remaining > 0) {
      previewTimer.textContent = `${remaining}s`;
    } else {
      previewTimer.textContent = '--';
    }

    // Update active state in mode buttons
    document.querySelectorAll('.mode-btn').forEach((btn) => {
      btn.classList.toggle('active', btn.dataset.mode === mode);
    });
  }

  // ------------------------------------------------------------
  // Collapsible Cards 3 & 4
  // ------------------------------------------------------------
  const cardKeyManager = document.getElementById('card-key-manager');
  const cardDebugger = document.getElementById('card-debugger');
  const btnCollapseCard3 = document.getElementById('btn-collapse-card-3');
  const btnCollapseCard4 = document.getElementById('btn-collapse-card-4');

  function initCollapsible(cardElem, btnElem, storageKey) {
    if (!cardElem || !btnElem) return;

    const isCollapsed = localStorage.getItem(storageKey) === 'true';
    if (isCollapsed) {
      cardElem.classList.add('collapsed');
      btnElem.setAttribute('aria-expanded', 'false');
      const label = btnElem.querySelector('.collapse-label');
      if (label) label.textContent = 'Expand';
    }

    btnElem.addEventListener('click', () => {
      const currentlyCollapsed = cardElem.classList.toggle('collapsed');
      btnElem.setAttribute('aria-expanded', String(!currentlyCollapsed));
      const label = btnElem.querySelector('.collapse-label');
      if (label) {
        label.textContent = currentlyCollapsed ? 'Expand' : 'Collapse';
      }
      localStorage.setItem(storageKey, String(currentlyCollapsed));
    });
  }

  initCollapsible(cardKeyManager, btnCollapseCard3, 'traffic_card3_collapsed');
  initCollapsible(cardDebugger, btnCollapseCard4, 'traffic_card4_collapsed');

  // ------------------------------------------------------------
  // Server-Side Request Debugger (Incoming REST Requests)
  // ------------------------------------------------------------
  const debugRequestsContainer = document.getElementById('debug-requests-container');
  const debugLastUpdated = document.getElementById('debug-last-updated');
  const btnClearDebugLogs = document.getElementById('btn-clear-debug-logs');
  const btnManualRefreshDebug = document.getElementById('btn-manual-refresh-debug');
  const refreshIcon = document.getElementById('refresh-icon');

  let openReqPayloadIds = new Set();
  let openRespPayloadIds = new Set();
  let debugEventSource = null;

  async function fetchDebugRequests() {
    if (!debugRequestsContainer) return;

    try {
      const res = await fetch('/api/debug/requests?limit=10', {
        headers: {
          'X-API-KEY': activeApiKey,
        },
        credentials: 'same-origin',
      });

      if (!res.ok) {
        return;
      }

      const logs = await res.json();
      renderDebugRequests(logs);
    } catch (err) {
      // Silent error during background poll/refresh
    }
  }

  function formatPayload(raw) {
    if (!raw) return '';
    try {
      const parsed = JSON.parse(raw);
      return escapeHtml(JSON.stringify(parsed, null, 2));
    } catch (e) {
      return escapeHtml(raw);
    }
  }

  function renderDebugRequests(logs) {
    if (!logs || logs.length === 0) {
      debugRequestsContainer.innerHTML = '<div class="debug-empty-state">No requests recorded yet. Incoming REST API calls will appear here automatically.</div>';
      if (debugLastUpdated) {
        debugLastUpdated.textContent = 'Updated ' + new Date().toLocaleTimeString();
      }
      return;
    }

    debugRequestsContainer.innerHTML = '';
    logs.forEach((req) => {
      const card = document.createElement('div');
      card.className = 'debug-request-card';

      const timeStr = req.timestamp ? new Date(req.timestamp).toLocaleTimeString() : '--:--:--';
      const statusClass = req.status >= 200 && req.status < 300 ? 's2xx' : 's4xx';
      const hasReq = req.requestPayload && req.requestPayload.trim().length > 0;
      const hasResp = req.responsePayload && req.responsePayload.trim().length > 0;

      const isReqOpen = openReqPayloadIds.has(req.id);
      const isRespOpen = openRespPayloadIds.has(req.id);

      card.innerHTML = `
        <div class="debug-request-top">
          <div class="debug-request-main">
            <span class="debug-time">${timeStr}</span>
            <span class="debug-method ${req.method}">${req.method}</span>
            <span class="debug-path">${req.path}</span>
          </div>
          <div class="debug-request-meta">
            <span class="debug-status-pill ${statusClass}">${req.status}</span>
            <span class="debug-latency">${req.durationMs}ms</span>
          </div>
        </div>
        <div class="debug-request-details">
          <span class="debug-caller">Caller: <strong>${req.caller || 'Public'}</strong></span>
          <div class="debug-payload-actions">
            ${hasReq ? `<button type="button" class="btn-payload-toggle request" data-type="req" data-req-id="${req.id}">${isReqOpen ? 'Hide Request' : 'Request Body'}</button>` : ''}
            ${hasResp ? `<button type="button" class="btn-payload-toggle response" data-type="resp" data-req-id="${req.id}">${isRespOpen ? 'Hide Response' : 'Response Body'}</button>` : ''}
          </div>
        </div>
        ${hasReq ? `<pre class="debug-payload-box request ${isReqOpen ? 'visible' : ''}" id="payload-req-${req.id}"><div class="debug-payload-heading">Incoming Request Body:</div>${formatPayload(req.requestPayload)}</pre>` : ''}
        ${hasResp ? `<pre class="debug-payload-box response ${isRespOpen ? 'visible' : ''}" id="payload-resp-${req.id}"><div class="debug-payload-heading">Outgoing Response Body:</div>${formatPayload(req.responsePayload)}</pre>` : ''}
      `;

      if (hasReq) {
        const toggleBtn = card.querySelector('.btn-payload-toggle.request');
        const box = card.querySelector(`#payload-req-${req.id}`);
        toggleBtn.addEventListener('click', () => {
          const isOpen = box.classList.toggle('visible');
          toggleBtn.textContent = isOpen ? 'Hide Request' : 'Request Body';
          if (isOpen) {
            openReqPayloadIds.add(req.id);
          } else {
            openReqPayloadIds.delete(req.id);
          }
        });
      }

      if (hasResp) {
        const toggleBtn = card.querySelector('.btn-payload-toggle.response');
        const box = card.querySelector(`#payload-resp-${req.id}`);
        toggleBtn.addEventListener('click', () => {
          const isOpen = box.classList.toggle('visible');
          toggleBtn.textContent = isOpen ? 'Hide Response' : 'Response Body';
          if (isOpen) {
            openRespPayloadIds.add(req.id);
          } else {
            openRespPayloadIds.delete(req.id);
          }
        });
      }

      debugRequestsContainer.appendChild(card);
    });

    if (debugLastUpdated) {
      debugLastUpdated.textContent = 'Live Synced ' + new Date().toLocaleTimeString();
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  // Manual refresh button
  if (btnManualRefreshDebug) {
    btnManualRefreshDebug.addEventListener('click', async () => {
      if (refreshIcon) refreshIcon.classList.add('spinning');
      await fetchDebugRequests();
      setTimeout(() => {
        if (refreshIcon) refreshIcon.classList.remove('spinning');
      }, 500);
    });
  }

  // Clear debug logs
  if (btnClearDebugLogs) {
    btnClearDebugLogs.addEventListener('click', async () => {
      try {
        await fetch('/api/debug/requests', {
          method: 'DELETE',
          headers: { 'X-API-KEY': activeApiKey },
          credentials: 'same-origin',
        });
        openReqPayloadIds.clear();
        openRespPayloadIds.clear();
        fetchDebugRequests();
      } catch (err) {
        console.error('Failed to clear debug logs:', err);
      }
    });
  }

  // Live Auto-Refresh SSE Connection
  function connectDebugSSE() {
    if (debugEventSource) {
      debugEventSource.close();
    }

    try {
      debugEventSource = new EventSource('/api/debug/stream');
      debugEventSource.onmessage = () => {
        // Immediate auto-refresh when any request arrives on server!
        fetchDebugRequests();
      };
      debugEventSource.onerror = () => {
        debugEventSource.close();
        setTimeout(connectDebugSSE, 4000);
      };
    } catch (e) {
      // Fallback
    }
  }

  // ------------------------------------------------------------
  // Authenticated REST Request Execution
  // ------------------------------------------------------------
  async function callApi(method, path, payload) {
    const start = performance.now();

    try {
      const headers = {
        'X-API-KEY': activeApiKey,
      };
      if (payload) {
        headers['Content-Type'] = 'application/json';
      }

      const res = await fetch(path, {
        method,
        headers,
        body: payload ? JSON.stringify(payload) : undefined,
        credentials: 'same-origin',
      });

      const latencyMs = Math.round(performance.now() - start);
      let data = null;
      try {
        data = await res.json();
      } catch (e) {
        data = null;
      }

      // Immediately refresh debug logs so user's own call shows up instantly
      fetchDebugRequests();
      return { status: res.status, data };
    } catch (err) {
      fetchDebugRequests();
      return { status: 0, error: err.message };
    }
  }

  // ------------------------------------------------------------
  // Control Deck Event Listeners
  // ------------------------------------------------------------
  // Mode Selection
  document.querySelectorAll('.mode-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const mode = btn.dataset.mode;
      callApi('POST', '/api/traffic-light/mode', { mode });
    });
  });

  // Emergency Stop (Side button)
  document.getElementById('emergency-stop-btn').addEventListener('click', () => {
    callApi('POST', '/api/traffic-light/mode', { mode: 'EMERGENCY' });
  });

  // Manual Light Buttons
  document.querySelectorAll('.ctrl-btn[data-state]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const state = btn.dataset.state;
      callApi('POST', '/api/traffic-light/state', { state });
    });
  });

  // Advance Phase Button
  document.getElementById('advance-phase-btn').addEventListener('click', () => {
    callApi('POST', '/api/traffic-light/next');
  });

  // ------------------------------------------------------------
  // Multi-Key Management
  // ------------------------------------------------------------
  async function loadKeys() {
    try {
      const res = await callApi('GET', '/api/keys');
      if (res.status === 200 && Array.isArray(res.data)) {
        keysList = res.data;
        renderKeys();
      }
    } catch (err) {
      console.error('Failed to load keys:', err);
    }
  }

  function renderKeys() {
    // Populate Select Dropdown
    activeKeySelect.innerHTML = '';
    keysList.forEach((k) => {
      const opt = document.createElement('option');
      opt.value = k.key;
      opt.textContent = `${k.name} [${k.role}] (${k.key.substring(0, 12)}...) ${!k.enabled ? '❌ REVOKED' : '✓'}`;
      if (k.key === activeApiKey) {
        opt.selected = true;
      }
      activeKeySelect.appendChild(opt);
    });

    // Option to simulate invalid key
    const invalidOpt = document.createElement('option');
    invalidOpt.value = 'invalid-test-key-999';
    invalidOpt.textContent = '⚠️ Simulated Invalid Key (Test 401 Unauthorized)';
    if (activeApiKey === 'invalid-test-key-999') {
      invalidOpt.selected = true;
    }
    activeKeySelect.appendChild(invalidOpt);

    // Populate Table
    keysTableBody.innerHTML = '';
    keysList.forEach((k) => {
      const tr = document.createElement('tr');
      const isCurrentActive = k.key === activeApiKey;

      tr.innerHTML = `
        <td><strong>${k.name}</strong> ${isCurrentActive ? '<span style="color: #6366f1;">(Current)</span>' : ''}</td>
        <td><span class="role-badge ${k.role.toLowerCase()}">${k.role}</span></td>
        <td><code class="key-token-display">${k.key}</code></td>
        <td>${k.enabled ? '<span style="color:#10b981;">Active</span>' : '<span style="color:#ef4444;">Revoked</span>'}</td>
        <td>
          <button class="btn-revoke" data-id="${k.id}" ${!k.enabled ? 'disabled' : ''}>
            ${k.enabled ? 'Revoke' : 'Revoked'}
          </button>
        </td>
      `;

      keysTableBody.appendChild(tr);
    });

    // Wire up Revoke buttons
    keysTableBody.querySelectorAll('.btn-revoke:not(:disabled)').forEach((btn) => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        if (confirm(`Revoke this API Key? Any requests using it will be denied with 401 Unauthorized.`)) {
          await callApi('DELETE', `/api/keys/${id}`);
          loadKeys();
        }
      });
    });
  }

  activeKeySelect.addEventListener('change', () => {
    activeApiKey = activeKeySelect.value;
    localStorage.setItem('traffic_active_key', activeApiKey);
    setCurlPreview('POST', '/api/traffic-light/state', { state: 'GREEN' });
    renderKeys();
  });

  createKeyForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = newKeyName.value.trim();
    const role = newKeyRole.value;

    if (!name) return;

    const res = await callApi('POST', '/api/keys', { name, role });
    if (res.status === 201) {
      newKeyName.value = '';
      activeApiKey = res.data.key;
      localStorage.setItem('traffic_active_key', activeApiKey);
      loadKeys();
    }
  });

  // ------------------------------------------------------------
  // Server-Sent Events (SSE)
  // ------------------------------------------------------------
  function connectSSE() {
    if (eventSource) {
      eventSource.close();
    }

    eventSource = new EventSource('/api/traffic-light/events');

    eventSource.onopen = () => {
      statusDot.className = 'status-dot connected';
      statusText.textContent = 'SSE Live Synchronized';
    };

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        updatePreview(data);
        fetchDebugRequests();
      } catch (err) {
        console.error('Error parsing SSE event:', err);
      }
    };

    eventSource.onerror = () => {
      statusDot.className = 'status-dot disconnected';
      statusText.textContent = 'SSE Reconnecting...';
      eventSource.close();
      setTimeout(connectSSE, 3000);
    };
  }

  // Initial Boot
  loadKeys();
  connectSSE();
  connectDebugSSE();
  fetchDebugRequests();
  setInterval(fetchDebugRequests, 2000);
});

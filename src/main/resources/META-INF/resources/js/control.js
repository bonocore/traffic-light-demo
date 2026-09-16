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

  // cURL & Terminal Elements
  const curlCode = document.getElementById('curl-code');
  const btnCopyCurl = document.getElementById('btn-copy-curl');
  const terminalLogs = document.getElementById('terminal-logs');
  const btnClearLogs = document.getElementById('btn-clear-logs');

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
  // Terminal Logging & cURL Generation
  // ------------------------------------------------------------
  function logActivity(method, endpoint, status, message, latencyMs) {
    const time = new Date().toTimeString().split(' ')[0];
    const entry = document.createElement('div');
    entry.className = 'log-entry';

    const statusClass = status === 200 || status === 201 ? 's200' : 's401';

    entry.innerHTML = `
      <span class="log-time">[${time}]</span>
      <span class="log-method">${method}</span>
      <span class="log-msg">${endpoint}</span>
      <span class="log-status ${statusClass}">${status}</span>
      <span class="log-time">(${latencyMs}ms)</span>
    `;

    terminalLogs.appendChild(entry);
    terminalLogs.scrollTop = terminalLogs.scrollHeight;
  }

  function setCurlPreview(method, path, payload) {
    const origin = window.location.origin;
    let cmd = `curl -i -X ${method} "${origin}${path}" \\\n  -H "X-API-KEY: ${activeApiKey}"`;

    if (payload) {
      cmd += ` \\\n  -H "Content-Type: application/json" \\\n  -d '${JSON.stringify(payload)}'`;
    }

    curlCode.textContent = cmd;
  }

  btnCopyCurl.addEventListener('click', () => {
    navigator.clipboard.writeText(curlCode.textContent).then(() => {
      const original = btnCopyCurl.textContent;
      btnCopyCurl.textContent = '✓ Copied!';
      setTimeout(() => (btnCopyCurl.textContent = original), 1500);
    });
  });

  btnClearLogs.addEventListener('click', () => {
    terminalLogs.innerHTML = '';
  });

  // ------------------------------------------------------------
  // Authenticated REST Request Execution
  // ------------------------------------------------------------
  async function callApi(method, path, payload) {
    setCurlPreview(method, path, payload);
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
      });

      const latency = Math.round(performance.now() - start);
      const data = await res.json().catch(() => ({}));

      logActivity(method, path, res.status, res.statusText, latency);

      if (!res.ok) {
        console.warn('API Call returned error:', res.status, data);
      }

      return { status: res.status, data };
    } catch (err) {
      const latency = Math.round(performance.now() - start);
      logActivity(method, path, 'ERR', err.message, latency);
      throw err;
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
  setCurlPreview('POST', '/api/traffic-light/state', { state: 'GREEN' });
  loadKeys();
  connectSSE();
});

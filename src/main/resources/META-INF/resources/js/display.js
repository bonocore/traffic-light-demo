// Display UI Controller (Server-Sent Events Consumer)
document.addEventListener('DOMContentLoaded', () => {
  const lensRed = document.getElementById('lens-red');
  const lensAmber = document.getElementById('lens-amber');
  const lensGreen = document.getElementById('lens-green');
  const ambientGlow = document.getElementById('ambient-glow');

  const hudValue = document.getElementById('hud-value');
  const hudTimer = document.getElementById('hud-timer');
  const hudMode = document.getElementById('hud-mode');
  const statusDot = document.getElementById('status-dot');
  const statusText = document.getElementById('status-text');

  let eventSource = null;

  function updateVisuals(data) {
    if (!data) return;

    // Reset all light classes
    lensRed.classList.remove('lit');
    lensAmber.classList.remove('lit', 'blinking');
    lensGreen.classList.remove('lit');
    ambientGlow.className = 'ambient-glow';

    const state = data.state || 'OFF';
    const mode = data.mode || 'AUTO';
    const remaining = data.remainingSeconds || 0;

    switch (state) {
      case 'RED':
        lensRed.classList.add('lit');
        ambientGlow.classList.add('red');
        hudValue.innerHTML = '<span style="color: #ff4d5a;">●</span> RED • STOP';
        break;
      case 'AMBER':
        lensAmber.classList.add('lit');
        ambientGlow.classList.add('amber');
        hudValue.innerHTML = '<span style="color: #ffb01f;">●</span> AMBER • PREPARE';
        break;
      case 'GREEN':
        lensGreen.classList.add('lit');
        ambientGlow.classList.add('green');
        hudValue.innerHTML = '<span style="color: #10b981;">●</span> GREEN • GO';
        break;
      case 'FLASHING_AMBER':
        lensAmber.classList.add('blinking');
        ambientGlow.classList.add('amber');
        hudValue.innerHTML = '<span style="color: #ffb01f;">⚠️</span> FLASHING AMBER';
        break;
      case 'OFF':
      default:
        ambientGlow.classList.add('off');
        hudValue.innerHTML = '<span style="color: #64748b;">○</span> LIGHTS OFF';
        break;
    }

    // Update Mode
    hudMode.textContent = mode;
    hudMode.className = `hud-mode ${mode.toLowerCase()}`;

    // Update Timer
    if (mode === 'AUTO' && remaining > 0) {
      hudTimer.textContent = `${String(remaining).padStart(2, '0')}s`;
    } else {
      hudTimer.textContent = '--';
    }
  }

  function setConnectionStatus(status) {
    statusDot.className = 'status-dot ' + status;
    if (status === 'connected') {
      statusText.textContent = 'Live SSE Connected';
    } else if (status === 'connecting') {
      statusText.textContent = 'Connecting...';
    } else {
      statusText.textContent = 'Disconnected';
    }
  }

  function connectSSE() {
    setConnectionStatus('connecting');

    if (eventSource) {
      eventSource.close();
    }

    eventSource = new EventSource('/api/traffic-light/events');

    eventSource.onopen = () => {
      setConnectionStatus('connected');
    };

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        updateVisuals(data);
      } catch (err) {
        console.error('Error parsing SSE event:', err);
      }
    };

    eventSource.onerror = (err) => {
      console.warn('SSE disconnected, retrying in 3s...', err);
      setConnectionStatus('disconnected');
      eventSource.close();
      setTimeout(connectSSE, 3000);
    };
  }

  // Fetch initial state once on load
  fetch('/api/traffic-light/state')
    .then((res) => res.json())
    .then((data) => updateVisuals(data))
    .catch((err) => console.warn('Initial fetch error:', err))
    .finally(() => {
      connectSSE();
    });
});

(() => {
  'use strict';

  if (window.__topbarNotificationsInitialized) return;
  window.__topbarNotificationsInitialized = true;

  function escapeHtml(str) {
    if (!str) return '';
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function formatTime(isoString) {
    if (!isoString) return '';
    try {
      const date = new Date(isoString);
      if (isNaN(date.getTime())) return isoString;

      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffMins = Math.floor(diffMs / 60000);
      const diffHours = Math.floor(diffMs / 3600000);
      const diffDays = Math.floor(diffMs / 86400000);

      const pad = n => String(n).padStart(2, '0');
      const timeStr = `${pad(date.getHours())}:${pad(date.getMinutes())}`;
      const dateStr = `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;

      if (diffMins < 1) return 'Vừa xong';
      if (diffMins < 60) return `${diffMins} phút trước`;
      if (diffHours < 24) return `${diffHours} giờ trước`;
      const days = Math.max(1, diffDays);
      if (days < 30) return `${days} ngày trước`;
      return `${timeStr}, ${dateStr}`;
    } catch (e) {
      return isoString;
    }
  }

  function getCsrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    const headers = { 'Accept': 'application/json' };
    if (token) {
      headers[header] = token;
    }
    return headers;
  }

  // Một kết nối WebSocket dùng chung cho mọi chuông trên trang
  const pushListeners = [];
  let pushClient = null;

  function onPush(listener) {
    pushListeners.push(listener);
    if (pushClient || !window.StompJs) return;
    const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
    pushClient = new window.StompJs.Client({
      brokerURL: `${scheme}://${location.host}/ws`,
      reconnectDelay: 10000,
      onConnect: () => {
        pushClient.subscribe('/user/queue/notifications', frame => {
          try {
            const data = JSON.parse(frame.body);
            pushListeners.forEach(fn => fn(data.unreadCount));
          } catch (err) {
            // bỏ qua tin lỗi định dạng
          }
        });
      }
    });
    pushClient.activate();
  }

  function initNotificationWidget(widget) {
    const trigger = widget.querySelector('.topbar-notification-btn');
    const dropdown = widget.querySelector('.topbar-notif-dropdown');
    const badge = widget.querySelector('.topbar-notif-badge');
    const list = widget.querySelector('.topbar-notif-list');
    const markAllBtn = widget.querySelector('.topbar-notif-mark-all');

    if (!trigger || !dropdown) return;

    let isOpen = false;
    let isFetching = false;

    function updateBadge(count) {
      if (!badge) return;
      const num = parseInt(count, 10) || 0;
      if (num > 0) {
        badge.textContent = num > 99 ? '99+' : String(num);
        badge.removeAttribute('hidden');
      } else {
        badge.textContent = '0';
        badge.setAttribute('hidden', '');
      }
    }

    async function fetchUnreadCount() {
      try {
        const res = await fetch('/api/notifications/unread-count', {
          headers: { 'Accept': 'application/json' }
        });
        if (res.ok) {
          const data = await res.json();
          updateBadge(data.unreadCount);
        }
      } catch (err) {
        // silent fail
      }
    }

    async function loadRecentNotifications() {
      if (!list) return;
      isFetching = true;
      list.innerHTML = '<div class="topbar-notif-loading">Đang tải thông báo...</div>';

      try {
        const res = await fetch('/api/notifications/recent', {
          headers: { 'Accept': 'application/json' }
        });
        if (!res.ok) throw new Error('Network error');
        const data = await res.json();
        updateBadge(data.unreadCount);

        const items = data.notifications || [];
        if (items.length === 0) {
          list.innerHTML = '<div class="topbar-notif-empty">Không có thông báo nào</div>';
          return;
        }

        list.innerHTML = items.map(n => {
          const unreadClass = n.isRead ? 'is-read' : 'is-unread';
          const link = n.linkUrl && n.linkUrl.trim() !== '' ? n.linkUrl : '/notifications';
          return `
            <a href="${escapeHtml(link)}" class="topbar-notif-item ${unreadClass}" data-id="${n.notificationId}" data-read="${n.isRead}">
              <div class="topbar-notif-icon-circle" aria-hidden="true">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                  <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                </svg>
              </div>
              <div class="topbar-notif-item-body">
                <div class="topbar-notif-item-title">${escapeHtml(n.title)}</div>
                <div class="topbar-notif-item-time">${escapeHtml(formatTime(n.createdAt))}</div>
              </div>
              <div class="topbar-notif-unread-dot" aria-hidden="true"></div>
            </a>
          `;
        }).join('');

        // Attach click listener for each item to mark as read
        list.querySelectorAll('.topbar-notif-item').forEach(itemEl => {
          itemEl.addEventListener('click', async (e) => {
            const notifId = itemEl.getAttribute('data-id');
            const isRead = itemEl.getAttribute('data-read') === 'true';
            if (!isRead && notifId) {
              try {
                fetch(`/api/notifications/${notifId}/read`, {
                  method: 'POST',
                  headers: getCsrfHeaders(),
                  keepalive: true
                });
              } catch (err) {}
            }
          });
        });

      } catch (err) {
        list.innerHTML = '<div class="topbar-notif-empty">Không thể tải thông báo lúc này</div>';
      } finally {
        isFetching = false;
      }
    }

    function openDropdown() {
      isOpen = true;
      dropdown.removeAttribute('hidden');
      trigger.setAttribute('aria-expanded', 'true');
      loadRecentNotifications();
    }

    function closeDropdown() {
      if (!isOpen) return;
      isOpen = false;
      dropdown.setAttribute('hidden', '');
      trigger.setAttribute('aria-expanded', 'false');
    }

    trigger.addEventListener('click', (e) => {
      e.stopPropagation();
      if (isOpen) {
        closeDropdown();
      } else {
        openDropdown();
      }
    });

    if (markAllBtn) {
      markAllBtn.addEventListener('click', async (e) => {
        e.stopPropagation();
        e.preventDefault();
        try {
          const res = await fetch('/api/notifications/read-all', {
            method: 'POST',
            headers: getCsrfHeaders()
          });
          if (res.ok) {
            updateBadge(0);
            if (list) {
              list.querySelectorAll('.topbar-notif-item').forEach(el => {
                el.classList.remove('is-unread');
                el.classList.add('is-read');
                el.setAttribute('data-read', 'true');
              });
            }
          }
        } catch (err) {}
      });
    }

    document.addEventListener('click', (e) => {
      if (!widget.contains(e.target)) {
        closeDropdown();
      }
    });

    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && isOpen) {
        closeDropdown();
        trigger.focus();
      }
    });

    // Auto-refresh when tab/window is focused or becomes visible
    window.addEventListener('focus', fetchUnreadCount);
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) {
        fetchUnreadCount();
      }
    });

    // WebSocket: server đẩy số chưa đọc ngay khi có thông báo mới; đang mở dropdown thì tải lại danh sách
    onPush(count => {
      updateBadge(count);
      if (isOpen && !isFetching) loadRecentNotifications();
    });

    // Dự phòng khi WebSocket mất kết nối hoặc không tải được thư viện
    setInterval(fetchUnreadCount, 180000);

    // Initial check on load
    fetchUnreadCount();
  }

  function initAll() {
    document.querySelectorAll('.topbar-notification').forEach(initNotificationWidget);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAll);
  } else {
    initAll();
  }
})();

// Khung chat công khai dùng trực tiếp API RMS. Không gửi nội dung hoặc dữ liệu ứng viên tới webhook ngoài.
const root = document.querySelector('#rms-recruitment-chat[data-api-url]');

if (root) {
  const launcher = root.querySelector('[data-chatbot-open]');
  const panel = root.querySelector('#rms-chat-panel');
  const closeButton = root.querySelector('[data-chatbot-close]');
  const clearButton = root.querySelector('[data-chatbot-clear]');
  const messages = root.querySelector('[data-chatbot-messages]');
  const status = root.querySelector('[data-chatbot-status]');
  const form = root.querySelector('[data-chatbot-form]');
  const input = root.querySelector('[data-chatbot-input]');
  const sendButton = root.querySelector('[data-chatbot-send]');
  let started = false;
  let busy = false;
  let requestVersion = 0;
  let pendingRequest;
  let hideTimer;
  let openFrame;

  function node(tag, className, content) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (content != null) element.textContent = String(content);
    return element;
  }

  function appendMessage(kind, content) {
    const article = node('div', 'chatbot__message' + (kind === 'user' ? ' chatbot__message--user' : ''));
    if (content) article.append(node('p', '', content));
    messages.append(article);
    messages.scrollTop = messages.scrollHeight;
    return article;
  }

  function addMeta(card, values) {
    const visible = values.filter(Boolean);
    if (visible.length) card.append(node('small', '', visible.join(' · ')));
  }

  function addLines(card, title, values) {
    if (!Array.isArray(values) || !values.length) return;
    card.append(node('strong', '', title));
    const list = node('ul');
    for (const value of values) list.append(node('li', '', value));
    card.append(list);
  }

  function safeHref(href) {
    return typeof href === 'string' && /^\/(?!\/)/.test(href) ? href : null;
  }

  function renderAnswer(answer) {
    const article = appendMessage('assistant', answer.reply || 'Mình chưa có câu trả lời cho câu hỏi này.');
    for (const job of answer.jobs || []) {
      const card = node('div', 'chatbot__card');
      card.append(node('strong', '', job.title));
      addMeta(card, [job.departmentName, job.workLocation, job.salaryDisplay, job.deadline]);
      if (job.reason) card.append(node('small', '', job.reason));
      if (Number.isInteger(job.id) && job.id > 0) {
        const actions = node('div', 'chatbot__actions');
        const detail = node('button', '', 'Xem chi tiết');
        detail.type = 'button';
        detail.dataset.chatbotQuick = '';
        detail.addEventListener('click', () => send({ jobPostingId: job.id }, 'Xem chi tiết: ' + job.title));
        actions.append(detail);
        card.append(actions);
      }
      article.append(card);
    }
    if (answer.jobDetail) {
      const detail = answer.jobDetail;
      const card = node('div', 'chatbot__card');
      card.append(node('strong', '', detail.title));
      addMeta(card, [detail.departmentName, detail.workLocation, detail.salaryDisplay, detail.deadline]);
      addLines(card, 'Yêu cầu', detail.requirements);
      addLines(card, 'Quyền lợi', detail.benefits);
      article.append(card);
    }
    for (const application of answer.applications || []) {
      const card = node('div', 'chatbot__card');
      card.append(node('strong', '', application.postingTitle));
      addMeta(card, [application.statusLabel, application.statusNote]);
      article.append(card);
    }
    if (answer.contact) {
      const contact = answer.contact;
      const card = node('div', 'chatbot__card');
      card.append(node('strong', '', 'Liên hệ Nhân sự'));
      addMeta(card, [contact.email, contact.phone, contact.workingHours]);
      article.append(card);
    }
    const actions = node('div', 'chatbot__actions');
    for (const link of answer.links || []) {
      const href = safeHref(link.href);
      if (!href) continue;
      const anchor = node('a', link.primary ? 'chatbot__primary' : '', link.label);
      anchor.href = href;
      actions.append(anchor);
    }
    for (const quick of answer.quickReplies || []) {
      if (!quick.message && !quick.jobPostingId) continue;
      const button = node('button', '', quick.label);
      button.type = 'button';
      button.dataset.chatbotQuick = '';
      button.addEventListener('click', () => {
        const payload = quick.jobPostingId ? { jobPostingId: quick.jobPostingId } : { message: quick.message };
        send(payload, quick.label);
      });
      actions.append(button);
    }
    if (actions.childElementCount) article.append(actions);
    messages.scrollTop = messages.scrollHeight;
  }

  function setBusy(value) {
    busy = value;
    sendButton.disabled = value;
    input.disabled = value;
    root.querySelectorAll('[data-chatbot-quick]').forEach(button => { button.disabled = value; });
    status.hidden = !value;
    status.textContent = value ? 'Đang trả lời…' : '';
  }

  async function send(payload, label) {
    if (busy) return;
    if (label) appendMessage('user', label);
    setBusy(true);
    const version = ++requestVersion;
    pendingRequest = new AbortController();
    const headers = { 'Content-Type': 'application/json' };
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
    try {
      const response = await fetch(root.dataset.apiUrl, {
        method: 'POST', credentials: 'same-origin', headers,
        body: JSON.stringify(payload), signal: pendingRequest.signal,
      });
      const result = await response.json();
      if (!response.ok || !result.success || !result.data) {
        throw new Error(result.message || 'Không thể gửi câu hỏi lúc này.');
      }
      if (version === requestVersion) renderAnswer(result.data);
    } catch (error) {
      if (version === requestVersion) {
        const article = appendMessage('assistant', error.message || 'Mất kết nối. Vui lòng thử lại.');
        const actions = node('div', 'chatbot__actions');
        const retry = node('button', '', 'Thử lại');
        retry.type = 'button';
        retry.addEventListener('click', () => { article.remove(); send(payload, null); });
        actions.append(retry);
        article.append(actions);
      }
    } finally {
      if (version === requestVersion) {
        pendingRequest = null;
        setBusy(false);
      }
    }
  }

  function setOpen(open) {
    window.clearTimeout(hideTimer);
    if (openFrame) window.cancelAnimationFrame(openFrame);
    launcher.setAttribute('aria-expanded', String(open));
    launcher.setAttribute('aria-label', open ? 'Đóng trợ lý tuyển dụng' : 'Mở trợ lý tuyển dụng');
    if (open) {
      panel.hidden = false;
      panel.inert = false;
      openFrame = window.requestAnimationFrame(() => {
        panel.classList.add('is-open');
        openFrame = null;
      });
      input.focus();
      if (!started) {
        started = true;
        send({ message: null }, null);
      }
    } else {
      panel.inert = true;
      panel.classList.remove('is-open');
      if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        panel.hidden = true;
      } else {
        hideTimer = window.setTimeout(() => { panel.hidden = true; }, 240);
      }
      launcher.focus();
    }
  }

  launcher.addEventListener('click', () => setOpen(launcher.getAttribute('aria-expanded') !== 'true'));
  closeButton.addEventListener('click', () => setOpen(false));
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && launcher.getAttribute('aria-expanded') === 'true') setOpen(false);
  });
  clearButton.addEventListener('click', () => {
    requestVersion++;
    pendingRequest?.abort();
    pendingRequest = null;
    setBusy(false);
    messages.replaceChildren();
    input.value = '';
    input.focus();
    send({ message: null }, null);
  });
  form.addEventListener('submit', event => {
    event.preventDefault();
    const message = input.value.trim();
    if (!message || busy) return;
    input.value = '';
    send({ message }, message);
  });
}

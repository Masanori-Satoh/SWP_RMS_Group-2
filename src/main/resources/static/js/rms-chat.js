import { createChat } from './vendor/n8n-chat/1.42.6/chat.bundle.es.js';

const loader = document.querySelector('[data-rms-chat-loader]');
const mountId = 'rms-recruitment-chat';

if (loader && !document.getElementById(mountId)) {
  const webhookUrl = loader.dataset.webhookUrl;
  const localOllama = loader.dataset.localOllama === 'true';
  const root = document.createElement('div');
  root.id = mountId;
  root.lang = 'en';
  document.body.append(root);

  createChat({
    webhookUrl,
    webhookConfig: { method: 'POST' },
    target: root,
    mode: 'window',
    showWindowCloseButton: true,
    showWelcomeScreen: false,
    sessionId: crypto.randomUUID(),
    loadPreviousSession: false,
    chatInputKey: 'chatInput',
    chatSessionKey: 'sessionId',
    defaultLanguage: 'en',
    enableStreaming: false,
    allowFileUploads: localOllama,
    allowedFilesMimeTypes: 'application/pdf',
    enableMessageActions: false,
    initialMessages: [
      'Hello! I can help you explore current RMS vacancies, understand job requirements and find roles that may fit your skills.',
      localOllama
        ? 'You may attach one text-based PDF CV up to 5 MB. CVs are processed temporarily on this local server using Ollama; no external AI provider is configured. Do not upload confidential third-party data.'
        : 'CV upload is unavailable until local Ollama processing is configured. You can still ask about current vacancies.',
    ],
    i18n: {
      en: {
        title: 'RMS Career Assistant',
        subtitle: 'Explore current jobs and requirements.',
        footer: 'Read-only guidance. Recommendations are not hiring decisions.',
        getStarted: 'Start a conversation',
        inputPlaceholder: 'Ask about jobs or requirements…',
        closeButtonTooltip: 'Close career assistant',
      },
    },
  });

  // The pinned widget renders its launcher as a div. Keep its native click
  // behavior and add keyboard access without replacing any widget component.
  const toggle = root.querySelector('.chat-window-toggle');
  const chatWindow = root.querySelector('.chat-window');
  if (toggle && chatWindow) {
    chatWindow.id = 'rms-career-chat-window';
    chatWindow.setAttribute('role', 'region');
    chatWindow.setAttribute('aria-label', 'RMS Career Assistant');
    toggle.setAttribute('role', 'button');
    toggle.setAttribute('tabindex', '0');
    toggle.setAttribute('aria-controls', chatWindow.id);
    const updateLabel = () => {
      const open = chatWindow.style.display !== 'none';
      toggle.setAttribute('aria-expanded', String(open));
      toggle.setAttribute('aria-label', `${open ? 'Close' : 'Open'} career assistant`);
    };
    toggle.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        toggle.click();
      }
    });
    new MutationObserver(updateLabel).observe(chatWindow, {
      attributes: true,
      attributeFilter: ['style'],
    });
    updateLabel();
  }
}

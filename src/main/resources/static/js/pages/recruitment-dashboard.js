/* Recruitment charts enhance the server-rendered values and links. */
(() => {
    'use strict';
    if (typeof window.Chart !== 'function') return;

    const styles = getComputedStyle(document.documentElement);
    const token = (name) => styles.getPropertyValue(name).trim();
    const colors = [token('--brand'), token('--success'), token('--warning'),
        token('--neutral'), token('--brand-dark'), token('--error')];

    document.querySelectorAll('.dash-chart-panel[data-chart-type]').forEach((panel) => {
        const canvas = panel.querySelector('canvas');
        const shell = panel.querySelector('.dash-chart-canvas');
        const elements = Array.from(panel.querySelectorAll('[data-chart-point]'));
        if (!canvas || !shell || !elements.length) return;

        try {
            const points = elements.map((element) => {
                const count = Number(element.dataset.count);
                if (!Number.isSafeInteger(count) || count < 0) throw new Error('Invalid chart count');
                const href = element instanceof HTMLAnchorElement ? new URL(element.href, location.href) : null;
                if (href && href.origin !== location.origin) throw new Error('External chart link');
                return { label: element.dataset.label, count, href: href?.href ?? null };
            });
            if (!points.some((point) => point.count > 0)) return;

            const line = panel.dataset.chartType === 'line';
            const chart = new Chart(canvas, {
                type: line ? 'line' : 'bar',
                data: {
                    labels: points.map((point) => point.label),
                    datasets: [{
                        label: 'Số lượng', data: points.map((point) => point.count),
                        backgroundColor: line ? token('--brand-soft') : points.map((_, index) => colors[index % colors.length]),
                        borderColor: token('--brand-dark'), borderWidth: line ? 2 : 0,
                        pointBackgroundColor: token('--brand-dark'), fill: line,
                        tension: 0.25, borderRadius: line ? 0 : 4, maxBarThickness: 50
                    }]
                },
                options: {
                    responsive: true, maintainAspectRatio: false, animation: false,
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            backgroundColor: token('--ink'), titleColor: token('--surface'),
                            bodyColor: token('--surface'), displayColors: false,
                            callbacks: { label: (context) => `${context.parsed.y} mục` }
                        }
                    },
                    scales: {
                        x: { grid: { display: false }, ticks: { color: token('--muted'), maxTicksLimit: 7 } },
                        y: { beginAtZero: true, grid: { color: token('--line') },
                            ticks: { color: token('--muted'), precision: 0 } }
                    },
                    onHover: (_event, hits) => canvas.classList.toggle('dash-chart-interactive',
                        hits.length === 1 && Boolean(points[hits[0].index]?.href))
                }
            });
            canvas.setAttribute('aria-label', points.map((point) => `${point.label}: ${point.count}`).join(', '));
            shell.hidden = false;
            canvas.addEventListener('click', (event) => {
                if (event.button !== 0) return;
                const hits = chart.getElementsAtEventForMode(event, 'nearest', { intersect: true }, false);
                if (hits.length !== 1) return;
                const point = points[hits[0].index];
                if (point?.href && point.count > 0) location.assign(point.href);
            });
        } catch (_error) {
            shell.hidden = true;
        }
    });
})();

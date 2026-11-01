/* Admin dashboard: progressive enhancement of server-rendered counts and links. */
(() => {
    'use strict';
    const dashboard = document.querySelector('.adm-dashboard');
    if (!dashboard) return;

    const styles = getComputedStyle(document.documentElement);
    const token = (name) => styles.getPropertyValue(name).trim();
    const readPoints = (root) => Array.from(root.querySelectorAll('[data-chart-point]')).map((link) => {
        const count = Number(link.dataset.count);
        const url = new URL(link.href, window.location.href);
        if (!Number.isSafeInteger(count) || count < 0 || url.origin !== window.location.origin) {
            throw new Error('Invalid chart point');
        }
        return { key: link.dataset.key, label: link.dataset.label, count, href: link.href };
    });
    const statusColors = { Active: token('--success'), Inactive: token('--neutral') };

    function createChart(canvas, getPoints, horizontal) {
        const points = getPoints();
        const colors = points.map((point) => horizontal ? token('--brand') : statusColors[point.key]);
        const chart = new Chart(canvas, {
            type: 'bar',
            data: {
                labels: points.map((point) => point.label),
                datasets: [{ label: 'Tài khoản', data: points.map((point) => point.count),
                    backgroundColor: colors, hoverBackgroundColor: colors,
                    hoverBorderColor: token('--brand-dark'), hoverBorderWidth: 1, borderRadius: 4,
                    maxBarThickness: horizontal ? 20 : 64, categoryPercentage: .72, barPercentage: .8 }]
            },
            options: {
                indexAxis: horizontal ? 'y' : 'x',
                responsive: true, maintainAspectRatio: false, animation: false,
                font: { family: token('--sans') },
                interaction: { mode: 'nearest', intersect: true },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: token('--ink'), titleColor: token('--surface'), bodyColor: token('--surface'),
                        displayColors: false, padding: 12,
                        titleFont: { family: token('--sans') }, bodyFont: { family: token('--sans') },
                        filter: (item) => item.raw > 0,
                        callbacks: { label: (item) => item.raw + ' tài khoản', afterLabel: () => 'Mở danh sách đã lọc ↗' }
                    }
                },
                scales: {
                    x: {
                        beginAtZero: true,
                        grace: horizontal ? '15%' : 0,
                        border: { display: false },
                        grid: { color: token('--line'), display: horizontal },
                        ticks: { color: token('--muted'), precision: 0, maxTicksLimit: horizontal ? 5 : undefined,
                            font: { family: token('--sans'), size: 13 } }
                    },
                    y: {
                        beginAtZero: true,
                        grace: horizontal ? 0 : '15%',
                        border: { display: false },
                        grid: { color: token('--line'), display: !horizontal },
                        ticks: { color: token('--muted'), precision: 0, maxTicksLimit: horizontal ? undefined : 5,
                            font: { family: token('--sans'), size: 13 }, padding: 8 }
                    }
                },
                onHover: (_event, hits) => canvas.classList.toggle('adm-chart-canvas--interactive',
                    hits.length === 1 && getPoints()[hits[0].index]?.count > 0 && Boolean(getPoints()[hits[0].index]?.href))
            },
            plugins: [{
                id: 'adminCountLabels',
                afterDatasetsDraw(chart) {
                    const { ctx } = chart;
                    ctx.save();
                    ctx.font = '600 13px ' + token('--sans');
                    ctx.fillStyle = token('--ink');
                    ctx.textAlign = horizontal ? 'left' : 'center';
                    ctx.textBaseline = horizontal ? 'middle' : 'bottom';
                    chart.getDatasetMeta(0).data.forEach((bar, index) => {
                        const count = chart.data.datasets[0].data[index];
                        ctx.fillText(String(count), horizontal ? bar.x + 8 : bar.x,
                            horizontal ? bar.y : bar.y - 6);
                    });
                    ctx.restore();
                }
            }]
        });
        canvas.addEventListener('mouseleave', () => canvas.classList.remove('adm-chart-canvas--interactive'));
        canvas.addEventListener('click', (event) => {
            if (event.button !== 0) return;
            const hits = chart.getElementsAtEventForMode(event, 'nearest', { intersect: true }, false);
            if (hits.length !== 1) return;
            const point = getPoints()[hits[0].index];
            if (point?.href && point.count > 0) window.location.assign(point.href);
        });
        return chart;
    }

    function setupStatus() {
        const panel = document.getElementById('adm-status-panel');
        if (!panel) return;
        const groups = Array.from(panel.querySelectorAll('.adm-status-group'));
        if (!groups.length) return;
        const selector = panel.querySelector('select');
        const formatPercent = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 });
        const showFallbackBars = () => {
            groups.forEach((group) => { group.hidden = false; });
            panel.querySelector('.adm-scope-control').hidden = true;
            panel.querySelector('[data-chart-fallback]').hidden = false;
        };
        try {
            // Each bar uses the same server-rendered count and native filtered link.
            // Zero stays an empty track, including when the whole group is empty.
            groups.forEach((group) => {
                const points = readPoints(group);
                const total = points.reduce((sum, point) => sum + point.count, 0);
                group.querySelectorAll('[data-chart-point]').forEach((link, index) => {
                    const share = document.createElement('span');
                    share.className = 'adm-status-share';
                    share.setAttribute('aria-hidden', 'true');
                    share.textContent = formatPercent.format(total ? points[index].count / total * 100 : 0) + '%';
                    const bar = document.createElement('progress');
                    bar.className = 'adm-status-meter';
                    bar.max = total || 1;
                    bar.value = points[index].count;
                    bar.setAttribute('aria-hidden', 'true');
                    link.append(share, bar);
                });
            });
            function selectGroup() {
                const group = groups.find((item) => item.id === selector.value);
                if (!group) throw new Error('Unknown account scope');
                groups.forEach((item) => { item.hidden = item !== group; });
            }
            selectGroup();
            panel.querySelector('.adm-scope-control').hidden = false;
            panel.querySelector('[data-chart-fallback]').hidden = true;
            selector.addEventListener('change', () => {
                try { selectGroup(); } catch (_error) { showFallbackBars(); }
            });
        } catch (_error) { showFallbackBars(); }
    }

    function setupRoles() {
        if (typeof window.Chart !== 'function') return;
        const panel = document.getElementById('adm-role-panel');
        if (!panel) return;
        const points = readPoints(panel);
        if (!points.length || !points.some((point) => point.count > 0)) return;
        const shell = panel.querySelector('.adm-chart-shell');
        shell.hidden = false;
        try {
            createChart(panel.querySelector('canvas'), () => points, true);
            panel.querySelector('[data-chart-fallback]').hidden = true;
            // Canvas remains the visual view; keep the same links for keyboard/AT and failure fallback.
            panel.querySelector('.adm-role-links').classList.add('adm-role-links--chart');
            panel.querySelector('canvas').setAttribute('aria-label',
                points.map((point) => point.label + ' ' + point.count).join(', '));
        } catch (_error) {
            shell.hidden = true;
        }
    }

    // One chart failing must not break the other chart or any ordinary HTML link.
    for (const setup of [setupStatus, setupRoles]) {
        try { setup(); } catch (_error) { /* Server-rendered links remain available. */ }
    }
})();

/* Dashboard nội bộ: vẽ biểu đồ Chart.js từ danh sách data-chart-point do server render; lỗi thì giữ danh sách. */
(() => {
    'use strict';
    if (typeof window.Chart !== 'function') return;

    const styles = getComputedStyle(document.documentElement);
    const token = (name) => styles.getPropertyValue(name).trim();
    const font = { family: token('--sans'), size: 13 };

    // Mỗi điểm dữ liệu là một <li data-chart-point>; số đọc từ data-* và phải là số nguyên không âm
    function readPoints(list) {
        return Array.from(list.querySelectorAll('[data-chart-point]')).map((item) => {
            const point = Object.assign({}, item.dataset);
            ['count', 'hired', 'missing', 'needed', 'pending', 'approved'].forEach((key) => {
                if (point[key] === undefined) return;
                const value = Number(point[key]);
                if (!Number.isSafeInteger(value) || value < 0) throw new Error('Invalid chart number');
                point[key] = value;
            });
            return point;
        });
    }

    // Phần cấu hình chung: chữ, lưới, tooltip theo token của hệ thống
    function baseOptions(points) {
        return {
            responsive: true, maintainAspectRatio: false, animation: false,
            plugins: {
                legend: { position: 'top', align: 'end',
                    labels: { color: token('--muted'), font, boxWidth: 12, boxHeight: 12, useBorderRadius: true, borderRadius: 3 } },
                tooltip: { backgroundColor: token('--ink'), padding: 12, titleFont: font, bodyFont: font,
                    callbacks: { title: (items) => points[items[0].dataIndex].title || points[items[0].dataIndex].label } }
            },
            scales: {
                x: { border: { display: false }, grid: { color: token('--line') }, ticks: { color: token('--muted'), font, precision: 0 } },
                y: { border: { display: false }, grid: { color: token('--line') }, ticks: { color: token('--muted'), font, precision: 0 } }
            }
        };
    }

    // HM: thanh ngang chồng Đã tuyển / Còn thiếu; bấm thanh mở chi tiết yêu cầu
    function progressChart(canvas, points) {
        const options = baseOptions(points);
        options.indexAxis = 'y';
        options.scales.x.stacked = true;
        options.scales.x.beginAtZero = true;
        options.scales.y.stacked = true;
        options.scales.y.grid.display = false;
        options.plugins.tooltip.callbacks.label = (item) => item.datasetIndex === 0
            ? 'Đã tuyển: ' + points[item.dataIndex].hired + '/' + points[item.dataIndex].needed + ' người'
            : 'Còn thiếu: ' + item.raw + ' người';
        options.plugins.tooltip.callbacks.footer = (items) => points[items[0].dataIndex].note;
        options.onClick = (_event, hits) => {
            if (hits.length === 1 && points[hits[0].index].href) window.location.assign(points[hits[0].index].href);
        };
        return new Chart(canvas, {
            type: 'bar',
            data: {
                labels: points.map((point) => point.label),
                datasets: [
                    { label: 'Đã tuyển', data: points.map((point) => point.hired), backgroundColor: token('--brand-hover'), borderRadius: 3, maxBarThickness: 26 },
                    { label: 'Còn thiếu', data: points.map((point) => point.missing), backgroundColor: token('--brand-caption'), borderRadius: 3, maxBarThickness: 26 }
                ]
            },
            options
        });
    }

    // HR: đường số hồ sơ mới theo tuần
    function trendChart(canvas, points) {
        const options = baseOptions(points);
        options.plugins.legend.display = false;
        options.scales.x.grid.display = false;
        options.plugins.tooltip.callbacks.label = (item) => item.raw + ' hồ sơ mới';
        return new Chart(canvas, {
            type: 'line',
            data: {
                labels: points.map((point) => point.label),
                datasets: [{ label: 'Hồ sơ mới', data: points.map((point) => point.count),
                    borderColor: token('--brand-dark'), backgroundColor: token('--brand-soft'), fill: true,
                    pointBackgroundColor: token('--brand-dark'), pointRadius: 4, tension: 0.3 }]
            },
            options
        });
    }

    // Director: cột chồng Chờ duyệt / Đã duyệt theo phòng ban
    function departmentChart(canvas, points) {
        const options = baseOptions(points);
        options.scales.x.stacked = true;
        options.scales.x.grid.display = false;
        options.scales.y.stacked = true;
        options.scales.y.beginAtZero = true;
        options.plugins.tooltip.callbacks.label = (item) => item.dataset.label + ': ' + item.raw + ' yêu cầu';
        return new Chart(canvas, {
            type: 'bar',
            data: {
                labels: points.map((point) => point.label),
                datasets: [
                    { label: 'Đã duyệt', data: points.map((point) => point.approved), backgroundColor: token('--brand-dark'), borderRadius: 3, maxBarThickness: 48 },
                    { label: 'Chờ duyệt', data: points.map((point) => point.pending), backgroundColor: token('--warning'), borderRadius: 3, maxBarThickness: 48 }
                ]
            },
            options
        });
    }

    const builders = { progress: progressChart, trend: trendChart, departments: departmentChart };

    // Vẽ được thì hiện khung biểu đồ và ẩn danh sách (vẫn để cho trình đọc màn hình); lỗi thì giữ danh sách
    document.querySelectorAll('.dash-chart[data-chart]').forEach((shell) => {
        const list = shell.nextElementSibling;
        const build = builders[shell.dataset.chart];
        if (!build || !list || !list.hasAttribute('data-chart-data')) return;
        try {
            shell.hidden = false;
            build(shell.querySelector('canvas'), readPoints(list));
            list.classList.add('sr-only');
        } catch (_error) {
            shell.hidden = true;
        }
    });
})();

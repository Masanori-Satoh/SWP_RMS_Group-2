/* =============================================================================
   ROW-MENU.JS — Menu ⋯ nổi trong bảng danh sách (Yêu cầu tuyển dụng, Tin tuyển dụng)
   Markup: <details class="more-menu more-menu--floating"> của catalog (components.css).
   Vì sao cần: menu catalog mở NGAY TRONG ô của bảng, làm cột thao tác rộng ra và dòng cao lên
   (bảng bị xô lệch). Ở đây danh sách được đặt "position: fixed" (CSS của từng trang), JS chỉ
   tính vị trí: ngay dưới nút ⋯, căn mép phải; phía dưới không đủ chỗ thì lật lên trên.
   Menu đóng khi: bấm ra ngoài, nhấn Esc, cuộn trang / cuộn ngang bảng, đổi kích thước cửa sổ.
   ============================================================================= */
(() => {
    'use strict';

    const menus = document.querySelectorAll('details.more-menu--floating');
    if (menus.length === 0) return;

    // Khoảng cách (px) giữa nút ⋯ và danh sách, và lề tối thiểu với mép màn hình
    const GAP = 4;

    // Đặt danh sách ngay dưới nút ⋯, căn mép phải với nút
    function placeList(menu) {
        const button = menu.querySelector('.more-menu__toggle').getBoundingClientRect();
        const list = menu.querySelector('.more-menu__list');
        const listSize = list.getBoundingClientRect();

        let top = button.bottom + GAP;
        const notEnoughRoomBelow = top + listSize.height > window.innerHeight;
        const enoughRoomAbove = button.top - GAP - listSize.height > 0;
        if (notEnoughRoomBelow && enoughRoomAbove) {
            // Dòng gần cuối màn hình: mở lên phía trên nút
            top = button.top - GAP - listSize.height;
        }

        let left = button.right - listSize.width;
        if (left < GAP) {
            left = GAP;
        }

        list.style.top = top + 'px';
        list.style.left = left + 'px';
    }

    function closeAllMenus() {
        menus.forEach(menu => {
            menu.open = false;
        });
    }

    menus.forEach(menu => {
        const toggle = menu.querySelector('.more-menu__toggle');

        // Tự mở / đóng thay cho trình duyệt để tính vị trí NGAY khi mở (không bị chớp ở chỗ cũ).
        // Bấm Enter / Space trên nút ⋯ cũng sinh sự kiện click nên bàn phím dùng được như cũ.
        toggle.addEventListener('click', event => {
            event.preventDefault();
            const willOpen = !menu.open;
            closeAllMenus();
            if (willOpen) {
                menu.open = true;
                placeList(menu);
            }
        });

        menu.addEventListener('keydown', event => {
            if (event.key === 'Escape' && menu.open) {
                event.preventDefault();
                menu.open = false;
                toggle.focus();
            }
        });
    });

    // Bấm ra ngoài menu đang mở thì đóng
    document.addEventListener('click', event => {
        menus.forEach(menu => {
            if (menu.open && !menu.contains(event.target)) {
                menu.open = false;
            }
        });
    });

    // Danh sách đứng yên trên màn hình nên khi trang / bảng cuộn thì đóng lại.
    // Tham số true (capture) để bắt cả cuộn ngang của khung bảng, không chỉ cuộn trang.
    window.addEventListener('scroll', closeAllMenus, true);
    window.addEventListener('resize', closeAllMenus);
})();

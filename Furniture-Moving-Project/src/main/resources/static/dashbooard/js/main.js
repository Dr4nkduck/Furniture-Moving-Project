/* main.js — dashboard core + hover-to-peek sidebar
   Yêu cầu: jQuery + Bootstrap Bundle nạp trước file này
*/
(function ($) {
    "use strict";

    // =========================
    // 0) Utilities
    // =========================
    const onReady = (fn) => {
        if (document.readyState !== 'loading') fn();
        else document.addEventListener('DOMContentLoaded', fn, { once: true });
    };

    // Helper: throttle
    const throttle = (fn, wait) => {
        let t = 0, savedArgs, savedThis, pending = false;
        const later = () => {
            t = Date.now();
            pending = false;
            fn.apply(savedThis, savedArgs);
        };
        return function throttled(...args) {
            const now = Date.now();
            const remaining = wait - (now - t);
            savedArgs = args;
            savedThis = this;
            if (remaining <= 0 || remaining > wait) {
                if (pending) { clearTimeout(pending); pending = false; }
                t = now;
                fn.apply(savedThis, savedArgs);
            } else if (!pending) {
                pending = setTimeout(later, remaining);
            }
        };
    };

    onReady(function () {
        // Cache
        const $window    = $(window);
        const $document  = $(document);
        const $sidebar   = $('.sidebar');
        const $content   = $('.content');
        const $togglers  = $('.sidebar-toggler');
        const $backToTop = $('.back-to-top');

        // =========================
        // 1) Spinner (tùy chọn)
        // =========================
        (function initSpinner() {
            const $spinner = $('#spinner, .spinner-grow').first();
            if ($spinner.length) {
                setTimeout(() => $spinner.fadeOut(150), 200);
            }
        })();

        // =========================
        // 2) Sidebar: CLICK toggle (giữ nguyên hành vi cũ)
        // =========================
        (function initSidebarClickToggle() {
            if ($sidebar.length === 0 || $content.length === 0 || $togglers.length === 0) return;
            // tránh trùng lặp listener
            $togglers.off('.core').on('click.core', function (e) {
                e.preventDefault();
                $sidebar.toggleClass('open');
                $content.toggleClass('open');
            });
        })();

        // =========================
        // 3) Sidebar: HOVER-to-PEEK (tự mở khi rê chuột, tự ẩn khi rời)
        // =========================
        (function initSidebarHoverPeek() {
            if ($sidebar.length === 0 || $content.length === 0 || $togglers.length === 0) return;

            var HIDE_DELAY_MS = 180;
            var hideTimer = null;

            function showPeek() {
                clearTimeout(hideTimer);
                $sidebar.addClass('open');
                $content.addClass('open');
            }
            function scheduleHidePeek() {
                clearTimeout(hideTimer);
                hideTimer = setTimeout(function () {
                    $sidebar.removeClass('open');
                    $content.removeClass('open');
                }, HIDE_DELAY_MS);
            }

            // gỡ handler cũ (nếu có) rồi bind mới, tránh nhân đôi
            $togglers.off('.peek')
                .on('mouseenter.peek', showPeek)
                .on('mouseleave.peek', scheduleHidePeek)
                .on('click.peek touchstart.peek', function (e) {
                    // chạm/click để “nhá” mở trên mobile
                    e.preventDefault();
                    if ($sidebar.hasClass('open')) scheduleHidePeek();
                    else showPeek();
                });

            $sidebar.off('.peek')
                .on('mouseenter.peek', showPeek)
                .on('mouseleave.peek', scheduleHidePeek);

            // click ngoài → ẩn ngay (thân thiện mobile)
            $document.off('click.peek').on('click.peek', function (e) {
                var $t = $(e.target);
                var insideSidebar = $t.closest('.sidebar').length > 0;
                var onToggler = $t.closest('.sidebar-toggler').length > 0;
                if (!insideSidebar && !onToggler) {
                    clearTimeout(hideTimer);
                    $sidebar.removeClass('open');
                    $content.removeClass('open');
                }
            });

            // accessibility: tab rời khỏi toggler/sidebar thì ẩn
            $document.off('keyup.peek').on('keyup.peek', throttle(function (e) {
                if (e.key === 'Tab') {
                    var $active = $(document.activeElement);
                    var focusInSidebar = $active.closest('.sidebar').length > 0;
                    var focusOnToggler = $active.closest('.sidebar-toggler').length > 0;
                    if (!focusInSidebar && !focusOnToggler) {
                        $sidebar.removeClass('open');
                        $content.removeClass('open');
                    }
                }
            }, 50));
        })();

        // =========================
        // 4) Back to top
        // =========================
        (function initBackToTop() {
            if ($backToTop.length === 0) return;
            const onScroll = throttle(function () {
                if ($window.scrollTop() > 300) $backToTop.fadeIn(150);
                else $backToTop.fadeOut(150);
            }, 100);
            $window.off('scroll.core-btt').on('scroll.core-btt', onScroll);
            onScroll();
            $backToTop.off('click.core-btt').on('click.core-btt', function (e) {
                e.preventDefault();
                $('html, body').animate({ scrollTop: 0 }, 300, 'swing');
            });
        })();

        // =========================
        // 5) Tooltip / Popover (Bootstrap)
        // =========================
        (function initTooltips() {
            $('[data-bs-toggle="tooltip"]').each(function () {
                try { const tip = bootstrap.Tooltip.getInstance(this); if (tip) tip.dispose(); } catch (_) {}
            });
            $('[data-bs-toggle="popover"]').each(function () {
                try { const pop = bootstrap.Popover.getInstance(this); if (pop) pop.dispose(); } catch (_) {}
            });
            $('[data-bs-toggle="tooltip"]').tooltip({ boundary: 'window' });
            $('[data-bs-toggle="popover"]').popover();
        })();

        // =========================
        // 6) Theme toggle text (tùy chọn)
        // =========================
        // in main.js
        (function initThemeToggleText() {
            const $btn = $('#theme-toggle');
            if ($btn.length === 0) return;
            const setLabel = () => {
                const isDark = document.documentElement.classList.contains('dark');
                $btn.text(isDark ? '☀️ Light' : '🌙 Dark');
            };
            $btn.off('click.core-theme').on('click.core-theme', function () {
                const root = document.documentElement;
                const isDark = root.classList.contains('dark');
                root.classList.toggle('dark', !isDark);
                localStorage.setItem('theme', !isDark ? 'dark' : 'light');
                setLabel();
            });
            setLabel();
        })();

        // === Sidebar backdrop (overlay) that follows sidebar open/close ===
        (function initSidebarBackdrop() {
            var $sidebar = $('.sidebar');
            if ($sidebar.length === 0) return;

            // Create once if missing
            if ($('#sidebar-backdrop').length === 0) {
                $('body').append('<div id="sidebar-backdrop" aria-hidden="true"></div>');
            }
            var $backdrop = $('#sidebar-backdrop');

            // Keep backdrop in sync with sidebar's open/closed state
            // (works even if other code toggles classes)
            try {
                var target = $sidebar.get(0);
                var obs = new MutationObserver(function () {
                    if ($sidebar.hasClass('open')) $backdrop.addClass('show');
                    else $backdrop.removeClass('show');
                });
                obs.observe(target, { attributes: true, attributeFilter: ['class'] });
                // initial sync
                if ($sidebar.hasClass('open')) $backdrop.addClass('show'); else $backdrop.removeClass('show');
            } catch (e) {
                // Fallback (rare): poll once after a tick
                setTimeout(function () {
                    if ($sidebar.hasClass('open')) $backdrop.addClass('show'); else $backdrop.removeClass('show');
                }, 0);
            }

            // Click backdrop to close sidebar
            $(document).off('click.sidebar-backdrop').on('click.sidebar-backdrop', '#sidebar-backdrop', function () {
                $sidebar.removeClass('open');
                $('.content').removeClass('open'); // harmless if you still toggle this elsewhere
            });
        })();


        // =========================
        // 7) Responsive hooks (tùy chỉnh thêm nếu cần)
        // =========================
        (function initResponsiveAdjustments() {
            const onResize = throttle(function () {
                // chỗ dành cho xử lý responsive bổ sung
            }, 120);
            $window.off('resize.core').on('resize.core', onResize);
        })();

    }); // onReady end

})(jQuery);

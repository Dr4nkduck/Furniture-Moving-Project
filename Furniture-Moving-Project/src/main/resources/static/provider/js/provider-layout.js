/* ===== Provider Layout: sidebar + darkmode ===== */
(function () {
    // Early theme boot to reduce flash (runs immediately)
    try {
        var saved = localStorage.getItem('theme');
        var prefers = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
        var dark = saved ? (saved === 'dark') : prefers;
        document.documentElement.classList.toggle('dark', !!dark);
    } catch (e) {
    }

    document.addEventListener('DOMContentLoaded', function () {
        const sidebar = document.querySelector('.sidebar');
        const backdrop = document.getElementById('sidebar-backdrop') || (function () {
            const el = document.createElement('div');
            el.id = 'sidebar-backdrop';
            document.body.appendChild(el);
            return el;
        })();
        const toggler = document.querySelector('.sidebar-toggler');
        const themeBtn = document.getElementById('theme-toggle');

        function syncBackdrop() {
            backdrop.classList.toggle('show', sidebar && sidebar.classList.contains('open'));
        }

        if (toggler) {
            toggler.addEventListener('click', function (e) {
                e.preventDefault();
                // On mobile, toggle overlay; on desktop, no effect (sidebar always visible)
                if (window.matchMedia('(max-width: 991px)').matches) {
                    sidebar.classList.toggle('open');
                    syncBackdrop();
                }
            });
        }
        backdrop.addEventListener('click', function () {
            sidebar.classList.remove('open');
            syncBackdrop();
        });

        function setTheme(isDark) {
            document.documentElement.classList.toggle('dark', !!isDark);
            try {
                localStorage.setItem('theme', isDark ? 'dark' : 'light');
            } catch (e) {
            }
            if (themeBtn) themeBtn.textContent = isDark ? '☀️ Light' : '🌙 Dark';
        }

        if (themeBtn) {
            const isDark = document.documentElement.classList.contains('dark');
            setTheme(isDark);
            themeBtn.addEventListener('click', function () {
                setTheme(!document.documentElement.classList.contains('dark'));
            });
        }

        // Active menu highlight (basic)
        const current = location.pathname;
        document.querySelectorAll('.sidebar .nav a').forEach(a => {
            if (a.getAttribute('href') && current.startsWith(a.getAttribute('href'))) {
                a.classList.add('active');
            }
        });

        syncBackdrop();
    });
})();

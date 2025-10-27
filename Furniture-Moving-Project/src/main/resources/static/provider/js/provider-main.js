(function ($) {
    "use strict";

    $(function () {
        var $sidebar = $('#provSidebar');

        // Sidebar toggle (overlay)
        $(document).off('click.prov-toggle').on('click.prov-toggle', '#provSidebarToggler', function (e) {
            e.preventDefault();
            $sidebar.toggleClass('open');
            syncBackdrop();
        });

        // Backdrop sync & click-to-close
        function ensureBackdrop() {
            if (!$('#prov-backdrop').length) return false;
            return true;
        }
        function syncBackdrop() {
            if (!ensureBackdrop()) return;
            $('#prov-backdrop').toggleClass('show', $sidebar.hasClass('open'));
        }
        $(document).off('click.prov-backdrop').on('click.prov-backdrop', '#prov-backdrop', function () {
            $sidebar.removeClass('open'); syncBackdrop();
        });
        syncBackdrop();

        // Dark mode toggle (provider area)
        function setLabel() {
            var btn = document.getElementById('provider-theme-toggle');
            if (!btn) return;
            var isDark = document.documentElement.classList.contains('dark');
            btn.textContent = isDark ? '☀️ Light' : '🌙 Dark';
            btn.setAttribute('aria-pressed', String(isDark));
        }
        $(document).off('click.prov-theme').on('click.prov-theme', '#provider-theme-toggle', function (e) {
            e.preventDefault();
            var root = document.documentElement;
            var isDark = root.classList.contains('dark');
            root.classList.toggle('dark', !isDark);
            try { localStorage.setItem('theme', !isDark ? 'dark' : 'light'); } catch(e){}
            setLabel();
        });
        if (document.readyState !== 'loading') setLabel(); else document.addEventListener('DOMContentLoaded', setLabel, {once:true});
        setTimeout(setLabel, 0);
    });

})(jQuery);

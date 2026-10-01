/**
 * App shell behavior: sidebar toggle, back-to-top, tooltips, form validation.
 */
(function () {
    "use strict";

    var select = function (el, all) {
        el = el.trim();
        if (all) {
            return [].slice.call(document.querySelectorAll(el));
        }
        return document.querySelector(el);
    };

    var on = function (type, el, listener, all) {
        if (all) {
            select(el, true).forEach(function (e) { e.addEventListener(type, listener); });
        } else {
            var target = select(el);
            if (target) {
                target.addEventListener(type, listener);
            }
        }
    };

    var onscroll = function (el, listener) {
        el.addEventListener('scroll', listener);
    };

    // Sidebar toggle
    if (select('.toggle-sidebar-btn')) {
        on('click', '.toggle-sidebar-btn', function () {
            document.body.classList.toggle('toggle-sidebar');
        });
    }

    // Toggle .header-scrolled class to #header when the page is scrolled
    var selectHeader = select('#header');
    if (selectHeader) {
        var headerScrolled = function () {
            if (window.scrollY > 100) {
                selectHeader.classList.add('header-scrolled');
            } else {
                selectHeader.classList.remove('header-scrolled');
            }
        };
        window.addEventListener('load', headerScrolled);
        onscroll(document, headerScrolled);
    }

    // Back to top button
    var backtotop = select('.back-to-top');
    if (backtotop) {
        var toggleBacktotop = function () {
            if (window.scrollY > 100) {
                backtotop.classList.add('active');
            } else {
                backtotop.classList.remove('active');
            }
        };
        window.addEventListener('load', toggleBacktotop);
        onscroll(document, toggleBacktotop);
    }

    // Bootstrap tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.forEach(function (el) {
        new bootstrap.Tooltip(el);
    });

    // Bootstrap client-side validation styling
    var needsValidation = document.querySelectorAll('.needs-validation');
    [].slice.call(needsValidation).forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });
})();

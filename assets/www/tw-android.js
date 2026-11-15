/*
 * Turbowarp Android injection layer.
 *
 * Loaded from <head> so the language override happens before the editor bundle
 * reads navigator.language.
 *
 *  - forces the app language onto the editor
 *  - applies the app accent colour / high contrast to the editor
 *  - keeps the editor's project title in sync with the app
 *  - replaces TurboWarp's "save to computer" with the native bridge
 *  - hides the TurboWarp-only menu entries
 */
(function () {
    'use strict';

    var L = {
        save: 'Save',
        saving: 'Saving...',
        failed: 'Save failed',
        notReady: 'Editor is still loading',
        loadFailed: 'Could not open the project'
    };

    function bridge(name) {
        try {
            return (window.TWAndroid && TWAndroid[name]) ? TWAndroid[name] : null;
        } catch (e) {
            return null;
        }
    }

    /* ------------------------------------------------ 1. language (earliest) */

    var LOCALE = '';
    try {
        if (bridge('locale')) LOCALE = TWAndroid.locale();
    } catch (e) { }
    if (LOCALE) {
        var fix = function (prop, value) {
            try {
                Object.defineProperty(window.navigator, prop, {
                    get: function () { return value; },
                    configurable: true
                });
            } catch (e) { }
        };
        fix('language', LOCALE);
        fix('languages', [LOCALE]);
        try { document.documentElement.lang = LOCALE; } catch (e) { }
    }

    /* ------------------------------------------------- 2. dark mode (earliest) */

    var DARK = false;
    try {
        if (bridge('dark')) DARK = TWAndroid.dark() === '1';
    } catch (e) { }
    try {
        if (DARK) localStorage.setItem('tw:theme', '{"gui":"dark"}');
        else localStorage.removeItem('tw:theme');
    } catch (e) { }

    try {
        if (bridge('label')) {
            L.save = TWAndroid.label('save') || L.save;
            L.saving = TWAndroid.label('saving') || L.saving;
            L.failed = TWAndroid.label('failed') || L.failed;
            L.notReady = TWAndroid.label('notready') || L.notReady;
            L.loadFailed = TWAndroid.label('loadfailed') || L.loadFailed;
        }
    } catch (e) { }

    function log(m) {
        try { TWAndroid.log(String(m)); } catch (e) { }
    }

    function toast(m) {
        try { TWAndroid.toast(String(m)); } catch (e) { }
    }

    // ---------------------------------------------------------------- helpers

    function base64(u8) {
        var out = '';
        var CHUNK = 0x8000;
        for (var i = 0; i < u8.length; i += CHUNK) {
            out += String.fromCharCode.apply(
                null, u8.subarray(i, Math.min(i + CHUNK, u8.length)));
        }
        return btoa(out);
    }

    function withTimeout(promise, ms, fallback) {
        return new Promise(function (resolve) {
            var done = false;
            var t = setTimeout(function () {
                if (done) return;
                done = true;
                resolve(fallback);
            }, ms);
            Promise.resolve(promise).then(function (v) {
                if (done) return;
                done = true;
                clearTimeout(t);
                resolve(v);
            }, function () {
                if (done) return;
                done = true;
                clearTimeout(t);
                resolve(fallback);
            });
        });
    }

    function waitForVm() {
        return new Promise(function (resolve) {
            var tries = 0;
            var timer = setInterval(function () {
                tries++;
                if (window.vm && window.vm.initialized) {
                    clearInterval(timer);
                    resolve(window.vm);
                } else if (tries > 600) {
                    clearInterval(timer);
                    resolve(null);
                }
            }, 300);
        });
    }

    // ---------------------------------------------------------------- theming

    function hexToRgb(hex) {
        var h = String(hex || '').replace('#', '');
        if (h.length === 3) h = h[0] + h[0] + h[1] + h[1] + h[2] + h[2];
        var n = parseInt(h, 16);
        if (isNaN(n)) return [255, 76, 76];
        return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
    }

    function shade(hex, f) {
        var c = hexToRgb(hex);
        return 'rgb(' + Math.round(c[0] * f) + ',' + Math.round(c[1] * f) + ',' + Math.round(c[2] * f) + ')';
    }

    function tint(hex, f) {
        var c = hexToRgb(hex);
        return 'rgb(' + Math.round(c[0] + (255 - c[0]) * f) + ',' +
            Math.round(c[1] + (255 - c[1]) * f) + ',' +
            Math.round(c[2] + (255 - c[2]) * f) + ')';
    }

    function applyTheme() {
        var accent = '#ff4c4c';
        var hc = false;
        try {
            if (bridge('accent')) accent = TWAndroid.accent() || accent;
            if (bridge('highContrast')) hc = TWAndroid.highContrast() === '1';
        } catch (e) { }

        var dark = shade(accent, 0.78);
        var c = hexToRgb(accent);
        var rgba = function (a) {
            return 'rgba(' + c[0] + ',' + c[1] + ',' + c[2] + ',' + a + ')';
        };

        var map = {
            '--motion-primary': accent,
            '--motion-primary-default': accent,
            '--motion-primary-transparent': rgba(0.9),
            '--motion-primary-transparent-default': rgba(0.9),
            '--motion-tertiary': dark,
            '--motion-tertiary-default': dark,
            '--looks-secondary': accent,
            '--looks-secondary-default': accent,
            '--looks-transparent': rgba(0.35),
            '--looks-transparent-default': rgba(0.35),
            '--looks-light-transparent': rgba(0.15),
            '--looks-light-transparent-default': rgba(0.15),
            '--looks-secondary-dark': dark,
            '--looks-secondary-dark-default': dark,
            '--drop-highlight': tint(accent, 0.35),
            '--drop-highlight-default': tint(accent, 0.35),
            '--menu-bar-background': accent,
            '--menu-bar-background-default': accent,
            '--ui-modal-header-background': accent,
            '--ui-modal-header-background-default': accent
        };
        if (hc) {
            map['--text-primary'] = '#000000';
            map['--text-primary-default'] = '#000000';
            map['--text-primary-transparent'] = 'rgba(0,0,0,0.85)';
            map['--text-primary-transparent-default'] = 'rgba(0,0,0,0.85)';
            map['--ui-black-transparent'] = 'rgba(0,0,0,0.45)';
            map['--ui-black-transparent-default'] = 'rgba(0,0,0,0.45)';
            map['--menu-bar-background'] = '#000000';
            map['--menu-bar-background-default'] = '#000000';
        }

        var root = document.documentElement;
        if (!root) return;
        for (var k in map) {
            if (Object.prototype.hasOwnProperty.call(map, k)) {
                root.style.setProperty(k, map[k]);
            }
        }
        var btn = document.getElementById('tw-save-btn');
        if (btn) btn.style.background = hc ? '#000000' : accent;
    }

    // ------------------------------------------------------------- thumbnails

    window.__twSnapshot = function () {
        return new Promise(function (resolve) {
            try {
                var r = window.vm && window.vm.renderer;
                if (!r) return resolve('');
                if (r.requestSnapshot) {
                    var done = false;
                    var guard = setTimeout(function () {
                        if (!done) { done = true; resolve(legacySnapshot()); }
                    }, 1500);
                    r.requestSnapshot(function (dataUrl) {
                        if (done) return;
                        done = true;
                        clearTimeout(guard);
                        resolve(!dataUrl ? legacySnapshot() : dataUrl);
                    });
                } else {
                    resolve(legacySnapshot());
                }
            } catch (e) {
                resolve('');
            }
        });
    };

    function legacySnapshot() {
        try {
            var c = window.vm && window.vm.renderer && window.vm.renderer.canvas;
            return c ? c.toDataURL('image/png') : '';
        } catch (e) {
            return '';
        }
    }

    // ------------------------------------------------------------ project title

    var titleKnown = null;

    function titleInput() {
        return document.querySelector('input[class*="project-title-input"]');
    }

    function readTitle() {
        var el = titleInput();
        return el ? el.value.trim() : '';
    }

    function pushTitle(name) {
        var el = titleInput();
        if (!el || !name || el.value === name) return;
        try {
            var setter = Object.getOwnPropertyDescriptor(
                window.HTMLInputElement.prototype, 'value').set;
            setter.call(el, name);
            el.dispatchEvent(new Event('input', { bubbles: true }));
            titleKnown = name;
        } catch (e) {
            log('title set failed: ' + e);
        }
    }

    function hookTitle() {
        var el = titleInput();
        if (!el || el.__twTitleHooked) return;
        el.__twTitleHooked = true;
        if (titleKnown === null) {
            try { titleKnown = TWAndroid.projectTitle(); } catch (e) { titleKnown = ''; }
            if (titleKnown) pushTitle(titleKnown);
        }
        var pendingTimer = null;
        var send = function () {
            var v = el.value.trim();
            if (!v || v === titleKnown) return;
            titleKnown = v;
            try { TWAndroid.setTitle(v); } catch (e) { }
        };
        el.addEventListener('input', function () {
            if (pendingTimer) clearTimeout(pendingTimer);
            pendingTimer = setTimeout(send, 600);
        });
        el.addEventListener('blur', function () {
            if (pendingTimer) clearTimeout(pendingTimer);
            send();
        });
    }

    // ------------------------------------------------------------------- save

    window.__twSave = function () {
        var vm = window.vm;
        if (!vm || !vm.saveProjectSb3) {
            toast(L.notReady);
            return;
        }
        toast(L.saving);
        var name = readTitle();
        try { TWAndroid.startSave(name); } catch (e) { }
        withTimeout(vm.saveProjectSb3(), 30000, null).then(function (blob) {
            if (!blob) {
                toast(L.failed);
                try { TWAndroid.endSave(''); } catch (e) { }
                return;
            }
            return blob.arrayBuffer().then(function (buffer) {
                var bytes = new Uint8Array(buffer);
                var CHUNK = 128 * 1024;
                for (var i = 0; i < bytes.length; i += CHUNK) {
                    TWAndroid.chunk(base64(bytes.subarray(i, Math.min(i + CHUNK, bytes.length))));
                }
                return withTimeout(window.__twSnapshot(), 2500, '').then(function (thumb) {
                    TWAndroid.endSave(thumb || '');
                });
            });
        }).catch(function (e) {
            log('save error: ' + e);
            toast(L.failed + ': ' + e);
            try { TWAndroid.endSave(''); } catch (e2) { }
        });
    };

    // ------------------------------------------------------------------- load

    var loadStarted = false;

    window.__twLoad = function () {
        if (loadStarted) return;
        loadStarted = true;
        waitForVm().then(function (vm) {
            if (!vm) {
                log('vm never became ready');
                return;
            }
            fetch('/__local/project.sb3?t=' + Date.now(), { cache: 'no-store' })
                .then(function (res) {
                    if (!res.ok) throw new Error('http ' + res.status);
                    return res.arrayBuffer();
                })
                .then(function (buffer) {
                    if (buffer.byteLength < 100) throw new Error('empty project');
                    vm.loadProject(buffer).then(function () {
                        log('project loaded (' + buffer.byteLength + ' bytes)');
                        setTimeout(hookTitle, 800);
                    }).catch(function (e) {
                        log('vm.loadProject failed: ' + e);
                        dropProject(buffer);
                    });
                })
                .catch(function (e) {
                    log('load failed: ' + e);
                    if (String(e).indexOf('http 404') >= 0 || String(e).indexOf('empty') >= 0) return;
                    TWAndroid.reportProblem(L.loadFailed + ': ' + e);
                });
        });
    };

    function dropProject(buffer) {
        try {
            var file = new File([buffer], 'project.sb3',
                { type: 'application/x-scratch-project' });
            var dt = new DataTransfer();
            dt.items.add(file);
            var target = document.elementFromPoint(
                Math.round(window.innerWidth / 2), Math.round(window.innerHeight / 2)) || document.body;
            target.dispatchEvent(new DragEvent('drop', { bubbles: true, cancelable: true, dataTransfer: dt }));
            document.body.dispatchEvent(new DragEvent('drop', { bubbles: true, cancelable: true, dataTransfer: dt }));
            log('drop dispatched');
        } catch (e) {
            log('drop failed: ' + e);
        }
    }

    // -------------------------------------------------- remove TurboWarp-only UI

    var HIDE_RULES = [
        // "See Project Page" / "查看作品页面"
        function (t) { return /project\s*page/i.test(t) || t.indexOf('作品页面') >= 0; },
        // "TurboWarp Feedback" / "TurboWarp 反馈"
        function (t) { return /turbowarp\s*feedback/i.test(t) || (t.indexOf('TurboWarp') >= 0 && t.indexOf('反馈') >= 0); },
        // TurboWarp's own save / export entries (the app has its own save)
        function (t) { return /^保存到(文件|电脑|计算机|本地)|^另存为|^打包作品/.test(t); },
        function (t) { return /^save (as|to)/i.test(t) || /^package project/i.test(t); }
    ];

    function shouldHide(t) {
        for (var i = 0; i < HIDE_RULES.length; i++) {
            try { if (HIDE_RULES[i](t)) return true; } catch (e) { }
        }
        return false;
    }

    // never collapse the menu bar itself or the app root, even if their text happens
    // to match while the editor is still rendering
    function protectedNode(el) {
        if (!el || !el.classList) return true;
        if (el.id === 'app') return true;
        var cls = String(el.className && el.className.baseVal !== undefined
            ? el.className.baseVal : (el.className || ''));
        if (cls.indexOf('gui_menu-bar') >= 0) return true;
        if (cls.indexOf('main-menu') >= 0) return true;
        var tokens = cls.split(/\s+/);
        for (var i = 0; i < tokens.length; i++) {
            // the bar container itself (menu-bar_menu-bar_abc123), not its items
            if (tokens[i] === 'menu-bar_menu-bar' || /^menu-bar_menu-bar_[A-Za-z0-9]+$/.test(tokens[i])) {
                return true;
            }
        }
        return false;
    }

    function looksLikeMenuEntry(el) {
        var cur = el;
        for (var i = 0; i < 4 && cur && cur !== document.body; i++) {
            var cls = (cur.className || '').toString();
            if (cls.indexOf('menu') >= 0) return true;
            cur = cur.parentElement;
        }
        return false;
    }

    function norm(s) {
        return (s || '').replace(/\s+/g, ' ').trim();
    }

    var hiddenTops = [];

    function sweep() {
        // re-apply hiding if React rewrote our style
        for (var h = 0; h < hiddenTops.length; h++) {
            var node = hiddenTops[h];
            if (!node.isConnected) { hiddenTops.splice(h, 1); h--; continue; }
            if (node.style.display !== 'none') node.style.display = 'none';
        }
        var nodes;
        try {
            nodes = document.querySelectorAll('div,a,button,span,li');
        } catch (e) {
            return;
        }
        for (var i = 0; i < nodes.length; i++) {
            var el = nodes[i];
            if (el.__twHidden) continue;
            if (el.childElementCount > 8) continue;
            var text = norm(el.textContent);
            if (!text || text.length > 40) continue;
            if (!shouldHide(text)) continue;
            if (el.id === 'tw-save-btn' || (el.closest && el.closest('#tw-save-btn'))) continue;
            if (!looksLikeMenuEntry(el)) continue;
            var top = el;
            var climbed = 0;
            while (climbed < 3 && top.parentElement && top.parentElement !== document.body &&
                norm(top.parentElement.textContent) === text &&
                !protectedNode(top.parentElement)) {
                top = top.parentElement;
                climbed++;
            }
            if (protectedNode(top)) continue;
            if (top.childElementCount > 4) continue;
            if (top.getBoundingClientRect().height > 64) continue;
            top.style.display = 'none';
            top.__twHidden = true;
            hiddenTops.push(top);
        }
    }

    // ------------------------------------------------------------ save button

    function ensureSaveButton() {
        var existing = document.getElementById('tw-save-btn');
        if (existing) return;
        var bar = document.querySelector('[class*="menu-bar_menu-bar"]');
        if (!bar) return;
        var rect = bar.getBoundingClientRect();
        if (rect.height <= 0) return;

        var btn = document.createElement('div');
        btn.id = 'tw-save-btn';
        btn.textContent = L.save;
        btn.setAttribute('role', 'button');
        var top = Math.max(4, rect.top + (rect.height - 34) / 2);
        btn.style.cssText = [
            'position:fixed',
            'right:10px',
            'top:' + Math.round(top) + 'px',
            'height:34px',
            'min-width:74px',
            'padding:0 18px',
            'display:flex',
            'align-items:center',
            'justify-content:center',
            'box-sizing:border-box',
            'border-radius:17px',
            'background:#ff4c4c',
            'color:#ffffff',
            'font-family:inherit',
            'font-size:14px',
            'font-weight:700',
            'letter-spacing:.5px',
            'cursor:pointer',
            'user-select:none',
            'white-space:nowrap',
            'box-shadow:0 2px 6px rgba(0,0,0,.28)',
            'z-index:2147483000'
        ].join(';');
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            window.__twSave();
        }, true);
        document.body.appendChild(btn);
        applyTheme();
        log('save button added');
    }

    // --------------------------------------------------------------- observers

    var scheduled = false;
    function schedule() {
        if (scheduled) return;
        scheduled = true;
        setTimeout(function () {
            scheduled = false;
            sweep();
            ensureSaveButton();
            hookTitle();
        }, 400);
    }

    function boot() {
        applyTheme();
        sweep();
        ensureSaveButton();
        hookTitle();
        try {
            new MutationObserver(schedule).observe(document.documentElement, {
                childList: true,
                subtree: true
            });
        } catch (e) {
            log('observer failed: ' + e);
        }
        setInterval(function () {
            sweep();
            ensureSaveButton();
            hookTitle();
            applyTheme();
        }, 2000);
        if (/[?&]twload=1/.test(window.location.search)) window.__twLoad();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }

    log('tw-android layer ready');
})();

(() => {
    'use strict';

    function fallbackCopy(text) {
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.readOnly = true;
        textarea.setAttribute('aria-hidden', 'true');
        textarea.style.position = 'fixed';
        textarea.style.inset = '0 auto auto 0';
        textarea.style.opacity = '0';
        textarea.style.pointerEvents = 'none';
        document.body.appendChild(textarea);

        textarea.focus();
        textarea.select();
        textarea.setSelectionRange(0, textarea.value.length);

        try {
            return document.execCommand('copy');
        } catch {
            return false;
        } finally {
            textarea.remove();
        }
    }

    async function copyText(text) {
        if (!text) return false;

        if (window.isSecureContext && navigator.clipboard?.writeText) {
            try {
                await navigator.clipboard.writeText(text);
                return true;
            } catch {
                // Some mobile browsers expose Clipboard API but deny access.
            }
        }

        return fallbackCopy(text);
    }

    function selectForManualCopy(element) {
        element.focus({preventScroll: true});
        element.select();
        element.setSelectionRange(0, element.value.length);
    }

    document.addEventListener('click', async event => {
        if (!(event.target instanceof Element)) return;

        const button = event.target.closest('[data-copy-target]');
        if (!button || button.dataset.copying === 'true') return;

        const target = document.getElementById(button.dataset.copyTarget);
        if (!(target instanceof HTMLInputElement)
            && !(target instanceof HTMLTextAreaElement)) {
            return;
        }

        const originalContent = button.innerHTML;
        button.dataset.copying = 'true';
        button.disabled = true;

        const copied = await copyText(target.value);
        if (copied) {
            button.textContent = '✓ Copied';
        } else {
            selectForManualCopy(target);
            button.textContent = 'Select & copy';
        }

        window.setTimeout(() => {
            button.innerHTML = originalContent;
            button.disabled = false;
            delete button.dataset.copying;
        }, 1500);
    });

    window.copyTextToClipboard = copyText;
})();

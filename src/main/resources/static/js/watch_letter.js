(() => {
    'use strict';

    const envelope = document.getElementById('letter-envelope');

    if (!envelope) {
        return;
    }

    const letterCard = envelope.querySelector('.letter-card');
    const status = document.getElementById('letter-status');
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    let isOpen = false;

    const autoLink = (text) => {
        return text.replace(
            /\bhttps?:\/\/[^\s]+/gu,
            (url) => `<a class="letter-link" href="${url}" target="_blank" rel="noopener">${url}</a>`
        );
    };

    const wrapLetters = (element) => {
        const text = element.textContent;

        if (text.includes('http://') || text.includes('https://')) {
            element.innerHTML = autoLink(text);
            return;
        }

        const fragment = document.createDocumentFragment();

        element.setAttribute('aria-label', text);

        text.split(/(\s+)/u).forEach((part) => {
            if (!part) {
                return;
            }

            if (/^\s+$/u.test(part)) {
                fragment.appendChild(document.createTextNode(part));
                return;
            }

            const word = document.createElement('span');
            word.className = 'animated-word';
            word.setAttribute('aria-hidden', 'true');

            Array.from(part).forEach((character) => {
                const letter = document.createElement('span');
                letter.className = 'animated-letter';
                letter.textContent = character;
                word.appendChild(letter);
            });

            fragment.appendChild(word);
        });

        element.replaceChildren(fragment);
    };

    const animatedBlocks = Array.from(letterCard.querySelectorAll('[data-animate-letters]'));
    animatedBlocks.forEach(wrapLetters);

    const setLetterDelays = () => {
        let blockStart = 300;

        animatedBlocks.forEach((block) => {
            const letters = Array.from(block.querySelectorAll('.animated-letter'));
            const stagger = Math.min(30, 1800 / Math.max(letters.length, 1));

            letters.forEach((letter, index) => {
                letter.style.setProperty('--letter-delay', `${blockStart + stagger * index}ms`);
            });

            blockStart += Math.min(950, stagger * letters.length) + 160;
        });
    };

    setLetterDelays();

    const showLetterText = () => {
        letterCard.classList.add('is-animating-text');
    };

    // Убирает оболочку конверта, оставляя на экране только открытое письмо.
    const hideEnvelope = () => {
        envelope.classList.add('is-envelope-hidden');
        envelope.removeAttribute('role');
        envelope.removeAttribute('tabindex');
    };

    const openEnvelope = () => {
        if (isOpen) {
            return;
        }

        isOpen = true;
        envelope.classList.remove('is-closed');
        envelope.classList.add('is-open');
        envelope.setAttribute('aria-expanded', 'true');
        envelope.setAttribute('aria-label', 'Your romantic letter is open');
        letterCard.setAttribute('aria-hidden', 'false');

        if (status) {
            status.textContent = 'Your romantic letter is open.';
        }

        if (reduceMotion.matches) {
            showLetterText();
            hideEnvelope();
            return;
        }

        window.setTimeout(showLetterText, 1050);
        window.setTimeout(hideEnvelope, 1350);
    };

    envelope.addEventListener('click', openEnvelope);
    envelope.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            openEnvelope();
        }
    });
})();

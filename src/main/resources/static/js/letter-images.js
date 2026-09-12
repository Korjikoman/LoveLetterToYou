(() => {
    'use strict';

    const supportedTypes = new Set(['image/jpeg', 'image/png']);
    const wait = milliseconds => new Promise(resolve => window.setTimeout(resolve, milliseconds));

    async function readJson(response) {
        const body = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(body.error || `Ошибка запроса (${response.status})`);
        }
        return body;
    }

    /** Загружает выбранные картинки заранее и возвращает их готовые UUID для письма. */
    window.createLetterImageManager = function createLetterImageManager(options = {}) {
        const input = document.getElementById(options.inputId || 'letterImages');
        const list = document.getElementById(options.listId || 'letterImageList');
        const status = document.getElementById(options.statusId || 'letterImageStatus');
        const maxImages = options.maxImages || 10;
        const uploads = new Set();
        let lastError = null;

        if (!input || !list || !status) {
            throw new Error('Не найдены элементы загрузки изображений');
        }

        function setStatus(message, isError = false) {
            status.textContent = message;
            status.classList.toggle('is-error', isError);
        }

        function countImages() {
            return list.querySelectorAll('.letter-image-item:not([data-cancelled="true"])').length;
        }

        function createItem(file) {
            const item = document.createElement('article');
            item.className = 'letter-image-item is-uploading';
            item.dataset.ready = 'false';

            const preview = document.createElement('img');
            preview.alt = file.name;
            const localUrl = URL.createObjectURL(file);
            preview.src = localUrl;

            const label = document.createElement('span');
            label.textContent = 'Обработка…';

            const remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'letter-image-remove';
            remove.textContent = 'Удалить';
            remove.setAttribute('aria-label', `Удалить ${file.name}`);

            item.append(preview, label, remove);
            list.appendChild(item);
            return {item, preview, label, localUrl};
        }

        async function waitUntilReady(imageId) {
            for (let attempt = 0; attempt < 150; attempt++) {
                const response = await csrfFetch(`/api/images/${encodeURIComponent(imageId)}/status`);
                const data = await readJson(response);

                if (data.status === 'READY' || data.status === 'ATTACHED') {
                    return data;
                }
                if (['FAILED', 'DELETE_PENDING', 'DELETED'].includes(data.status)) {
                    throw new Error('Сервер не смог обработать изображение');
                }
                await wait(500);
            }
            throw new Error('Обработка изображения заняла слишком много времени');
        }

        async function cancelUploaded(imageId) {
            if (!imageId) return;
            await csrfFetch(`/api/images/${encodeURIComponent(imageId)}`, {
                method: 'DELETE'
            }).catch(() => undefined);
        }

        async function upload(file) {
            const view = createItem(file);
            let imageId = null;

            try {
                const formData = new FormData();
                formData.append('file', file);

                const uploadResponse = await csrfFetch('/api/images?purpose=LETTER', {
                    method: 'POST',
                    body: formData
                });
                const uploaded = await readJson(uploadResponse);
                imageId = uploaded.imageId;
                view.item.dataset.imageId = imageId;

                if (view.item.dataset.cancelled === 'true') {
                    await cancelUploaded(imageId);
                    return;
                }

                const ready = await waitUntilReady(imageId);
                if (view.item.dataset.cancelled === 'true') {
                    await cancelUploaded(imageId);
                    return;
                }

                view.item.dataset.ready = 'true';
                view.item.dataset.existing = 'false';
                view.item.classList.remove('is-uploading');
                view.label.textContent = 'Готово';
                view.preview.src = ready.contentUrl;
                setStatus(`Готово изображений: ${countImages()}/${maxImages}`);
            } catch (error) {
                lastError = error;
                view.item.remove();
                await cancelUploaded(imageId);
                setStatus(error.message || 'Не удалось загрузить изображение', true);
                throw error;
            } finally {
                URL.revokeObjectURL(view.localUrl);
            }
        }

        input.addEventListener('change', () => {
            const files = Array.from(input.files || []);
            input.value = '';
            if (files.length === 0) return;

            if (countImages() + files.length > maxImages) {
                setStatus(`Можно прикрепить не больше ${maxImages} изображений`, true);
                return;
            }
            if (files.some(file => !supportedTypes.has(file.type))) {
                setStatus('Разрешены только JPEG и PNG', true);
                return;
            }

            lastError = null;
            for (const file of files) {
                const task = upload(file);
                uploads.add(task);
                task.then(
                    () => uploads.delete(task),
                    () => uploads.delete(task)
                );
            }
            setStatus('Загрузка и проверка изображений…');
        });

        list.addEventListener('click', event => {
            const button = event.target.closest('.letter-image-remove');
            if (!button) return;

            const item = button.closest('.letter-image-item');
            if (!item) return;
            item.dataset.cancelled = 'true';
            item.remove();

            // Уже прикреплённые изображения удалит транзакция обновления письма.
            if (item.dataset.existing !== 'true') {
                cancelUploaded(item.dataset.imageId);
            }
            setStatus(`Выбрано изображений: ${countImages()}/${maxImages}`);
        });

        return {
            async waitUntilReady() {
                while (uploads.size > 0) {
                    await Promise.allSettled(Array.from(uploads));
                }
                if (lastError) {
                    throw lastError;
                }
            },

            getImageIds() {
                return Array.from(
                    list.querySelectorAll('.letter-image-item[data-ready="true"]')
                ).map(item => item.dataset.imageId);
            }
        };
    };
})();

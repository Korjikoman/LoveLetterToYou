document.addEventListener("DOMContentLoaded", () => {
    const avatarImg = document.getElementById("avatarImage");
    const avatarInput = document.getElementById("avatarInput");
    const passwordInput = document.getElementById("passwordInput");
    const passwordWarning = document.getElementById("passwordWarning");
    const applyBtn = document.getElementById("applyBtn");

    // Avatar init
    const hasAvatar = avatarImg.dataset.hasAvatar === "true";
    const avatarPath = avatarImg.dataset.avatarPath;

    avatarImg.src = (hasAvatar && avatarPath)
        ? avatarPath
        : "/images/default-avatar-icon.jpg";

    // Avatar preview
    avatarInput.addEventListener("change", () => {
        const file = avatarInput.files[0];
        if (file) {
            avatarImg.src = URL.createObjectURL(file);
        }
    });

    // Password warning
    passwordInput.addEventListener("focus", () => {
        passwordWarning.classList.add("visible");
    });

    passwordInput.addEventListener("blur", () => {
        if (!passwordInput.value) {
            passwordWarning.classList.remove("visible");
        }
    });

    // Сохраняем данные профиля и аватар отдельными согласованными запросами.
    applyBtn.addEventListener("click", async () => {
        applyBtn.disabled = true;
        let unattachedImageId = null;

        try {
            const username = document.getElementById("usernameInput").value.trim();
            const password = passwordInput.value;
            const avatarFile = avatarInput.files[0];

            await requestJson("/profile/api", {
                method: "PATCH",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    username: username || null,
                    password: password || null
                })
            });

            if (avatarFile) {
                const formData = new FormData();
                formData.append("file", avatarFile);

                const upload = await requestJson("/api/images?purpose=AVATAR", {
                    method: "POST",
                    body: formData
                });
                unattachedImageId = upload.imageId;

                const ready = await waitUntilReady(upload.imageId);
                const avatar = await requestJson("/profile/api/avatar", {
                    method: "PUT",
                    headers: {"Content-Type": "application/json"},
                    body: JSON.stringify({imageId: upload.imageId})
                });

                unattachedImageId = null;
                avatarImg.src = `${avatar.contentUrl}?v=${Date.now()}`;
                avatarImg.dataset.hasAvatar = "true";
                avatarImg.dataset.avatarPath = ready.contentUrl;
                avatarInput.value = "";
            }

            passwordInput.value = "";
            showToast("Profile saved");
        } catch (error) {
            if (unattachedImageId) {
                csrfFetch(`/api/images/${encodeURIComponent(unattachedImageId)}`, {
                    method: "DELETE"
                }).catch(() => {});
            }
            showToast(error.message || "Cannot save profile", true);
        } finally {
            applyBtn.disabled = false;
        }
    });

    async function waitUntilReady(imageId) {
        for (let attempt = 0; attempt < 60; attempt++) {
            const state = await requestJson(
                `/api/images/${encodeURIComponent(imageId)}/status`
            );

            if (state.status === "READY" || state.status === "ATTACHED") {
                return state;
            }
            if (["FAILED", "DELETED", "DELETE_PENDING", "DELETE_AFTER_PROMOTION"]
                .includes(state.status)) {
                throw new Error("Image processing failed");
            }
            await new Promise(resolve => window.setTimeout(resolve, 500));
        }
        throw new Error("Image processing timed out");
    }

    async function requestJson(url, options = {}) {
        const response = await csrfFetch(url, options);
        const body = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(body.error || `Request failed (${response.status})`);
        }
        return body;
    }


    // showToast logic

    function showToast(message, isError = false) {
        const toast = document.getElementById("toast");

        toast.textContent = message;
        toast.classList.remove("error");

        if (isError) {
            toast.classList.add("error");
        }

        toast.classList.add("show");

        setTimeout(() => {
            toast.classList.remove("show");
        }, 3000);
    }


});

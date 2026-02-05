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

    // Apply button 
    applyBtn.addEventListener("click", async () => {
        const username = document.getElementById("usernameInput").value;
        const password = document.getElementById("passwordInput").value;
        const avatarFile = document.getElementById("avatarInput").files[0];

        const formData = new FormData();
        if (username) formData.append("username", username);
        if (password) formData.append("password", password);
        if (avatarFile) formData.append("file", avatarFile);

        try {
            const response = await fetch("/profile/get", {
                method: "POST",
                body: formData
            });
            const data = await response.json();

            if (data.success) {
                showToast(data.message);
            } else {
                showToast(data.message, true);
            }
        } catch (err) {
            console.error(err);
            showToast("Error while saving changes", true);
        }
    });


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

document.addEventListener("DOMContentLoaded", async () => {
    await window.CampusSyncSession?.ready;
    if (window.CampusSyncSession?.error) return;
    const user = window.CampusSyncLocal.getCurrentUser();
    const language = document.getElementById("language");
    const syncButton = document.querySelector(".sync-btn");
    const syncStatusText = document.getElementById("syncStatusText");
    const preferenceInputs = Array.from(document.querySelectorAll("[data-preference]"));
    const darkMode = document.getElementById("darkMode");

    function collectSettings() {
        return {
            theme: darkMode?.checked ? "dark" : "light",
            language: language?.value || "english",
            assignmentAlerts: document.getElementById("assignmentAlerts")?.checked ?? true,
            classReminders: document.getElementById("classReminders")?.checked ?? true,
            announcements: document.getElementById("announcements")?.checked ?? true,
            offlineMode: document.getElementById("offlineMode")?.checked ?? true
        };
    }

    function applySettings(settings) {
        if (settings.theme) {
            const isDark = settings.theme === "dark";
            if (darkMode) darkMode.checked = isDark;
            document.documentElement.classList.toggle("dark", isDark);
            document.body.classList.toggle("dark", isDark);
            localStorage.setItem("theme", settings.theme);
            localStorage.setItem("campusSyncDarkMode", String(isDark));
        }
        if (language && settings.language) {
            language.value = settings.language;
            localStorage.setItem("campusSyncLanguage", settings.language);
        }
        preferenceInputs.forEach(input => {
            const value = settings[input.dataset.preference];
            if (typeof value === "boolean") input.checked = value;
        });
    }

    try {
        const settings = user
            ? window.CampusSyncLocal.getSettings(user.id)
            : {};
        applySettings({
            theme: localStorage.getItem("theme") ||
                (localStorage.getItem("campusSyncDarkMode") === "true" ? "dark" : "light"),
            language: localStorage.getItem("campusSyncLanguage") || "english",
            ...settings
        });
        if (syncStatusText) syncStatusText.textContent = "Preferences are saved in this browser.";
    } catch (error) {
        if (syncStatusText) syncStatusText.textContent = error.message;
    }

    function persistCurrentSettings() {
        const settings = collectSettings();
        if (user) {
            try {
                window.CampusSyncLocal.saveSettings(user.id, settings);
            } catch (error) {
                if (syncStatusText) syncStatusText.textContent = error.message;
                return;
            }
        }
        applySettings(settings);
        if (syncStatusText) syncStatusText.textContent = "Preferences saved in this browser.";
    }

    preferenceInputs.forEach(input => input.addEventListener("change", persistCurrentSettings));
    language?.addEventListener("change", () => {
        persistCurrentSettings();
        if (language.value === "zulu") {
            alert("isiZulu preference saved. Full page translation is not available yet.");
        }
    });
    darkMode?.addEventListener("change", persistCurrentSettings);

    if (syncButton) {
        syncButton.removeAttribute("onclick");
        syncButton.addEventListener("click", () => {
            persistCurrentSettings();
            syncButton.textContent = "Saved ✓";
            window.setTimeout(() => {
                syncButton.textContent = "Save Preferences";
            }, 1600);
        });
    }
});

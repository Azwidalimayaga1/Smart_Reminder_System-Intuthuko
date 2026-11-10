(function () {
    function applyTheme(theme) {
        const isDark = theme === "dark";
        document.documentElement.classList.toggle("dark", isDark);
        document.body?.classList.toggle("dark", isDark);
        localStorage.setItem("theme", isDark ? "dark" : "light");
        localStorage.setItem("campusSyncDarkMode", String(isDark));
        const toggle = document.getElementById("darkMode");
        if (toggle) toggle.checked = isDark;
    }

    const cachedTheme = localStorage.getItem("theme") ||
        (localStorage.getItem("campusSyncDarkMode") === "true" ? "dark" : "light");
    applyTheme(cachedTheme);

    document.addEventListener("DOMContentLoaded", () => {
        const toggle = document.getElementById("darkMode");
        if (toggle) {
            toggle.addEventListener("change", () => applyTheme(toggle.checked ? "dark" : "light"));
        }
    });
})();

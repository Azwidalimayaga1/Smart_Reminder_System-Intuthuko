(function () {
    document.addEventListener("DOMContentLoaded", async () => {
        const app = document.querySelector("[data-role-page]");
        if (!app) return;

        await window.CampusSyncSession?.ready;
        const allowedRoles = (app.dataset.rolePage || "").split(",").filter(Boolean);
        const user = window.CampusSyncLocal.getCurrentUser();
        if (!user || !allowedRoles.includes(user.role) || (app.hasAttribute("data-cloud-only") && !user.cloud)) {
            const role = allowedRoles[0] || "admin";
            window.location.replace(user?.cloud ? window.CampusSyncLocal.dashboards[user.role] : window.CampusSyncLocal.loginPages[role]);
            return;
        }

        document.body.classList.add("role-verified");
        document.dispatchEvent(new CustomEvent("campussync:role-ready", { detail: user }));
    });
})();

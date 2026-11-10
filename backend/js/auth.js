document.addEventListener("DOMContentLoaded", () => {
    const user = window.CampusSyncLocal.getCurrentUser();
    const actions = document.querySelectorAll("[data-auth-actions]");

    actions.forEach(container => {
        if (!user) {
            container.innerHTML =
                '<a href="login.html" class="btn btn-outline">Login</a>' +
                '<a href="register.html" class="btn btn-primary">Get Started</a>';
            return;
        }

        const dashboardLink = document.createElement("a");
        dashboardLink.href = window.CampusSyncLocal.dashboards[user.role];
        dashboardLink.className = "user-chip nav-account-link";
        dashboardLink.textContent = user.fullName;
        const logoutButton = document.createElement("button");
        logoutButton.type = "button";
        logoutButton.className = "btn btn-outline logout-btn";
        logoutButton.textContent = "Log out";
        logoutButton.addEventListener("click", () => {
            window.CampusSyncLocal.removeCurrentUser();
            window.location.assign("index.html");
        });
        const accountLinks = [dashboardLink];
        if (user.role === "student") {
            const notificationsLink = document.createElement("a");
            notificationsLink.href = "notifications.html";
            notificationsLink.className = "user-chip nav-account-link";
            notificationsLink.textContent = "Notifications";
            accountLinks.push(notificationsLink);
        }
        accountLinks.push(logoutButton);
        container.replaceChildren(...accountLinks);
    });

    const scheduleAction = document.getElementById("scheduleAction");
    if (scheduleAction && user?.role === "student") {
        scheduleAction.textContent = "Manage your schedule";
        scheduleAction.href = "schedule.html";
    }
});

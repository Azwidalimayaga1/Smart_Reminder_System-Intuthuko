document.addEventListener("DOMContentLoaded", async () => {
    await window.CampusSyncSession?.ready;
    if (window.CampusSyncSession?.error) return;
    const iconByType = {
        task: "fa-clipboard-list",
        exam: "fa-graduation-cap",
        note: "fa-sticky-note",
        system: "fa-circle-info"
    };
    const user = window.CampusSyncLocal.getCurrentUser();
    const list = document.getElementById("notifList");
    const empty = document.getElementById("notifEmpty");
    let notifications = [];
    let activeFilter = "all";

    function render() {
        if (!list || !empty) {
            console.error("Notification list elements were not found.");
            return;
        }
        list.replaceChildren();
        const visible = notifications.filter(notification => {
            if (activeFilter === "all") return true;
            if (activeFilter === "unread") return !notification.read;
            return notification.type === activeFilter;
        });
        empty.textContent = visible.length ? "" : "No notifications to show.";
        empty.style.display = visible.length ? "none" : "block";

        visible.forEach(notification => {
            const card = document.createElement("div");
            card.className = `notif-card${notification.read ? "" : " unread"}`;
            card.dataset.id = notification.id;
            const icon = document.createElement("div");
            icon.className = `notif-icon type-${notification.type}`;
            const iconElement = document.createElement("i");
            iconElement.className = `fas ${iconByType[notification.type] || "fa-bell"}`;
            icon.appendChild(iconElement);

            const body = document.createElement("div");
            body.className = "notif-body";
            const topRow = document.createElement("div");
            topRow.className = "notif-top-row";
            const title = document.createElement("div");
            title.className = "notif-title";
            title.textContent = notification.title || "CampusSync Notification";
            const time = document.createElement("div");
            time.className = "notif-time";
            const date = new Date(notification.created_at);
            time.textContent = Number.isNaN(date.getTime())
                ? ""
                : date.toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
            topRow.append(title, time);
            const message = document.createElement("div");
            message.className = "notif-message";
            message.textContent = notification.message || "";
            body.append(topRow, message);
            card.append(icon, body);

            if (!notification.read) {
                const dot = document.createElement("div");
                dot.className = "notif-dot";
                dot.title = "Unread";
                card.appendChild(dot);
            }
            card.addEventListener("click", () => {
                if (!notification.read && user?.role === "student") {
                    window.CampusSyncLocal.markNotificationRead(user.id, notification.id);
                    notification.read = true;
                    render();
                }
            });
            list.appendChild(card);
        });

        const unreadCount = notifications.filter(notification => !notification.read).length;
        const badge = document.getElementById("unreadCount");
        if (badge) {
            badge.textContent = unreadCount > 99 ? "99+" : unreadCount;
            badge.style.display = unreadCount ? "inline-flex" : "none";
        }
    }

    if (!user || user.role !== "student") {
        empty.textContent = "Sign in with a student account to view notifications.";
        empty.style.display = "block";
        return;
    }

    try {
        notifications = window.CampusSyncLocal.getNotifications(user.id);
    } catch (error) {
        empty.textContent = error.message;
        empty.style.display = "block";
        return;
    }
    render();

    document.getElementById("markAllBtn")?.addEventListener("click", () => {
        notifications.filter(notification => !notification.read).forEach(notification => {
            window.CampusSyncLocal.markNotificationRead(user.id, notification.id);
            notification.read = true;
        });
        render();
    });

    document.getElementById("clearAllBtn")?.addEventListener("click", () => {
        window.CampusSyncLocal.clearNotifications(user.id);
        notifications = [];
        render();
    });

    document.querySelectorAll(".notif-filter-btn").forEach(button => {
        button.addEventListener("click", () => {
            document.querySelectorAll(".notif-filter-btn").forEach(item => item.classList.remove("active"));
            button.classList.add("active");
            activeFilter = button.dataset.filter;
            render();
        });
    });
});

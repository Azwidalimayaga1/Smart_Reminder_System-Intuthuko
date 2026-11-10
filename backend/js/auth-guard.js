document.addEventListener("DOMContentLoaded", async () => {
    const session = window.CampusSyncSession;
    await session?.ready;
    if (session?.error) {
        document.documentElement.style.visibility = "visible";
        const main = document.createElement("main");
        main.className = "container";
        const message = document.createElement("p");
        message.textContent = "Could not restore your CampusSync session. Try signing in again.";
        const login = document.createElement("a");
        login.href = "login.html";
        login.textContent = "Return to sign in";
        main.append(message, login);
        document.body.replaceChildren(main);
        return;
    }
    const user = window.CampusSyncLocal.getCurrentUser();
    if (!user) {
        window.location.replace("login.html");
        return;
    }

    if (user.role !== "student") {
        window.location.replace(window.CampusSyncLocal.dashboards[user.role] || "login.html");
        return;
    }
    document.documentElement.style.visibility = "visible";
});

function logout() {
    const user = window.CampusSyncLocal.getCurrentUser();
    const loginPage = user
        ? window.CampusSyncLocal.loginPages[user.role]
        : "login.html";
    window.CampusSyncLocal.removeCurrentUser();
    window.location.replace(loginPage);
}

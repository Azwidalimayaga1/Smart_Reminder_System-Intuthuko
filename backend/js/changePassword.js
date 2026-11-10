document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("changePasswordForm");
    const message = document.getElementById("passwordMessage");
    const user = window.CampusSyncLocal.getCurrentUser();

    if (!user || !["admin", "super_admin", "tutor"].includes(user.role)) {
        window.location.replace(user
            ? window.CampusSyncLocal.loginPages[user.role]
            : "admin-login.html");
        return;
    }

    form.addEventListener("submit", event => {
        event.preventDefault();
        message.hidden = true;
        const currentPassword = document.getElementById("currentPassword").value;
        const newPassword = document.getElementById("newPassword").value;
        const confirmPassword = document.getElementById("confirmNewPassword").value;

        if (newPassword !== confirmPassword) {
            message.textContent = "The new passwords do not match.";
            message.hidden = false;
            return;
        }
        if (newPassword.length < 12) {
            message.textContent = "Choose a new password with at least 12 characters.";
            message.hidden = false;
            return;
        }

        try {
            window.CampusSyncLocal.changePassword(currentPassword, newPassword);
            message.textContent = "Password changed in this browser.";
            message.hidden = false;
            form.reset();
        } catch (error) {
            message.textContent = error.message;
            message.hidden = false;
        }
    });
});

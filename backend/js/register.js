document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("registerForm");
    const errorMessage = document.getElementById("errorMessage");
    const successMessage = document.getElementById("successMessage");

    errorMessage.style.display = "none";
    successMessage.style.display = "none";

    form.addEventListener("submit", async event => {
        event.preventDefault();
        errorMessage.style.display = "none";
        successMessage.style.display = "none";

        const fullName = document.getElementById("fullName").value.trim();
        const studentNumber = document.getElementById("studentNumber").value.trim();
        const email = document.getElementById("email").value.trim();
        const password = document.getElementById("password").value;
        const confirmPassword = document.getElementById("confirmPassword").value;

        if (password !== confirmPassword) {
            errorMessage.textContent = "Passwords do not match.";
            errorMessage.style.display = "block";
            return;
        }
        if (password.length < 8) {
            errorMessage.textContent = "Password must be at least 8 characters.";
            errorMessage.style.display = "block";
            return;
        }

        try {
            await window.CampusSyncSession.signOut();
            window.CampusSyncLocal.registerStudent({ fullName, studentNumber, email, password });
            successMessage.textContent = "Account created and saved in this browser.";
            successMessage.style.display = "block";
            window.setTimeout(() => window.location.assign("dashboard.html"), 700);
        } catch (error) {
            errorMessage.textContent = error.message || "Could not create the account.";
            errorMessage.style.display = "block";
        }
    });
});

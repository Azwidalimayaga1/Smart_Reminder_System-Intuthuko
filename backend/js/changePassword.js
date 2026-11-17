document.addEventListener('DOMContentLoaded', async () => {
    const session = window.CampusSyncSession;
    const firebase = await session.ready;
    const user = window.CampusSyncLocal.getCurrentUser();
    if (!user?.cloud || !firebase) { location.replace('login.html'); return; }
    const form = document.getElementById('changePasswordForm');
    const message = document.getElementById('passwordMessage');
    const button = form.querySelector('button[type=submit]');
    const account = await firebase.currentUser();
    let hasPassword = account.providerData.some(provider => provider.providerId === 'password');
    const currentPassword = document.getElementById('currentPassword');
    const resetButton = document.getElementById('emailPasswordReset');
    if (!hasPassword) {
        currentPassword.required = false;
        currentPassword.closest('.form-group').hidden = true;
        currentPassword.closest('.form-group').style.display = 'none';
        document.getElementById('passwordInstructions').textContent = 'Add a CampusSync password to your existing Google account. Confirm the same Google account when prompted. This keeps your tasks and role and does not change your Google password.';
        button.textContent = 'Confirm Google account and add password';
    }
    resetButton.addEventListener('click', async () => {
        if (resetButton.disabled || button.disabled) return;
        resetButton.disabled = true;
        button.disabled = true;
        message.hidden = false;
        try {
            await firebase.resetPassword(account.email);
            message.textContent = 'If this email has an account, a password-reset link will arrive shortly. Check your inbox and spam folder. Open the link to choose a strong new password, then sign in with it on the website or app.';
            setTimeout(() => { resetButton.disabled = false; }, 60000);
        } catch (error) {
            message.textContent = firebase.friendlyError(error);
            resetButton.disabled = false;
        } finally { button.disabled = false; }
    });
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (button.disabled) return;
        button.disabled = true;
        message.hidden = false;
        try {
            const password = document.getElementById('newPassword').value;
            if (password !== document.getElementById('confirmNewPassword').value) throw new Error('The new passwords do not match.');
            await firebase.changeAccountPassword(document.getElementById('currentPassword').value, password);
            form.reset();
            message.textContent = 'Your CampusSync password is updated. Use it with ' + account.email + ' on both the website and app. Your tasks and account role are unchanged.';
            if (!hasPassword) {
                hasPassword = true;
                currentPassword.closest('.form-group').hidden = false;
                currentPassword.closest('.form-group').style.display = '';
                currentPassword.required = true;
                button.textContent = 'Update password';
                document.getElementById('passwordInstructions').textContent = 'Enter your current CampusSync password to choose a new one.';
            }
        } catch (error) { message.textContent = firebase.friendlyError(error); }
        finally { button.disabled = false; }
    });
});

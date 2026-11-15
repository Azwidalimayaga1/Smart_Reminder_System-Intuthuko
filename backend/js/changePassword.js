document.addEventListener('DOMContentLoaded', async () => {
    const session = window.CampusSyncSession;
    const firebase = await session.ready;
    const user = window.CampusSyncLocal.getCurrentUser();
    if (!user?.cloud || !firebase) { location.replace('login.html'); return; }
    const form = document.getElementById('changePasswordForm');
    const message = document.getElementById('passwordMessage');
    const button = form.querySelector('button[type=submit]');
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
            message.textContent = 'Password changed for your CampusSync account.';
        } catch (error) { message.textContent = firebase.friendlyError(error); }
        finally { button.disabled = false; }
    });
});

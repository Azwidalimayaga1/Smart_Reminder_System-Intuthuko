document.addEventListener('DOMContentLoaded', () => {
    const staffForm = document.getElementById('staffLoginForm');
    const form = staffForm || document.getElementById('loginForm');
    const errorMessage = document.getElementById(staffForm ? 'staffLoginMessage' : 'errorMessage');
    const demoButton = document.getElementById('staffDemoSignIn');
    const local = window.CampusSyncLocal;
    const demoRoles = staffForm ? form.dataset.allowedRoles.split(',') : ['student'];
    const redirect = account => location.assign(local.dashboards[account.role]);
    const googleButton = document.getElementById('googleSignIn');
    const session = window.CampusSyncSession;
    const emailButton = form.querySelector('[type=submit]');
    const resetButton = document.getElementById('forgotPassword');
    const resetMessage = document.getElementById('passwordResetMessage');
    let resetAvailableAt = 0;
    let busy = false;

    function setBusy(value) {
        busy = value;
        emailButton.disabled = value;
        googleButton.disabled = value;
        resetButton.disabled = value || Date.now() < resetAvailableAt;
        if (demoButton) demoButton.disabled = value;
    }
    let firebase;
    session.loadFirebase().then(async client => {
        firebase = client;
        const pending = await client.currentUser();
        if (pending && !pending.emailVerified) window.CampusSyncVerification.show(client);
    }).catch(() => {});

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.style.display = 'block';
    }

    resetButton.addEventListener('click', async () => {
        if (busy || Date.now() < resetAvailableAt) return;
        const emailInput = document.getElementById(staffForm ? 'staffEmail' : 'email');
        if (!emailInput.reportValidity()) return;
        setBusy(true);
        errorMessage.style.display = 'none';
        resetMessage.textContent = '';
        try {
            firebase = await session.loadFirebase();
            await firebase.resetPassword(emailInput.value);
            resetMessage.textContent = 'If this email has an account, a password-reset link will arrive shortly. Check your inbox and spam folder. If you use Google, you can also choose Continue with Google.';
            resetAvailableAt = Date.now() + 60000;
            resetButton.textContent = 'Reset link requested';
            setTimeout(() => {
                resetButton.textContent = 'Forgot password?';
                resetButton.disabled = busy;
            }, 60000);
        } catch (error) {
            showError(firebase ? firebase.friendlyError(error) : 'Could not load password reset. Check your internet connection and try again.');
        } finally { setBusy(false); }
    });

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (busy) return;
        setBusy(true);
        errorMessage.style.display = 'none';
        const email = document.getElementById(staffForm ? 'staffEmail' : 'email').value.trim();
        const password = document.getElementById(staffForm ? 'staffPassword' : 'password').value;
        const useDemo = form.dataset.cloudOnly !== 'true' &&
            (event.submitter === demoButton && Boolean(demoButton) || email.toLowerCase().endsWith('@campussync.local'));
        try {
            if (useDemo) {
                await session.signOut();
                redirect(local.signIn(email, password, demoRoles));
            } else {
                firebase = await session.loadFirebase();
                redirect(await session.signInWithEmail(firebase, email, password));
            }
        } catch (error) {
            if (error.code === 'auth/email-not-verified') window.CampusSyncVerification.show(firebase);
            else showError(!useDemo && firebase ? firebase.friendlyError(error) : error.message);
        } finally { setBusy(false); }
    });

    googleButton.addEventListener('click', async () => {
        if (busy) return;
        errorMessage.style.display = 'none';
        if (!firebase) {
            showError('Google sign-in is loading. Try again in a moment.');
            try {
                firebase = await session.loadFirebase();
                errorMessage.style.display = 'none';
            } catch {
                showError('Could not load Google sign-in. Check your internet connection and try again.');
            }
            return;
        }
        setBusy(true);
        try {
            redirect(await session.signInWithGoogle(firebase));
        } catch (error) {
            if (error.code === 'auth/email-not-verified') window.CampusSyncVerification.show(firebase);
            else showError(firebase.friendlyError(error));
        } finally {
            setBusy(false);
        }
    });
});

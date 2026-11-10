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
    let busy = false;

    function setBusy(value) {
        busy = value;
        emailButton.disabled = value;
        googleButton.disabled = value;
        if (demoButton) demoButton.disabled = value;
    }
    let firebase;
    session.loadFirebase().then(client => { firebase = client; }).catch(() => {});

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.style.display = 'block';
    }

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
            showError(!useDemo && firebase ? firebase.friendlyError(error) : error.message);
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
            showError(firebase.friendlyError(error));
        } finally {
            setBusy(false);
        }
    });
});

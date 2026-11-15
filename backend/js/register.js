document.addEventListener('DOMContentLoaded', async () => {
    const form = document.getElementById('registerForm');
    const error = document.getElementById('errorMessage');
    const password = document.getElementById('password');
    const strength = document.getElementById('passwordStrength');
    const submit = form.querySelector('button[type=submit]');
    const {passwordChecks, requireStrongPassword} = await import('./password-policy.mjs');
    const updateStrength = () => {
        const checks = passwordChecks(password.value);
        strength.textContent = checks.map(([label, valid]) => (valid ? '✓ ' : '○ ') + label).join(' · ');
    };
    password.addEventListener('input', updateStrength);
    updateStrength();
    let busy = false;
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (busy) return;
        error.style.display = 'none';
        busy = true;
        submit.disabled = true;
        let firebase;
        try {
            requireStrongPassword(password.value);
            if (password.value !== document.getElementById('confirmPassword').value) throw new Error('Passwords do not match.');
            await window.CampusSyncSession.signOut();
            firebase = await window.CampusSyncSession.loadFirebase();
            await firebase.registerAccount(document.getElementById('fullName').value, document.getElementById('email').value,
                password.value, document.getElementById('studentNumber').value);
            password.value = '';
            document.getElementById('confirmPassword').value = '';
            window.CampusSyncVerification.show(firebase, 'Verification email sent. Check your inbox and spam folder.');
        } catch (failure) {
            const pending = firebase && await firebase.currentUser();
            if (pending && !pending.emailVerified) {
                password.value = '';
                document.getElementById('confirmPassword').value = '';
                window.CampusSyncVerification.show(firebase, 'Your account was created. Use Resend verification email to request a new link.');
            } else {
                error.textContent = firebase ? firebase.friendlyError(failure) : failure.message;
                error.style.display = 'block';
            }
        } finally { busy = false; submit.disabled = false; }
    });
});

(() => {
    const panel = document.getElementById('emailVerification');
    const message = document.getElementById('verificationMessage');
    const check = document.getElementById('checkVerification');
    const resend = document.getElementById('resendVerification');
    const signout = document.getElementById('verificationSignOut');
    let client;
    let busy = false;
    let resendAfter = 0;
    let timer;

    function setBusy(value) {
        busy = value;
        check.disabled = value;
        resend.disabled = value || Date.now() < resendAfter;
        signout.disabled = value;
    }
    window.CampusSyncVerification = {
        show(firebase, text = 'Check your inbox and spam folder for the verification link.') {
            client = firebase;
            panel.hidden = false;
            document.querySelector('main form').hidden = true;
            const google = document.getElementById('googleSignIn');
            if (google) google.hidden = true;
            message.textContent = text;
        }
    };
    check.addEventListener('click', async () => {
        if (busy) return;
        setBusy(true);
        try {
            const account = await window.CampusSyncSession.completeVerification(client);
            location.assign(window.CampusSyncLocal.dashboards[account.role]);
        } catch (error) { message.textContent = client.friendlyError(error); }
        finally { setBusy(false); }
    });
    resend.addEventListener('click', async () => {
        if (busy || Date.now() < resendAfter) return;
        resendAfter = Date.now() + 60000;
        clearTimeout(timer);
        timer = setTimeout(() => { if (!busy) resend.disabled = false; }, 60000);
        setBusy(true);
        try {
            await client.resendVerification();
            message.textContent = 'Verification email sent. Check your inbox and spam folder.';
        } catch (error) { message.textContent = client.friendlyError(error); }
        finally { setBusy(false); }
    });
    signout.addEventListener('click', async () => {
        setBusy(true);
        try {
            await window.CampusSyncSession.signOut();
            location.replace('login.html');
        } catch (error) { message.textContent = client.friendlyError(error); setBusy(false); }
    });
})();

(() => {
    const marker = 'campusSyncFirebaseSession';
    let client;
    let cloudUser = null;
    let restoreError = null;

    function loadFirebase() {
        client ??= import('./firebase-client.mjs').catch(error => {
            client = null;
            throw error;
        });
        return client;
    }

    async function restore() {
        if (localStorage.getItem(marker) !== 'active') return null;
        const firebase = await loadFirebase();
        const account = await firebase.currentUser();
        if (!account) {
            localStorage.removeItem(marker);
            return null;
        }
        cloudUser = await firebase.sessionAccount();
        return firebase;
    }

    const ready = restore().catch(error => {
        if (error.code === 'auth/email-not-verified') {
            localStorage.removeItem(marker);
            if (!/\/(login|admin-login|tutor-login|super-admin-login|register)\.html$/.test(location.pathname)) {
                location.replace('login.html');
            }
        }
        restoreError = error;
        console.error('Could not restore the Firebase session.', error);
        return null;
    });

    function activateCloudSession(account) {
        window.CampusSyncLocal.removeCurrentUser();
        localStorage.setItem(marker, 'active');
        cloudUser = account;
        restoreError = null;
        return account;
    }

    window.CampusSyncSession = {
        ready,
        get user() { return cloudUser; },
        get error() { return restoreError; },
        loadFirebase,
        async signInWithEmail(firebase, email, password) {
            return activateCloudSession(await firebase.emailLogin(email, password));
        },
        async completeVerification(firebase) {
            return activateCloudSession(await firebase.finishVerification());
        },
        async signInWithGoogle(firebase) {
            return activateCloudSession(await firebase.googleLogin());
        },
        async signOut() {
            await ready;
            await (await loadFirebase()).logout();
            localStorage.removeItem(marker);
            cloudUser = null;
            restoreError = null;
        }
    };
})();

(() => {
    const el = id => document.getElementById(id);
    const form = el('grantAdminForm');
    const button = form.querySelector('button');
    const status = el('superAdminMessage');
    const session = window.CampusSyncSession;

    async function loadDashboard(firebase) {
        const {admins, tutorCount} = await firebase.loadStaffRoles();
        const superAdmins = admins.filter(account => account.superAdmin === true);
        el('totalAdmins').textContent = admins.length - superAdmins.length;
        el('totalSuperAdmins').textContent = superAdmins.length;
        el('totalTutors').textContent = tutorCount;
        const table = el('adminAccounts');
        table.replaceChildren();
        for (const account of admins) {
            const row = table.insertRow();
            for (const value of [account.name || account.uid, account.email || '—',
                account.superAdmin ? 'Super Admin' : 'Admin', account.disabled ? 'Disabled' : 'Active']) {
                row.insertCell().textContent = value;
            }
        }
    }

    function displayError(error) {
        status.textContent = error.code === 'permission-denied'
            ? 'Firebase denied the request. Verify that the Super Admin rules are published and the target account has signed in.'
            : error.message;
    }

    document.addEventListener('campussync:super-admin-ready', async () => {
        const firebase = await session.ready;
        try {
            await loadDashboard(firebase);
            button.disabled = false;
        } catch (error) {
            el('adminAccounts').rows[0].cells[0].textContent = 'Could not load administrator accounts.';
            displayError(error);
        }
        form.addEventListener('submit', async event => {
            event.preventDefault();
            if (button.disabled) return;
            button.disabled = true;
            status.textContent = '';
            try {
                await firebase.grantAdminAccess(el('adminUid').value.trim());
                form.reset();
                status.textContent = 'Admin access granted.';
                document.getElementById("refreshBtn").click();
                try { await loadDashboard(firebase); }
                catch { status.textContent = 'Admin access granted. Refresh to reload the staff list.'; }
            } catch (error) { displayError(error); }
            finally { button.disabled = false; }
        });
    });

})();

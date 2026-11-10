(() => {
    const store = window.CampusSyncLocal;
    const el = id => document.getElementById(id);
    const set = (id, value) => { el(id).textContent = value; };
    const charts = {};
    const superAdmin = document.body.hasAttribute('data-super-admin');
    let firebase;
    let currentStudents = [];
    const dateLabel = value => value ? new Date(value).toLocaleDateString() : '—';
    const completed = task => task.status === 'completed';
    const dateKey = date => [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-');

    function row(values) {
        const tr = document.createElement('tr');
        for (const value of values) {
            const td = document.createElement('td');
            td.append(value instanceof Node ? value : String(value ?? '—'));
            tr.append(td);
        }
        return tr;
    }

    function emptyTable(body, columns, text) {
        const tr = row([text]);
        tr.firstElementChild.colSpan = columns;
        body.replaceChildren(tr);
    }

    function renderStudents(students) {
        const search = el('studentSearch').value.trim().toLowerCase();
        const results = students.filter(student => [student.fullName, student.email, student.studentNumber]
            .some(value => String(value ?? '').toLowerCase().includes(search)));
        set('studentCount', `${results.length} student${results.length === 1 ? '' : 's'}`);
        const body = el('studentsTableBody');
        if (!results.length) { emptyTable(body, 6, 'No students found.'); return; }
        body.replaceChildren(...results.map(student => {
            const suspended = student.accountStatus === 'suspended';
            const status = document.createElement('span');
            status.className = 'account-status ' + (suspended ? 'suspended' : 'active');
            status.textContent = suspended ? 'Suspended' : 'Active';
            const action = document.createElement('button');
            action.type = 'button';
            action.className = 'account-action-btn';
            action.textContent = suspended ? 'Reactivate' : 'Suspend';
            action.addEventListener('click', async () => {
                if (!suspended && !window.confirm(`Suspend ${student.fullName}'s account?`)) return;
                action.disabled = true;
                try {
                    const status = suspended ? 'active' : 'suspended';
                    await firebase.setFirebaseStudentStatus(student.id, status);
                    student.accountStatus = status;
                    renderStudents(students);
                    await refreshDashboard();
                } catch (error) { showError(error); }
                finally { action.disabled = false; }
            });
            return row([student.fullName, student.studentNumber || '—', student.email, dateLabel(student.createdAt), status, action]);
        }));
    }

    function chart(id, type, labels, data, label, colors) {
        if (typeof Chart === 'undefined') return;
        charts[id]?.destroy();
        charts[id] = new Chart(el(id), {
            type,
            data: {labels, datasets: [{label, data, backgroundColor: colors, borderColor: '#0055cc'}]},
            options: {responsive: true, maintainAspectRatio: false}
        });
    }

    function renderTrends(students, tasks) {
        const today = new Date();
        const months = Array.from({length: 6}, (_, i) => new Date(today.getFullYear(), today.getMonth() - 5 + i, 1));
        const labels = months.map(month => month.toLocaleDateString(undefined, {month:'short', year:'2-digit'}));
        const counts = records => months.map(month => records.filter(record => record.createdAt?.startsWith(dateKey(month).slice(0, 7))).length);
        chart('registrationsChart', 'bar', labels, counts(students), 'Registrations', '#0055cc');
        chart('tasksChart', 'bar', labels, counts(tasks), 'Tasks created', '#467fbb');
        chart('taskStatusChart', 'doughnut', ['Completed', 'Open'],
            [tasks.filter(completed).length, tasks.filter(task => !completed(task)).length], 'Tasks', ['#0055cc','#cbd5e1']);
    }

    function showError(error) {
        set('dashboardError', error.message);
        el('dashboardError').classList.add('show');
    }

    function renderOptions(select, accounts, placeholder) {
        const previous = select.value;
        select.replaceChildren(new Option(placeholder, ''), ...accounts.map(account => new Option(account.fullName, account.id)));
        if (accounts.some(account => account.id === previous)) select.value = previous;
    }

    async function loadDashboard() {
        el('dashboardError').classList.remove('show');
        const data = await firebase.loadStaffOverview();
        const {students, tutors, tasks, reminders, notifications, completedTasks} = data;
        currentStudents = students;
        const names = new Map(students.map(student => [student.id, student.fullName]));
        const studentTasks = tasks.filter(task => names.has(task.userId));
        const now = new Date();
        const upcomingReminders = reminders.filter(item => new Date(item.remindAt) > now).length;
        const counts = {
            totalStudents: students.length, totalTasks: tasks.length, completedTasks,
            overdueTasks: tasks.filter(task => !completed(task) && task.dueDate && task.dueDate.slice(0, 10) < dateKey(now)).length,
            sentReminders: reminders.length, totalNotifications: notifications.length,
            reminderSentDetail: reminders.length, reminderPendingDetail: upcomingReminders,
            reminderCancelledDetail: reminders.length - upcomingReminders,
            unreadNotifications: notifications.filter(item => !item.read).length,
            activeStudents: students.filter(student => student.accountStatus !== 'suspended').length,
            activityTasksCreated: studentTasks.length,
            activityRemindersCreated: reminders.filter(item => names.has(item.userId)).length,
            activityNotifications: notifications.filter(item => names.has(item.userId)).length,
            apiStatus: 'Signed in',
            dbStatus: 'Connected',
            healthChecked: 'Checked ' + now.toLocaleTimeString(), lastUpdated: 'Updated ' + now.toLocaleTimeString()
        };
        Object.entries(counts).forEach(([id, value]) => set(id, value));
        el('apiDot').classList.add('ok');
        el('dbDot').classList.remove('bad');
        el('dbDot').classList.add('ok');
        const activity = [
            ...students.map(student => ({name: student.fullName, type:'Registration', detail:'Student registered', time:student.createdAt})),
            ...studentTasks.map(task => ({name:names.get(task.userId), type:'Task', detail:task.title, time:task.createdAt}))
        ].sort((a,b) => (b.time || '').localeCompare(a.time || '')).slice(0, 12);
        const activityTable = el('activityTableBody');
        if (activity.length) activityTable.replaceChildren(...activity.map(item => row([item.name, item.type, item.detail, dateLabel(item.time)])));
        else emptyTable(activityTable, 4, 'No student activity recorded yet.');
        renderStudents(students);
        renderTrends(students, tasks);
        if (superAdmin) return;
        renderOptions(el('assignmentTutor'), tutors, 'Select a tutor');
        renderOptions(el('assignmentStudent'), students, 'Select a student');
        el('tutorList').replaceChildren(...(tutors.length ? tutors.map(tutor => {
            const item = document.createElement('p');
            item.textContent = `${tutor.fullName} · ${tutor.studentCount} students`;
            return item;
        }) : ['No tutors added yet.']));
    }

    async function refreshDashboard() {
        const button = el('refreshBtn');
        if (button.disabled) return;
        button.disabled = true;
        try { await loadDashboard(); }
        catch (error) {
            set('dbStatus', 'Could not load data');
            el('dbDot').classList.remove('ok');
            el('dbDot').classList.add('bad');
            showError(error.code === 'permission-denied'
                ? new Error('Publish the updated Firestore rules to enable staff analytics.')
                : error);
        } finally { button.disabled = false; }
    }

    document.addEventListener('DOMContentLoaded', async () => {
        firebase = await window.CampusSyncSession.ready;
        const user = store.getCurrentUser();
        const expectedRole = superAdmin ? 'super_admin' : 'admin';
        if (user?.role !== expectedRole || !user.cloud) {
            location.replace(user?.cloud ? store.dashboards[user.role] : store.loginPages[expectedRole]);
            return;
        }
        set('adminName', user.fullName);
        document.documentElement.classList.remove('auth-pending');
        el('logoutBtn').addEventListener('click', async () => {
            try {
                await window.CampusSyncSession.signOut();
                store.removeCurrentUser();
                location.replace(store.loginPages[expectedRole]);
            } catch (error) { showError(error); }
        });
        el('refreshBtn').addEventListener('click', refreshDashboard);
        el('studentSearchBtn').addEventListener('click', () => renderStudents(currentStudents));
        el('studentSearch').addEventListener('input', () => renderStudents(currentStudents));
        if (superAdmin) {
            document.dispatchEvent(new CustomEvent('campussync:super-admin-ready'));
            refreshDashboard();
            return;
        }
        el('createTutorForm').addEventListener('submit', async event => {
            event.preventDefault();
            const form = event.currentTarget;
            const button = form.querySelector('button');
            button.disabled = true;
            try {
                await firebase.grantTutorAccess(el('tutorUid').value.trim());
                form.reset();
                set('adminMessage', 'Tutor access granted.');
                await refreshDashboard();
            } catch (error) { set('adminMessage', error.message); }
            finally { button.disabled = false; }
        });
        el('assignStudentForm').addEventListener('submit', async event => {
            event.preventDefault();
            const button = event.currentTarget.querySelector('button');
            button.disabled = true;
            try {
                await firebase.assignFirebaseStudent(el('assignmentTutor').value, el('assignmentStudent').value);
                set('assignmentMessage', 'Student assigned to tutor.');
                await refreshDashboard();
            } catch (error) { set('assignmentMessage', error.message); }
            finally { button.disabled = false; }
        });
        refreshDashboard();
    });
})();


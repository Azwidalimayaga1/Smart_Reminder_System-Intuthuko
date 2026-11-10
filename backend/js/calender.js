document.addEventListener('DOMContentLoaded', async () => {
    const session = window.CampusSyncSession;
    const connect = document.getElementById('signinBtn');
    const disconnect = document.getElementById('signoutBtn');
    const status = document.getElementById('calendarStatus');
    const tasksList = document.getElementById('calendarAssignments');
    const eventsList = document.getElementById('eventsList');
    const firebase = await session.ready;
    const user = window.CampusSyncLocal.getCurrentUser();
    if (!user || user.role !== 'student') return;

    const renderTasks = tasks => {
        const entries = tasks.map(task => ({
            title: task.title,
            date: calendarDate(task.dueDate || task.due_date),
            status: task.status
        })).filter(task => task.date).sort((a, b) => a.date - b.date);
        renderList(tasksList, entries, 'No assignments with due dates yet.', entry =>
            [entry.date.toLocaleDateString(undefined, { dateStyle: 'medium' }), entry.status].filter(Boolean).join(' · '));
    };
    const taskError = () => showMessage(tasksList, 'Could not load assignments. Refresh the page to try again.');
    if (firebase) await firebase.watchItems('tasks', renderTasks, taskError).catch(taskError);
    else renderTasks(window.CampusSyncLocal.getTasks(user.id));

    const meetings = document.getElementById('campusMeetings');
    if (firebase) {
        const meetingError = () => showMessage(meetings, 'Could not load CampusSync meetings.');
        await firebase.watchItems('calendarEvents', items => {
            const entries = items.filter(event => event.eventType === 'Meeting').map(event => ({
                title: event.title, description: event.description,
                date: calendarDate(event.eventDate + 'T' + (event.eventTime || '12:00:00')),
                timed: Boolean(event.eventTime)
            })).filter(event => event.date).sort((a,b) => a.date - b.date);
            renderList(meetings, entries, 'No CampusSync meetings scheduled.', entry => [
                entry.timed ? entry.date.toLocaleString(undefined, {dateStyle:'medium', timeStyle:'short'})
                    : entry.date.toLocaleDateString(undefined, {dateStyle:'medium'}),
                entry.description
            ].filter(Boolean).join(' · '));
        }, meetingError).catch(meetingError);
    } else showMessage(meetings, 'Sign in to view CampusSync meetings.');

    if (!firebase) {
        status.textContent = 'Sign in to your CampusSync account to connect Google Calendar.';
        return;
    }
    connect.disabled = false;
    connect.addEventListener('click', async () => {
        connect.disabled = true;
        status.textContent = 'Connecting to Google Calendar…';
        try {
            const token = await firebase.googleCalendarAccess();
            const query = new URLSearchParams({
                timeMin: new Date().toISOString(), maxResults: '15', singleEvents: 'true', orderBy: 'startTime'
            });
            const response = await fetch('https://www.googleapis.com/calendar/v3/calendars/primary/events?' + query,
                { headers: { Authorization: `Bearer ${token}` } });
            if (response.status === 403) throw new Error('Google did not allow calendar access. Reconnect and approve the calendar permission.');
            if (!response.ok) throw new Error('Google Calendar could not load events. Try connecting again.');
            const {items = []} = await response.json();
            const entries = items.map(event => ({
                title: event.summary || 'Untitled event',
                date: calendarDate(event.start?.dateTime || event.start?.date),
                timed: Boolean(event.start?.dateTime)
            })).filter(event => event.date);
            renderList(eventsList, entries, 'No upcoming Google Calendar events.', entry => entry.timed
                ? entry.date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
                : entry.date.toLocaleDateString(undefined, { dateStyle: 'medium' }) + ' · All day');
            status.textContent = 'Google Calendar connected';
            connect.hidden = true;
            disconnect.hidden = false;
        } catch (error) {
            status.textContent = ['auth/user-mismatch', 'auth/user-not-found'].includes(error.code)
                ? 'Sign in to CampusSync with Continue with Google, then connect using the same Google account.'
                : firebase.friendlyError(error);
        } finally {
            connect.disabled = false;
        }
    });
    disconnect.addEventListener('click', () => {
        disconnect.hidden = true;
        connect.hidden = false;
        eventsList.replaceChildren();
        status.textContent = 'Connect Google Calendar to see your upcoming events.';
    });
});

function calendarDate(value) {
    if (!value) return null;
    const date = new Date(/^\d{4}-\d{2}-\d{2}$/.test(value) ? value + 'T12:00:00' : value);
    return Number.isNaN(date.getTime()) ? null : date;
}

function showMessage(container, message) {
    const paragraph = document.createElement('p');
    paragraph.className = 'empty-state';
    paragraph.textContent = message;
    container.replaceChildren(paragraph);
}

function renderList(container, entries, emptyMessage, detail) {
    if (!entries.length) { showMessage(container, emptyMessage); return; }
    container.replaceChildren(...entries.map(entry => {
        const row = document.createElement('div');
        row.className = 'task-item';
        const info = document.createElement('div');
        info.className = 'task-info';
        const title = document.createElement('h4');
        title.textContent = entry.title;
        const description = document.createElement('p');
        description.textContent = detail(entry);
        info.append(title, description);
        row.append(info);
        return row;
    }));
}


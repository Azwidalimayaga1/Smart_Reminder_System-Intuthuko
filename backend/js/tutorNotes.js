document.addEventListener('DOMContentLoaded', async () => {
    const firebase = await window.CampusSyncSession.ready;
    const user = window.CampusSyncLocal.getCurrentUser();
    if (!firebase || user?.role !== 'student') return;

    document.getElementById('tutorNotesSection').hidden = false;
    const list = document.getElementById('tutorNotesList');
    const status = document.getElementById('tutorNotesStatus');
    status.textContent = 'Loading notes…';
    let notes;
    let links;
    function render() {
        if (!notes || !links) return;
        const cards = notes.filter(note => links.has(note.id))
            .sort((a,b) => (b.updatedAt || '').localeCompare(a.updatedAt || ''))
            .map(note => {
                const card = document.createElement('article');
                card.className = 'task-item';
                const info = document.createElement('div');
                info.className = 'task-info';
                const title = document.createElement('h4');
                title.textContent = note.title;
                const body = document.createElement('p');
                body.style.whiteSpace = 'pre-wrap';
                body.textContent = note.body || '';
                info.append(title, body);
                const fileName = links.get(note.id);
                if (fileName) info.append(firebase.tutorPdfButton(note.id, fileName));
                card.append(info);
                return card;
            });
        list.replaceChildren(...cards);
        status.textContent = cards.length ? '' : 'Your tutor hasn’t posted any notes yet.';
        status.hidden = cards.length > 0;
    }
    const failure = () => {
        status.hidden = false;
        status.textContent = 'Could not load tutor notes. Refresh to try again.';
    };
    try {
        await firebase.watchItems('notes', records => { notes = records; render(); }, failure);
        await firebase.watchTutorLinks(records => { links = records; render(); }, failure);
    } catch { failure(); }
});

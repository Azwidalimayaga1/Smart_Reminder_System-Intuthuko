(() => {
    const store = window.CampusSyncLocal;
    const el = id => document.getElementById(id);
    const dateKey = date => [date.getFullYear(), String(date.getMonth()+1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-');

    function taskList(id, tasks, emptyMessage) {
        const items = tasks.map(task => {
            const item = document.createElement('div');
            item.className = 'role-list-item';
            const text = document.createElement('strong');
            text.textContent = task.title;
            const detail = document.createElement('small');
            detail.textContent = `${task.studentName} · Due ${task.dueDate || '—'}`;
            item.append(text, detail);
            return item;
        });
        el(id).replaceChildren(...(items.length ? items : [emptyMessage]));
    }

    const allLearners = "*";
    let firebase;
    let learners = new Map();

    async function load() {
        const overview = await firebase.loadTutorAssignments();
        const students = new Map(overview.students.map(student => [student.id, student.fullName]));
        learners = students;
        const tasks = overview.tasks
            .map(task => ({...task, studentName:students.get(task.userId)}))
            .sort((a,b) => (a.dueDate || '').localeCompare(b.dueDate || ''));
        const pending = tasks.filter(task => task.status !== 'completed');
        el('tutorStudentCount').textContent = students.size;
        el('tutorTaskCount').textContent = tasks.length;
        el('tutorPendingCount').textContent = pending.length;
        el('tutorCompletedCount').textContent = tasks.length - pending.length;
        for (const [pickerId, buttonId] of [
            ['taskStudent','assignTaskButton'], ['noteStudent','postNoteButton'], ['meetingStudent','scheduleMeetingButton']
        ]) {
            const picker = el(pickerId);
            const previous = picker.value;
            picker.replaceChildren(new Option(students.size ? 'Select a learner' : 'Ask an admin to assign a learner', ''));
            if (students.size) picker.add(new Option(`All assigned learners (${students.size})`, allLearners));
            for (const [id, name] of students) picker.add(new Option(name, id));
            if (students.has(previous) || (previous === allLearners && students.size)) picker.value = previous;
            el(buttonId).disabled = !students.size || el(buttonId).form.dataset.sending === 'true' || el(buttonId).form.dataset.retryPending === 'true';
        }
        const posts = (overview.posts || []).map(post => {
            const row = document.createElement('div');
            row.className = 'role-list-item';
            const title = document.createElement('strong');
            title.textContent = post.title;
            const detail = document.createElement('small');
            const type = post.kind === 'notes' ? 'Note' : [post.eventDate, post.eventTime].filter(Boolean).join(' ');
            detail.textContent = [students.get(post.userId), type, post.fileName].filter(Boolean).join(' · ');
            row.append(title, detail);
            return row;
        });
        el('tutorPosts').replaceChildren(...(posts.length ? posts : ['No notes or meetings posted yet.']));
        const today = new Date();
        const end = new Date(today);
        end.setDate(end.getDate() + 7);
        taskList('tutorHighPriority', pending.filter(task => task.priority === 'high').slice(0,5), 'No high priority tasks.');
        taskList('tutorUpcoming', pending.filter(task => task.dueDate >= dateKey(today) && task.dueDate < dateKey(end)).slice(0,10), 'No deadlines in the next 7 days.');

        const table = el('learnerTasks');
        table.replaceChildren();
        if (!tasks.length) {
            const cell = table.insertRow().insertCell();
            cell.colSpan = 5;
            cell.textContent = 'No tasks assigned yet.';
            return;
        }
        for (const task of tasks) {
            const row = table.insertRow();
            for (const value of [[task.title, task.fileName].filter(Boolean).join(' · '), task.studentName, task.dueDate, task.priority]) row.insertCell().textContent = value;
            const status = document.createElement('select');
            status.setAttribute('aria-label', 'Status for ' + task.title);
            status.add(new Option('Pending', 'pending'));
            status.add(new Option('Completed', 'completed'));
            status.value = task.status === 'completed' ? 'completed' : 'pending';
            status.addEventListener('change', async () => {
                status.disabled = true;
                try {
                    await firebase.setTutorAssignmentStatus(task.userId, task.id, status.value);
                    await load();
                    el('tutorTaskMessage').textContent = 'Task status updated.';
                } catch (error) { el('tutorTaskMessage').textContent = error.message; }
                finally { status.disabled = false; }
            });
            row.insertCell().append(status);
        }
    }

    function bindForm(formId, pickerId, buttonId, messageId, success, prepare) {
        const form = el(formId);
        const button = el(buttonId);
        const message = el(messageId);
        const retry = document.createElement('button');
        retry.className = 'role-button';
        retry.type = 'button';
        retry.hidden = true;
        retry.textContent = 'Retry failed learners';
        message.after(retry);
        let failed = [];
        let send;
        let delivered = 0;
        let total = 0;

        async function deliver(recipients) {
            form.dataset.sending = 'true';
            button.disabled = true;
            retry.hidden = true;
            message.textContent = `Posting to ${recipients.length} learner${recipients.length === 1 ? '' : 's'}…`;
            failed = [];
            let firstError = '';
            for (const id of recipients) {
                try { await send(id); delivered++; }
                catch (error) { failed.push(id); firstError ||= error.message; }
            }
            if (failed.length) {
                message.textContent = `Posted to ${delivered} of ${total} learners. ${firstError || 'Some posts could not be saved.'}`;
                retry.hidden = false;
            } else {
                form.reset();
                message.textContent = total === 1 ? success : `${success} Sent to all ${total} learners.`;
            }
            if (delivered) {
                try { await load(); }
                catch { message.textContent += ' Refresh the page to reload the list.'; }
            }
            delete form.dataset.sending;
            if (failed.length) form.dataset.retryPending = 'true';
            else delete form.dataset.retryPending;
            button.disabled = !learners.size || Boolean(failed.length);
        }

        form.addEventListener('input', () => {
            if (form.dataset.sending === 'true') return;
            if (failed.length) {
                failed = [];
                delete form.dataset.retryPending;
                retry.hidden = true;
                button.disabled = !learners.size;
                message.textContent = 'Draft changed. Choose recipients for a new post.';
            }
        });
        form.addEventListener('submit', async event => {
            event.preventDefault();
            if (button.disabled || failed.length) return;
            const selected = el(pickerId).value;
            const recipients = selected === allLearners ? [...learners.keys()] : learners.has(selected) ? [selected] : [];
            if (!recipients.length) return;
            total = recipients.length;
            delivered = 0;
            send = prepare();
            await deliver(recipients);
        });
        retry.addEventListener('click', () => deliver(failed));
    }

    document.addEventListener('campussync:role-ready', async ({detail: tutor}) => {
        el('staffName').textContent = tutor.fullName;
        firebase = await window.CampusSyncSession.ready;
        const value = id => el(id).value;
        const file = id => el(id).files[0];
        bindForm('assignTaskForm', 'taskStudent', 'assignTaskButton', 'tutorTaskMessage', 'Assignment posted.', () => {
            const input = {title:value('taskTitle'), description:value('taskDescription'),
                dueDate:value('taskDue'), priority:value('taskPriority')};
            const pdf = file('taskPdf');
            return id => firebase.createTutorAssignment(id, input, pdf);
        });
        bindForm('postNoteForm', 'noteStudent', 'postNoteButton', 'tutorNoteMessage', 'Notes posted.', () => {
            const input = {title:value('noteTitle'), body:value('noteBody')};
            const pdf = file('notePdf');
            return id => firebase.postTutorNote(id, input, pdf);
        });
        bindForm('scheduleMeetingForm', 'meetingStudent', 'scheduleMeetingButton', 'tutorMeetingMessage', 'Meeting added to the calendar.', () => {
            const input = {title:value('meetingTitle'), description:value('meetingDetails'),
                eventDate:value('meetingDate'), eventTime:value('meetingTime')};
            return id => firebase.scheduleTutorMeeting(id, input);
        });
        try { await load(); }
        catch (error) {
            el('tutorTaskMessage').textContent = error.code === 'permission-denied'
                ? 'Publish the updated Firestore rules to enable the Tutor workspace.'
                : error.message;
        }
    });

    document.querySelector('[data-logout]').addEventListener('click', async () => {
        try {
            await window.CampusSyncSession.signOut();
            store.removeCurrentUser();
            location.replace('tutor-login.html');
        } catch (error) { alert(error.message); }
    });
})();


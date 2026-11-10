function openModal() {
    document.getElementById("assignmentModal").style.display = "flex";
}

function closeModal() {
    document.getElementById("assignmentModal").style.display = "none";
}

async function addTask(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const firebase = await window.CampusSyncSession.ready;
    const name = document.getElementById("assignmentName").value.trim();
    const module = document.getElementById("assignmentModule").value.trim();
    const dueDate = document.getElementById("assignmentDate").value;
    const description = document.getElementById("assignmentDescription").value.trim();
    const reminderAt = document.getElementById("reminderDateTime").value;
    const user = window.CampusSyncLocal.getCurrentUser();

    if (!user || user.role !== "student") {
        alert("Sign in with a student account to add assignments.");
        return;
    }
    if (!name || !module || !dueDate) {
        alert("Please enter an assignment name, module, and due date.");
        return;
    }
    if (reminderAt) {
        const reminderDate = new Date(reminderAt);
        const deadline = new Date(`${dueDate}T23:59:59`);
        if (Number.isNaN(reminderDate.getTime()) || reminderDate <= new Date() || reminderDate > deadline) {
            alert("Choose a valid reminder time in the future and before the assignment deadline.");
            return;
        }
    }

    try {
        const assignment = {
            title: name,
            description: [module, description].filter(Boolean).join(" — "),
            dueDate,
            priority: "medium"
        };
        if (firebase) await firebase.createTask({...assignment, reminderAt});
        else window.CampusSyncLocal.addTask(user.id, assignment, reminderAt);
        form.reset();
        closeModal();
        if (!firebase) renderTasks(window.CampusSyncLocal.getTasks(user.id), user, null);
    } catch (error) {
        console.error("Could not save assignment.", error);
        alert(error.message);
    }
}

let selectedTaskFilter='all';
function filterTasks(filter,button){selectedTaskFilter=filter;document.querySelectorAll('.filter-btn').forEach(item=>item.classList.toggle('active',item===button));document.querySelectorAll('.task-row').forEach(row=>{row.hidden=filter!=='all'&&row.dataset.status!==filter;});}

window.addEventListener("click", event => {
    const modal = document.getElementById("assignmentModal");
    if (event.target === modal) closeModal();
});

function renderTasks(tasks, user, firebase, tutorLinks = new Map()) {
    const container = document.getElementById('taskContainer');
    container.replaceChildren();
    if (!tasks.length) {
        const empty = document.createElement('p');
        empty.className = 'empty-state';
        empty.textContent = 'No personal reminders yet. Create one to track your own deadlines.';
        container.append(empty);
        return;
    }

    const wrapper = document.createElement('div');
    wrapper.className = 'table-responsive';
    const table = document.createElement('table');
    table.className = 'tasks-table';
    table.innerHTML = '<thead><tr><th>Task</th><th>Due Date</th><th>Priority</th><th>Status</th><th>Actions</th></tr></thead>';
    const body = table.createTBody();
    const ordered = [...tasks].sort((a, b) => (a.dueDate || '').localeCompare(b.dueDate || ''));
    const labels = {pending: 'Pending', submitted: 'Submitted', completed: 'Completed'};

    for (const task of ordered) {
        const state = String(task.status || 'pending').toLowerCase();
        const row = body.insertRow();
        row.className = 'task-row';
        row.dataset.status = state;
        row.hidden = selectedTaskFilter !== 'all' && state !== selectedTaskFilter;

        const title = document.createElement('strong');
        title.className = 'task-title';
        title.textContent = task.title;
        const description = document.createElement('p');
        description.className = 'task-desc';
        description.textContent = task.description || 'No details added.';
        const details = row.insertCell();
        details.append(title, description);
        const fileName = tutorLinks.get(task.id);
        if (fileName) details.append(firebase.tutorPdfButton(task.id, fileName));

        const due = row.insertCell();
        due.className = 'task-due';
        due.textContent = task.dueDate
            ? new Date(task.dueDate + 'T12:00:00').toLocaleDateString(undefined, {year: 'numeric', month: 'short', day: 'numeric'})
            : 'No due date';
        const level = String(task.priority || 'medium').toLowerCase();
        const priority = document.createElement('span');
        priority.className = 'badge badge-' + level;
        priority.textContent = level[0].toUpperCase() + level.slice(1);
        row.insertCell().append(priority);
        const status = document.createElement('span');
        status.className = 'badge badge-' + state;
        status.textContent = labels[state] || labels.pending;
        row.insertCell().append(status);

        const group = document.createElement('div');
        group.className = 'admin-inline-form';
        const select = document.createElement('select');
        select.className = 'status-select';
        select.setAttribute('aria-label', 'Status for ' + task.title);
        for (const value of state === 'submitted' ? ['pending', 'submitted', 'completed'] : ['pending', 'completed']) {
            select.add(new Option(labels[value], value));
        }
        select.value = labels[state] ? state : 'pending';

        const save = document.createElement('button');
        save.type = 'button';
        save.className = 'btn btn-sm btn-primary';
        save.textContent = 'Save';
        save.onclick = async () => {
            save.disabled = true;
            try {
                if (firebase) await firebase.updateItem('tasks', task.id, {status: labels[select.value]});
                else {
                    window.CampusSyncLocal.updateTask(user.id, task.id, {status: select.value});
                    renderTasks(window.CampusSyncLocal.getTasks(user.id), user, null);
                }
            } catch (error) { alert(error.message); }
            finally { save.disabled = false; }
        };
        const remove = document.createElement('button');
        remove.type = 'button';
        remove.className = 'btn btn-sm btn-danger';
        remove.textContent = 'Delete';
        remove.onclick = async () => {
            if (!confirm('Delete this assignment?')) return;
            remove.disabled = true;
            try {
                if (firebase) await firebase.removeItem('tasks', task.id);
                else {
                    window.CampusSyncLocal.deleteTask(user.id, task.id);
                    renderTasks(window.CampusSyncLocal.getTasks(user.id), user, null);
                }
            } catch (error) { alert(error.message); }
            finally { remove.disabled = false; }
        };
        group.append(select, save, remove);
        row.insertCell().append(group);
    }
    wrapper.append(table);
    container.append(wrapper);
}

document.addEventListener('DOMContentLoaded', async () => {
    const firebase = await window.CampusSyncSession.ready;
    const user = window.CampusSyncLocal.getCurrentUser();
    if (user?.role !== 'student') return;
    const showError = error => { document.getElementById('taskContainer').textContent = error.message; };
    try {
        if (firebase) {
            let tasks = [];
            let tutorLinks = new Map();
            await firebase.watchItems('tasks', records => {
                tasks = records;
                renderTasks(tasks, user, firebase, tutorLinks);
            }, showError);
            if (firebase.watchTutorLinks) await firebase.watchTutorLinks(links => {
                tutorLinks = links;
                renderTasks(tasks, user, firebase, tutorLinks);
            }, error => console.error('Could not load tutor attachments.', error)).catch(error =>
                console.error('Could not load tutor attachments.', error));
        } else renderTasks(window.CampusSyncLocal.getTasks(user.id), user, null);
    } catch (error) { showError(error); }
});

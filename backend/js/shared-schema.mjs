// This is the existing Android CloudStore schema, not the old MySQL schema.
export const personalKinds = new Set(['tasks', 'notes', 'calendarEvents', 'notifications', 'userSubjects', 'gpaModules']);

export function unpackItem(id, data) {
  if (!personalKinds.has(data.kind)) return null;
  const value = JSON.parse(data.payload);
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('Invalid saved record.');
  return { ...value, id, kind: data.kind };
}

export function packItem(kind, value) {
  if (!personalKinds.has(kind)) throw new Error('Unsupported record type.');
  const payload = { ...value };
  for (const field of ['id', 'kind', 'userId', 'assignedBy', 'taskId', 'noteId', 'eventId', 'notificationId', 'moduleId']) delete payload[field];
  const json = JSON.stringify(payload);
  if (json.length > 60000) throw new Error('This record is too large. Please shorten it.');
  return { kind, payload: json };
}

export function newTask({title, description = '', dueDate, reminderAt = null}) {
  if (!title?.trim() || !/^\d{4}-\d{2}-\d{2}$/.test(dueDate || '')) throw new Error('Enter an assignment name and due date.');
  if (reminderAt && (!Number.isFinite(new Date(reminderAt).getTime()) || new Date(reminderAt) <= new Date() || reminderAt.slice(0, 10) > dueDate)) {
    throw new Error('Choose a future reminder time on or before the due date.');
  }
  return { title: title.trim(), description: description.trim(), dueDate, reminderAt: reminderAt || null,
    priority: 'Medium', status: 'Pending', createdAt: new Date().toISOString(), isSelfCreated: true };
}

export function defaultProfile(user, name, studentNumber = '') {
  return { username: (name || user.displayName || user.email.split('@')[0]).trim(), email: user.email,
    studentNumber, phone: '', institution: '', course: '', yearOfStudy: 1 };
}

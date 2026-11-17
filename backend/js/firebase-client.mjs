import { initializeApp } from 'https://www.gstatic.com/firebasejs/13.0.0/firebase-app.js';
import { getAuth, signOut, GoogleAuthProvider, signInWithPopup, signInWithEmailAndPassword, reauthenticateWithPopup, createUserWithEmailAndPassword, updateProfile, sendEmailVerification, sendPasswordResetEmail, reload, getIdToken, updatePassword, reauthenticateWithCredential, linkWithCredential, EmailAuthProvider } from 'https://www.gstatic.com/firebasejs/13.0.0/firebase-auth.js';
import { getFirestore, doc, collection, getDocFromServer, getDocsFromServer, query, where, runTransaction, writeBatch, onSnapshot,
  setDoc } from 'https://www.gstatic.com/firebasejs/13.0.0/firebase-firestore.js';
import { firebaseConfig } from './firebase-config.mjs';
import { requireStrongPassword, passwordRequirements } from './password-policy.mjs';
import { unpackItem, packItem, defaultProfile, newTask } from './shared-schema.mjs';

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
let openedAccount;
let opening;
const listeners = new Set();

export function friendlyError(error) {
  const messages = {
    'auth/invalid-credential': 'We could not sign you in. Check your email, or use Forgot password? below to choose a new CampusSync password.',
    'auth/wrong-password': 'The current password was not accepted. Use the password-reset email option to choose a new password.',
    'auth/user-mismatch': 'Choose the same Google account you are currently signed in with.',
    'auth/credential-already-in-use': 'This sign-in method belongs to another account. Sign in to that account instead.',
    'auth/email-already-in-use': 'This email is already registered. Sign in with your app password.',
    'auth/weak-password': passwordRequirements,
    'auth/password-does-not-meet-requirements': passwordRequirements,
    'auth/too-many-requests': 'Too many attempts. Wait a while before trying again.',
    'auth/unauthorized-domain': 'Add this website address to Firebase Authentication → Settings → Authorized domains.',
    'permission-denied': 'Firebase could not allow this request. Check that the app’s Firestore rules are published.',
    'auth/popup-blocked': 'Allow pop-ups for this website and try again.',
    'auth/popup-closed-by-user': 'Google sign-in was closed before it finished.',
    'auth/operation-not-allowed': 'This sign-in method is disabled in Firebase Authentication.',
    'auth/network-request-failed': 'Unable to reach Firebase. Check your internet connection.'
  };
  return messages[error?.code] || error?.message || 'Firebase could not complete this request.';
}

export async function currentUser() {
  await auth.authStateReady();
  return auth.currentUser;
}

function requireVerified(user) {
  if (!user.emailVerified) throw Object.assign(new Error('Verify your email before signing in. Check your inbox and spam folder.'), {code:'auth/email-not-verified'});
}

async function openAccount(user, name, studentNumber) {
  requireVerified(user);
  const role = (await getDocFromServer(doc(db, 'roles', user.uid))).data();
  if (role?.disabled === true) throw new Error('This account is disabled. Contact your campus administrator.');
  const profile = doc(db, 'profiles', user.uid);
  const account = doc(db, 'accounts', user.uid);
  await runTransaction(db, async transaction => {
    const profileSnapshot = await transaction.get(profile);
    const accountSnapshot = await transaction.get(account);
    if (!profileSnapshot.exists()) transaction.set(profile, defaultProfile(user, name, studentNumber));
    if (!accountSnapshot.exists()) transaction.set(account, {schemaVersion: 1});
  });
  openedAccount = {
    id: user.uid, email: user.email, fullName: user.displayName || user.email,
    role: role?.role === 'Admin' && role.superAdmin === true ? 'super_admin'
      : role?.role === 'Admin' ? 'admin'
      : role?.role === 'Tutor' ? 'tutor' : 'student',
    cloud: true
  };
  return openedAccount;
}

export async function requireUser() {
  const user = await currentUser();
  if (!user) throw new Error('Please sign in to your Firebase account.');
  requireVerified(user);
  if (openedAccount?.id !== user.uid) {
    if (!opening) opening = openAccount(user).finally(() => { opening = null; });
    await opening;
  }
  return user;
}

export async function sessionAccount() {
  await requireUser();
  return openedAccount;
}

async function finishLogin(credential) {
  try { return await openAccount(credential.user); }
  catch (error) { if (error.code !== 'auth/email-not-verified') await logout(); throw error; }
}

export async function emailLogin(email, password) {
  const credential = await signInWithEmailAndPassword(auth, email.trim(), password);
  return finishLogin(credential);
}

export async function resetPassword(email) {
  try { await sendPasswordResetEmail(auth, email.trim()); }
  catch (error) {
    // Keep the response the same whether the address has an account or not.
    if (error.code !== 'auth/user-not-found') throw error;
  }
}

export async function registerAccount(name, email, password, studentNumber) {
  requireStrongPassword(password);
  if (name.trim().length < 3) throw new Error('Enter a name of at least 3 characters.');
  const {user} = await createUserWithEmailAndPassword(auth, email.trim(), password);
  // Complete the profile only after the email is verified and Firestore allows access.
  try { await updateProfile(user, {displayName:name.trim()}); }
  catch { /* Registration succeeded. The verification screen still lets the user continue. */ }
  sessionStorage.setItem('campusSyncPendingRegistration', JSON.stringify({uid:user.uid, name:name.trim(), studentNumber:studentNumber.trim()}));
  await resendVerification();
}

export async function resendVerification() {
  const user = await currentUser();
  if (!user) throw new Error('Sign in first to request a verification email.');
  if (user.emailVerified) throw new Error('Your email is already verified. Continue to sign in.');
  await sendEmailVerification(user);
}

export async function finishVerification() {
  const user = await currentUser();
  if (!user) throw new Error('Please sign in again.');
  await reload(user);
  requireVerified(user);
  await getIdToken(user, true);
  let details;
  try { details = JSON.parse(sessionStorage.getItem('campusSyncPendingRegistration') || 'null'); } catch {}
  const account = await openAccount(user, details?.uid === user.uid ? details.name : undefined,
      details?.uid === user.uid ? details.studentNumber : undefined);
  sessionStorage.removeItem('campusSyncPendingRegistration');
  return account;
}

export async function changeAccountPassword(currentPassword, password) {
  requireStrongPassword(password);
  const user = await requireUser();
  if (!user.providerData.some(provider => provider.providerId === 'password')) {
    if (!user.providerData.some(provider => provider.providerId === 'google.com')) {
      throw new Error('Use the password-reset email option to set a CampusSync password.');
    }
    const provider = new GoogleAuthProvider();
    provider.setCustomParameters({prompt: 'select_account', login_hint: user.email});
    await reauthenticateWithPopup(user, provider);
    await linkWithCredential(user, EmailAuthProvider.credential(user.email, password));
    return;
  }
  await reauthenticateWithCredential(user, EmailAuthProvider.credential(user.email, currentPassword));
  await updatePassword(user, password);
}

export async function googleLogin() {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: 'select_account' });
  const credential = await signInWithPopup(auth, provider);
  return finishLogin(credential);
}
export async function googleCalendarAccess() {
  const user = auth.currentUser;
  if (!user) throw new Error('Sign in to CampusSync before connecting Google Calendar.');
  const provider = new GoogleAuthProvider();
  provider.addScope('https://www.googleapis.com/auth/calendar.events.readonly');
  provider.setCustomParameters({ login_hint: user.email });
  const result = await reauthenticateWithPopup(user, provider);
  const token = GoogleAuthProvider.credentialFromResult(result)?.accessToken;
  if (!token) throw new Error('Google did not return calendar access. Try connecting again.');
  return token;
}


export async function logout() {
  for (const stop of listeners) stop();
  listeners.clear(); openedAccount = null;
  await signOut(auth);
  // Remove the previous backend session; Firebase manages its own persistence.
  for (const key of ['token', 'user', 'campusSyncCurrentUser']) localStorage.removeItem(key);
}

export async function loadStaffRoles() {
  const records = await getDocsFromServer(query(collection(db, 'roles'), where('role', 'in', ['Admin', 'Tutor'])));
  const roles = records.docs.map(record => ({...record.data(), uid: record.id}));
  const admins = await Promise.all(roles.filter(record => record.role === 'Admin').map(async record => {
    const profile = (await getDocFromServer(doc(db, 'profiles', record.uid))).data();
    return {...record, name: profile?.username, email: profile?.email};
  }));
  return {admins, tutorCount: roles.filter(record => record.role === 'Tutor').length};
}

export async function grantAdminAccess(uid) {
  if (!uid || uid.length > 128 || uid.includes('/')) throw new Error('Enter a valid Firebase Authentication UID.');
  const roleDoc = doc(db, 'roles', uid);
  await runTransaction(db, async transaction => {
    const current = (await transaction.get(roleDoc)).data();
    if (current?.disabled) throw new Error('This account is disabled. Restore it in Firebase before granting access.');
    if (current?.role === 'Admin') throw new Error('This account already has administrator access.');
    transaction.set(roleDoc, {role: 'Admin'}, {merge: true});
  });
}

async function loadProfileDirectory(user) {
  const token = await user.getIdToken();
  const project = encodeURIComponent(firebaseConfig.projectId);
  const response = await fetch(`https://firestore.googleapis.com/v1/projects/${project}/databases/(default)/documents:runQuery`, {
    method: 'POST',
    headers: {Authorization: `Bearer ${token}`, 'Content-Type': 'application/json'},
    body: JSON.stringify({structuredQuery: {
      from: [{collectionId: 'profiles'}],
      select: {fields: ['username','email','studentNumber'].map(fieldPath => ({fieldPath}))}
    }})
  });
  if (response.status === 403) throw Object.assign(new Error('Firebase denied the staff dashboard request. Publish the updated Firestore rules.'), {code:'permission-denied'});
  if (!response.ok) throw new Error('Could not load the Firebase student directory.');
  const records = await response.json();
  return records.flatMap(({document}) => document ? [{
    id: document.name.split('/').pop(),
    fullName: document.fields?.username?.stringValue,
    email: document.fields?.email?.stringValue,
    studentNumber: document.fields?.studentNumber?.stringValue,
    createdAt: document.createTime
  }] : []);
}

export async function loadStaffOverview() {
  const user = await requireUser();
  const [profiles, roles, roster] = await Promise.all([loadProfileDirectory(user), getDocsFromServer(collection(db, 'roles')), getDocsFromServer(collection(db, 'tutorAssignments'))]);
  const roleByUid = new Map(roles.docs.map(record => [record.id, record.data()]));
  const students = profiles.filter(profile => (roleByUid.get(profile.id)?.role || 'Student') === 'Student')
    .map(profile => ({...profile, accountStatus: roleByUid.get(profile.id)?.disabled ? 'suspended' : 'active'}))
    .sort((a,b) => (b.createdAt || '').localeCompare(a.createdAt || ''));
  const tasks = [];
  const reminders = [];
  const notifications = [];
  for (let index = 0; index < students.length; index += 8) {
    await Promise.all(students.slice(index, index + 8).map(async student => {
      const snapshots = await getDocsFromServer(query(collection(db, 'accounts', student.id, 'items'), where('kind', 'in', ['tasks','notifications'])));
      for (const record of snapshots.docs) {
        const item = unpackItem(record.id, record.data());
        if (item.kind === 'tasks') {
          tasks.push({...item, userId: student.id, status: String(item.status || 'pending').toLowerCase()});
          if (item.reminderAt) reminders.push({userId:student.id, remindAt:item.reminderAt});
        } else {
          notifications.push({userId:student.id, read:Boolean(item.isRead ?? item.read)});
        }
      }
    }));
  }
  const tutors = profiles.filter(profile => roleByUid.get(profile.id)?.role === 'Tutor' && !roleByUid.get(profile.id)?.disabled)
    .map(profile => ({...profile, studentCount:roster.docs.filter(entry => entry.data().tutorUid === profile.id).length}));
  return {students, tutors, tasks, reminders, notifications, completedTasks: tasks.filter(task => task.status === 'completed').length};
}

export async function setFirebaseStudentStatus(uid, status) {
  if (!['active','suspended'].includes(status)) throw new Error('Invalid account status.');
  const roleDoc = doc(db, 'roles', uid);
  await runTransaction(db, async transaction => {
    const current = (await transaction.get(roleDoc)).data();
    if (current?.superAdmin || (current?.role && current.role !== 'Student')) throw new Error('Only student accounts can be suspended here.');
    const disabled = status === 'suspended';
    if (current) transaction.set(roleDoc, {disabled}, {merge:true});
    else transaction.set(roleDoc, {role:'Student', disabled});
  });
}


export async function grantTutorAccess(uid) {
  if (!uid || uid.length > 128 || uid.includes('/')) throw new Error('Enter a valid Firebase Authentication UID.');
  const role = doc(db, 'roles', uid);
  await runTransaction(db, async transaction => {
    const current = (await transaction.get(role)).data();
    if (current?.role === 'Tutor') return;
    if (current?.disabled || (current?.role && current.role !== 'Student')) throw new Error('Choose an active student account.');
    transaction.set(role, {role:'Tutor'}, {merge:true});
  });
}

export async function assignFirebaseStudent(tutorUid, studentUid) {
  await setDoc(doc(db, 'tutorAssignments', studentUid), {tutorUid});
}

export async function loadTutorAssignments() {
  const user = await requireUser();
  const roster = await getDocsFromServer(query(collection(db, 'tutorAssignments'), where('tutorUid', '==', user.uid)));
  const results = await Promise.all(roster.docs.map(async entry => {
    const studentId = entry.id;
    try {
      const profile = (await getDocFromServer(doc(db, 'profiles', studentId))).data();
      const links = await getDocsFromServer(query(collection(db, 'tutorAssignments', studentId, 'tasks'), where('tutorUid', '==', user.uid)));
      const tasks = await Promise.all(links.docs.map(async link => {
        const record = await getDocFromServer(doc(db, 'accounts', studentId, 'items', link.id));
        if (!record.exists()) return null;
        const post = unpackItem(record.id, record.data());
        return {...post, userId:studentId, fileName:link.get('fileName'), status: String(post.status || 'pending').toLowerCase(), priority: String(post.priority || 'medium').toLowerCase()};
      }));
      return {student:{id:studentId, fullName:profile?.username || 'Student'}, tasks:tasks.filter(Boolean)};
    } catch (error) {
      if (error.code === 'permission-denied') return null;
      throw error;
    }
  }));
  const active = results.filter(Boolean);
  const posts = active.flatMap(entry => entry.tasks);
  return {students:active.map(entry => entry.student), tasks:posts.filter(post => post.kind === 'tasks'),
    posts:posts.filter(post => post.kind !== 'tasks')};
}

const maxTutorPdfBytes = 512 * 1024;

async function readTutorPdf(file) {
  if (!file) return null;
  if (!/\.pdf$/i.test(file.name) || file.size > maxTutorPdfBytes) throw new Error('Choose a PDF up to 512 KB.');
  const bytes = new Uint8Array(await file.arrayBuffer());
  if (new TextDecoder().decode(bytes.subarray(0, 5)) !== '%PDF-') throw new Error('The selected file is not a PDF.');
  let binary = '';
  for (let start = 0; start < bytes.length; start += 16384) {
    binary += String.fromCharCode(...bytes.subarray(start, start + 16384));
  }
  return {fileName:file.name.slice(0, 146).replace(/\.pdf$/i, '') + '.pdf', base64:btoa(binary)};
}

async function createTutorPost(studentUid, kind, data, file) {
  const user = await requireUser();
  const attachment = await readTutorPdf(file);
  const item = doc(collection(db, 'accounts', studentUid, 'items'));
  const link = doc(db, 'tutorAssignments', studentUid, 'tasks', item.id);
  const metadata = {tutorUid:user.uid};
  const batch = writeBatch(db);
  if (attachment) {
    metadata.fileName = attachment.fileName;
    batch.set(doc(db, 'accounts', studentUid, 'tutorFiles', item.id), {base64:attachment.base64});
  }
  batch.set(item, packItem(kind, data));
  batch.set(link, metadata);
  await batch.commit();
}

export async function createTutorAssignment(studentUid, input, file) {
  const data = {...newTask(input), isSelfCreated:false};
  data.priority = ({high:'High', medium:'Medium', low:'Low'})[input.priority] || 'Medium';
  return createTutorPost(studentUid, 'tasks', data, file);
}

export async function postTutorNote(studentUid, {title, body}, file) {
  if (!title.trim() || (!body.trim() && !file)) throw new Error('Enter a title and note text or attach a PDF.');
  return createTutorPost(studentUid, 'notes', {
    title:title.trim(), body:body.trim(), subjectName:'Tutor notes', updatedAt:new Date().toISOString()
  }, file);
}

export async function scheduleTutorMeeting(studentUid, {title, description, eventDate, eventTime}) {
  if (!title.trim() || !/^\d{4}-\d{2}-\d{2}$/.test(eventDate) || !/^\d{2}:\d{2}$/.test(eventTime)) {
    throw new Error('Enter a meeting title, date and time.');
  }
  return createTutorPost(studentUid, 'calendarEvents', {
    title:title.trim(), description:description.trim(), eventType:'Meeting', eventDate, eventTime, color:'#1e40af'
  });
}

export async function watchTutorLinks(next, failure) {
  const user = await requireUser();
  const stop = onSnapshot(collection(db, 'tutorAssignments', user.uid, 'tasks'), snapshot => {
    next(new Map(snapshot.docs.map(link => [link.id, link.get('fileName')])));
  }, failure);
  listeners.add(stop);
}

export async function downloadTutorPdf(id, name) {
  const user = await requireUser();
  const file = await getDocFromServer(doc(db, 'accounts', user.uid, 'tutorFiles', id));
  if (!file.exists()) throw new Error('This attachment is unavailable.');
  const bytes = Uint8Array.from(atob(file.get('base64')), char => char.charCodeAt(0));
  const url = URL.createObjectURL(new Blob([bytes], {type:'application/pdf'}));
  const link = document.createElement('a');
  link.href = url;
  link.download = name.replace(/[\\/]/g, '_');
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export async function setTutorAssignmentStatus(studentUid, id, status) {
  if (!['pending','completed'].includes(status)) throw new Error('Choose a valid task status.');
  const task = doc(db, 'accounts', studentUid, 'items', id);
  await runTransaction(db, async transaction => {
    const snapshot = await transaction.get(task);
    if (!snapshot.exists() || snapshot.data().kind !== 'tasks') throw new Error('That assignment is no longer available.');
    const data = unpackItem(snapshot.id, snapshot.data());
    transaction.set(task, packItem('tasks', {...data, status:status === 'completed' ? 'Completed' : 'Pending'}));
  });
}


export function tutorPdfButton(id, name) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'btn btn-sm btn-outline';
  button.textContent = 'Download ' + name;
  button.addEventListener('click', async () => {
    button.disabled = true;
    try { await downloadTutorPdf(id, name); }
    catch (error) { alert(error.message); }
    finally { button.disabled = false; }
  });
  return button;
}

async function itemsCollection() {
  const user = await requireUser();
  return collection(db, 'accounts', user.uid, 'items');
}
export async function watchItems(kind, next, failure) {
  const items = await itemsCollection();
  const stop = onSnapshot(items, snapshot => {
    try { next(snapshot.docs.filter(record => record.get('kind') === kind).map(record => unpackItem(record.id, record.data()))); }
    catch (error) { failure(error); }
  }, failure);
  listeners.add(stop);
  return () => { stop(); listeners.delete(stop); };
}
export async function createItem(kind, value) {
  const ref = doc(await itemsCollection());
  await setDoc(ref, packItem(kind, value));
  return ref.id;
}
export async function updateItem(kind, id, changes) {
  const ref = doc(await itemsCollection(), id);
  await runTransaction(db, async transaction => {
    const snapshot = await transaction.get(ref);
    if (!snapshot.exists() || snapshot.data().kind !== kind) throw new Error('This record no longer exists.');
    const existing = unpackItem(id, snapshot.data());
    transaction.set(ref, packItem(kind, {...existing, ...changes}));
  });
}
export async function removeItem(kind, id) {
  const ref = doc(await itemsCollection(), id);
  await runTransaction(db, async transaction => {
    const snapshot = await transaction.get(ref);
    if (snapshot.exists() && snapshot.data().kind !== kind) throw new Error('Record type does not match.');
    transaction.delete(ref);
  });
}
export async function createTask(values) { return createItem('tasks', newTask(values)); }

import {readFile} from 'node:fs/promises';
import {before, after, test} from 'node:test';
import {initializeTestEnvironment, assertFails, assertSucceeds} from '@firebase/rules-unit-testing';
import {doc, collection, getDoc, getDocs, setDoc, updateDoc, deleteDoc, writeBatch} from 'firebase/firestore';
let env;
const context = (uid) => env.authenticatedContext(uid, {email:`${uid}@example.test`}).firestore();
const profile = (uid) => ({username:'A Student',email:`${uid}@example.test`,studentNumber:'',phone:'',institution:'College',course:'Science',yearOfStudy:1});
const record = {kind:'notes',payload:JSON.stringify({title:'Private note',body:'Only its owner can read this'})};
before(async()=>{
  env = await initializeTestEnvironment({projectId:'demo-campussync',firestore:{host:'127.0.0.1',port:8088,rules:await readFile('../firestore.rules','utf8')}});
  await env.withSecurityRulesDisabled(async c=>{
    const db=c.firestore();
    await Promise.all([
      setDoc(doc(db,'roles/tutor'),{role:'Tutor',disabled:false}),
      setDoc(doc(db,'roles/admin'),{role:'Admin',disabled:false}),
      setDoc(doc(db,'roles/disabled'),{role:'Student',disabled:true}),
      setDoc(doc(db,'accounts/alice'),{schemaVersion:1}),
      setDoc(doc(db,'accounts/alice/items/private'),record),
      setDoc(doc(db,'profiles/alice'),profile('alice'))
    ]);
  });
});
after(async()=>{ await env?.cleanup(); });
test('unauthenticated clients cannot read or write data',async()=>{
  const db=env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db,'accounts/alice/items/private')));
  await assertFails(setDoc(doc(db,'profiles/guest'),profile('guest')));
  await assertFails(getDocs(collection(db,'subjects')));
});
test('owner can create, read, edit and delete a personal record',async()=>{
  const db=context('bob'), ref=doc(db,'accounts/bob/items/note');
  await assertSucceeds(setDoc(ref,record));
  await assertSucceeds(getDoc(ref));
  await assertSucceeds(updateDoc(ref,{payload:'{"title":"Edited"}'}));
  await assertSucceeds(deleteDoc(ref));
});
test('another student cannot read, list, edit or delete private records',async()=>{
  const db=context('bob'),ref=doc(db,'accounts/alice/items/private');
  await assertFails(getDoc(ref));
  await assertFails(getDocs(collection(db,'accounts/alice/items')));
  await assertFails(setDoc(ref,record));
  await assertFails(deleteDoc(ref));
});
test('Tutor and Admin cannot read another persons private notes or profile',async()=>{
  for(const uid of ['tutor','admin']){
    const db=context(uid);
    await assertFails(getDoc(doc(db,'accounts/alice/items/private')));
    await assertFails(getDoc(doc(db,'profiles/alice')));
  }
});
test('roles cannot be self-granted, changed, or assigned by any app client',async()=>{
  for(const uid of ['alice','tutor','admin']){
    const db=context(uid);
    await assertSucceeds(getDoc(doc(db,`roles/${uid}`)));
    await assertFails(setDoc(doc(db,`roles/${uid}`),{role:'Admin'}));
    await assertFails(setDoc(doc(db,'roles/other'),{role:'Tutor'}));
    await assertFails(getDocs(collection(db,'roles')));
  }
});
test('profile cannot contain password, role, UID or an altered sign-in email',async()=>{
  const db=context('bob'), ref=doc(db,'profiles/bob');
  await assertSucceeds(setDoc(ref,profile('bob')));
  for(const extra of [{role:'Admin'},{password:'secret'},{authUid:'alice'},{email:'alice@example.test'},{yearOfStudy:100}]){
    await assertFails(setDoc(ref,{...profile('bob'),...extra}));
  }
  await assertFails(getDoc(doc(db,'profiles/alice')));
});
test('only Admin can manage the shared subject catalogue',async()=>{
  const value={subjectId:123,subjectName:'Mathematics',monthlyFee:250};
  for(const uid of ['alice','tutor']) await assertFails(setDoc(doc(context(uid),'subjects/123'),value));
  const ref=doc(context('admin'),'subjects/123');
  await assertSucceeds(setDoc(ref,value));
  await assertSucceeds(getDocs(collection(context('alice'),'subjects')));
  await assertFails(setDoc(ref,{...value,monthlyFee:-1}));
  await assertFails(setDoc(ref,{...value,subjectId:999}));
  await assertSucceeds(deleteDoc(ref));
});
test('Tutor can publish updates, Student cannot spoof staff or overwrite updates',async()=>{
  const news={announcementId:234,title:'Campus news',message:'Library open',timestamp:'2026-10-18',isDismissed:false,authorUid:'tutor'};
  await assertFails(setDoc(doc(context('alice'),'announcements/234'),news));
  await assertSucceeds(setDoc(doc(context('tutor'),'announcements/234'),news));
  await assertFails(updateDoc(doc(context('alice'),'announcements/234'),{message:'Spoofed'}));
  await assertFails(setDoc(doc(context('tutor'),'announcements/235'),{...news,announcementId:235,authorUid:'admin'}));
  await assertSucceeds(getDocs(collection(context('alice'),'announcements')));
});
test('disabled accounts lose access even to their own private data and shared catalogue',async()=>{
  const db=context('disabled');
  await assertFails(setDoc(doc(db,'accounts/disabled/items/note'),record));
  await assertFails(getDocs(collection(db,'subjects')));
  await assertSucceeds(getDoc(doc(db,'roles/disabled')));
});
test('invalid record kinds, unexpected fields, and oversized records are rejected',async()=>{
  const ref=doc(context('bob'),'accounts/bob/items/invalid');
  for(const value of [{...record,role:'Admin'},{kind:'roles',payload:'{}'},{kind:'notes',payload:'x'.repeat(65001)}]) await assertFails(setDoc(ref,value));
});
test('migration marker cannot be changed or removed by clients',async()=>{
  const db=context('newcomer'),ref=doc(db,'accounts/newcomer');
  await assertSucceeds(setDoc(ref,{schemaVersion:1}));
  await assertFails(updateDoc(ref,{schemaVersion:2}));
  await assertFails(deleteDoc(ref));
});
test('individual record writes preserve another devices records',async()=>{
  const first=context('shared'),second=context('shared');
  await assertSucceeds(setDoc(doc(first,'accounts/shared/items/device-one'),record));
  await assertSucceeds(setDoc(doc(second,'accounts/shared/items/device-two'),{...record,kind:'tasks'}));
  await assertSucceeds(deleteDoc(doc(first,'accounts/shared/items/device-one')));
  const remaining=await assertSucceeds(getDocs(collection(second,'accounts/shared/items')));
  if(remaining.size!==1 || remaining.docs[0].id!=='device-two') throw Error('Independent record was lost');
});


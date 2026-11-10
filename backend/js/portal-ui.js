document.addEventListener('DOMContentLoaded', async () => {
  await window.CampusSyncSession?.ready;
  const user=window.CampusSyncLocal?.getCurrentUser();
  const welcome=document.getElementById('portalWelcome');
  if(welcome&&user)welcome.textContent=`Welcome, ${user.fullName || 'Student'} (${user.role.replace('_',' ')})`;
  const button=document.querySelector('[data-portal-menu]');
  button?.addEventListener('click',()=>{
    const menu=document.getElementById('navbarMenu');
    const open=menu.classList.toggle('active');button.classList.toggle('active',open);button.setAttribute('aria-expanded',String(open));
  });
  const logout=document.querySelector('[data-portal-logout]');
  if(logout&&!logout.hasAttribute('data-logout')){
    logout.textContent=user?'Logout':'Sign In';
    logout.addEventListener('click', async () => {
      try {
        await window.CampusSyncSession?.signOut();
        if (user) window.CampusSyncLocal.removeCurrentUser();
        location.href = 'login.html';
      } catch (error) { alert(error.message); }
    });
  }
  const current=location.pathname.split('/').pop();
  document.querySelectorAll('.nav-link[href]').forEach(link=>{if(link.getAttribute('href')===current){link.classList.add('nav-link-active');link.setAttribute('aria-current','page');}});
  for (const link of document.querySelectorAll('.portal-more-menu a')) {
    if (link.getAttribute('href') !== current) continue;
    link.setAttribute('aria-current', 'page');
    link.closest('details').querySelector('summary').classList.add('nav-link-active');
  }
  if(user&&user.role!=='student'){
    const menu=document.getElementById('navbarMenu');
    if(menu){menu.replaceChildren();const li=document.createElement('li');const a=document.createElement('a');a.className='nav-link nav-link-active';a.href=window.CampusSyncLocal.dashboards[user.role];a.textContent='Overview';li.append(a);menu.append(li);}
    const brand=document.querySelector('.navbar-brand');if(brand)brand.href=window.CampusSyncLocal.dashboards[user.role];
  }
  document.querySelectorAll('[data-show-password]').forEach(button=>button.addEventListener('click',()=>{
    const field=document.getElementById(button.dataset.showPassword);const visible=field.type==='password';field.type=visible?'text':'password';button.textContent=visible?'Hide':'Show';button.setAttribute('aria-label',visible?'Hide password':'Show password');
  }));
});

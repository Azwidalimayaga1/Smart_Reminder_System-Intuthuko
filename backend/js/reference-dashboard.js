document.addEventListener('DOMContentLoaded', async () => {
    const firebase = await window.CampusSyncSession.ready;
    const user = window.CampusSyncLocal.getCurrentUser();
    if (user?.role !== 'student') return;
    const showError = error => {
        const status = document.getElementById('dashboardStatus');
        status.textContent = error.message;
        status.hidden = false;
    };
    try {
        if (firebase) await firebase.watchItems('tasks', renderOverview, showError);
        else renderOverview(window.CampusSyncLocal.getTasks(user.id));
    } catch (error) { showError(error); }
});

function renderOverview(records) {
  const tasks = records.map(task => ({...task, status: String(task.status || 'pending').toLowerCase()}));
  const pending=tasks.filter(task=>task.status!=='completed');const completed=tasks.filter(task=>task.status==='completed');
  const today=new Date();today.setHours(0,0,0,0);const end=new Date(today);end.setDate(end.getDate()+7);
  const due=task=>{const value=task.dueDate||task.due_date;const date=value?new Date(/^\d{4}-\d{2}-\d{2}$/.test(value) ? value + 'T12:00:00' : value):null;return date&&!Number.isNaN(date.getTime())?date:null;};
  const percentage=tasks.length?Math.round(completed.length/tasks.length*100):0;
  const values={totalTaskCount:tasks.length,activeTaskCount:pending.length,dueTodayCount:pending.filter(task=>{const date=due(task);return date&&date.toDateString()===today.toDateString();}).length,progressPercentage:percentage+'%',rewardPoints:completed.length*10,progressSummary:`${completed.length} of ${tasks.length} tasks completed`,dueThisWeek:`${pending.filter(task=>{const date=due(task);return date&&date>=today&&date<end;}).length} deadlines in the next 7 days`};
  for(const [id,value] of Object.entries(values))document.getElementById(id).textContent=value;
  document.getElementById('progressFill').style.width=percentage+'%';document.querySelector('.progress-track').setAttribute('aria-valuenow',String(percentage));
  render('highPriorityTasks',pending.filter(task=>String(task.priority).toLowerCase()==='high'),'No high priority tasks. Great job!');
  render('upcoming-submissions',tasks.filter(task=>{const date=due(task);return date&&date>=today&&date<end;}),'No upcoming deadlines. You’re all set!');
  function render(id,items,message){
    const list=document.getElementById(id);list.replaceChildren();
    if(!items.length){const p=document.createElement('p');p.className='empty-state';p.textContent=message;list.append(p);return;}
    items.sort((a,b)=>due(a)-due(b)).slice(0,10).forEach(task=>{
      const row=document.createElement('div');row.className='task-item';
      const info=document.createElement('div');info.className='task-info';
      const title=document.createElement('h4');title.textContent=task.title;
      const date=document.createElement('p');date.className='task-due';date.textContent='Due: '+(due(task)?.toLocaleDateString(undefined,{year:'numeric',month:'long',day:'numeric'})||'No date');info.append(title,date);
      const meta=document.createElement('div');meta.className='task-meta';
      const priority=document.createElement('span');const level=['high','medium','low'].includes(task.priority)?task.priority:'medium';priority.className='badge badge-'+level;priority.textContent=level[0].toUpperCase()+level.slice(1);
      const status=document.createElement('span');status.className='status-'+task.status;status.textContent=task.status==='completed'?'Completed':'Pending';meta.append(priority,status);row.append(info,meta);list.append(row);
    });
  }
}

-- Running Processes
select
  substring(p.running_pid, locate('@', p.running_pid) + 1) as "Machine",
  substring(p.running_pid, 1, locate('@', p.running_pid) - 1) as "PID",
  p.name as "Project", p.cloneUrl as "Clone URL", 
  format(p.size/1000, 1) as "Size (MB)", 
  format(p.commits_count, 0) as "Commits",
  format(100 * (select count(*) from revisiongit rev where rev.project = p.id)/p.commits_count, 1) as "Progress (%)"
from projectgit p
where p.running_pid not in ('wait', 'filtered', 'error') and p.analyzed = 0
order by substring(p.running_pid, locate('@', p.running_pid) + 1), p.running_pid;

-- General progress
select
  count(p.id) as "Projects",
  sum(if(p.analyzed = 1, 1, 0)) as "Analyzed Projects",
  format(sum(p.commits_count), 0) as "Commits",
  format((select count(*) from revisiongit rev), 0) as "Analyzed Commits",
  format(100 * (select count(*) from revisiongit rev)/sum(p.commits_count), 3) as "Progress (%)"
from projectgit p
where p.running_pid is null or p.running_pid <> 'filtered';

-- Analyzed Commits by Machine 
select
  substring(p.running_pid, locate('@', p.running_pid) + 1) as "Machine",
  format(count(rev.id), 0) as "Analyzed Commits"
from projectgit p join revisiongit rev on rev.project = p.id
where p.running_pid not in ('wait', 'filtered', 'error') and p.analyzed = 1
group by substring(p.running_pid, locate('@', p.running_pid) + 1)
order by count(rev.id) desc;

create or replace view refactoringweeks
as select
  p.id as "projectId",
  substring(p.cloneUrl, 20) as "Project",
  w.start as "Week",
  UNIX_TIMESTAMP(w.start) as "Time",
  count(distinct rev.id) as "Commits",
  count(ref.id) as "Refactorings"
from projectgit p
  join duplication dup on dup.project = p.id
  join weeks w on p.created_at <= w.start
  left join revisiongit rev on rev.project = p.id and rev.commitTime between w.start and date_add(w.start, interval 7 day)
  left join refactoringgit ref on ref.revision = rev.id
where p.analyzed = 1 and p.running_pid not in ('filtered') and ratio > 0.8
and (ref.refactoringType is null or ref.refactoringType not in ('Merge Operation', 'Extract & Move Operation', 'Extract Interface', 'Extract Superclass', 'Convert Anonymous Class to Type'))
group by p.id, p.cloneUrl, w.start
order by p.id asc, w.start desc;

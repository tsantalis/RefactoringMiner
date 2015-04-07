create or replace view refactoringtype
as select
  replace(ref.refactoringType, 'Operation', 'Method') as "refactoringType",
  count(ref.id) as "count"
from projectgit p join revisiongit rev on rev.project = p.id join refactoringgit ref on ref.revision = rev.id join duplication dup on dup.project = p.id
where p.analyzed = 1 and p.running_pid not in ('filtered') and ratio > 0.8
and ref.refactoringType not in ('Merge Operation', 'Extract & Move Operation', 'Extract Interface', 'Extract Superclass', 'Convert Anonymous Class to Type')
group by ref.refactoringType
order by count(ref.id) desc;

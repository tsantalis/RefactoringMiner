create or replace view refactoringtypeproj
as select
  p.id as "projectId",
  substring(p.cloneUrl, 20) as "Project",
  count(ref.id) as "Refs",
  sum(if(ref.refactoringType = 'Move Class', 1, 0))/count(ref.id) as "Move Class",
  sum(if(ref.refactoringType = 'Rename Method', 1, 0))/count(ref.id) as "Rename Method",
  sum(if(ref.refactoringType = 'Extract Method', 1, 0))/count(ref.id) as "Extract Method",
  sum(if(ref.refactoringType = 'Rename Class', 1, 0))/count(ref.id) as "Rename Class",
  sum(if(ref.refactoringType = 'Move Attribute', 1, 0))/count(ref.id) as "Move Attribute",
  sum(if(ref.refactoringType = 'Inline Method', 1, 0))/count(ref.id) as "Inline Method",
  sum(if(ref.refactoringType = 'Move Method', 1, 0))/count(ref.id) as "Move Method",
  sum(if(ref.refactoringType = 'Pull Up Method', 1, 0))/count(ref.id) as "Pull Up Method",
  sum(if(ref.refactoringType = 'Pull Up Attribute', 1, 0))/count(ref.id) as "Pull Up Attribute",
  sum(if(ref.refactoringType = 'Push Down Method', 1, 0))/count(ref.id) as "Push Down Method",
  sum(if(ref.refactoringType = 'Push Down Attribute', 1, 0))/count(ref.id) as "Push Down Attribute"
from projectgit p join revisiongit rev on rev.project = p.id join refactoringgit ref on ref.revision = rev.id join duplication dup on dup.project = p.id
where p.analyzed = 1 and p.running_pid not in ('filtered') and ratio > 0.8
and ref.refactoringType not in ('Merge Method', 'Extract And Move Method', 'Extract Interface', 'Extract Superclass', 'Convert Anonymous Class to Type')
group by p.id, p.cloneUrl;

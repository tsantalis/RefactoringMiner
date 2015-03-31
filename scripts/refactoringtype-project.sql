create view refactoringtype
as select
  substring(p.cloneUrl, 20) as "Project",
  count(ref.id) as "Refs",
  sum(if(ref.refactoringType = 'Extract Operation', 1, 0))/count(ref.id) as "Extract Method",
  sum(if(ref.refactoringType = 'Rename Class', 1, 0))/count(ref.id) as "Rename Class",
  sum(if(ref.refactoringType = 'Move Attribute', 1, 0))/count(ref.id) as "Move Attribute",
  sum(if(ref.refactoringType = 'Rename Method', 1, 0))/count(ref.id) as "Rename Method",
  sum(if(ref.refactoringType = 'Inline Operation', 1, 0))/count(ref.id) as "Inline Method",
  sum(if(ref.refactoringType = 'Move Operation', 1, 0))/count(ref.id) as "Move Method",
  sum(if(ref.refactoringType = 'Pull Up Operation', 1, 0))/count(ref.id) as "Pull Up Method",
  sum(if(ref.refactoringType = 'Move Class', 1, 0))/count(ref.id) as "Move Class",
  sum(if(ref.refactoringType = 'Pull Up Attribute', 1, 0))/count(ref.id) as "Pull Up Attribute",
  sum(if(ref.refactoringType = 'Push Down Attribute', 1, 0))/count(ref.id) as "Push Down Attribute",
  sum(if(ref.refactoringType = 'Push Down Operation', 1, 0))/count(ref.id) as "Push Down Method"
from projectgit p join revisiongit rev on rev.project = p.id join refactoringgit ref on ref.revision = rev.id join duplication dup on dup.project = p.id
where p.analyzed = 1 and p.running_pid not in ('filtered') and ratio > 0.8
and ref.refactoringType not in ('Merge Operation', 'Extract & Move Operation', 'Extract Interface', 'Extract Superclass', 'Convert Anonymous Class to Type')
group by p.id, p.cloneUrl;

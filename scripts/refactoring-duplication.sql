
select p.name, ref.description, count(*),
  CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', max(rev.commitId),'?diff=split') as diffUrl1,
  CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', min(rev.commitId),'?diff=split') as diffUrl2
from refactoringgit ref
join revisiongit rev on rev.id = ref.revision
join projectgit p on p.id = rev.project
-- where p.name = 'junit'
and p.name = 'cw-omnibus'
group by p.name, ref.description having count(*) > 1
order by count(*) desc;

select p.name, p.commits_count, count(distinct ref.id) as refs, count(distinct ref.description) as drefs, (count(distinct ref.description)/ count(distinct ref.id)) ratio, p.cloneUrl
from refactoringgit ref
join revisiongit rev on rev.id = ref.revision
join projectgit p on p.id = rev.project
where p.analyzed = 1
group by p.name
order by (count(distinct ref.description)/ count(distinct ref.id)) desc;

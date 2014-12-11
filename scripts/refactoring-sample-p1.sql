(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Rename Method'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Move Class'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Move Operation'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Extract Operation'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Rename Class'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Extract & Move Operation'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Inline Operation'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Move Attribute'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Pull Up Operation'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Extract Superclass'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Push Down Operation'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Extract Interface'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Convert Anonymous Class to Type'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Pull Up Attribute'
order by rand() limit 0, 10)
union all
(select ref.refactoringType, p.name, ref.description, CONCAT(SUBSTRING_INDEX(p.cloneUrl, '.git', 1), '/commit/', rev.commitId,'?diff=split') as diffUrl
from refactoringgit ref join revisiongit rev on rev.id = ref.revision join projectgit p on p.id = rev.project
where p.name in ('clojure', 'k-9', 'junit', 'voldemort', 'netty', 'storm', 'jsoup', 'android-async-http', 'RxJava', 'titan') and ref.refactoringType = 'Push Down Attribute'
order by rand() limit 0, 10)
;
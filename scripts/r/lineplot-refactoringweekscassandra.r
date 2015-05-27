library(DBI)
library(xts)

con <- dbConnect(RMySQL::MySQL(),
                 default.file = 'c:/.danilofs-refactoring.cnf',
                 groups = 'refactoring')
on.exit(dbDisconnect(con))

refactoringActivityPerWeek <- function(con, cloneUrl, title){
  query <- paste("select
  UNIX_TIMESTAMP(w.start) as time,
  count(distinct rev.id) as commits,
  count(ref.id) as refactorings
from projectgit p
  join duplication dup on dup.project = p.id
  join weeks w on p.created_at <= w.start
  left join revisiongit rev on rev.project = p.id and rev.commitTime between w.start and date_add(w.start, interval 7 day)
  left join refactoringgit ref on ref.revision = rev.id
where p.analyzed = 1 and p.running_pid not in ('filtered') and ratio > 0.8
and (ref.refactoringType is null or ref.refactoringType not in ('Merge Method', 'Extract And Move Method', 'Extract Interface', 'Extract Superclass', 'Convert Anonymous Class to Type'))
and p.cloneUrl = '", cloneUrl, "'
and w.start between '2013-01-01' and '2015-01-01'
group by p.id, p.cloneUrl, w.start
order by p.id asc, w.start desc", sep = '')
  rs <- dbSendQuery(con, query)
  data <- fetch(rs, n=-1)
  dbClearResult(rs)
  
  #pdf(file=paste("weeks-", title, ".pdf", sep=''), height=6, width=12, family="sans")
  time <- as.POSIXct(data$time, origin="1970-01-01", tz="GMT")
  
  plot(range(time), range(c(0, 200)), type="n", xlab="Time", ylab="", main=title)
  lines(data$commits ~ time, type="l",col="black")
  lines(data$refactorings ~ time, type="l",col="blue")
  #lines((data$refactorings/data$commits) ~ time, type="l",col="red")
  legend(x='topright', bty='n', lty=c(1,1), legend=c('commits','refactorings'), col=c('black','blue'))
  
  #plot(range(time), range(c(0, 10)), type="n", xlab="Time", ylab="", main=title)
  #lines((data$refactorings/data$commits) ~ time, type="l",col="red")
  
  #plot(data$refactorings ~ time, type="l",col="blue", xlim=range(time), ylim=range(c(data$commits,data$refactorings)))
  #dev.off()
}

pdf(file="weeks.pdf", height=24, width=12, family="sans")
par(mfrow=c(7,3))
refactoringActivityPerWeek(con, 'https://github.com/spring-projects/spring-framework.git', 'spring-framework')
refactoringActivityPerWeek(con, 'https://github.com/apache/cassandra.git', 'apache cassandra')
refactoringActivityPerWeek(con, 'https://github.com/hibernate/hibernate-orm.git', 'hibernate-orm')
refactoringActivityPerWeek(con, 'https://github.com/eclipse/jetty.project.git', 'jetty')

refactoringActivityPerWeek(con, 'https://github.com/apache/camel.git', 'apache camel')
refactoringActivityPerWeek(con, 'https://github.com/k9mail/k-9.git', 'k-9 mail')
refactoringActivityPerWeek(con, 'https://github.com/orientechnologies/orientdb.git', 'orientdb')
refactoringActivityPerWeek(con, 'https://github.com/junit-team/junit.git', 'junit')

refactoringActivityPerWeek(con, 'https://github.com/apache/tomcat.git', 'tomcat')
refactoringActivityPerWeek(con, 'https://github.com/languagetool-org/languagetool.git', 'languagetool')
refactoringActivityPerWeek(con, 'https://github.com/vaadin/vaadin.git', 'vaadin')
refactoringActivityPerWeek(con, 'https://github.com/grails/grails-core.git', 'grails-core')

refactoringActivityPerWeek(con, 'https://github.com/VoltDB/voltdb.git', 'voltdb')
refactoringActivityPerWeek(con, 'https://github.com/k9mail/k-9.git', 'k-9')
refactoringActivityPerWeek(con, 'https://github.com/hazelcast/hazelcast.git', 'hazelcast')
refactoringActivityPerWeek(con, 'https://github.com/rstudio/rstudio.git', 'rstudio')

refactoringActivityPerWeek(con, 'https://github.com/M66B/XPrivacy.git', 'XPrivacy')
refactoringActivityPerWeek(con, 'https://github.com/netty/netty.git', 'netty')
refactoringActivityPerWeek(con, 'https://github.com/igniterealtime/Openfire.git', 'Openfire')
refactoringActivityPerWeek(con, 'https://github.com/cgeo/cgeo.git', 'cgeo')


dev.off()

dbDisconnect(con)
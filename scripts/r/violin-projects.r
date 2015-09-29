library(DBI)
library(xts)
library(vioplot)

con <- dbConnect(RMySQL::MySQL(),
                 default.file = 'c:/.danilofs-refactoring.cnf',
                 groups = 'refactoring')
on.exit(dbDisconnect(con))

query <- "select id, java_files, PERIOD_DIFF('201508', date_format(created_at, '%Y%m')) as months, contributors, commits_count 
          from projectgit where monitoring_enabled = 1"
rs <- dbSendQuery(con, query)
data <- fetch(rs, n=-1)
dbClearResult(rs)

query2 <- "select id, java_files, PERIOD_DIFF('201508', date_format(created_at, '%Y%m')) as months, contributors, commits_count 
from projectgit p where monitoring_enabled = 1 and exists (select * from revisiongit rev where rev.project = p.id and rev.commitTime > '2015-06-01')"
rs2 <- dbSendQuery(con, query2)
data2 <- fetch(rs2, n=-1)
dbClearResult(rs2)

query3 <- "select id, java_files, PERIOD_DIFF('201508', date_format(created_at, '%Y%m')) as months, contributors, commits_count 
from projectgit p where studied = 1"
rs3 <- dbSendQuery(con, query3)
data3 <- fetch(rs3, n=-1)
dbClearResult(rs3)


pdf(file="projects-files.pdf", height=5, width=4, family="sans")
plot(0:1,0:1,type="n",xlim=c(0.5,3.5),ylim=range(log10(data$java_files)),axes=FALSE,ann=FALSE)
vioplot(log10(data$java_files), log10(data2$java_files), log10(data3$java_files), add=TRUE, col="gray")
axis(side=1,at=1:3,labels=c("all","active","studied"))
axis(side=2,at=0:5,labels=format(10^(0:5),big.mark=",", trim=TRUE,scientific=FALSE))
title(ylab="Number of Java files (log scale)")
dev.off()

pdf(file="projects-months.pdf", height=5, width=4, family="sans")
vioplot(data$months, data2$months, data3$months, names=c("all", "active", "studied"), col="gray")
title(ylab="Age (months)")
dev.off()

pdf(file="projects-contributors.pdf", height=5, width=4, family="sans")
# vioplot(data$contributors, data2$contributors, horizontal=FALSE, names=c("all", "active"), col="gray")
plot(0:1,0:1,type="n",xlim=c(0.5,3.5),ylim=range(log10(data$contributors)),axes=FALSE,ann=FALSE)
vioplot(log10(data$contributors), log10(data2$contributors), log10(data3$contributors), add=TRUE, col="gray")
axis(side=1,at=1:3,labels=c("all","active","studied"))
axis(side=2,at=0:5,labels=format(10^(0:5),big.mark=",", trim=TRUE,scientific=FALSE))
title(ylab="Number of contributors (log scale)")
dev.off()

pdf(file="projects-commits.pdf", height=5, width=4, family="sans")
plot(0:1,0:1,type="n",xlim=c(0.5,3.5),ylim=range(log10(data$commits_count)),axes=FALSE,ann=FALSE)
vioplot(log10(data$commits_count), log10(data2$commits_count), log10(data3$commits_count), add=TRUE, col="gray")
axis(side=1,at=1:3,labels=c("all","active","studied"))
axis(side=2,at=0:5,labels=format(10^(0:5),big.mark=",", trim=TRUE,scientific=FALSE))
title(ylab="Number of commits (log scale)")
dev.off()

dbDisconnect(con)


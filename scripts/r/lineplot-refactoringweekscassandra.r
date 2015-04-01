library(RMySQL)
library(xts)

con <- dbConnect(MySQL(),
                 user = 'danilofs',
                 password = 'oefudfs2#',
                 host = 'icse.labsoft.dcc.ufmg.br',
                 dbname='danilofs-refactoring')
on.exit(dbDisconnect(con))

data <- dbReadTable(conn = con, name = 'refactoringweeks')
pdf(file="refactoringweekscassandra.pdf", height=6, width=12, family="sans")
par(mar=c(9,4,1,1))
times <- as.POSIXct(data$Time,origin="1970-01-01",tz="GMT")
plot(data$Commits ~ times, type="l",col="red", xlim=range(times), ylim=range(c(data$Commits,data$Refactorings)))
par(new=T)
plot(data$Refactorings ~ times, type="l",col="green", xlim=range(times), ylim=range(c(data$Commits,data$Refactorings)))
dev.off()
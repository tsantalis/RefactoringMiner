library(RMySQL)

con <- dbConnect(MySQL(),
                 user = 'danilofs',
                 password = 'oefudfs2#',
                 host = 'icse.labsoft.dcc.ufmg.br',
                 dbname='danilofs-refactoring')
on.exit(dbDisconnect(con))

data <- dbReadTable(conn = con, name = 'refactoringtypeproj')
pdf(file="refactoringtypeproj.pdf", height=6, width=12, family="sans")
par(mar=c(9,3,1,1))
boxplot(data[4:14],las=2)
dev.off()
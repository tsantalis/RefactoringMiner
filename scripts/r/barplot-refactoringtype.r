library(RMySQL)

con <- dbConnect(MySQL(),
                 user = 'danilofs',
                 password = 'oefudfs2#',
                 host = 'icse.labsoft.dcc.ufmg.br',
                 dbname='danilofs-refactoring')
on.exit(dbDisconnect(con))

data <- dbReadTable(conn = con, name = 'refactoringtype')
pdf(file="refactoringtype.pdf", height=6, width=12, family="sans")
par(mar=c(9,4,1,1))
barplot(data[["count"]], names.arg=data[["refactoringType"]], las=2)
dev.off()
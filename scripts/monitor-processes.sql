-- Progresso dos Projetos em análise no momento
select 
  p.running_pid as "ID do Processo", p.name as "Projeto Atual", p.cloneUrl as "URL do Repositorio", 
  format(p.size/1000, 1) as "Tamanho (MB)", 
  format(p.commits_count, 0) as "Commits",
  format(100 * (select count(*) from revisiongit rev where rev.project = p.id)/p.commits_count, 1) as "Progresso (%)"
from projectgit p
where p.running_pid not in ('wait', 'filtered', 'error');

-- Progresso Geral
select
  count(p.id) as "Projetos",
  sum(if(p.analyzed = 1, 1, 0)) as "Projetos Analisados",
  format(sum(p.commits_count), 0) as "Commits",
  format((select count(*) from revisiongit rev), 0) as "Commits Analisados",
  format(100 * (select count(*) from revisiongit rev)/sum(p.commits_count), 3) as "Progresso (%)"
from projectgit p
where p.running_pid is null or p.running_pid <> 'filtered';
select 
  p.running_pid as "ID do Processo", 
  p.name as "Projeto Atual", p.cloneUrl as "URL do Repositorio", 
  format(p.size/1000, 1) as "Tamanho (MB)", 
  format(p.commits_count, 0) as "Commits",
  format(100 * (select count(*) from revisiongit rev where rev.project = p.id)/p.commits_count, 1) as "Progresso (%)"
from projectgit p
where p.running_pid <> 'wait';
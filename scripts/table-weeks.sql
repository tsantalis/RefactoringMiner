create table weeks (
  id int auto_increment primary key, 
  start date
);

insert into weeks (start)
values
('2015-03-22'),
(DATE_SUB('2015-03-22',INTERVAL 7 DAY)),
(DATE_SUB('2015-03-22',INTERVAL 14 DAY)),
(DATE_SUB('2015-03-22',INTERVAL 21 DAY)),
(DATE_SUB('2015-03-22',INTERVAL 28 DAY));
DELIMITER $$

DROP FUNCTION IF EXISTS `danilofs-refactoring`.`normalizeSignature` $$
CREATE FUNCTION `normalizeSignature`(input VARCHAR(5000)) RETURNS VARCHAR(255)
BEGIN
DECLARE r VARCHAR(255);
declare tname varchar(255);
declare sep varchar(255);
DECLARE s VARCHAR(5000);
DECLARE c INT;
set r = CONCAT(SUBSTRING_INDEX(input, '(', 1), '(');
set s = SUBSTRING_INDEX(input, '(', -1);
set c = 0;
WHILE LENGTH(s) > 1 and c < 10 DO
  if LOCATE(', ', s) > 0 then
    set tname = SUBSTRING(s, LOCATE(' ', s)+1, LOCATE(', ', s) - LOCATE(' ', s) - 1);
    set s = SUBSTRING(s, LOCATE(', ', s) + 2);
    set sep = ', ';
  else
    set tname = SUBSTRING(s, LOCATE(' ', s)+1, LOCATE(')', s) - LOCATE(' ', s) - 1);
    set s = SUBSTRING(s, LOCATE(')', s) + 2);
    set sep = '';
  end if;
  if (LOCATE('<', tname) > 0) then 
    set tname = concat(SUBSTRING_INDEX(tname, '<', 1), SUBSTRING_INDEX(tname, '>', -1));
  end if;
  set r = concat(r, tname, sep);
  set c = c + 1;
END WHILE;
set r = concat(r, ')');
RETURN r;
END $$

DELIMITER ;  
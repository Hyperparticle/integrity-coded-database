

## The following MySQL script defines a procedure to generate a one-code-per-tuple ICDB
## from an existing database

DROP procedure IF EXISTS `convert_icdb_oct`;

DELIMITER $$

CREATE DEFINER=`root`@`localhost` PROCEDURE `convert_icdb_oct`()
BEGIN
    
  # Creates duplicate database with _SVC columns
	DECLARE done int default false;
    DECLARE t_name CHAR(255);		# Table name
    DECLARE db_name CHAR(255);
    DECLARE icdb_name CHAR(255);

    DECLARE cur1 cursor for SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
                  WHERE table_schema = DATABASE()
                  AND TABLE_TYPE LIKE 'BASE TABLE';
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    SET db_name = DATABASE();
    SET icdb_name = CONCAT(UPPER(DATABASE()), '_ICDB');
    
    SELECT CONCAT('DROP DATABASE IF EXISTS ', icdb_name)
	INTO @sql;
	PREPARE stmt FROM @sql;
	EXECUTE stmt;
	DROP PREPARE stmt;
    
    SELECT CONCAT('CREATE DATABASE ', icdb_name)
	INTO @sql;
	PREPARE stmt FROM @sql;
	EXECUTE stmt;
	DROP PREPARE stmt;
    
	open cur1;
    
	myloop: loop
		fetch cur1 into t_name;
		if done then
			leave myloop;
		end if;
        
        SELECT CONCAT('CREATE TABLE ', icdb_name, '.', t_name, ' LIKE ', db_name, '.', t_name) 
        INTO @sql;
		PREPARE stmt FROM @sql;
		EXECUTE stmt;
        DROP PREPARE stmt;
        
        SELECT CONCAT('ALTER TABLE ', icdb_name, '.', t_name, ' ADD COLUMN `Serial` TEXT NOT NULL')
		INTO @sql;
		PREPARE stmt FROM @sql;
		EXECUTE stmt;
        DROP PREPARE stmt;
        
        SELECT CONCAT('ALTER TABLE ', icdb_name, '.', t_name, ' ADD COLUMN `IC` TEXT NOT NULL')
		INTO @sql;
		PREPARE stmt FROM @sql;
		EXECUTE stmt;
        DROP PREPARE stmt;
	end loop;

	close cur1;
    
END$$

DELIMITER ;

## Call the procedure ##
call convert_icdb_oct();

DROP procedure IF EXISTS `convert_icdb_oct`;
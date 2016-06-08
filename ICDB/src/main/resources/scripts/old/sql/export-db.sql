
## The following MySQL script defines a procedure to export all data in a database,
## one file per table

DROP procedure IF EXISTS `export_db`;

DELIMITER $$

CREATE DEFINER=`root`@`localhost` PROCEDURE `export_db`()
BEGIN
	
  # Creates a .unl file for each table in the database
	DECLARE done int default false;
    DECLARE t_name CHAR(255);		# Table name
    DECLARE dir TEXT;		# Directory to save the files to

    DECLARE cur1 cursor for SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
                  WHERE table_schema = DATABASE()
                  AND TABLE_TYPE LIKE 'BASE TABLE';
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
	SET dir = '~/IntegrityCodedDatabase/ICDB/tmp/db-files/data';
    
	open cur1;
    
	myloop: loop
		fetch cur1 into t_name;
		if done then
			leave myloop;
		end if;
		set @sql = CONCAT('select * ', 
			'into outfile \'', dir, '/', t_name, '.unl\' ', 
            'character set utf8 ',
            'fields terminated by \'|\' ',
            'lines terminated by \'\n\' ',
            'from ', t_name);
		prepare stmt from @sql;
		execute stmt;
		drop prepare stmt;
	end loop;

	close cur1;
    
END$$

DELIMITER ;

## Call the procedure ##
call export_db();

DROP procedure IF EXISTS `export_db`;
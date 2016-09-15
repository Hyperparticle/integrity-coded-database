icdb -c shatuple.json benchmark -q "SELECT * FROM salaries" --fetch LAZY 
icdb -c shafield.json benchmark -q "SELECT * FROM salaries" --fetch LAZY 
icdb -c aestuple.json benchmark -q "SELECT * FROM salaries" --fetch LAZY 
icdb -c aesfield.json benchmark -q "SELECT * FROM salaries" --fetch LAZY 
icdb -c rsatuple.json benchmark -q "SELECT * FROM salaries" --fetch LAZY 
icdb -c rsafield.json benchmark -q "SELECT * FROM salaries" --fetch LAZY 


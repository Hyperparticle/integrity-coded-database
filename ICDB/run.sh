icdb -c shatuple.json benchmark -q "SELECT * FROM salaries" --fetch EAGER --threads 1
icdb -c shafield.json benchmark -q "SELECT * FROM salaries" --fetch EAGER --threads 1
icdb -c aestuple.json benchmark -q "SELECT * FROM salaries" --fetch EAGER --threads 1
icdb -c aesfield.json benchmark -q "SELECT * FROM salaries" --fetch EAGER --threads 1
icdb -c rsatuple.json benchmark -q "SELECT * FROM salaries" --fetch EAGER --threads 1
icdb -c rsafield.json benchmark -q "SELECT * FROM salaries" --fetch EAGER --threads 1


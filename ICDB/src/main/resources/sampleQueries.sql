SELECT * FROM departments;
SELECT dept_name FROM departments;
SELECT dept_name FROM departments WHERE dept_no IN ('d001', 'd002');
SELECT dept_name FROM departments WHERE dept_no > 'd003';
SELECT to_date FROM dept_emp WHERE from_date = '1986-06-26';

INSERT INTO departments VALUES ('d010', 'Manufacturing');
INSERT INTO employees.departments VALUES ('d010', 'Manufacturing'), ('d011', 'Operations');

DELETE FROM departments WHERE dept_no = 'd010';
DELETE FROM departments WHERE dept_no IN ('d010', 'd011');

UPDATE departments SET dept_name = 'Marketing' WHERE dept_no = 'd001';

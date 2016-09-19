# Benchmark Queries

## Include

- Algorithms: AES, RSA, SHA
- Granularity: TUPLE, FIELD

## Queries

- Select
```
select * from salaries limit 125000
select * from salaries limit 250000
select * from salaries limit 500000
select * from salaries limit 1000000
select * from salaries limit 2000000
```

- Delete
```
delete from salaries limit 125000
delete from salaries limit 250000
delete from salaries limit 500000
delete from salaries limit 1000000
delete from salaries limit 2000000
```

- Insert
```
insert into salaries values ()
```

- Update
```
update
```
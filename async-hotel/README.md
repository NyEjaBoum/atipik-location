readme.bici

imp atipik/atipik@localhost:1521/DBCOURS.UNEPH.HT file="atipik0601.dmp" log="import.log" full=y

CREATE USER atipik IDENTIFIED BY atipik;
GRANT CONNECT, RESOURCE, IMP_FULL_DATABASE TO atipik;
ALTER USER atipik QUOTA UNLIMITED ON USERS;


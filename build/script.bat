:: The comments specify which peers should have a copy of the backed up files after all these commands are executed

java src.service.TestApp rmi3 BACKUP heavy_file.txt 2 
::  1,2 
java src.service.TestApp rmi1 BACKUP medium_file.zip 2 
:: 2,3 -> 2 only (latter reclaim)
java src.service.TestApp rmi2 BACKUP medium_file2.zip 5 
:: 1,3 -> 1 only (latter reclaim)
java src.service.TestApp rmi1 BACKUP medium_file3.zip 2 
:: 2,3 (DEL)
java src.service.TestApp rmi2 BACKUP light_file.zip 2 
:: 1,3 -> 1 only (latter reclaim)
java src.service.TestApp rmi2 RESTORE medium_file.zip
java src.service.TestApp rmi2 RESTORE medium_file2.zip
java src.service.TestApp rmi2 BACKUP test_file.rar 1 
:: 1
java src.service.TestApp rmi2 RESTORE medium_file3.zip
java src.service.TestApp rmi3 BACKUP test_file.txt 2 
:: 1,2 (DEL)
java src.service.TestApp rmi3 RECLAIM 0
java src.service.TestApp rmi2 RESTORE test_file.zip
java src.service.TestApp rmi2 RESTORE heavy_file.txt
java src.service.TestApp rmi2 RESTORE light_file.zip
java src.service.TestApp rmi2 RESTORE test_file.txt
java src.service.TestApp rmi2 DELETE medium_file3.zip
java src.service.TestApp rmi2 DELETE test_file.txt
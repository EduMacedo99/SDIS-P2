:: Commands to be run one by one, for better understanding of what happens

java src.service.TestApp rmi3 BACKUP test_big2.pdf 1
java src.service.TestApp rmi3 BACKUP test_big1.pdf 1

java src.service.TestApp rmi1 RECLAIM 4000000

java src.service.TestApp rmi3 RESTORE test_big1.pdf
java src.service.TestApp rmi3 RESTORE test_big2.pdf
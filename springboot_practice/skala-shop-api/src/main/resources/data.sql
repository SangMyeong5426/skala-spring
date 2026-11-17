-- version 컬럼(@Version 낙관적 락)은 SQL로 직접 넣는 행에도 반드시 초기값(0)을 채워야 한다.
-- 비워두면 Hibernate가 이후 UPDATE 시 "현재 버전"을 알 수 없어 NPE(Versioning.increment)가 발생한다.
INSERT INTO product (product_name, product_price, product_stock, version) VALUES ('무선마우스', 15000, 50, 0);
INSERT INTO product (product_name, product_price, product_stock, version) VALUES ('블루투스키보드', 29000, 30, 0);
INSERT INTO product (product_name, product_price, product_stock, version) VALUES ('USB허브', 39000, 20, 0);
INSERT INTO product (product_name, product_price, product_stock, version) VALUES ('27인치 모니터', 259000, 10, 0);
INSERT INTO product (product_name, product_price, product_stock, version) VALUES ('기계식 키보드', 89000, 15, 0);
INSERT INTO product (product_name, product_price, product_stock, version) VALUES ('노트북 파우치', 22000, 40, 0);

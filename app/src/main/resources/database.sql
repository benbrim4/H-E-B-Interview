CREATE DATABASE heb;

GRANT ALL PRIVILEGES ON *.* TO test_user@`%`;

CREATE TABLE `images` (image_id BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT, image_label VARCHAR(50), object_detection BOOLEAN, image LONGBLOB);

CREATE TABLE `image_objects` (image_id BIGINT, object VARCHAR(50), FOREIGN KEY (image_id) REFERENCES images(image_id)  ON DELETE CASCADE ON UPDATE CASCADE);

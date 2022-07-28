CREATE TABLE `images` (image_id BIGINT PRIMARY KEY AUTO_INCREMENT, image_label VARCHAR(50), object_detection BOOLEAN, image BLOB);

CREATE TABLE `image_objects` (image_id BIGINT, object VARCHAR(50), FOREIGN KEY (image_id) REFERENCES images(image_id));

INSERT INTO images VALUES
(1, "image1", true, NULL),
(2, "image2", true, NULL),
(3, "image3", true, NULL),
(4, "image4", true, NULL);

INSERT INTO image_objects VALUES
(1,"cat"),
(2,"dog"),
(2,"cat"),
(3,"dog"),
(3,"cat"),
(4,"airplane");

SELECT images.*, CONCAT('[',GROUP_CONCAT(image_objects.object),']') objects from images INNER JOIN image_objects ON images.image_id = image_objects.image_id GROUP BY image_id;

SELECT images.*, CONCAT('[',GROUP_CONCAT(image_objects.object),']') objects from images INNER JOIN image_objects ON images.image_id = image_objects.image_id GROUP BY image_id having objects like '%dog%' AND objects like '%cat%';

SELECT images.*, CONCAT('[',GROUP_CONCAT(image_objects.object),']') objects from images INNER JOIN image_objects ON images.image_id = image_objects.image_id where images.image_id=2;
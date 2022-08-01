package com.heb.interview;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageSource;
import com.google.protobuf.ByteString;
import com.heb.interview.data.ImageData;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Http;
import io.helidon.common.reactive.Multi;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRow;
import io.helidon.media.multipart.ReadableBodyPart;
import io.helidon.webserver.Routing.Rules;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

public class ImageService implements Service {

    private final DbClient dbClient;
    private final ObjectDetector objectDetector;

    public ImageService(DbClient dbClient) {
        this.dbClient = dbClient;
        this.objectDetector = new ObjectDetector();
    }

    @Override
    public void update(Rules rules) {
        rules
                .post("/", this::processPostRequest)
                .get("/", this::listImages)
                .get("/{imageID}", this::getImage);

    }

    private void listImages(ServerRequest req, ServerResponse res) {

        List<String> objects = req.queryParams().toMap().get("objects");

        if (objects == null || objects.isEmpty()) {
            Multi<ImageData> rows = dbClient.execute(exec -> exec.namedQuery("select-all-images"))
                    .ifEmpty(() -> sendNotFound(res, "No images have been uploaded")).map(it -> it.as(ImageData.class));
            res.send(rows, ImageData.class);
        } else {
            Multi<ImageData> rows = dbClient.execute(exec -> exec.query(getImageQuery(objects)).ifEmpty(() -> {
                System.out.println("empty");
                sendNotFound(res, "not found");
                return;
            }))
                    .map(it -> it.as(ImageData.class))
                    .onError((err) -> sendError(err, res));
            res.send(rows, ImageData.class);

        }
    }

    private void getImage(ServerRequest req, ServerResponse res) {
        String imageId = req.path().param("imageID");

        dbClient.execute(exec -> exec.namedGet("get-image", imageId))
                .thenAccept(opt -> opt.ifPresentOrElse(it -> sendRow(it, res),
                        () -> sendNotFound(res, "Image not found")))
                .exceptionally(throwable -> sendError(throwable, res));

    }

    public void processPostRequest(ServerRequest req, ServerResponse res) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        AtomicReference<String> url = new AtomicReference<>();
        ImageData imageData = new ImageData();
        req.content().asStream(ReadableBodyPart.class)
                .forEach(part -> {
                    System.out.println(part.name());
                    switch (part.name()) {
                        case "img":
                            part.content().map(DataChunk::data).flatMapIterable(Arrays::asList).forEach((buffer) -> {
                                while (buffer.hasRemaining()) {
                                    byteStream.write(buffer.get());
                                }
                            });
                            break;
                        case "imgURL":
                            part.content().as(String.class).thenAccept(imageURL -> {
                                if (!imageURL.isBlank() && isURL(imageURL)) {
                                    url.set(imageURL);
                                }
                            });
                            break;
                        case "label":
                            part.content().as(String.class).thenAccept(label -> {
                                System.out.println(label);
                                if (label.isBlank()) {
                                    imageData.setLabel(UUID.randomUUID().toString());
                                } else {
                                    imageData.setLabel(label);
                                }
                            });
                            break;
                        case "objectDetection":
                            part.content().as(String.class).thenAccept(detectObjects -> {
                                imageData.setObjectDetection(detectObjects.equals("on"));
                            });
                            break;
                        default:
                            // when streaming, unconsumed parts needs to be drained
                            part.drain();
                    }
                })
                .onError(res::send)
                .onComplete(() -> {
                    Image img;
                    if (url.get() != null) {
                        img = Image.newBuilder().setSource(ImageSource.newBuilder().setImageUri(url.get())).build();
                        imageData.setImageUrl(url.get());
                    } else {
                        byte[] imgBytes = byteStream.toByteArray();
                        ByteString byteString = ByteString.copyFrom(imgBytes);
                        imageData.setImageUrl("data:image/png;base64," + Base64.getEncoder().encodeToString(imgBytes));
                        img = Image.newBuilder().setContent(byteString).build();
                    }
                    if (imageData.isObjectDetection()) {
                        imageData.setObjects(objectDetector.detectObjects(img));
                        System.out.println(imageData.getObjects().toString());
                    }

                    dbClient.inTransaction(
                            (exec) -> exec.createNamedInsert("insert-image").namedParam(imageData).execute())
                            .thenRun(() -> {
                                dbClient.inTransaction(exec -> exec.createGet("SELECT LAST_INSERT_ID()").execute())
                                        .thenAccept((result) -> {
                                            result.ifPresent((row) -> {
                                                imageData.setId(Long
                                                        .parseLong(row.column("LAST_INSERT_ID()").as(String.class)));
                                                System.out.println("image ID: " + imageData.getId());
                                                for (String object : imageData.getObjects()) {
                                                    dbClient.inTransaction(exec -> exec.namedInsert("insert-object",
                                                            imageData.getId(), object)).thenAccept((count) -> {
                                                                System.out.println("Inserted " + count + " rows");
                                                            });
                                                }
                                                res.send(imageData);
                                            });
                                        });
                            }).exceptionally(throwable -> sendError(throwable, res));
                }).ignoreElement();
    }

    private String getImageQuery(List<String> objectList) {
        String[] objects = objectList.get(0).split(",");
        StringBuilder objectSearch = new StringBuilder();
        objectSearch.append(
                "SELECT images.*, CONCAT('[',GROUP_CONCAT(image_objects.object),']') objects from images INNER JOIN image_objects ON images.image_id = image_objects.image_id GROUP BY image_id having objects like '%")
                .append(objects[0]).append("%' ");
        for (int i = 1; i < objects.length; i++) {
            objectSearch.append("AND objects like '%").append(objects[i]).append("%' ");
        }
        return objectSearch.toString();
    }

    private boolean isURL(String urlString) {
        try {
            URL url = new URL(urlString);
            url.toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendRow(DbRow row, ServerResponse response) {
        if (row.column("image_id").as(String.class) == null) {
            sendNotFound(response, "No images found matching request");
            return;
        }
        response.send(row.as(ImageData.class));
    }

    private void sendNotFound(ServerResponse response, String message) {
        response.status(Http.Status.NOT_FOUND_404);
        response.send(message);
    }

    private <T> T sendError(Throwable throwable, ServerResponse res) {
        Throwable realCause = throwable;
        if (throwable instanceof CompletionException) {
            realCause = throwable.getCause();
        }
        res.status(Http.Status.INTERNAL_SERVER_ERROR_500);
        res.send("Failed to process request: " + realCause.getClass().getName() + "(" + realCause.getMessage() + ")");
        return null;
    }

}

package com.heb.interview;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionException;

import javax.json.JsonObject;

import com.google.cloud.vision.v1.Image;
import com.google.protobuf.ByteString;

import io.helidon.common.http.Http;
import io.helidon.common.reactive.Multi;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRow;
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
        // dbClient.execute(exec -> exec.namedGet("select-all-images"))
        // .thenAccept(opt -> opt.ifPresentOrElse(it -> sendRow(it, res),
        // () -> sendNotFound(res, "Image not found")))
        // .exceptionally(throwable -> sendError(throwable, res));

        List<String> objects = req.queryParams().toMap().get("objects");

        if (objects.isEmpty()) {
            Multi<JsonObject> rows = dbClient.execute(exec -> exec.namedQuery("select-all-images"))
                    .map(it -> it.as(JsonObject.class));
            res.send(rows, JsonObject.class);
        } else {
            dbClient.execute(exec -> exec.namedGet("search-object", objects.get(0)))
                    .thenAccept(opt -> opt.ifPresentOrElse(it -> sendRow(it, res),
                            () -> sendNotFound(res, "Image not found")))
                    .exceptionally(throwable -> sendError(throwable, res));
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
        try {
            ByteString imgBytes = ByteString.readFrom(new FileInputStream("/Users/benbrim/Desktop/projects/H-E-B-interview/app/src/main/resources/nola.jpeg"));
            Image img = Image.newBuilder().setContent(imgBytes).build();
            
            List<String> objects = objectDetector.detectObjects(img);
            // dbClient.execute(exec -> exec.namedInsert("insert-image", "test", true, null));
            System.out.println(objects.toString());
            res.status(200);
            res.send(objects.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void sendRow(DbRow row, ServerResponse response) {
        response.send(row.as(JsonObject.class));
    }

    protected void sendNotFound(ServerResponse response, String message) {
        response.status(Http.Status.NOT_FOUND_404);
        response.send(message);
    }

    protected <T> T sendError(Throwable throwable, ServerResponse res) {
        Throwable realCause = throwable;
        if (throwable instanceof CompletionException) {
            realCause = throwable.getCause();
        }
        res.status(Http.Status.INTERNAL_SERVER_ERROR_500);
        res.send("Failed to process request: " + realCause.getClass().getName() + "(" + realCause.getMessage() + ")");
        return null;
    }

}

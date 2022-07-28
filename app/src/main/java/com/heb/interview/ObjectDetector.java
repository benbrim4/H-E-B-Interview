package com.heb.interview;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.LocalizedObjectAnnotation;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.protobuf.ByteString;

public class ObjectDetector {

    private ImageAnnotatorClient client;

    public ObjectDetector() {
        try {
            client = ImageAnnotatorClient.create();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> detectObjects(String filePath) throws FileNotFoundException, IOException {
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
        System.out.println(imgBytes.toStringUtf8());
        Image img = Image.newBuilder().setContent(imgBytes).build();

        return detectObjects(img);
    }

    public List<String> detectObjects(Image image) {
        List<String> objects = new ArrayList<>();
        List<AnnotateImageRequest> requests = new ArrayList<>();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(Feature.newBuilder().setType(Type.OBJECT_LOCALIZATION))
                .setImage(image)
                .build();
        requests.add(request);

        // Perform the request
        BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
        List<AnnotateImageResponse> responses = response.getResponsesList();

        // Display the results
        for (AnnotateImageResponse res : responses) {
            for (LocalizedObjectAnnotation entity : res.getLocalizedObjectAnnotationsList()) {
                objects.add(entity.getName());
            }
        }
        return objects;
    }

    public void shutdown() {
        client.close();
    }

}

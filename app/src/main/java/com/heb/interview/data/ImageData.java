package com.heb.interview.data;

import java.util.ArrayList;
import java.util.List;

public class ImageData {

    private Long id = 0L;
    private String label;
    private Boolean objectDetection = false;
    private List<String> objects = new ArrayList<>();
    private String imageUrl;

    public ImageData() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean isObjectDetection() {
        return objectDetection;
    }

    public void setObjectDetection(Boolean objectDetection) {
        this.objectDetection = objectDetection;
    }

    public List<String> getObjects() {
        return objects;
    }

    public void setObjects(List<String> objects) {
        this.objects = objects;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imgData) {
        this.imageUrl = imgData;
    }

}

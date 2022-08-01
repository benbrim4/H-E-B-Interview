package com.heb.interview.data;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.serial.SerialBlob;

import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

public class ImageDataMapper implements DbMapper<ImageData> {

    @Override
    public ImageData read(DbRow row) {
        DbColumn id = row.column("image_id");
        DbColumn label = row.column("image_label");
        DbColumn objectDetection = row.column("object_detection");
        DbColumn objects = row.column("objects");
        DbColumn image = row.column("image");
        ImageData imageData = new ImageData();
        imageData.setId(id.as(Long.class));
        imageData.setLabel(label.as(String.class));
        imageData.setObjectDetection(objectDetection.as(Boolean.class));
        List<String> objectArray = new ArrayList<>();
        String objectsString = objects.as(String.class);
        if (objectsString != null) {
            objectsString = objectsString.replaceAll("^\\[|]$", "");
            objectArray = Arrays.asList(objectsString.split(","));
        }
        imageData.setObjects(objectArray);
        byte[] imgBlob = image.as(byte[].class);
        imageData.setImageUrl(new String(imgBlob));

        return imageData;
    }

    @Override
    public Map<String, ?> toNamedParameters(ImageData value) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", value.getId());
        map.put("label", value.getLabel());
        map.put("objectDetection", value.isObjectDetection());
        try {
            map.put("imgBytes", new SerialBlob(value.getImageUrl().getBytes()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public List<?> toIndexedParameters(ImageData value) {
        List<Object> list = new ArrayList<>();
        list.add(value.getId());
        list.add(value.getLabel());
        list.add(value.isObjectDetection());
        try {
            Blob imgBlob = new SerialBlob(value.getImageUrl().getBytes());
            list.add(imgBlob);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

}

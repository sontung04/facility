package com.example.facility.facility.dto.request;

import lombok.Data;

/**
 * Request body for PUT /api/v1/facilities/rooms/{roomId}/scene.
 * {@code objects} is typed as {@code Object} so Jackson can deserialise any valid
 * JSON value (array, object, null) without needing JsonNode support, which changed
 * in Jackson 3.x (Spring Boot 4).  The service re-serialises it back to a TEXT
 * string for storage.
 */
@Data
public class SaveSceneRequest {
    private Long roomId;
    private Double width;
    private Double height;
    private Double length;
    private Object objects;   // JSON array of SceneObject — kept as Object for Jackson 3 compat
}

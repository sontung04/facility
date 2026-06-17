package com.example.facility.facility.dto.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for GET/PUT /api/v1/facilities/rooms/{roomId}/scene.
 *
 * {@code objects} is annotated with {@code @JsonRawValue} so that the stored
 * JSON string (e.g. {@code [{"id":"…",…}]}) is embedded verbatim in the
 * response body instead of being double-encoded as a string literal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneResponse {
    private Long roomId;
    private Double width;
    private Double height;
    private Double length;

    @JsonRawValue
    private String objects;     // raw JSON array

    private LocalDateTime updatedAt;
}

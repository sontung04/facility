package com.example.facility.facility;

/** Lightweight projection of a Device for use by other modules. */
public record DeviceInfo(Long id, String deviceCode, Long roomId) {}

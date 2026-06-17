package com.example.facility.facility;

/**
 * Public API of the Facility module.
 * Other modules must go through this interface — never inject facility repositories directly.
 */
public interface FacilityApi {

    /** Returns device info, or throws RESOURCE_NOT_FOUND if the device does not exist. */
    DeviceInfo getDevice(Long deviceId);

    /** Returns category info, or throws RESOURCE_NOT_FOUND if the category does not exist. */
    CategoryInfo getCategory(Long categoryId);

    /** Returns true if the given manager is responsible for the given room. */
    boolean isManagerOfRoom(Long roomId, Long managerId);
}

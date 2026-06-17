package com.example.facility.facility.service;

import com.example.facility.facility.CategoryInfo;
import com.example.facility.facility.DeviceInfo;
import com.example.facility.facility.FacilityApi;
import com.example.facility.facility.model.Category;
import com.example.facility.facility.model.Device;
import com.example.facility.facility.repository.CategoryRepository;
import com.example.facility.facility.repository.DeviceRepository;
import com.example.facility.facility.repository.RoomRepository;
import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FacilityApiImpl implements FacilityApi {

    private final DeviceRepository deviceRepository;
    private final CategoryRepository categoryRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public DeviceInfo getDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        return new DeviceInfo(device.getId(), device.getDeviceCode(), device.getRoom().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryInfo getCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        return new CategoryInfo(category.getId(), category.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isManagerOfRoom(Long roomId, Long managerId) {
        return roomRepository.existsByIdAndManagerId(roomId, managerId);
    }
}

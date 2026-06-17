package com.example.facility.facility.repository;

import com.example.facility.facility.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceCode(String deviceCode);

    List<Device> findByRoomId(Long roomId);

    List<Device> findByRoomIdAndEnabled(Long roomId, Boolean enabled);
}


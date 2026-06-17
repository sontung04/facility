package com.example.facility.facility.repository;

import com.example.facility.facility.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByBuildingId(Long buildingId);

    List<Room> findByBuildingIdAndFloorNumber(Long buildingId, Integer floorNumber);

    /** All rooms managed by a given user — used for MANAGER's "my rooms" view. */
    List<Room> findByManagerId(Long managerId);

    /** Used by TicketService to enforce the room-manager restriction. */
    boolean existsByIdAndManagerId(Long roomId, Long managerId);
}


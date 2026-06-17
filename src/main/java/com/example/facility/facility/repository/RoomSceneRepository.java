package com.example.facility.facility.repository;

import com.example.facility.facility.model.RoomScene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomSceneRepository extends JpaRepository<RoomScene, Long> {

    Optional<RoomScene> findByRoom_Id(Long roomId);

    boolean existsByRoom_Id(Long roomId);

    void deleteByRoom_Id(Long roomId);
}

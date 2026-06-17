package com.example.facility.dispatch.service;

import com.example.facility.dispatch.DispatchApi;
import com.example.facility.dispatch.repository.TechnicianSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DispatchApiImpl implements DispatchApi {

    private final TechnicianSkillRepository technicianSkillRepository;

    @Override
    @Transactional(readOnly = true)
    public long getTotalTechnicianCount() {
        return technicianSkillRepository.count();
    }
}

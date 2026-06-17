package com.example.facility.identity.service;

import com.example.facility.identity.IdentityApi;
import com.example.facility.identity.UserInfo;
import com.example.facility.identity.model.User;
import com.example.facility.identity.model.UserRole;
import com.example.facility.identity.repository.UserRepository;
import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IdentityApiImpl implements IdentityApi {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Long getUserIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new WebException(ErrorCode.USER_NOT_FOUND))
                .getId();
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfo findById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WebException(ErrorCode.USER_NOT_FOUND));
        return toUserInfo(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserInfo> findAllById(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(this::toUserInfo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findUserIdsByRole(String role) {
        UserRole userRole = UserRole.valueOf(role);
        return userRepository.findByRole(userRole).stream()
                .map(User::getId)
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private UserInfo toUserInfo(User user) {
        return new UserInfo(user.getId(), user.getUsername(), user.getRole().name());
    }
}

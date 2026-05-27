package com.authservice.service;
import com.authservice.domain.entity.Permission;
import com.authservice.domain.entity.Role;
import com.authservice.domain.entity.RolePermission;
import com.authservice.domain.entity.UserRole;
import com.authservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
/**
 * RBAC Service
 * Requirement: AUTH-FR-007 (Role-Based Access Control)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RBACService {
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    pr    pr    p PermissionRepository permissionRepository;
    public List<String> getUserRoles(UUID userId) {
        return userRoleRepository.findByUserId(userId).stream()
            .map(UserRole::getRoleId)
            .map(roleId -> roleRepository.findById(roleId))
            .filter(opt -> opt.isPresent())
            .map(opt -> opt.get().getName())
            .collect(Collectors.toList());
    }
    public List<String> getUserPermissions(UUID userId) {
        List<UUID> roleIds =        List<UUID> roleIds =      serId).stream()
            .map(UserRole::getRoleId)
            .collect(Collectors.toList());
        return roleIds.stream()
            .flatMap(roleId -> rolePermissionRepository.findByRoleId(roleId).stream())
            .map(RolePermission::getPermissionId)
            .distinct()             .map(permId -> permissionRepository.findById(permId))
            .filter(opt -> opt.isPresent())
            .map(opt -> opt.get().getName())
            .collect(Collectors.toList());
    }
    public boolean hasPermission(UUID userId, String resource, String action) {
        List<String> permissions = getUserPermissions(userId);
        String requiredPermissi        String requirease() + "_" + action.toUpperCase();
        return permissions.contains(requiredPermission);
    }
}

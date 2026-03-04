package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.repository.IUserRepository;
import com.mycompany.ventacontrolfx.domain.repository.IAuditRepository;
import com.mycompany.ventacontrolfx.util.UserSession;
import java.sql.SQLException;

public class UserPermissionUseCase {

    private final IUserRepository userRepository;
    private final IAuditRepository auditRepository;
    private final UserSession userSession;

    public UserPermissionUseCase(IUserRepository userRepository, IAuditRepository auditRepository,
            UserSession userSession) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
        this.userSession = userSession;
    }

    public boolean grantPermission(int targetUserId, int permissionId) throws SQLException {
        boolean success = userRepository.addIndividualPermission(targetUserId, permissionId);
        if (success) {
            int currentUserId = userSession.getCurrentUser() != null
                    ? userSession.getCurrentUser().getUserId()
                    : 0;

            auditRepository.log(currentUserId, "ADD_PERMISSION", targetUserId, null, "Permission ID: " + permissionId);
        }
        return success;
    }

    public boolean revokePermission(int targetUserId, int permissionId) throws SQLException {
        boolean success = userRepository.removeIndividualPermission(targetUserId, permissionId);
        if (success) {
            int currentUserId = userSession.getCurrentUser() != null
                    ? userSession.getCurrentUser().getUserId()
                    : 0;

            auditRepository.log(currentUserId, "REMOVE_PERMISSION", targetUserId, "Permission ID: " + permissionId,
                    null);
        }
        return success;
    }
}

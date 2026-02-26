package com.mycompany.ventacontrolfx.util;

import com.mycompany.ventacontrolfx.model.User;

/**
 * Manages the current user session.
 * Now injectable via ServiceContainer.
 */
public class UserSession {
    private User currentUser;

    public UserSession() {
        // Public constructor for ServiceContainer
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}

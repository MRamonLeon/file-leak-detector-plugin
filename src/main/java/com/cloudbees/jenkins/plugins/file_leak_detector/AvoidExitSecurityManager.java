package com.cloudbees.jenkins.plugins.file_leak_detector;

import java.security.Permission;

public class AvoidExitSecurityManager extends SecurityManager {
    @Override
    public void checkExit(int status) {
        // Avoid System.exit() from the file_leak_detector library that ends up in killing Jenkins.
        // For example, when you fill an invalid argument before launching the File Leak Detector.
        throw new IllegalExitSecurityException(String.format("The thread %s tryed to exit with %s return code", Thread.currentThread().getName(), status));
    }

    @Override public void checkPermission(Permission perm) {
        // Allow all permissions
    }
}

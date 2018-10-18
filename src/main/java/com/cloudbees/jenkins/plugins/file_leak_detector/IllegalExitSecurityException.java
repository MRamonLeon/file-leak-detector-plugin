package com.cloudbees.jenkins.plugins.file_leak_detector;

public class IllegalExitSecurityException extends SecurityException {
    public IllegalExitSecurityException(String message) {
        super(message);
    }
}

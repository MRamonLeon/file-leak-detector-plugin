package com.cloudbees.jenkins.plugins.file_leak_detector;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

class AvoidExitAndDelegateSecurityManager extends SecurityManager {
    private SecurityManager previousSecurityManager;
    
    public AvoidExitAndDelegateSecurityManager(SecurityManager previousSecurityManager) {
        if(previousSecurityManager == null) {
            throw new NullPointerException("The previous security manager should not be null");
        }
        this.previousSecurityManager = previousSecurityManager;
    }

    @Override
    public boolean getInCheck() {
        return previousSecurityManager.getInCheck();
    }

    @Override
    public Object getSecurityContext() {
        return previousSecurityManager.getSecurityContext();
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        previousSecurityManager.checkPermission(perm, context);
    }

    @Override
    public void checkCreateClassLoader() {
        previousSecurityManager.checkCreateClassLoader();
    }

    @Override
    public void checkAccess(Thread t) {
        previousSecurityManager.checkAccess(t);
    }

    @Override
    public void checkAccess(ThreadGroup g) {
        previousSecurityManager.checkAccess(g);
    }

    @Override
    public void checkExec(String cmd) {
        previousSecurityManager.checkExec(cmd);
    }

    @Override
    public void checkLink(String lib) {
        previousSecurityManager.checkLink(lib);
    }

    @Override
    public void checkRead(FileDescriptor fd) {
        previousSecurityManager.checkRead(fd);
    }

    @Override
    public void checkRead(String file) {
        previousSecurityManager.checkRead(file);
    }

    @Override
    public void checkRead(String file, Object context) {
        previousSecurityManager.checkRead(file, context);
    }

    @Override
    public void checkWrite(FileDescriptor fd) {
        previousSecurityManager.checkWrite(fd);
    }

    @Override
    public void checkWrite(String file) {
        previousSecurityManager.checkWrite(file);
    }

    @Override
    public void checkDelete(String file) {
        previousSecurityManager.checkDelete(file);
    }

    @Override
    public void checkConnect(String host, int port) {
        previousSecurityManager.checkConnect(host, port);
    }

    @Override
    public void checkConnect(String host, int port, Object context) {
        previousSecurityManager.checkConnect(host, port, context);
    }

    @Override
    public void checkListen(int port) {
        previousSecurityManager.checkListen(port);
    }

    @Override
    public void checkAccept(String host, int port) {
        previousSecurityManager.checkAccept(host, port);
    }

    @Override
    public void checkMulticast(InetAddress maddr) {
        previousSecurityManager.checkMulticast(maddr);
    }

    @Override
    public void checkMulticast(InetAddress maddr, byte ttl) {
        previousSecurityManager.checkMulticast(maddr, ttl);
    }

    @Override
    public void checkPropertiesAccess() {
        previousSecurityManager.checkPropertiesAccess();
    }

    @Override
    public void checkPropertyAccess(String key) {
        previousSecurityManager.checkPropertyAccess(key);
    }

    @Override
    public boolean checkTopLevelWindow(Object window) {
        return previousSecurityManager.checkTopLevelWindow(window);
    }

    @Override
    public void checkPrintJobAccess() {
        previousSecurityManager.checkPrintJobAccess();
    }

    @Override
    public void checkSystemClipboardAccess() {
        previousSecurityManager.checkSystemClipboardAccess();
    }

    @Override
    public void checkAwtEventQueueAccess() {
        previousSecurityManager.checkAwtEventQueueAccess();
    }

    @Override
    public void checkPackageAccess(String pkg) {
        previousSecurityManager.checkPackageAccess(pkg);
    }

    @Override
    public void checkPackageDefinition(String pkg) {
        previousSecurityManager.checkPackageDefinition(pkg);
    }

    @Override
    public void checkSetFactory() {
        previousSecurityManager.checkSetFactory();
    }

    @Override
    public void checkMemberAccess(Class<?> clazz, int which) {
        previousSecurityManager.checkMemberAccess(clazz, which);
    }

    @Override
    public void checkSecurityAccess(String target) {
        previousSecurityManager.checkSecurityAccess(target);
    }

    @Override
    public ThreadGroup getThreadGroup() {
        return previousSecurityManager.getThreadGroup();
    }

    @Override
    public void checkExit(int status) {
        // Avoid System.exit() from the file_leak_detector library that ends up in killing Jenkins.
        // For example, when you fill an invalid argument before launching the File Leak Detector.
        throw new IllegalExitSecurityException(String.format("The thread %s tryed to exit with %s return code", Thread.currentThread().getName(), status));
    }

    @Override
    public void checkPermission(Permission perm) {
        previousSecurityManager.checkPermission(perm);
    }
}
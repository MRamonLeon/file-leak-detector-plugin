package com.cloudbees.jenkins.plugins.file_leak_detector;

import hudson.Extension;
import hudson.Util;
import hudson.model.Hudson;
import hudson.model.ManagementLink;
import hudson.os.PosixAPI;
import hudson.remoting.Which;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.io.IOUtils;
import org.kohsuke.file_leak_detector.Listener;
import org.kohsuke.file_leak_detector.Main;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerResponse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class FileHandleDump extends ManagementLink {
    private static Object fileLeadDetectorLock = new Object();

    public String getIconFileName() {
        return "help.png";
    }

    public String getDisplayName() {
        return "Open File Handles";
    }

    public String getUrlName() {
        return "file-handles";
    }

    @Override
    public String getDescription() {
        return "Monitor the current open file handles on the master JVM";
    }

    /**
     * Dumps the currently opened files.
     */
    public HttpResponse doIndex(StaplerResponse response) throws Exception {
        Hudson.getInstance().checkPermission(Hudson.ADMINISTER);

        Class<?> listener = loadListener();

        if (listener==null) {
            return HttpResponses.forwardToView(this,"_notRunning");
        }

        response.setContentType("text/plain;charset=UTF-8");
        listener.getMethod("dump", Writer.class).invoke(null, response.getWriter());
        return null;
    }
    
    /**
     * Activates the file leak detector.
     */
    @RequirePOST
    public HttpResponse doActivate(@QueryParameter String opts) throws Exception {
        Hudson.getInstance().checkPermission(Hudson.ADMINISTER);

        if (loadListener()!=null)
            return HttpResponses.plainText("File leak detector is already activated");

        // to activate, we need to use the JVM attach API, which internally uses JNI.
        // so if someone else tries to do the same (by creating a new classloader that loads tools.jar),
        // either we or they will fail. To avoid it, we'll launch a separate process and have that install
        // the agent
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(new File(System.getProperty("java.home"),"bin/java"))
            .add("-jar")
            .add(Which.jarFile(Main.class))
            .add(PosixAPI.jnr().getpid())
            .add(Util.fixEmpty(opts));

        // Avoid that a second thread assigns to the oldSecurityManager the new one established by the first thread
        // which will end up in the SecurityManager restored at the end being the new one, not the original. And
        // therefore won't be any way to finish Jenkins because the checkExit of the restored Security Manager is
        // avoiding it. Even with the if (loadListener() != null) a second thread could enter here before the first one
        // set the listener, when they both are in the if (loadListener() != null) being it yet null.
        synchronized (fileLeadDetectorLock) {
            // Create a Security Manager to catch the System.exit from the process. This Security Manager delegates in
            // the old one, except the checkExit method.
            SecurityManager oldSecurityManager = System.getSecurityManager();

            if (oldSecurityManager == null) {
                // Allow all except the exit
                System.setSecurityManager(new AvoidExitSecurityManager());
            } else {
                // Delegate to the old security manager except the exit
                System.setSecurityManager(new AvoidExitAndDelegateSecurityManager(oldSecurityManager));
            }

            Process p = new ProcessBuilder(args.toCommandArray())
                    .redirectErrorStream(true)
                    .start();

            p.getOutputStream().close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(p.getInputStream(),baos);
            IOUtils.closeQuietly(p.getInputStream());
            IOUtils.closeQuietly(p.getErrorStream());

            p.waitFor();

            // Restore the former Security Manager
            System.setSecurityManager(oldSecurityManager);

            // We don't know whether there was an error or not, so just output the response, without assuming success or
            // failure
            return HttpResponses.plainText("Activated file leak detector. Output: \n" + baos.toString("UTF-8"));
        }
    }

    /**
     * Obtains the reference to the {@link Listener} class.
     *
     * We need to use reflection and load it from the system classloader, because we bundle it as a dependency.
     * Servlet's "child first" classloading means non-reflective use will resolve to our own copy in plugin
     * classloader, and not from the running agent.
     *
     * @return
     *      null if the agent isn't running.
     */
    private Class<?> loadListener() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        try {
            Class<?> listener = ClassLoader.getSystemClassLoader().loadClass("org.kohsuke.file_leak_detector.Listener");
            boolean isAgentInstalled = (Boolean)listener.getMethod("isAgentInstalled").invoke(null);
            if (!isAgentInstalled)  return null;
            return listener;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}

package com.hms;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;

/**
 * cr-java-0107: Embedded Tomcat launcher.
 *
 * <p>Replaces the external WAR deployment model with a self-contained executable
 * JAR that starts an embedded Apache Tomcat instance. This enables the application
 * to be deployed as a container on Amazon ECS, EKS, or AWS Fargate without
 * requiring an external application server.</p>
 *
 * <p>The HTTP port is read from the {@code PORT} environment variable (default 8080)
 * following the 12-factor app principle of externalising configuration.</p>
 *
 * <p>Usage: {@code java -jar Doctor-Patient-Portal-jar-with-dependencies.jar}</p>
 */
public class Main {

    /** Environment variable name for the HTTP listen port (12-factor config). */
    private static final String PORT_ENV_VAR = "PORT";

    /** Default HTTP port when {@code PORT} environment variable is not set. */
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws LifecycleException {

        // ----------------------------------------------------------------
        // Resolve HTTP port from environment variable (12-factor principle)
        // ----------------------------------------------------------------
        int port = DEFAULT_PORT;
        String portEnv = System.getenv(PORT_ENV_VAR);
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                port = Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException e) {
                System.err.println("[Main] Invalid PORT value '" + portEnv
                        + "', falling back to default " + DEFAULT_PORT);
            }
        }

        // ----------------------------------------------------------------
        // Configure embedded Tomcat
        // ----------------------------------------------------------------
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(createTempDir("tomcat-base"));

        // HTTP/1.1 connector
        Connector connector = new Connector();
        connector.setPort(port);
        tomcat.setConnector(connector);

        // ----------------------------------------------------------------
        // Locate the webapp root.
        // When running from the fat JAR the webapp resources are on the
        // classpath; when running from the IDE they live under src/main/webapp.
        // ----------------------------------------------------------------
        String webappDirLocation = "src/main/webapp";
        File webappDir = new File(webappDirLocation);
        String docBase = webappDir.exists()
                ? webappDir.getAbsolutePath()
                : new File(".").getAbsolutePath();

        Context ctx = tomcat.addWebapp("", docBase);

        // Add compiled classes and resources from the classpath so that
        // servlets registered in web.xml are found when running from the JAR.
        WebResourceRoot resources = new StandardRoot(ctx);
        File classesDir = new File("target/classes");
        if (classesDir.exists()) {
            resources.addPreResources(new DirResourceSet(
                    resources,
                    "/WEB-INF/classes",
                    classesDir.getAbsolutePath(),
                    "/"));
        }
        ctx.setResources(resources);

        // ----------------------------------------------------------------
        // Start Tomcat and await requests
        // ----------------------------------------------------------------
        tomcat.start();
        System.out.println("[Main] Doctor-Patient-Portal started on port " + port);
        tomcat.getServer().await();
    }

    // ----------------------------------------------------------------
    // Helper: create a temporary base directory for Tomcat work files
    // ----------------------------------------------------------------
    private static String createTempDir(String prefix) {
        try {
            File tempDir = File.createTempFile(prefix, "");
            tempDir.delete();
            tempDir.mkdir();
            tempDir.deleteOnExit();
            return tempDir.getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temp directory for Tomcat", e);
        }
    }
}

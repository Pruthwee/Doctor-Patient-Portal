package com.hms;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;

/**
 * Application entry point for the executable JAR with embedded Tomcat.
 *
 * Migrated from WAR packaging to executable JAR with embedded servlet
 * container to enable container deployment on Amazon ECS, EKS, or
 * AWS Fargate without requiring an external application server.
 *
 * Server port is resolved from the PORT environment variable (default 8080)
 * following 12-factor app principles.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        // Resolve port from environment variable (ECS / Fargate task definition)
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector(); // Trigger connector creation

        // Locate the webapp directory
        String webappDir = new File("src/main/webapp").getAbsolutePath();
        Context ctx = tomcat.addWebapp("", webappDir);

        // Add compiled classes to the context
        File additionWebInfClasses = new File("target/classes");
        StandardRoot resources = new StandardRoot(ctx);
        resources.addPreResources(
                new DirResourceSet(resources, "/WEB-INF/classes",
                        additionWebInfClasses.getAbsolutePath(), "/"));
        ctx.setResources(resources);

        tomcat.start();
        System.out.println("[HMS] Doctor-Patient Portal started on port " + port);
        tomcat.getServer().await();
    }
}

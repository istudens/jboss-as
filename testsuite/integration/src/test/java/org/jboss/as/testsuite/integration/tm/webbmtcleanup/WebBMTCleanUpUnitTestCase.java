/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.testsuite.integration.tm.webbmtcleanup;


import org.apache.catalina.connector.Connector;
import org.apache.coyote.memory.MemoryProtocolHandler;
import org.apache.tomcat.util.buf.ByteChunk;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.web.WebServer;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;

/**
 * Tests for BMT CleanUp in a webapp
 *
 * @author adrian@jboss.com
 * @author istudens@redhat.com
 */
@RunWith(Arquillian.class)
public class WebBMTCleanUpUnitTestCase {
    private static final Logger log = Logger.getLogger(WebBMTCleanUpUnitTestCase.class);

    public static final String ARCHIVE_NAME = "bmtcleanuptest";
    public static final String BASE_URI     = "/" + ARCHIVE_NAME;

    @Inject
    public ServiceContainer serviceContainer;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war")
                .addPackage(WebBMTCleanUpUnitTestCase.class.getPackage())
                .addAsWebResource("tm/webbmtcleanup/test1.jsp")
                .addAsWebResource("tm/webbmtcleanup/test2.jsp")
                .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
    }


    @Test
//    @Ignore
    public void testWithJSPs() throws Exception {
        doTest(BASE_URI + "/test1.jsp", BASE_URI + "/test2.jsp");
    }

    @Test
    public void testWithServlets() throws Exception {
        doTest(BASE_URI + "/testservlet1", BASE_URI + "/testservlet2");
    }


    private void doTest(String uri1, String uri2) throws Exception {
        // get WebServer service
        ServiceController<?> controller = serviceContainer.getService(WebSubsystemServices.JBOSS_WEB);
        Service<WebServer> service = (Service<WebServer>) controller.getService();
        WebServer webServer = service.getValue();
        // register new connector into it
        Connector connector = new Connector("org.apache.coyote.memory.MemoryProtocolHandler");
        MemoryProtocolHandler handler = (MemoryProtocolHandler) connector.getProtocolHandler();
        webServer.addConnector(connector);
        // perform test methods on servlets
        try {

            log.info("tx status = " + getTxStatus());

            ByteChunk input = new ByteChunk(1024);
            ByteChunk output = new ByteChunk(1024);
            org.apache.coyote.Request req = new org.apache.coyote.Request();
            req.decodedURI().setString(uri1);
            req.method().setString("GET");
            org.apache.coyote.Response resp = new org.apache.coyote.Response();
            handler.process(req, input, resp, output);
            if (resp.getStatus() != 200)
                throw new Error(output.toString());

            log.info("tx status after test1 = " + getTxStatus());

            input = new ByteChunk(1024);
            output = new ByteChunk(1024);
            req = new org.apache.coyote.Request();
            req.decodedURI().setString(uri2);
            req.method().setString("GET");
            resp = new org.apache.coyote.Response();
            handler.process(req, input, resp, output);
            if (resp.getStatus() != 200)
                throw new Error(output.toString());

            log.info("tx status after test2 = " + getTxStatus());

        } finally {
            try {
                connector.stop();
            } finally {
                connector.destroy();
            }
        }
    }

    private int getTxStatus() {
        try {
            TransactionManager tm = (TransactionManager) new InitialContext().lookup("java:jboss/TransactionManager");
            return tm.getStatus();
        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

}

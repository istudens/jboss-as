/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.cli;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DeployEarTestCase extends AbstractCliTestBase {

    private static File earFile;

    private static String expectedResponse = "you should be able to read this content";

    @ArquillianResource URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(DeployEarTestCase.class);
        return ja;
    }

    @BeforeClass
    public static void before() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "wardeployment0.war");
        war.addClass(EarServlet.class);

//        JavaArchive warjar = ShrinkWrap.create(JavaArchive.class, "warlib.jar");
//        warjar.add(new StringAsset(expectedResponse), "jar-info.txt");
//        war.addAsLibraries(warjar);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        jar.add(new StringAsset(expectedResponse), "jar-info.txt");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "eardeployment0.ear");
        ear.addAsModule(war);
        ear.addAsLibraries(jar);

        String tempDir = TestSuiteEnvironment.getTmpDir();
        earFile = new File(tempDir + File.separator + ear.getName());
        new ZipExporterImpl(ear).exportTo(earFile, true);

        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        earFile.delete();
        AbstractCliTestBase.closeCLI();
    }

    @Test
    public void testDeployUndeploy() throws Exception {
        testDeploy();
        testUndeploy();
    }

    public void testDeploy() throws Exception {

        // deploy to server
        cli.sendLine("deploy " + earFile.getAbsolutePath());

        // check deployment
        String response = HttpRequest.get(getBaseURL(url) + "wardeployment0/EarServlet", 1000, 10, TimeUnit.SECONDS);
        assertEquals(expectedResponse, response);
    }

    public void testUndeploy() throws Exception {

        //undeploy
        cli.sendLine("undeploy eardeployment0.ear");

        // check undeployment
        assertTrue(checkUndeployed(getBaseURL(url) + "wardeployment0/EarServlet"));
    }

}

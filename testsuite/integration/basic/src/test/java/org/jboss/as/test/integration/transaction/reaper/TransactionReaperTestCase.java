/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.transaction.reaper;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBTransactionRolledbackException;
import javax.jms.*;
import javax.naming.InitialContext;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(Arquillian.class)
public class TransactionReaperTestCase {
    private static final Logger log = Logger.getLogger(TransactionReaperTestCase.class);

    private static final String persistenceXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\">\n" +
                    "   <persistence-unit name=\"reaper-tests\">\n" +
                    "       <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>\n" +
                    "       <properties>\n" +
                    "           <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>\n" +
                    "       </properties>\n" +
                    "   </persistence-unit>\n" +
                    "</persistence>";

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test-reaper-rollback.jar")
                .addPackage(TransactionReaperTestCase.class.getPackage())
                .addAsManifestResource(new StringAsset(persistenceXml), "persistence.xml")
                .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
    }

    @ArquillianResource
    InitialContext ctx;

    @Test
    public void testReaperRollback() throws Throwable {
        ReaperTestBeanLocal testBean = (ReaperTestBeanLocal) ctx.lookup("java:module/ReaperTestBean!org.jboss.as.test.integration.transaction.reaper.ReaperTestBeanLocal");
        ReaperTestEntity initEntity = testBean.initEntity();
        log.info("initEntity = " + initEntity);
        try {
            testBean.testRollback();
        } catch (EJBTransactionRolledbackException expected) {
        }
        ReaperTestEntity afterEntity = testBean.getEntity();
        log.info("afterEntity = " + afterEntity);
        assertEquals("Rollback expected", initEntity.getA(), afterEntity.getA());
    }

    @Test
    public void testReaperRollbackWithJMS() throws Throwable {
        ReaperTestBeanLocal testBean = (ReaperTestBeanLocal) ctx.lookup("java:module/ReaperTestBean!org.jboss.as.test.integration.transaction.reaper.ReaperTestBeanLocal");
        ReaperTestEntity initEntity = testBean.initEntity();
        log.info("initEntity = " + initEntity);
        try {
            testBean.testRollbackWithJMS();
        } catch (EJBTransactionRolledbackException expected) {
        }
        ReaperTestEntity afterEntity = testBean.getEntity();
        log.info("afterEntity = " + afterEntity);
        assertEquals("Rollback expected", initEntity.getA(), afterEntity.getA());
        String message = checkMessageResult();
        assertNull("Expected null message", message);
    }


    private String checkMessageResult() {
        String receivedMessage = null;

        Connection connection = null;
        try {
            ConnectionFactory connectionFactory = (ConnectionFactory) ctx.lookup("java:/JmsXA");

            Queue crashRecoveryQueue = (Queue) ctx.lookup(ReaperTestBean.TEST_QUEUE_JNDI_NAME);

            connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(crashRecoveryQueue);

            connection.start();

            log.info("waiting to receive a message from " + ReaperTestBean.TEST_QUEUE_JNDI_NAME + "...");
            TextMessage message = (TextMessage) consumer.receive(5 * 1000);

            if (message != null) {
                receivedMessage = message.getText();
                log.debug("received message: " + receivedMessage);
            }
        } catch (Exception e) {
            log.warn("Error in receiving a message:", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {}
            }
        }

        return receivedMessage;
    }


}

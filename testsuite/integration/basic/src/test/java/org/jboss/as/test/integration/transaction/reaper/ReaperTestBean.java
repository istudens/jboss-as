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

import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.concurrent.TimeUnit;

@Stateless
@TransactionTimeout(value=5, unit=TimeUnit.SECONDS)
public class ReaperTestBean implements ReaperTestBeanLocal {
    private static final Logger log = Logger.getLogger(ReaperTestBean.class);

    public static final int TEST_ENTITY_INIT_VALUE  = 1;
    private static final String TEST_ENTITY_KEY     = "reaper-rollback-test";

    public static final String TEST_QUEUE_NAME      = "queue/testQueue";
    public static final String TEST_QUEUE_JNDI_NAME = "java:jboss/" + TEST_QUEUE_NAME;

    @PersistenceContext
    EntityManager em;

    @EJB
    ReaperTestBeanLocal bean;

    public void testRollback() throws Throwable {
        log.info("first increase");
        bean.increaseEntity();
        log.info("waiting for reaper");
        Thread.sleep(15 * 1000);
        log.info("second increase");
        bean.increaseEntity();
    }

    public void testRollbackWithJMS() throws Throwable {
        log.info("first increase");
        bean.increaseEntity();
        log.info("waiting for reaper");
        Thread.sleep(15 * 1000);
        log.info("second increase");
        bean.sendMessage();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ReaperTestEntity initEntity() {
        ReaperTestEntity entity = getEntity();
        if (entity == null) {
            entity = new ReaperTestEntity(TEST_ENTITY_KEY, TEST_ENTITY_INIT_VALUE);
            em.persist(entity);
        } else {
            entity.setA(TEST_ENTITY_INIT_VALUE);
        }
        return entity;
    }

    public void increaseEntity() {
        ReaperTestEntity entity = getEntity();
        entity.setA(entity.getA() + 1);
        log.info("entity = " + entity);
    }

    public ReaperTestEntity getEntity() {
        return em.find(ReaperTestEntity.class, TEST_ENTITY_KEY);
    }


    public void sendMessage() throws NamingException, JMSException {
        InitialContext ic = null;
        Connection conn = null;
        try {
            ic = new InitialContext();

            ConnectionFactory connectionFactory = (ConnectionFactory) ic.lookup("java:/JmsXA");
            Queue testQueue = (Queue) ic.lookup(TEST_QUEUE_JNDI_NAME);

            conn = connectionFactory.createConnection();
            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(testQueue);

            producer.send(session.createTextMessage("sending of this message should be rolled back"));

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignored) {
                }
            }
            if (ic != null) {
                try {
                    ic.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

}

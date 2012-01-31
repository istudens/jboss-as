package org.jboss.as.test.manual;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.junit.*;
import org.junit.runner.RunWith;

/**
 * AS7-1415 Ensures injection of the {@link ManagementClient} is working correctly in Arquillian manual-mode.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class InjectManagementClientManualTestCase {

    private static final String CONTAINER = "jboss";

    @ArquillianResource
    private ContainerController controller;

    @ArquillianResource
    private ManagementClient managementClient;

    @Before
    public void setUp() throws Exception {
        controller.start(CONTAINER);
    }

    @After
    public void tearDown() throws Exception {
        controller.stop(CONTAINER);
    }

    @Test
    public void ensureManagementClientInjected() {
        Assert.assertNotNull("Management client must be injected", managementClient);
        Assert.assertTrue("Management client should report server as running",
                managementClient.isServerInRunningState());
    }

}

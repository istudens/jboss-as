/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.naming;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.wildfly.security.manager.WildFlySecurityManager;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.util.Hashtable;

import static org.jboss.as.naming.NamingLogger.ROOT_LOGGER;
import static org.jboss.as.naming.NamingMessages.MESSAGES;

/**
 * Initial context factory for factories based by a module.
 *
 * It expects {@link ModuleInitialContextFactory#MODULE_NAME} and
 * {@link ModuleInitialContextFactory#MODULE_INITIAL_CONTEXT_FACTORY} in its environment properties.
 *
 * A typical use-case might be a JNDI lookup against legacy JBossAS server. For this it is
 * necessary to define a legacy jboss-as client module with the jars related to jnp, e.g.:
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <module xmlns="urn:jboss:module:1.1" name="legacy-jbossas-jnp-client">
 *    <resources>
 *       <resource-root path="jnp-client.jar"/>
 *       <resource-root path="jbosssx-client.jar"/>
 *       <resource-root path="jbosssx-as-client.jar"/>
 *       <resource-root path="jboss-security-spi.jar"/>
 *       <resource-root path="jboss-logging-spi.jar"/>
 *       <resource-root path="jboss-javaee.jar"/>
 *    </resources>
 *    <dependencies>
 *       <module name="javax.api"/>
 *    </dependencies>
 * </module>
 * }
 * And then perform a look-up as follows:
 * <pre>
 * {@code
 *    Properties properties = new Properties();
      properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.as.naming.ModuleInitialContextFactory");
      properties.put(ModuleInitialContextFactory.MODULE_NAME, "legacy-jbossas-jnp-client");
      properties.put(ModuleInitialContextFactory.MODULE_INITIAL_CONTEXT_FACTORY, "org.jboss.security.jndi.JndiLoginInitialContextFactory");
      properties.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
      properties.put(Context.PROVIDER_URL, "jnp://"+ host +":"+port);
      properties.put(Context.SECURITY_PRINCIPAL, user);
      properties.put(Context.SECURITY_CREDENTIALS, pass);

      return new InitialContext(properties);
 * }
 * </pre>
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class ModuleInitialContextFactory implements InitialContextFactory {
    public static final String MODULE_INITIAL_CONTEXT_FACTORY = "jboss.naming.module.factory.initial";
    public static final String MODULE_NAME = "jboss.naming.module.name";

    /**
     * Get an initial context instance.
     *
     * @param environment The naming environment
     * @return A naming context instance
     * @throws javax.naming.NamingException
     */
    @SuppressWarnings("unchecked")
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        String moduleName = (String) environment.get(MODULE_NAME);
        if (moduleName == null)
            moduleName = WildFlySecurityManager.getPropertyPrivileged(MODULE_NAME, null);
        if (moduleName == null)
            throw MESSAGES.missingEnvironmentProperty(MODULE_NAME);

        String initialFactory = (String) environment.get(MODULE_INITIAL_CONTEXT_FACTORY);
        if (initialFactory == null)
            initialFactory = WildFlySecurityManager.getPropertyPrivileged(MODULE_INITIAL_CONTEXT_FACTORY, null);
        if (initialFactory == null)
            throw MESSAGES.missingEnvironmentProperty(MODULE_INITIAL_CONTEXT_FACTORY);

        final Module module;
        try {
            module = Module.getBootModuleLoader().loadModule(ModuleIdentifier.create(moduleName));
        } catch (ModuleLoadException e) {
            ROOT_LOGGER.failedToLoadModule(moduleName, e);
            throw MESSAGES.couldNotFindICFModule(moduleName);
        }

        final ClassLoader currentCL = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());

            final Hashtable<String, Object> envWithProperFactory = new Hashtable<String, Object>((Hashtable<String, Object>) environment);
            envWithProperFactory.put(Context.INITIAL_CONTEXT_FACTORY, initialFactory);

            return new InitialContext(envWithProperFactory);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(currentCL);
        }
    }

}

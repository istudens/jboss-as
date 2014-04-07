/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.txn.subsystem;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;

import java.util.List;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 * The {@link org.jboss.staxmapper.XMLElementReader} that handles the Transaction subsystem.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
class TransactionSubsystem21Parser extends TransactionSubsystem20Parser {

    public static final TransactionSubsystem21Parser INSTANCE = new TransactionSubsystem21Parser(Namespace.TRANSACTIONS_2_1);

    TransactionSubsystem21Parser(final Namespace validNamespace) {
        super(validNamespace);
    }

    @Override
    protected void readElement(final XMLExtendedStreamReader reader, final Element element, final List<ModelNode> operations, final ModelNode subsystemOperation, final ModelNode logStoreOperation) throws XMLStreamException {
        switch (element) {
            case RECOVERY_ENVIRONMENT: {
                parseRecoveryEnvironmentElement(reader, subsystemOperation);
                break;
            }
            case CORE_ENVIRONMENT: {
                parseCoreEnvironmentElement(reader, subsystemOperation);
                break;
            }
            case COORDINATOR_ENVIRONMENT: {
                parseCoordinatorEnvironmentElement(reader, subsystemOperation);
                break;
            }
            case OBJECT_STORE: {
                parseObjectStore(reader, logStoreOperation, subsystemOperation);
                break;
            }
            case JTS: {
                parseJts(reader, subsystemOperation);
                break;
            }
            case USEHORNETQSTORE: {
                // Deprecated. Ignored
                break;
            }
            case HORNETQ_STORE: {
                parseHornetqStore(reader, subsystemOperation);
//                subsystemOperation.get(CommonAttributes.USEHORNETQSTORE).set(true);
                break;
            }
            case JDBC_STORE: {
                parseJdbcStore(reader, subsystemOperation);
//                subsystemOperation.get(CommonAttributes.USE_JDBC_STORE).set(true);
                break;
            }
            default: {
                throw unexpectedElement(reader);
            }
        }
    }

    protected void parseObjectStore(final XMLExtendedStreamReader reader, final ModelNode logStoreOperation, final ModelNode operation) throws XMLStreamException {
        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case RELATIVE_TO:
                    TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO.parseAndSetParameter(value, operation, reader);
                    break;
                case PATH:
                    TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH.parseAndSetParameter(value, operation, reader);
                    break;
                case TYPE:
                    TransactionSubsystemRootResourceDefinition.OBJECT_STORE_TYPE.parseAndSetParameter(value, operation, reader);
                    final String storeType = operation.get(TransactionSubsystemRootResourceDefinition.OBJECT_STORE_TYPE.getName()).asString();
                    logStoreOperation.get(LogStoreConstants.LOG_STORE_TYPE.getName()).set(storeType);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        // Handle elements
        requireNoContent(reader);
    }

    protected void parseHornetqStore(final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLE_ASYNC_IO:
                    TransactionSubsystemRootResourceDefinition.HORNETQ_STORE_ENABLE_ASYNC_IO.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        // Handle elements
        requireNoContent(reader);
    }

    protected void parseJdbcStore(final XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DATASOURCE_JNDI_NAME:
                    TransactionSubsystemRootResourceDefinition.JDBC_STORE_DATASOURCE.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case JDBC_ACTION_STORE: {
                    parseJdbcStoreConfigElementAndEnrichOperation(reader, operation, TransactionSubsystemRootResourceDefinition.JDBC_ACTION_STORE_TABLE_PREFIX, TransactionSubsystemRootResourceDefinition.JDBC_ACTION_STORE_DROP_TABLE);
                    break;
                }
                case JDBC_STATE_STORE: {
                    parseJdbcStoreConfigElementAndEnrichOperation(reader, operation, TransactionSubsystemRootResourceDefinition.JDBC_STATE_STORE_TABLE_PREFIX, TransactionSubsystemRootResourceDefinition.JDBC_STATE_STORE_DROP_TABLE);
                    break;
                }
                case JDBC_COMMUNICATION_STORE: {
                    parseJdbcStoreConfigElementAndEnrichOperation(reader, operation, TransactionSubsystemRootResourceDefinition.JDBC_COMMUNICATION_STORE_TABLE_PREFIX, TransactionSubsystemRootResourceDefinition.JDBC_COMMUNICATION_STORE_DROP_TABLE);
                    break;
                }
            }
        }

    }

}

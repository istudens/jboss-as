/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.jaxrs.client;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


@RunWith(Arquillian.class)
@RunAsClient
public class BasicFormClientTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxrsformapp.war");
        war.addClasses(ClientFormResource.class, CustomBean.class);
        war.addAsWebInfResource(WebXml.get("<servlet-mapping>\n" +
                "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n" +
                "        <url-pattern>/myjaxrsform/*</url-pattern>\n" +
                "    </servlet-mapping>\n" +
                "\n"), "web.xml");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    static Client client;

    @BeforeClass
    public static void setUpClient() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    @Test
    public void testClient() throws Exception {
        MultivaluedMap<String, String> parameters = new MultivaluedHashMap<>();
        parameters.putSingle("param", "David");
        Response response = client.target(url.toExternalForm() + "myjaxrsform/clientform")
                .request(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.TEXT_PLAIN)
                .post(Entity.form(parameters));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(Response.Status.Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        Assert.assertEquals("POST: David", response.readEntity(String.class));
    }

    @Test
    @Ignore
    public void testHttp() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

            HttpPost request = new HttpPost(url + "myjaxrsform/clientform");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("param", "David"));
            request.setEntity(new UrlEncodedFormEntity(nvps));

            CloseableHttpResponse response = client.execute(request);
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            Assert.assertEquals("POST: David", EntityUtils.toString(response.getEntity()));
        }
    }
}

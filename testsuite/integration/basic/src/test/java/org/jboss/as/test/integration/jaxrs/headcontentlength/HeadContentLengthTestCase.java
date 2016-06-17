/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.jaxrs.headcontentlength;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HeadContentLengthTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxrsnoap.war");
        war.addPackage(HttpRequest.class.getPackage());
        war.addClasses(HeadContentLengthTestCase.class, SimpleResource.class);
        war.addAsWebInfResource(WebXml.get("<servlet-mapping>\n"
                + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
                + "        <url-pattern>/myjaxrs/*</url-pattern>\n" + "    </servlet-mapping>\n" + "\n"), "web.xml");
        return war;
    }

    @Test
    public void testHeadContentLength(@ArquillianResource URL url) throws Exception {
        Result getResult = get(url.toExternalForm() + "myjaxrs/headcontentlength", 10, TimeUnit.SECONDS);
        Assert.assertEquals("hello", getResult.getContent());
        Assert.assertEquals(5, getResult.getContentLength());

        Result headResult = head(url.toExternalForm() + "myjaxrs/headcontentlength", 10, TimeUnit.SECONDS);
        Assert.assertTrue(headResult.getContent() == null || headResult.getContent().isEmpty());
        Assert.assertEquals(getResult.getContentLength(), headResult.getContentLength());
    }


    private Result get(final String spec, final long timeout, final TimeUnit unit) throws IOException, ExecutionException, TimeoutException {
        final URL url = new URL(spec);
        Callable<Result> task = () -> {
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            return processResponse(conn);
        };
        return execute(task, timeout, unit);
    }

    private Result head(final String spec, final long timeout, final TimeUnit unit) throws IOException, ExecutionException, TimeoutException {
        return execRequestMethod(spec, timeout, unit, "HEAD");
    }

    /**
     * Executes an HTTP request to write the specified message.
     *
     * @param spec The {@link URL} in String form
     * @param timeout Timeout value
     * @param unit Timeout units
     * @param requestMethod Name of the HTTP method to execute (ie. HEAD, GET, POST)
     * @return
     * @throws MalformedURLException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    private Result execRequestMethod(final String spec, final long timeout, final TimeUnit unit, final String requestMethod) throws MalformedURLException, ExecutionException, TimeoutException {

        if(requestMethod==null||requestMethod.isEmpty()){
            throw new IllegalArgumentException("Request Method must be specified (ie. GET, PUT, DELETE etc)");
        }

        final URL url = new URL(spec);
        Callable<Result> task = () -> {
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod(requestMethod);
            final OutputStream out = conn.getOutputStream();
            try {
//                    write(out, message);
                return processResponse(conn);
            }
            finally {
                out.close();
            }
        };
        try {
            return execute(task, timeout, unit);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Result processResponse(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            final InputStream err = conn.getErrorStream();
            try {
                String response = err != null ? read(err) : null;
                throw new IOException(String.format("HTTP Status %d Response: %s", responseCode, response));
            }
            finally {
                if (err != null) {
                    err.close();
                }
            }
        }
        final InputStream in = conn.getInputStream();
        try {
            return new Result(read(in), conn.getContentLength());
        }
        finally {
            in.close();
        }
    }

    private static String read(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int b;
        while((b = in.read()) != -1) {
            out.write(b);
        }
        return out.toString();
    }

    private static Result execute(final Callable<Result> task, final long timeout, final TimeUnit unit) throws TimeoutException, IOException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<Result> result = executor.submit(task);
        try {
            return result.get(timeout, unit);
        } catch (TimeoutException e) {
            result.cancel(true);
            throw e;
        } catch (InterruptedException e) {
            // should not happen
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            // by virtue of the Callable redefinition above I can cast
            throw new IOException(e);
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(timeout, unit);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    class Result {
        private String content;
        private int contentLength;

        public Result(final String content, final int contentLength) {
            this.content = content;
            this.contentLength = contentLength;
        }

        public String getContent() {
            return content;
        }

        public int getContentLength() {
            return contentLength;
        }
    }

}

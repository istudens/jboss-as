package org.jboss.as.test.integration.jaxrs.client;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.FormParam;

@RequestScoped
public class CustomBean {

    @FormParam("param")
    private String param;

    public String getParam() {
        return param;
    }

}

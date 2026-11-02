package com.example.webauthn.security;

import com.vaadin.flow.server.VaadinServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Hands the current CSRF token to the browser so webauthn.js can call the
 * Spring Security WebAuthn endpoints, which are CSRF protected.
 */
public final class CsrfSupport {

    private CsrfSupport() {
    }

    /**
     * @return {@code {headerName, token}} for the current request
     */
    public static ObjectNode currentToken() {
        VaadinServletRequest request = VaadinServletRequest.getCurrent();
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            throw new IllegalStateException("No CSRF token in the current request");
        }
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put("headerName", token.getHeaderName());
        json.put("token", token.getToken());
        return json;
    }
}

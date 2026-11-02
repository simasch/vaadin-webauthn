package com.example.webauthn.ui;

import com.example.webauthn.security.CsrfSupport;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Sign in")
@AnonymousAllowed
@JsModule("./webauthn.js")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);

        Button passkeyButton = new Button("Sign in with a passkey", VaadinIcon.KEY.create(),
                e -> signInWithPasskey());
        passkeyButton.addThemeVariants(ButtonVariant.PRIMARY);
        passkeyButton.setWidth("20rem");

        Span hint = new Span("Demo users: user / password, admin / admin. "
                + "Sign in with a password once, register a passkey, then use it here.");
        hint.getStyle().set("max-width", "20rem").set("font-size", "var(--vaadin-font-size-s, 0.875rem)");

        add(new H1("WebAuthn Demo"), loginForm, new Paragraph("or"), passkeyButton, hint);
    }

    private void signInWithPasskey() {
        getElement().executeJs("return window.passkeys.authenticate($0)", CsrfSupport.currentToken())
                .then(String.class,
                        redirectUrl -> UI.getCurrent().getPage().setLocation(redirectUrl),
                        error -> {
                            Notification n = Notification.show("Passkey sign-in failed: " + error);
                            n.addThemeVariants(NotificationVariant.ERROR);
                        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        loginForm.setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
    }
}

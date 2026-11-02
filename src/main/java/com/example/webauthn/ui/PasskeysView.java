package com.example.webauthn.ui;

import com.example.webauthn.security.CsrfSupport;
import com.example.webauthn.security.PasskeyService;
import com.example.webauthn.security.PasskeyService.Passkey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

@Route("")
@PageTitle("My passkeys")
@PermitAll
@JsModule("./webauthn.js")
public class PasskeysView extends VerticalLayout {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault());

    private final PasskeyService passkeyService;
    private final String username;
    private final Grid<Passkey> grid = new Grid<>();

    public PasskeysView(PasskeyService passkeyService, AuthenticationContext authenticationContext) {
        this.passkeyService = passkeyService;
        this.username = authenticationContext.getPrincipalName().orElseThrow();

        setMaxWidth("60rem");
        getStyle().set("margin", "0 auto");

        Button logout = new Button("Sign out", VaadinIcon.SIGN_OUT.create(), e -> authenticationContext.logout());
        HorizontalLayout header = new HorizontalLayout(new H1("Hello, " + username), logout);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        TextField label = new TextField("Passkey name");
        label.setPlaceholder("e.g. MacBook Touch ID");
        label.setRequired(true);
        Button register = new Button("Register passkey", VaadinIcon.PLUS.create(), e -> register(label));
        register.addThemeVariants(ButtonVariant.PRIMARY);
        HorizontalLayout registerForm = new HorizontalLayout(label, register);
        registerForm.setAlignItems(FlexComponent.Alignment.BASELINE);

        grid.addColumn(Passkey::label).setHeader("Name").setAutoWidth(true);
        grid.addColumn(p -> format(p.created())).setHeader("Created").setAutoWidth(true);
        grid.addColumn(p -> format(p.lastUsed())).setHeader("Last used").setAutoWidth(true);
        grid.addColumn(Passkey::signatureCount).setHeader("Sign count").setAutoWidth(true);
        grid.addComponentColumn(this::deleteButton).setAutoWidth(true).setFlexGrow(0);
        grid.setEmptyStateText("No passkeys yet. Register one above.");
        grid.setAllRowsVisible(true);

        add(header,
                new H2("Passkeys"),
                new Span("A passkey lets you sign in without a password, using Touch ID, Windows Hello, "
                        + "your phone or a security key."),
                registerForm,
                grid);
        refresh();
    }

    private void register(TextField label) {
        if (label.isEmpty()) {
            label.setInvalid(true);
            label.setErrorMessage("Give the passkey a name");
            return;
        }
        label.setInvalid(false);
        getElement().executeJs("return window.passkeys.register($0, $1)", CsrfSupport.currentToken(), label.getValue())
                .then(Boolean.class,
                        ok -> {
                            label.clear();
                            refresh();
                            Notification.show("Passkey registered")
                                    .addThemeVariants(NotificationVariant.SUCCESS);
                        },
                        error -> Notification.show("Registration failed: " + error)
                                .addThemeVariants(NotificationVariant.ERROR));
    }

    private Button deleteButton(Passkey passkey) {
        Button delete = new Button(VaadinIcon.TRASH.create(), e -> {
            ConfirmDialog dialog = new ConfirmDialog("Delete passkey?",
                    "You will no longer be able to sign in with \"" + passkey.label() + "\".",
                    "Delete", confirm -> {
                        passkeyService.deletePasskey(username, passkey.id());
                        refresh();
                    });
            dialog.setCancelable(true);
            dialog.setConfirmButtonTheme("error primary");
            dialog.open();
        });
        delete.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.ERROR);
        delete.setAriaLabel("Delete " + passkey.label());
        return delete;
    }

    private void refresh() {
        grid.setItems(passkeyService.findPasskeys(username));
    }

    private static String format(Instant instant) {
        return instant == null ? "" : FORMAT.format(instant);
    }
}

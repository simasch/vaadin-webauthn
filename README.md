# Vaadin + Spring Boot passkeys (WebAuthn) example

This example shows how to add passkey sign-in to a Vaadin Flow application with Spring Security's built-in
WebAuthn support (`http.webAuthn()`).

Built with Vaadin 25.3, Spring Boot 4.1, Spring Security 7.1 and Java 21 or later.

## Run it

```bash
mvn spring-boot:run
```

Open <http://localhost:8080> and follow these steps:

1. Sign in with a password. The demo users are `user` / `password` and `admin` / `admin`.
2. Enter a name and click **Register passkey**. Your browser asks for Touch ID, Windows Hello, your phone or a
   security key.
3. Click **Sign out**, then click **Sign in with a passkey**.

> Use `localhost`, not `127.0.0.1`. WebAuthn needs the origin to match the relying party ID. It also only works
> on `localhost` or over HTTPS.
>
> You need a browser that supports the WebAuthn JSON helpers: Chrome or Edge 129+, Firefox 119+, or Safari 18.4+.

## How it works

| Piece | What it does |
|---|---|
| `SecurityConfig` | Calls `http.webAuthn()` with the relying party ID, name and allowed origins, then applies `VaadinSecurityConfigurer`. |
| `src/main/frontend/webauthn.js` | Runs the browser side of the WebAuthn ceremonies (`navigator.credentials.create/get`) against Spring Security's endpoints. It uses the WebAuthn JSON helpers (`PublicKeyCredential.parse*OptionsFromJSON`, `credential.toJSON()`) and is exposed as `window.passkeys`. |
| `LoginView` | Shows Vaadin's `LoginForm` and a **Sign in with a passkey** button that calls `passkeys.authenticate()` with `executeJs`. |
| `PasskeysView` | Registers, lists and deletes the signed-in user's passkeys. |
| `PasskeyService` | Reads and deletes credentials through `UserCredentialRepository` and `PublicKeyCredentialUserEntityRepository`. |
| `CsrfSupport` | Passes the CSRF token to the JavaScript, because the WebAuthn endpoints are CSRF protected. |

These are the endpoints Spring Security provides:

| Endpoint | Purpose |
|---|---|
| `POST /webauthn/register/options` | Gets the creation options. The user must be signed in. |
| `POST /webauthn/register` | Verifies and stores a new passkey. |
| `POST /webauthn/authenticate/options` | Gets the request options. Anonymous access is allowed. |
| `POST /login/webauthn` | Signs in with a passkey and returns `{authenticated, redirectUrl}`. |

### Two Vaadin-specific details in `SecurityConfig`

1. **The security context holder strategy.** Vaadin installs `VaadinAwareSecurityContextHolderStrategy` as the
   global strategy only after all singletons are created. The WebAuthn filters capture the global strategy when
   they are constructed, and `PublicKeyCredentialCreationOptionsFilter` has no setter for it. Without the fix,
   the filter sees no authenticated user and `/webauthn/register/options` returns **400**. The configuration
   therefore sets Vaadin's strategy before it builds the chain.
2. **Authorization for `/webauthn/**`.** `WebAuthnRegistrationFilter` runs after the `AuthorizationFilter`, and
   Vaadin's default rules deny URLs that are not views. `/webauthn/**` is therefore explicitly allowed for
   authenticated users. Without this rule, registration returns **403**.

## Production notes

- Credentials are kept in memory (`MapUserCredentialRepository`), so they are lost when the app restarts. Use
  `JdbcUserCredentialRepository` and `JdbcPublicKeyCredentialUserEntityRepository` to keep them in a database.
- Set `webauthn.rp-id` to your domain and `webauthn.allowed-origins` to your HTTPS origin in
  `application.properties`.

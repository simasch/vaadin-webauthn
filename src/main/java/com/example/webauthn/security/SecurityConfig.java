package com.example.webauthn.security;

import com.example.webauthn.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.webauthn.management.MapPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.MapUserCredentialRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            SecurityContextHolderStrategy securityContextHolderStrategy,
                                            @Value("${webauthn.rp-id}") String rpId,
                                            @Value("${webauthn.rp-name}") String rpName,
                                            @Value("${webauthn.allowed-origins}") String[] allowedOrigins) throws Exception {
        // Vaadin makes its VaadinAwareSecurityContextHolderStrategy global only after all singletons
        // are created. The WebAuthn filters read the global strategy when they are constructed (and
        // PublicKeyCredentialCreationOptionsFilter has no setter), so they would see an empty
        // SecurityContext and answer /webauthn/register/options with 400. Install it up front.
        SecurityContextHolder.setContextHolderStrategy(securityContextHolderStrategy);

        return http
                // POST /webauthn/register runs after the AuthorizationFilter, and Vaadin's default
                // rules deny everything that is not a Vaadin view or resource
                .authorizeHttpRequests(auth -> auth.requestMatchers("/webauthn/**").authenticated())
                // Registers the WebAuthn (passkey) endpoints:
                //   POST /webauthn/register/options     -> creation options (authenticated)
                //   POST /webauthn/register             -> store a new passkey (authenticated)
                //   POST /webauthn/authenticate/options -> request options (anonymous)
                //   POST /login/webauthn                -> sign in with a passkey
                .webAuthn(webAuthn -> webAuthn
                        .rpId(rpId)
                        .rpName(rpName)
                        .allowedOrigins(allowedOrigins)
                        // Vaadin provides the UI, so skip Spring Security's generated /webauthn/register page
                        .disableDefaultRegistrationPage(true))
                .with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin.loginView(LoginView.class))
                .build();
    }

    /**
     * Demo users. Each one can sign in with a password first and then register passkeys.
     */
    @Bean
    UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
                User.withUsername("user").password("{noop}password").roles("USER").build(),
                User.withUsername("admin").password("{noop}admin").roles("USER", "ADMIN").build());
    }

    /**
     * Maps a username to its WebAuthn user handle. Declared as a bean so the UI can use it too.
     * In-memory for the demo: use JdbcPublicKeyCredentialUserEntityRepository in production.
     */
    @Bean
    PublicKeyCredentialUserEntityRepository userEntityRepository() {
        return new MapPublicKeyCredentialUserEntityRepository();
    }

    /**
     * Stores the registered passkeys. In-memory for the demo: use JdbcUserCredentialRepository in production.
     */
    @Bean
    UserCredentialRepository userCredentialRepository() {
        return new MapUserCredentialRepository();
    }
}

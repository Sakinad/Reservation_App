package org.example.reservation_event.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Désactiver CSRF (ok pour Vaadin + H2)
                .csrf(csrf -> csrf.disable())

                // Autorisations avec AntPathRequestMatcher explicite
                .authorizeHttpRequests(auth -> auth

                        // ========== RESSOURCES VAADIN (CRITIQUE) ==========
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/VAADIN/**"),
                                AntPathRequestMatcher.antMatcher("/vaadinServlet/**"),
                                AntPathRequestMatcher.antMatcher("/frontend/**"),
                                AntPathRequestMatcher.antMatcher("/sw.js"),
                                AntPathRequestMatcher.antMatcher("/offline.html"),
                                AntPathRequestMatcher.antMatcher("/icons/**"),
                                AntPathRequestMatcher.antMatcher("/images/**"),
                                AntPathRequestMatcher.antMatcher("/styles/**"),
                                AntPathRequestMatcher.antMatcher("/manifest.webmanifest"),
                                AntPathRequestMatcher.antMatcher("/line-awesome/**"),
                                AntPathRequestMatcher.antMatcher("/themes/**")
                        ).permitAll()

                        // ========== PAGES PUBLIQUES ==========
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/"),
                                AntPathRequestMatcher.antMatcher("/login"),
                                AntPathRequestMatcher.antMatcher("/register"),
                                AntPathRequestMatcher.antMatcher("/events"),
                                AntPathRequestMatcher.antMatcher("/event/**"),
                                AntPathRequestMatcher.antMatcher("/forgot-password")
                        ).permitAll()

                        // ========== RESSOURCES STATIQUES ==========
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/uploads/**"),
                                AntPathRequestMatcher.antMatcher("/h2-console/**")
                        ).permitAll()

                        // ========== ADMIN ==========
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/admin/**"),
                                AntPathRequestMatcher.antMatcher("/admin/reservations")
                        ).hasAuthority("ADMIN")

                        // ========== ORGANIZER ==========
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/organizer/**")
                        ).hasAnyAuthority("ORGANIZER", "ADMIN")

                        // ========== CLIENT ==========
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/dashboard"),
                                AntPathRequestMatcher.antMatcher("/my-reservations"),
                                AntPathRequestMatcher.antMatcher("/profile"),
                                AntPathRequestMatcher.antMatcher("/eventsClient"),
                                AntPathRequestMatcher.antMatcher("/eventClient/**")
                        ).hasAnyAuthority("CLIENT", "ORGANIZER", "ADMIN")

                        // ========== TOUT LE RESTE ==========
                        .anyRequest().authenticated()
                )

                // Désactiver le formulaire de login par défaut
                .formLogin(form -> form.disable())

                // Configuration du logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // H2 Console (développement uniquement)
                .headers(headers ->
                        headers.frameOptions(frame -> frame.disable())
                );

        return http.build();
    }
}
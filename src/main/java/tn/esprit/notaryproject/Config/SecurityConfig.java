package tn.esprit.notaryproject.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // désactive CSRF pour API REST
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/**").permitAll()  // autorise librement tes endpoints d’utilisateur
                        .anyRequest().permitAll()                 // autorise tout le reste aussi
                )
                .formLogin(form -> form.disable())           // ⛔ désactive l’UI login HTML
                .httpBasic(basic -> basic.disable())         // ⛔ désactive aussi l’auth Basic
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}

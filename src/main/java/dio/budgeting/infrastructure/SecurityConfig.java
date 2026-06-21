package dio.budgeting.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração mínima de segurança: libera os endpoints da API e o Swagger.
 * Sem isso, o spring-boot-starter-security protege tudo com basic auth e os
 * curls do README retornam 401.
 *
 * Aberto de propósito por ser projeto educacional. Em produção, restrinja.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()));  // permite o console do H2 em frame
        return http.build();
    }
}

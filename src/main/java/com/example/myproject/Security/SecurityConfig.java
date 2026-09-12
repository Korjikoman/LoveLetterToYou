package com.example.myproject.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.myproject.Services.MyAppUserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationProvider authenticationProvider(
        MyAppUserService appUserService,
        PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(appUserService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
        return httpSecurity
            .csrf(Customizer.withDefaults())
            .formLogin(httpForm ->{
                httpForm.loginPage("/req/login").permitAll();
                httpForm.usernameParameter("email");
                httpForm.passwordParameter("password");
                // После входа не восстанавливаем случайно сохранённый запрос /error.
                httpForm.defaultSuccessUrl("/index", true);
                httpForm.failureUrl("/req/login?error");
                
            })
            
            .sessionManagement(session -> {
                session.maximumSessions(1);
            })
            .authorizeHttpRequests(registry ->{
                registry.requestMatchers(
                    "/req/**", "/css/**", "/js/**", "/watch/letter/**", "/error"
                ).permitAll();
                
                registry.requestMatchers("/api/**", "/create/**", "/letter/**", "/letters/**", "/profile/**", "/index").authenticated();
                
                registry.anyRequest().authenticated();
            })
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/req/login").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .build();
    }
    
}

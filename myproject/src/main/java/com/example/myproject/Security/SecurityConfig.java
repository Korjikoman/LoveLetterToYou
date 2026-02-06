package com.example.myproject.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.myproject.Services.MyAppUserService;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private final MyAppUserService appUserService;
    
    
    @Bean
    public UserDetailsService userDetailsService(){
        return appUserService;
    }
    
    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(appUserService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
        return httpSecurity
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(httpForm ->{
                httpForm.loginPage("/req/login").permitAll();
                httpForm.usernameParameter("email");
                httpForm.passwordParameter("password");
                httpForm.defaultSuccessUrl("/index");
                
            })
            
            .sessionManagement(session -> {
                session.maximumSessions(1);
            })
            .authorizeHttpRequests(registry ->{
                registry.requestMatchers("/req/**","/css/**","/js/**", "/watch/letter/**", "/api/**").permitAll();
                
                registry.requestMatchers("/create/**", "/letters/**", "/profile/**", "/index").authenticated();
                
                registry.anyRequest().authenticated();
            })
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/req/login").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .build();
    }
    
}
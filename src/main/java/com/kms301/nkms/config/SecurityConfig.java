package com.kms301.nkms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${nkms.admin.username:admin}")
    private String adminUsername;

    @Value("${nkms.admin.password:admin123}")
    private String adminPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/kham-pha/**", "/api/ai-analysis"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/kho/them",
                                "/kho/*/sua",
                                "/kho/*/duyet",
                                "/kho/*/luu-tru",
                                "/kho/bang-chu-cai/**"
                        ).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/kho/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/dang-nhap")
                        .defaultSuccessUrl("/kham-pha", true)
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("nkms-remember-me-key-301")
                        .tokenValiditySeconds(86400 * 30) // 30 ngày lưu đăng nhập qua cookie
                        .rememberMeParameter("remember-me")
                        .userDetailsService(userDetailsService)
                )
                .logout(logout -> logout
                        .logoutUrl("/dang-xuat")
                        .logoutSuccessUrl("/dang-nhap?logout=true")
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                );
        return http.build();
    }
}

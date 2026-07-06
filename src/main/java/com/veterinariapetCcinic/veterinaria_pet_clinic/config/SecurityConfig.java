package com.veterinariapetCcinic.veterinaria_pet_clinic.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            var usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

            return org.springframework.security.core.userdetails.User
                    .withUsername(usuario.getUsername())
                    .password(usuario.getPassword())
                    .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRol()))
                    .disabled(!usuario.getActivo())
                    .build();
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // Solo ADMIN y ADMINISTRADOR pueden acceder a estas rutas
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "ADMINISTRADOR")

                        // Recursos públicos
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/Imagen/**").permitAll()
                        .requestMatchers("/login").permitAll()
                        
                        // Solo VENDEDOR puede acceder a estas rutas 
                        .requestMatchers("/vendedor/**").hasRole("VENDEDOR")
                        
                        // Solo VETERINARIO puede acceder a estas rutas
                        .requestMatchers("/veterinaria/**").hasRole("VETERINARIO")
                        
                        // Solo FARMACEUTICO puede acceder a estas rutas
                        .requestMatchers("/farmaceutico/**").hasRole("FARMACEUTICO")
                        
                        // Solo RECEPCIONISTA puede acceder a estas rutas
                        .requestMatchers("/recepcionista/**").hasRole("RECEPCIONISTA")
                        .requestMatchers("/clientes/**").hasRole("RECEPCIONISTA")
                        .requestMatchers("/mascotas/**").hasRole("RECEPCIONISTA")
                        .requestMatchers("/citas/**").hasRole("RECEPCIONISTA")
                        .requestMatchers("/agenda/**").hasRole("RECEPCIONISTA")
                        .requestMatchers("/pagos/**").hasRole("RECEPCIONISTA")
                        .requestMatchers("/diagnostico/**").hasRole("RECEPCIONISTA")
                        .requestMatchers("/dashboard").authenticated()

                        // Cualquier otra ruta requiere autenticación
                        .anyRequest().authenticated())
                    .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean esAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                                            || authority.getAuthority().equals("ROLE_ADMINISTRADOR"));

                            boolean esVeterinario = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_VETERINARIO"));

                            boolean esFarmaceutico = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_FARMACEUTICO"));
                            
                            boolean esVendedor = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_VENDEDOR"));


                            if (esAdmin) {

                                response.sendRedirect("/admin/dashboard");
                            } else if (esVeterinario) {
                                response.sendRedirect("/veterinaria/dashboard");
                            } else if (esFarmaceutico) {
                                response.sendRedirect("/farmaceutico/dashboard");
                            } else if (esVendedor) {
                                response.sendRedirect("/vendedor");
                            } else {
                                response.sendRedirect("/recepcionista/dashboard");
                            }

                        })
                        .permitAll())

                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())

                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access-denied"));

        return http.build();
    }
}
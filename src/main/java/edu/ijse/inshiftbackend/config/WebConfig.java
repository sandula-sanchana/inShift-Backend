package edu.ijse.inshiftbackend.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.List;


@Configuration
@RequiredArgsConstructor
public class WebConfig {

    private final EmployeeRepository employeeRepository;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email ->employeeRepository.findByEmail(email).map(
                employee->org.springframework.security.core.userdetails.User.builder()
                        .username(employee.getEmail())
                        .password(employee.getPasswordHash())
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_"+employee.getRole().name()))).build()
        ).orElseThrow(()->new UsernameNotFoundException("User not found"));
    }
}

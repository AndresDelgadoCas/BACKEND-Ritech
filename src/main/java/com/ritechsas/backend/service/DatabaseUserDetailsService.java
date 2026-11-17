package com.ritechsas.backend.service;

import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.ritechsas.backend.entity.User;
import com.ritechsas.backend.repository.UserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    public DatabaseUserDetailsService(UserRepository userRepository) { this.userRepository = userRepository; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        var authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())).collect(Collectors.toSet());
        if (authorities.isEmpty() && user.getRole() != null) authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
            .password(user.getPasswordHash()).authorities(authorities).disabled(!user.isActive()).build();
    }
}

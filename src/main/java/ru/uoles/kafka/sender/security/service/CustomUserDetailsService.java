package ru.uoles.kafka.sender.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.uoles.kafka.sender.security.repository.UserRepository;

/** Загружает пользователя и его роли из SQLite для Spring Security. */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails result = userRepository.findByUsername(username)
                .map(account -> User.withUsername(account.username())
                        .password(account.passwordHash())
                        .disabled(!account.enabled())
                        .authorities(account.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        return result;
    }
}

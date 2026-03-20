package com.ridesharing.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ridesharing.domain.User;
import com.ridesharing.repository.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // BOTH role gets both DRIVER and PASSENGER authorities so hasRole() checks work correctly
        List<SimpleGrantedAuthority> authorities;
        if ("BOTH".equals(user.getRole())) {
            authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_DRIVER"),
                    new SimpleGrantedAuthority("ROLE_PASSENGER")
            );
        } else {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                authorities
        );
    }
}

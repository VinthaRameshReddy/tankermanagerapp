package com.tankermanager.security;

import com.tankermanager.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        return userAccountRepository.findByPhone(phone)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + phone));
    }
}

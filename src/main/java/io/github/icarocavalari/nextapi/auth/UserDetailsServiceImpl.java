package io.github.icarocavalari.nextapi.auth;


import io.github.icarocavalari.nextapi.user.Role;
import io.github.icarocavalari.nextapi.user.RoleName;
import io.github.icarocavalari.nextapi.user.User;
import io.github.icarocavalari.nextapi.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static org.springframework.security.core.userdetails.User.withUsername;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepo;

    public UserDetailsServiceImpl(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepo.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("erro"));

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new UsernameNotFoundException("erro");
        }

        return withUsername(email)
                .password(user.getPassword())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .map(RoleName::name)
                        .toArray(String[]::new))
                .build();
    }
}

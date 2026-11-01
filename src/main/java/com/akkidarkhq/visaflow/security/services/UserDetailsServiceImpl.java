package com.akkidarkhq.visaflow.security.services;

import com.akkidarkhq.visaflow.security.model.User;
import com.akkidarkhq.visaflow.security.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        logger.info("UserDetailsServiceImpl: Attempting to load user by username: {}", username);
        
        // Find user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("UserDetailsServiceImpl: User Not Found with username: {}", username);
                    return new UsernameNotFoundException("User Not Found with username: " + username);
                });

        logger.info("UserDetailsServiceImpl: Successfully found user in DB: {}", user.getUsername());
        return UserDetailsImpl.build(user);
    }
}

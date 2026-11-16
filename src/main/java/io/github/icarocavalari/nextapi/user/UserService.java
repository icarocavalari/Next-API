package io.github.icarocavalari.nextapi.user;

import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public UserResponse getById(Long id) {
        User user = userRepo.findById(id).orElseThrow(() ->
                new UserNotFoundException("User could not be found with the given id"));

        return UserResponse.from(user);
    }
}

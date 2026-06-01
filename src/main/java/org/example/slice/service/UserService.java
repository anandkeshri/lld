package org.example.slice.service;

import lombok.extern.slf4j.Slf4j;
import org.example.slice.exception.DuplicateResourceException;
import org.example.slice.exception.ResourceNotFoundException;
import org.example.slice.model.User;
import org.example.slice.repository.UserRepository;
import org.example.slice.validation.UserValidator;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserValidator userValidator;

    public UserService(UserRepository userRepository, UserValidator userValidator) {
        this.userRepository = userRepository;
        this.userValidator = userValidator;
    }

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        log.debug("getAllUsers - returned {} users", users.size());
        return users;
    }

    public User getUserById(Long id) {
        log.debug("getUserById - looking up user id={}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("getUserById - user not found id={}", id);
                    return new ResourceNotFoundException("User", id);
                });
    }

    public User createUser(User user) {
        log.debug("createUser - validating and saving user email={}", user.getEmail());
        userValidator.validateForCreate(user);
        userRepository.findByEmail(user.getEmail()).ifPresent(existing -> {
            log.warn("createUser - duplicate email={}", user.getEmail());
            throw new DuplicateResourceException("User", "email", user.getEmail());
        });
        User saved = userRepository.save(user);
        log.info("createUser - user created id={}", saved.getId());
        return saved;
    }

    public User updateUser(Long id, User incoming) {
        log.debug("updateUser - updating user id={}", id);
        User existing = getUserById(id);
        userValidator.validateForUpdate(existing, incoming);
        userRepository.findByEmail(incoming.getEmail())
                .filter(found -> !found.getId().equals(id))
                .ifPresent(conflict -> {
                    log.warn("updateUser - email conflict email={}", incoming.getEmail());
                    throw new DuplicateResourceException("User", "email", incoming.getEmail());
                });
        existing.setName(incoming.getName());
        existing.setEmail(incoming.getEmail());
        User saved = userRepository.save(existing);
        log.info("updateUser - user updated id={}", id);
        return saved;
    }

    public void deleteUser(Long id) {
        log.debug("deleteUser - deleting user id={}", id);
        getUserById(id);
        userRepository.deleteById(id);
        log.info("deleteUser - user deleted id={}", id);
    }
}
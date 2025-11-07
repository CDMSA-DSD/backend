package dsd.api.cdmsa.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dsd.api.cdmsa.exception.UserExistsException;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository repository;

    public boolean existUser(String email) {
        return repository.existsByEmail(email);
    }

    public User createUser(User user) {
        // Check if a user already exist
        if (!existUser(user.getEmail())) {
            // Store user
            return repository.save(user);
        }
        // Instead throw a exception that return 409- CONFLICT
        throw new UserExistsException(user.getName());
    }

    public Optional<User> searchById(int id) {
        return repository.findById(id);
    }

    public List<User> findUsers() {
        return repository.findAll();
    }

    public boolean existUserById(int id) {
        return repository.existsById(id);
    }

    public void deleteUser(int id) {
        repository.deleteById(id);
    }

}

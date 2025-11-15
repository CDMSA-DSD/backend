package dsd.api.cdmsa.service;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dsd.api.cdmsa.exception.UserExistsException;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {

    private final JWTService jwtService;

    private final AuthenticationManager authManager;


    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public boolean existUser(String email) {
        return repository.existsByEmail(email);
    }

    public User createUser(User user) {
        // Check if a user already exist
        if (!existUser(user.getEmail())) {
            //Hash the password
            user.setPassword(encoder.encode(user.getPassword()));
            // Store user
            return repository.save(user);
        }
        // Instead throw a exception that return 409- CONFLICT
        throw new UserExistsException(user.getName());
    }

    public String verify(User user) {
        Authentication authentication = authManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        if (authentication.isAuthenticated()) {
            return jwtService.generateToken(user.getUsername());
        } else {
            throw new InvalidParameterException();
        }
    }

    public Optional<User> searchById(Long id) {
        return repository.findById(id);
    }

    public List<User> findUsers() {
        return repository.findAll();
    }

    public boolean existUserById(Long id) {
        return repository.existsById(id);
    }

    public void deleteUser(Long id) {
        repository.deleteById(id);
    }


}

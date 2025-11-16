package dsd.api.cdmsa.service;

import java.security.InvalidParameterException;

import org.springframework.data.domain.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dsd.api.cdmsa.dto.LoginRequest;
import dsd.api.cdmsa.exception.UserExistsException;
import dsd.api.cdmsa.exception.UserNotFoundException;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.model.UserPrincipal;
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
            // Hash the password
            user.setPassword(encoder.encode(user.getPassword()));
            // Store user
            return repository.save(user);
        }
        // Instead throw a exception that return 409- CONFLICT
        throw new UserExistsException(user.getFirstname() + " " + user.getLastname());
    }

    public String verify(LoginRequest login) {
        Authentication authentication = authManager
                .authenticate(new UsernamePasswordAuthenticationToken(login.email(), login.password()));

        if (authentication.isAuthenticated()) {
            UserPrincipal authUser = (UserPrincipal) authentication.getPrincipal();
            User user = authUser.getUser();
            return jwtService.generateToken(user);

        } else {
            throw new InvalidParameterException();
        }
    }

    public User searchById(Long id) {
        User user = repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        return user;
    }

    public Page<User> findAllUsers(Long orgId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findByOrgId(orgId, pageable);
    }

    public boolean existUserById(Long id) {
        return repository.existsById(id);
    }

    public void deleteUser(Long id) {
        repository.deleteById(id);
    }

}

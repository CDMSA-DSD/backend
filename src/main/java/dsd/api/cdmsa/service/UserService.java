package dsd.api.cdmsa.service;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dsd.api.cdmsa.dto.ContextByAdminResponse;
import dsd.api.cdmsa.dto.LoginRequest;
import dsd.api.cdmsa.dto.SignInRequest;
import dsd.api.cdmsa.dto.LoginResponse;
import dsd.api.cdmsa.dto.UserResponse;
import dsd.api.cdmsa.exception.UserExistsException;
import dsd.api.cdmsa.exception.UserNotFoundException;
import dsd.api.cdmsa.model.OrganizationInvitation;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {

    private final JWTService jwtService;
    private final OrganizationInvitationService invitationService;
    private final ContextService contextService;

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

    public User createUserByInvitation(SignInRequest registration, String token) {

        OrganizationInvitation invitation = invitationService.getInvitationByToken(token); // retrive invitation with
                                                                                           // that token

        // Creates the user (set all the parameters)
        User user = new User();
        user.setFirstname(registration.firstname());
        user.setLastname(registration.lastname());
        user.setEmail(registration.email());
        user.setPassword(registration.password()); // Hashed later in createUser
        user.setOrg(invitation.getOrg());

        return createUser(user);
    }

    public LoginResponse login(LoginRequest login) {
        Authentication authentication = authManager
                .authenticate(new UsernamePasswordAuthenticationToken(login.email(), login.password()));

        if (authentication.isAuthenticated()) {
            UserPrincipal authUser = (UserPrincipal) authentication.getPrincipal();
            User user = authUser.getUser();
            String token = jwtService.generateToken(user);
            boolean isAdmin = isOrgAdmin(user);
            List<ContextByAdminResponse> contextsIsAdmin = contextService.findContextByAdmin(user);

            UserResponse dto = UserResponse.fromEntity(user);

            return new LoginResponse(dto, token, isAdmin, contextsIsAdmin);

        } else {
            throw new InvalidParameterException();
        }
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

    public List<User> findAllUsersByid(List<Long> userIds) {
        List<User> users = repository.findAllById(userIds);

        Set<Long> foundIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        List<Long> missing = userIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!missing.isEmpty()) {
            throw new UserNotFoundException(missing);
        }

        return users;
    }

    public boolean existUserById(Long id) {
        return repository.existsById(id);
    }

    public boolean isOrgAdmin(User user) {
        return user.getId().equals(user.getOrg().getAdminUser().getId());
    }

    public void deleteUser(Long id) {
        repository.deleteById(id);
    }

}

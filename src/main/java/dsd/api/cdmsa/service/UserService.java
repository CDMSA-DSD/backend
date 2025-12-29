package dsd.api.cdmsa.service;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dsd.api.cdmsa.dto.*;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dsd.api.cdmsa.exception.InvalidDomainException;
import dsd.api.cdmsa.exception.UserExistsException;
import dsd.api.cdmsa.exception.UserNotFoundException;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.OrganizationInvitation;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.repository.UserRepository;
import dsd.api.cdmsa.security.authentication.OAuthCodeAuthenticationToken;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserService {

    private final JWTService jwtService;
    private final OrganizationInvitationService invitationService;
    private final ContextService contextService;
    private final MSAuthService msAuthService;

    private final AuthenticationManager authManager;

    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public boolean existUser(String email) {
        return repository.existsByEmail(email);
    }

    public User createUser(User user) {
        // Check if a user already exist
        if (!existUser(user.getEmail())) {
            if (!user.getPassword().equals("OAUTH")) {
                // Hash the password
                user.setPassword(encoder.encode(user.getPassword()));
            }
            // Store user
            return repository.save(user);
        }
        // Instead throw a exception that return 409- CONFLICT
        throw new UserExistsException(user.getFirstname() + " " + user.getLastname());
    }

    private LoginResponse toLoginResponse(User user) {
        String token = jwtService.generateToken(user);
        boolean isAdmin = isOrgAdmin(user);
        List<ContextByAdminResponse> contextsIsAdmin = contextService.findContextByAdmin(user);

        UserResponse dto = UserResponse.fromEntity(user);

        return new LoginResponse(dto, token, isAdmin, contextsIsAdmin);
    }

    public LoginResponse createUserByInvitation(SignInRequest registration, String token) {

        OrganizationInvitation invitation = invitationService.getInvitationByToken(token); // retrive invitation with
                                                                                           // that token

        if (!isDomainCorrect(registration.email(), invitation.getOrg())) {
            throw new InvalidDomainException();
        }

        // Creates the user (set all the parameters)
        User user = new User();
        user.setFirstname(registration.firstname());
        user.setLastname(registration.lastname());
        user.setEmail(registration.email());
        user.setPassword(registration.password()); // Hashed later in createUser
        user.setOrg(invitation.getOrg());
        user.setProviderUserId(registration.providerId());

        return toLoginResponse(createUser(user));
    }

    private boolean isDomainCorrect(
            String email,
            Organization org) {
        String userDomain = email.split("@")[1];
        String orgDomain = org.getDomain();

        return orgDomain == null
                || userDomain.equals(orgDomain.toLowerCase())
                || userDomain.endsWith("." + orgDomain.toLowerCase());
    }

    public LoginResponse login(LoginRequest login) {

        if (login.password().equals("OAUTH")) {
            throw new InvalidParameterException();
        }

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

    public User searchById(Long id) {
        User user = repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        return user;
    }

    public Page<User> findAllUsers(Long orgId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findByOrgId(orgId, pageable);
    }

    public List<User> findAllUsersByid(List<Long> userIds, Long orgId) {
        List<User> users = repository.findByIdInAndOrgId(userIds, orgId);

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

    @Transactional
    public User updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request.firstname() != null && !request.firstname().isBlank()) {
            user.setFirstname(request.firstname().trim());
        }
        if (request.lastname() != null && !request.lastname().isBlank()) {
            user.setLastname(request.lastname().trim());
        }

        if (request.jobTitle() != null && !request.jobTitle().isBlank()) {
            user.setJobTitle(request.jobTitle().trim());
        }

        return repository.save(user);
    }

    public LoginResponse createUserByMS(MSSignInRequest request) {
        UserInfo userInfo = msAuthService.extractUserInfo(request.code());
        SignInRequest signIn = new SignInRequest(userInfo.email(),
                userInfo.firstname(),
                userInfo.lastname(),
                "OAUTH",
                userInfo.providerId());

        return createUserByInvitation(signIn, request.token());

    }

    public LoginResponse loginWithMS(MSSignInRequest request) {
        Authentication authentication = authManager
                .authenticate(new OAuthCodeAuthenticationToken(request.code(), "MS"));

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

    public User findByEmailAndProviderUserId(String providerId, String email) {
        User user = repository.findByEmailAndProviderUserId(email, providerId)
                .orElseThrow(() -> new UserNotFoundException(email));
        return user;
    }
}

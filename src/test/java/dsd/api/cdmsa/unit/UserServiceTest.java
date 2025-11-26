package dsd.api.cdmsa.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import dsd.api.cdmsa.dto.LoginRequest;
import dsd.api.cdmsa.dto.LoginResponse;
import dsd.api.cdmsa.dto.SignInRequest;
import dsd.api.cdmsa.exception.UserExistsException;
import dsd.api.cdmsa.exception.UserNotFoundException;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.OrganizationInvitation;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.repository.UserRepository;
import dsd.api.cdmsa.service.ContextService;
import dsd.api.cdmsa.service.JWTService;
import dsd.api.cdmsa.service.OrganizationInvitationService;
import dsd.api.cdmsa.service.UserService;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private JWTService jwtService;
    @Mock
    private OrganizationInvitationService invitationService;
    @Mock
    private ContextService contextService;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private UserRepository repository;
    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserService userService;

    // ---------------------------------------------------
    // existUser(email)
    // ---------------------------------------------------

    @Test
    void existUser_shouldReturnTrue_whenRepositorySaysTrue() {
        when(repository.existsByEmail("test@mail.com")).thenReturn(true);

        boolean result = userService.existUser("test@mail.com");

        assertTrue(result);
        verify(repository).existsByEmail("test@mail.com");
    }

    @Test
    void existUser_shouldReturnFalse_whenRepositorySaysFalse() {
        when(repository.existsByEmail("test@mail.com")).thenReturn(false);

        boolean result = userService.existUser("test@mail.com");

        assertFalse(result);
        verify(repository).existsByEmail("test@mail.com");
    }

    // ---------------------------------------------------
    // createUser(User)
    // ---------------------------------------------------

    @Test
    void createUser_shouldEncodePasswordAndSave_whenEmailDoesNotExists() {
        User user = new User();
        user.setEmail("new@email.com");
        user.setPassword("rawPassword");

        when(repository.existsByEmail("new@email.com")).thenReturn(false);
        when(encoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createUser(user);

        assertNotNull(result);
        assertEquals("encodedPassword", result.getPassword());

        verify(repository).existsByEmail("new@email.com");
        verify(encoder).encode("rawPassword");
        verify(repository).save(user);
    }

    @Test
    void createUser_shouldThrowUserExistsException_whenEmailAlreadyExists() {
        User user = new User();
        user.setEmail("existing@mail.com");

        when(repository.existsByEmail("existing@mail.com")).thenReturn(true);

        assertThrows(UserExistsException.class, () -> userService.createUser(user));

        verify(repository).existsByEmail("existing@mail.com");
        verify(repository, never()).save(any());
    }

    // ---------------------------------------------------
    // createUserByInvitation(SignInRequest, token)
    // ---------------------------------------------------

    @Test
    void createUserByInvitation_shouldCreateUserWithOrgFromInvitation() {
        String token = "invitation-token";

        SignInRequest request = new SignInRequest(
                "John",
                "Doe",
                "john.doe@mail.com",
                "rawPassword");

        Organization org = new Organization();
        OrganizationInvitation invitation = new OrganizationInvitation();
        invitation.setOrg(org);

        when(invitationService.getInvitationByToken(token)).thenReturn(invitation);
        when(repository.existsByEmail("john.doe@mail.com")).thenReturn(false);
        when(encoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createUserByInvitation(request, token);

        assertNotNull(result);
        assertEquals("John", result.getFirstname());
        assertEquals("Doe", result.getLastname());
        assertEquals("john.doe@mail.com", result.getEmail());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(org, result.getOrg());

        verify(invitationService).getInvitationByToken(token);
        verify(repository).existsByEmail("john.doe@mail.com");
        verify(encoder).encode("rawPassword");
        verify(repository).save(any(User.class));
    }

    // ---------------------------------------------------
    // searchById(id)
    // ---------------------------------------------------

    @Test
    void searchById_shouldReturnUser_whenUserExists() {
        User user = new User();
        user.setId(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.searchById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(repository).findById(1L);
    }

    @Test
    void searchById_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.searchById(99L));

        verify(repository).findById(99L);
    }

    @Test
    void findAllUsers_shouldCallRepositoryWithPageable() {
        Long orgId = 1L;
        int page = 0;
        int size = 5;

        User user = new User();
        Page<User> pageResult = new PageImpl<>(List.of(user));

        when(repository.findByOrgId(eq(orgId), any(Pageable.class))).thenReturn(pageResult);

        Page<User> result = userService.findAllUsers(orgId, page, size);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(repository).findByOrgId(eq(orgId), any(Pageable.class));

    }

    // ---------------------------------------------------
    // existUserById(id)
    // ---------------------------------------------------

    @Test
    void existUserById_shouldReturnTrue_whenRepositoryReturnsTrue() {
        when(repository.existsById(1L)).thenReturn(true);

        boolean result = userService.existUserById(1L);

        assertTrue(result);
        verify(repository).existsById(1L);
    }

    @Test
    void existUserById_shouldReturnFalse_whenRepositoryReturnsFalse() {
        when(repository.existsById(1L)).thenReturn(false);

        boolean result = userService.existUserById(1L);

        assertFalse(result);
        verify(repository).existsById(1L);
    }

    // ---------------------------------------------------
    // deleteUser(id)
    // ---------------------------------------------------

    @Test
    void deleteUser_shouldCallRepositoryDeleteById() {
        userService.deleteUser(1L);

        verify(repository).deleteById(1L);
    }

    // ---------------------------------------------------
    // login(LoginRequest)
    // ---------------------------------------------------

    @Test
    void login_shouldReturnLoginResponse_whenAuthenticationOk() {
        LoginRequest request = new LoginRequest("user@mail.com", "password");

        Authentication authentication = mock(Authentication.class);
        UserPrincipal principal = mock(UserPrincipal.class);

        Organization org = new Organization();

        User admin = new User();
        admin.setId(1L);

        org.setAdminUser(admin);

        User user = new User();
        user.setEmail("user@mail.com");
        user.setId(2L);
        user.setOrg(org);

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getUser()).thenReturn(user);

        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(contextService.findContextByAdmin(user)).thenReturn(Collections.emptyList());

        LoginResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(user);
        verify(contextService).findContextByAdmin(user);
    }

    @Test
    void login_shouldThrowInvalidParameterException_whenAuthenticationFails() {
        LoginRequest request = new LoginRequest("user@mail.com", "password");

        Authentication authentication = mock(Authentication.class);

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThrows(InvalidParameterException.class, () -> userService.login(request));
    }

    // ---------------------------------------------------
    // verify(LoginRequest)
    // ---------------------------------------------------

    @Test
    void verify_shouldReturnToken_whenAuthenticationOk() {
        LoginRequest request = new LoginRequest("user@mail.com", "password");

        Authentication authentication = mock(Authentication.class);
        UserPrincipal principal = mock(UserPrincipal.class);
        User user = new User();
        user.setEmail("user@mail.com");

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getUser()).thenReturn(user);

        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        String token = userService.verify(request);

        assertEquals("jwt-token", token);
        verify(jwtService).generateToken(user);
    }

    @Test
    void verify_shouldThrowInvalidParameterException_whenAuthenticationFails() {
        LoginRequest request = new LoginRequest("user@mail.com", "password");

        Authentication authentication = mock(Authentication.class);

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThrows(InvalidParameterException.class, () -> userService.verify(request));
    }

    // ---------------------------------------------------
    // verify(LoginRequest)
    // ---------------------------------------------------

    @Test
    void isOrgAdmin_shouldReturnTrue_whenUserIsAdminOfOrganization() {
        User admin = new User();
        admin.setId(1L);

        Organization org = new Organization();
        org.setAdminUser(admin);

        User user = new User();
        user.setId(1L);
        user.setOrg(org);

        boolean result = userService.isOrgAdmin(user);

        assertTrue(result);
    }

    @Test
    void isOrgAdmin_shouldReturnFalse_whenUserIsNotAdmin() {
        User admin = new User();
        admin.setId(1L);

        Organization org = new Organization();
        org.setAdminUser(admin);

        User user = new User();
        user.setId(2L);
        user.setOrg(org);

        boolean result = userService.isOrgAdmin(user);

        assertFalse(result);
    }
}

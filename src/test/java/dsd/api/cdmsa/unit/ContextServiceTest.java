package dsd.api.cdmsa.unit;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import dsd.api.cdmsa.dto.AddContextMemberRequest;
import dsd.api.cdmsa.dto.ContextAdminResponse;
import dsd.api.cdmsa.dto.ContextMemberResponse;
import dsd.api.cdmsa.dto.ContextResponse;
import dsd.api.cdmsa.dto.CreateContextRequest;
import dsd.api.cdmsa.dto.PromoteContextAdminRequest;
import dsd.api.cdmsa.dto.UpdateContextRequest;
import dsd.api.cdmsa.exception.ContextBadRequestException;
import dsd.api.cdmsa.exception.ContextForbiddenException;
import dsd.api.cdmsa.exception.ContextNotFoundException;
import dsd.api.cdmsa.model.Context;
import dsd.api.cdmsa.model.ContextMembership;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.ContextMembershipRepository;
import dsd.api.cdmsa.repository.ContextRepository;
import dsd.api.cdmsa.repository.UserRepository;
import dsd.api.cdmsa.service.ContextService;

/**
 * Unit tests for ContextService.
 * These tests use Mockito to mock repositories and focus on business logic.
 */
@ExtendWith(MockitoExtension.class)
class ContextServiceTest {

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContextMembershipRepository membershipRepository;

    @InjectMocks
    private ContextService contextService;

    // ------------------------------------------------------------
    // createContext
    // ------------------------------------------------------------

    @Test
    void createContext_shouldCreateContext_whenUserIsOrgAdminAndNameUnique() {
        Long userId = 1L;

        // Request
        CreateContextRequest request = new CreateContextRequest(
                "  Dev Team  ",
                "TEAM",
                "Context for dev team");

        // User + org setup
        Organization org = new Organization();
        org.setId(10L);

        User user = new User();
        user.setId(userId);
        user.setOrg(org);
        org.setAdminUser(user); // user is org admin

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(contextRepository.existsByOrganizationIdAndNameIgnoreCaseAndActiveTrue(eq(org.getId()), anyString()))
                .thenReturn(false);

        when(contextRepository.save(any(Context.class))).thenAnswer(invocation -> {
            Context c = invocation.getArgument(0);
            c.setId(100L);
            return c;
        });

        ContextResponse response = contextService.createContext(userId, request);

        assertNotNull(response);
        // Verificamos que el nombre se trimmeó y se guardó
        ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
        verify(contextRepository).save(captor.capture());
        assertEquals("Dev Team", captor.getValue().getName());
        verify(contextRepository).existsByOrganizationIdAndNameIgnoreCaseAndActiveTrue(org.getId(), "Dev Team");
    }

    @Test
    void createContext_shouldThrowBadRequest_whenNameAlreadyExists() {
        Long userId = 1L;

        CreateContextRequest request = new CreateContextRequest(
                "Dev Team",
                "TEAM",
                "Context for dev team");

        Organization org = new Organization();
        org.setId(10L);

        User user = new User();
        user.setId(userId);
        user.setOrg(org);
        org.setAdminUser(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(contextRepository.existsByOrganizationIdAndNameIgnoreCaseAndActiveTrue(org.getId(), "Dev Team"))
                .thenReturn(true);

        assertThrows(ContextBadRequestException.class,
                () -> contextService.createContext(userId, request));
    }

    @Test
    void createContext_shouldThrowBadRequest_whenRequestIsNull() {
        Long userId = 1L;

        assertThrows(ContextBadRequestException.class,
                () -> contextService.createContext(userId, null));
    }

    // ------------------------------------------------------------
    // listContexts
    // ------------------------------------------------------------

    @Test
    void listContexts_shouldReturnContextsForOrg() {
        Long userId = 1L;

        Organization org = new Organization();
        org.setId(10L);

        User user = new User();
        user.setId(userId);
        user.setOrg(org);

        Context ctx = new Context();
        ctx.setId(100L);
        ctx.setOrganization(org);
        ctx.setName("Dev Team");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(contextRepository.findByOrganizationIdAndActiveTrue(org.getId()))
                .thenReturn(List.of(ctx));

        List<ContextResponse> result = contextService.listContexts(userId);

        assertEquals(1, result.size());
        verify(contextRepository).findByOrganizationIdAndActiveTrue(org.getId());
    }

    // ------------------------------------------------------------
    // updateContext
    // ------------------------------------------------------------

    @Test
    void updateContext_shouldUpdateFields_whenOrgAdminAndNameFree() {
        Long userId = 1L;
        Long contextId = 100L;

        Organization org = new Organization();
        org.setId(10L);

        User user = new User();
        user.setId(userId);
        user.setOrg(org);
        org.setAdminUser(user); // user es admin

        Context context = new Context();
        context.setId(contextId);
        context.setOrganization(org);
        context.setName("Old Name");

        UpdateContextRequest request = new UpdateContextRequest(
                "New Name",
                "NEW_TYPE",
                "New description");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(contextRepository.findByIdAndOrganizationIdAndActiveTrue(contextId, org.getId()))
                .thenReturn(Optional.of(context));
        when(contextRepository.existsByOrganizationIdAndNameIgnoreCaseAndActiveTrue(org.getId(), "New Name"))
                .thenReturn(false);
        when(contextRepository.save(any(Context.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContextResponse response = contextService.updateContext(userId, contextId, request);

        assertNotNull(response);
        assertEquals("New Name", context.getName());
        assertEquals("NEW_TYPE", context.getType());
        assertEquals("New description", context.getDescription());
    }

    @Test
    void updateContext_shouldThrowNotFound_whenContextDoesNotExist() {
        Long userId = 1L;
        Long contextId = 100L;

        Organization org = new Organization();
        org.setId(10L);

        User user = new User();
        user.setId(userId);
        user.setOrg(org);
        org.setAdminUser(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(contextRepository.findByIdAndOrganizationIdAndActiveTrue(contextId, org.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ContextNotFoundException.class,
                () -> contextService.updateContext(userId, contextId, new UpdateContextRequest("name", null, null)));
    }

    // ------------------------------------------------------------
    // deleteContext
    // ------------------------------------------------------------

    @Test
    void deleteContext_shouldSetActiveFalse_whenOrgAdminAndContextExists() {
        Long userId = 1L;
        Long contextId = 200L;

        Organization org = new Organization();
        org.setId(10L);

        User user = new User();
        user.setId(userId);
        user.setOrg(org);
        org.setAdminUser(user);

        Context context = new Context();
        context.setId(contextId);
        context.setOrganization(org);
        context.setActive(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(contextRepository.findByIdAndOrganizationIdAndActiveTrue(contextId, org.getId()))
                .thenReturn(Optional.of(context));

        contextService.deleteContext(userId, contextId);

        assertFalse(context.isActive());
        verify(contextRepository).save(context);
    }

    // ------------------------------------------------------------
    // promoteContextAdmin / demoteContextAdmin
    // ------------------------------------------------------------

    @Test
    void promoteContextAdmin_shouldPromote_whenActingUserIsOrgAdminAndMemberExists() {
        Long actingUserId = 1L;
        Long targetUserId = 2L;
        Long contextId = 100L;

        Organization org = new Organization();
        org.setId(10L);

        User actingUser = new User();
        actingUser.setId(actingUserId);
        actingUser.setOrg(org);
        org.setAdminUser(actingUser); // acting user es admin org

        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setOrg(org);

        Context context = new Context();
        context.setId(contextId);
        context.setOrganization(org);

        ContextMembership membership = new ContextMembership();
        membership.setContext(context);
        membership.setUser(targetUser);
        membership.setContextAdmin(false);

        PromoteContextAdminRequest request = new PromoteContextAdminRequest(targetUserId);

        when(userRepository.findById(actingUserId)).thenReturn(Optional.of(actingUser));
        when(contextRepository.findById(contextId)).thenReturn(Optional.of(context));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(membershipRepository.findByContextIdAndUserId(contextId, targetUserId))
                .thenReturn(Optional.of(membership));

        ContextAdminResponse response = contextService.promoteContextAdmin(actingUserId, contextId, request);

        assertNotNull(response);
        assertTrue(membership.isContextAdmin());
        verify(membershipRepository).save(membership);
    }

    @Test
    void demoteContextAdmin_shouldDemote_whenActingUserIsOrgAdmin() {
        Long actingUserId = 1L;
        Long targetUserId = 2L;
        Long contextId = 100L;

        Organization org = new Organization();
        org.setId(10L);

        User actingUser = new User();
        actingUser.setId(actingUserId);
        actingUser.setOrg(org);
        org.setAdminUser(actingUser);

        Context context = new Context();
        context.setId(contextId);
        context.setOrganization(org);

        ContextMembership membership = new ContextMembership();
        membership.setContext(context);
        membership.setUser(new User());
        membership.setContextAdmin(true);

        when(userRepository.findById(actingUserId)).thenReturn(Optional.of(actingUser));
        when(contextRepository.findById(contextId)).thenReturn(Optional.of(context));
        when(membershipRepository.findByContextIdAndUserId(contextId, targetUserId))
                .thenReturn(Optional.of(membership));

        contextService.demoteContextAdmin(actingUserId, contextId, targetUserId);

        assertFalse(membership.isContextAdmin());
        verify(membershipRepository).save(membership);
    }

    // ------------------------------------------------------------
    // addMember / removeMember / listMembers
    // ------------------------------------------------------------

    @Test
    void addMember_shouldAddMember_whenActingUserIsOrgAdminAndUserBelongsToOrg() {
        Long actingUserId = 1L;
        Long contextId = 100L;
        String email = "member@example.com";

        Organization org = new Organization();
        org.setId(10L);

        User actingUser = new User();
        actingUser.setId(actingUserId);
        actingUser.setOrg(org);
        org.setAdminUser(actingUser);

        Context context = new Context();
        context.setId(contextId);
        context.setOrganization(org);

        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setEmail(email);
        targetUser.setOrg(org);

        AddContextMemberRequest request = new AddContextMemberRequest(email);

        when(userRepository.findById(actingUserId)).thenReturn(Optional.of(actingUser));
        when(contextRepository.findById(contextId)).thenReturn(Optional.of(context));
        when(userRepository.findByEmail(email.trim())).thenReturn(Optional.of(targetUser));
        when(membershipRepository.existsByContextIdAndUserId(contextId, targetUser.getId()))
                .thenReturn(false);
        when(membershipRepository.save(any(ContextMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContextMemberResponse response = contextService.addMember(actingUserId, contextId, request);

        assertNotNull(response);
        verify(membershipRepository).save(any(ContextMembership.class));
    }

    @Test
    void listMembers_shouldThrowForbidden_whenUserCannotManageMembers() {
        Long actingUserId = 1L;
        Long contextId = 100L;

        Organization org = new Organization();
        org.setId(10L);

        User actingUser = new User();
        actingUser.setId(actingUserId);
        actingUser.setOrg(org);
        // NO adminUser => no es admin org
        org.setAdminUser(null);

        Context context = new Context();
        context.setId(contextId);
        context.setOrganization(org);

        when(userRepository.findById(actingUserId)).thenReturn(Optional.of(actingUser));
        when(contextRepository.findById(contextId)).thenReturn(Optional.of(context));
        // no es context admin tampoco
        when(membershipRepository.existsByContextIdAndUserIdAndContextAdminTrue(contextId, actingUserId))
                .thenReturn(false);

        assertThrows(ContextForbiddenException.class,
                () -> contextService.listMembers(actingUserId, contextId));
    }
}

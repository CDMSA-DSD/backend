package dsd.api.cdmsa.unit;

import dsd.api.cdmsa.dto.LinkResponse;
import dsd.api.cdmsa.exception.InvitationExpiredException;
import dsd.api.cdmsa.exception.InvitationInvalidException;
import dsd.api.cdmsa.exception.InvitationNotFoundException;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.OrganizationInvitation;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.repository.OrganizationInvitationRepository;
import dsd.api.cdmsa.service.OrganizationInvitationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationInvitationServiceTest {

    @Mock
    private OrganizationInvitationRepository invitationRepository;

    @InjectMocks
    private OrganizationInvitationService invitationService;

    @BeforeEach
    void setup() {
        // Simulamos el valor de @Value("${app.frontend.base-url}")
        ReflectionTestUtils.setField(invitationService, "frontendBaseUrl", "https://frontend.test");
    }

    // -----------------------------------------------------------
    // createGeneralInvitationLink(UserPrincipal)
    // -----------------------------------------------------------

    @Test
    void createGeneralInvitationLink_shouldCreateInvitationAndReturnLinkResponse() {
        // Preparamos el usuario autenticado
        User user = new User();
        user.setId(1L);

        Organization org = new Organization();
        org.setId(100L);
        user.setOrg(org);

        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUser()).thenReturn(user);

        // Mock del save — devolvemos la invitación de vuelta
        when(invitationRepository.save(any(OrganizationInvitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LinkResponse result = invitationService.createGeneralInvitationLink(principal);

        assertNotNull(result);
        assertTrue(result.link().contains("https://frontend.test/accept-invitation?token="));
        assertNotNull(result.expiresAt());
        assertNotNull(result.state());

        verify(invitationRepository).save(any(OrganizationInvitation.class));
    }


    // -----------------------------------------------------------
    // getInvitation(Long)
    // -----------------------------------------------------------

    @Test
    void getInvitation_shouldReturnInvitation_whenExists() {
        OrganizationInvitation inv = new OrganizationInvitation();
        inv.setId(10L);

        when(invitationRepository.findById(10L)).thenReturn(Optional.of(inv));

        OrganizationInvitation result = invitationService.getInvitation(10L);

        assertEquals(10L, result.getId());

        verify(invitationRepository).findById(10L);
    }

    @Test
    void getInvitation_shouldThrowException_whenNotFound() {
        when(invitationRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(InvitationNotFoundException.class,
                () -> invitationService.getInvitation(10L));
    }


    // -----------------------------------------------------------
    // getInvitationByToken(String)
    // -----------------------------------------------------------

    @Test
    void getInvitationByToken_shouldReturnInvitation_whenTokenValidAndNotExpired() {
        OrganizationInvitation inv = new OrganizationInvitation();
        inv.setToken("abcd");
        inv.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        inv.setState(OrganizationInvitation.State.ACTIVE);

        when(invitationRepository.findByToken("abcd")).thenReturn(Optional.of(inv));

        OrganizationInvitation result = invitationService.getInvitationByToken("abcd");

        assertNotNull(result);
        verify(invitationRepository).findByToken("abcd");
    }

    @Test
    void getInvitationByToken_shouldThrowInvalid_whenTokenNotFound() {
        when(invitationRepository.findByToken("badtoken")).thenReturn(Optional.empty());

        assertThrows(InvitationInvalidException.class,
                () -> invitationService.getInvitationByToken("badtoken"));
    }

    @Test
    void getInvitationByToken_shouldThrowExpired_whenInvitationExpired() {
        OrganizationInvitation inv = new OrganizationInvitation();
        inv.setToken("expired");
        inv.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));  // Expired

        when(invitationRepository.findByToken("expired"))
                .thenReturn(Optional.of(inv));

        assertThrows(InvitationExpiredException.class,
                () -> invitationService.getInvitationByToken("expired"));
    }


    // -----------------------------------------------------------
    // findAllInvitations(orgId, page, size)
    // -----------------------------------------------------------

    @Test
    void findAllInvitations_shouldReturnPageOfInvitations() {
        OrganizationInvitation inv = new OrganizationInvitation();
        inv.setId(1L);

        Page<OrganizationInvitation> pageResult =
                new PageImpl<>(java.util.List.of(inv));

        when(invitationRepository.findByOrgId(eq(5L), any(Pageable.class)))
                .thenReturn(pageResult);

        Page<OrganizationInvitation> result =
                invitationService.findAllInvitations(5L, 0, 10);

        assertEquals(1, result.getTotalElements());
        verify(invitationRepository).findByOrgId(eq(5L), any(Pageable.class));
    }
}
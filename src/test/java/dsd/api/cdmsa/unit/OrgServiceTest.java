package dsd.api.cdmsa.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import dsd.api.cdmsa.dto.*;
import dsd.api.cdmsa.exception.OrgExistsException;
import dsd.api.cdmsa.exception.OrgNotFoundException;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.OrganizationRepository;
import dsd.api.cdmsa.repository.UserRepository;
import dsd.api.cdmsa.service.OrgService;
import dsd.api.cdmsa.service.UserService;

@ExtendWith(MockitoExtension.class)
class OrgServiceTest {

    @Mock
    private OrganizationRepository orgRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private UserService userService;

    @InjectMocks
    private OrgService orgService;

    // ---------------------------------------------------
    // existOrg(name) / existOrgById(id)
    // ---------------------------------------------------

    @Test
    void existOrg_shouldReturnTrue_whenOrgExists() {
        when(orgRepo.existsByName("Acme")).thenReturn(true);

        boolean result = orgService.existOrg("Acme");

        assertTrue(result);
        verify(orgRepo).existsByName("Acme");
    }

    @Test
    void existOrg_shouldReturnFalse_whenOrgDoesNotExist() {
        when(orgRepo.existsByName("Acme")).thenReturn(false);

        boolean result = orgService.existOrg("Acme");

        assertFalse(result);
        verify(orgRepo).existsByName("Acme");
    }

    @Test
    void existOrgById_shouldReturnTrue_whenOrgExists() {
        when(orgRepo.existsById(1L)).thenReturn(true);

        boolean result = orgService.existOrgById(1L);

        assertTrue(result);
        verify(orgRepo).existsById(1L);
    }

    @Test
    void existOrgById_shouldReturnFalse_whenOrgDoesNotExist() {
        when(orgRepo.existsById(1L)).thenReturn(false);

        boolean result = orgService.existOrgById(1L);

        assertFalse(result);
        verify(orgRepo).existsById(1L);
    }

    // ---------------------------------------------------
    // createOrg(OrgAdminRequest)
    // ---------------------------------------------------

    // @Test
    // void createOrg_shouldCreateOrgAndAdmin_whenOrgDoesNotExist() {
    //     OrganizationResponse orgDto = new OrganizationResponse(
    //             "Acme", "Desc", "acme.com", null, null, null);
    //     SignInRequest adminDto = new SignInRequest(
    //             "John", "Doe", "john@acme.com", "pwd");
    //     OrgAdminRequest request = new OrgAdminRequest(orgDto, adminDto);

    //     when(orgRepo.existsByName("Acme")).thenReturn(false);

    //     when(orgRepo.save(any(Organization.class))).thenAnswer(invocation -> {
    //         Organization org = invocation.getArgument(0);
    //         if (org.getId() == null) {
    //             org.setId(1L); 
    //         }
    //         return org;
    //     });

    //     when(userService.createUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    //     OrgAdminResponse response = orgService.createOrg(request);

    //     assertNotNull(response);
    //     assertEquals(1L, response.orgId());
    //     assertNotNull(response.adminUri());
    //     assertTrue(response.adminUri().containsKey("admin"));

    //     verify(orgRepo).existsByName("Acme");
    //     verify(orgRepo, times(2)).save(any(Organization.class)); 
    //     verify(userService).createUser(any(User.class));
    // }

    // @Test
    // void createOrg_shouldThrowOrgExistsException_whenOrgAlreadyExists() {
        // OrganizationResponse orgDto = new OrganizationResponse(
                // "Acme", "Desc", "acme.com", null, null, null);
        // SignInRequest adminDto = new SignInRequest(
                // "John", "Doe", "john@acme.com", "pwd");
        // OrgAdminRequest request = new OrgAdminRequest(orgDto, adminDto);
// 
        // when(orgRepo.existsByName("Acme")).thenReturn(true);
// 
        // assertThrows(OrgExistsException.class, () -> orgService.createOrg(request));
// 
        // verify(orgRepo).existsByName("Acme");
        // verify(orgRepo, never()).save(any());
        // verify(userService, never()).createUser(any());
    // }

    // ---------------------------------------------------
    // deleteOrg(id)
    // ---------------------------------------------------

    @Test
    void deleteOrg_shouldCallRepositoryDeleteById() {
        orgService.deleteOrg(1L);

        verify(orgRepo).deleteById(1L);
    }

    // ---------------------------------------------------
    // findAllOrgs(orgId, page, size)
    // ---------------------------------------------------

    @Test
    void findAllOrgs_shouldReturnPageOfOrganizations() {
        Long orgId = 1L;
        Organization org = new Organization();
        org.setId(orgId);
        Page<Organization> pageResult = new PageImpl<>(List.of(org));

        when(orgRepo.findById(eq(orgId), any(Pageable.class))).thenReturn(pageResult);

        Page<Organization> result = orgService.findAllOrgs(orgId, 0, 5);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        
        verify(orgRepo).findById(eq(orgId), any(Pageable.class));
    }

    // ---------------------------------------------------
    // getOrgDetails(userId)
    // ---------------------------------------------------

    @Test
    void getOrgDetails_shouldReturnOrganizationResponse_whenUserExists() {
        Long userId = 1L;

        Organization org = new Organization();
        org.setName("Acme");
        org.setDescription("Desc");
        org.setDomain("acme.com");
        org.setSelectedRepoName("repo1");
        org.setSelectedBranchName("main");
        org.setRepoOwner("acme-owner");

        User user = new User();
        user.setId(userId);
        user.setOrg(org);

        when(userRepo.findById(userId)).thenReturn(Optional.of(user));

        OrganizationResponse result = orgService.getOrgDetails(userId);

        assertNotNull(result);
        assertEquals("Acme", result.companyName());
        assertEquals("Desc", result.description());
        assertEquals("acme.com", result.domain());
        assertEquals("repo1", result.selectedRepoName());
        assertEquals("main", result.selectedBranchName());
        assertEquals("acme-owner", result.repoOwner());

        verify(userRepo).findById(userId);
    }

    @Test
    void getOrgDetails_shouldThrowRuntimeException_whenUserNotFound() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> orgService.getOrgDetails(99L));
    }

    // ---------------------------------------------------
    // getOrgDetailsAlt(orgId)
    // ---------------------------------------------------

    @Test
    void getOrgDetailsAlt_shouldReturnOrg_whenOrgExists() {
        Organization org = new Organization();
        org.setId(1L);
        org.setName("Acme");

        when(orgRepo.findById(1L)).thenReturn(Optional.of(org));

        Organization result = orgService.getOrgDetailsAlt(1L);

        assertNotNull(result);
        assertEquals("Acme", result.getName());
        verify(orgRepo).findById(1L);
    }

    @Test
    void getOrgDetailsAlt_shouldThrowOrgNotFoundException_whenOrgDoesNotExist() {
        when(orgRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrgNotFoundException.class, () -> orgService.getOrgDetailsAlt(99L));
    }

    // ---------------------------------------------------
    // updateOrgDetails(userId, request)
    // ---------------------------------------------------

    @Test
    void updateOrgDetails_shouldUpdateAndReturnOrgResponse() {
        Long userId = 1L;

        Organization org = new Organization();
        org.setName("Old");
        org.setDescription("OldDesc");
        org.setDomain("old.com");

        User user = new User();
        user.setId(userId);
        user.setOrg(org);

        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(orgRepo.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateOrganizationRequest request = new UpdateOrganizationRequest(
                "  NewName  ",
                "  NewDesc  ",
                "  new.com  ",
                false,
                "  repo1  ",
                "  main  ");

        OrganizationResponse response = orgService.updateOrgDetails(userId, request);

        assertEquals("NewName", response.companyName());
        assertEquals("NewDesc", response.description());
        assertEquals("new.com", response.domain());
        assertEquals("repo1", response.selectedRepoName());
        assertEquals("main", response.selectedBranchName());

        verify(userRepo).findById(userId);
        verify(orgRepo).save(org);
    }

    // ---------------------------------------------------
    // saveRepoBranchSelection(userId, request)
    // ---------------------------------------------------

    @Test
    void saveRepoBranchSelection_shouldUpdateSelectedRepoAndBranch() {
        Long userId = 1L;

        Organization org = new Organization();
        org.setName("Acme");
        org.setDescription("Desc");
        org.setDomain("acme.com");

        User user = new User();
        user.setId(userId);
        user.setOrg(org);

        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(orgRepo.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RepoBranchSelectionRequest request = new RepoBranchSelectionRequest(
                "repo1",
                "main");

        OrganizationResponse response = orgService.saveRepoBranchSelection(userId, request);

        assertEquals("repo1", response.selectedRepoName());
        assertEquals("main", response.selectedBranchName());
        assertEquals("Acme", response.companyName());

        verify(userRepo).findById(userId);
        verify(orgRepo).save(org);
    }
}
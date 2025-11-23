package dsd.api.cdmsa.unit;

import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.service.JWTService;
import io.jsonwebtoken.ExpiredJwtException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JWTServiceTest {

    private JWTService jwtService;

    private static final String BASE64_SECRET = "dGhpc19pc19hX3Zlcnlfc2VjcmV0X2p3dF9rZXlfMTIz";

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();

        ReflectionTestUtils.setField(jwtService, "secretKey", BASE64_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
    }

    private User buildUser() {
        Organization org = new Organization();
        org.setId(99L);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setOrg(org);

        return user;
    }

    @Test
    void generateToken_shouldCreateNonNullToken_withValidClaimsAndSubject() {
        User user = buildUser();

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());

        String username = jwtService.extractUsername(token);
        assertEquals("test@example.com", username);
    }

    @Test
    void validateToken_shouldReturnTrue_forValidTokenAndMatchingUserDetails() {
        User user = buildUser();
        String token = jwtService.generateToken(user);

        UserDetails userDetails = new UserPrincipal(user, user.getOrg().getId());

        boolean valid = jwtService.validateToken(token, userDetails);

        assertTrue(valid);
    }

    @Test
    void validateToken_shouldReturnFalse_forTokenWithDifferentUsername() {
        User user = buildUser();
        String token = jwtService.generateToken(user);

        User otherUser = new User();

        UserDetails otherUserDetails = new UserPrincipal(otherUser, user.getOrg().getId());

        boolean valid = jwtService.validateToken(token, otherUserDetails);

        assertFalse(valid);
    }

    @Test
    void validateToken_shouldThrowExpiredJwtExcption_whenTokenExpires() {

        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);

        User user = buildUser();
        String token = jwtService.generateToken(user);

        UserDetails userDetails = new UserPrincipal(user, user.getOrg().getId());

        assertThrows(ExpiredJwtException.class, () -> jwtService.validateToken(token, userDetails));


    }
}
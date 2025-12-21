package dsd.api.cdmsa.security.authentication;


import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import dsd.api.cdmsa.dto.UserInfo;
import dsd.api.cdmsa.exception.UserNotFoundException;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.repository.UserRepository;
import dsd.api.cdmsa.service.MSAuthService;
import dsd.api.cdmsa.service.UserService;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component
public class OAuthCodeAuthenticationProvider implements AuthenticationProvider {

    private final MSAuthService msAuthService;
    private final UserRepository userRepository; 

    @Override
    public Authentication authenticate(Authentication authentication) {
        OAuthCodeAuthenticationToken token = (OAuthCodeAuthenticationToken) authentication;

        UserInfo info = msAuthService.extractUserInfo(token.getCredentials().toString());

        System.out.println(info.providerId());

        User user = userRepository.findByEmailAndProviderUserId(info.email(), info.providerId()).orElseThrow(() -> new UserNotFoundException(info.email()));

        UserPrincipal principal = new UserPrincipal(user, user.getOrg().getId());

        return new OAuthCodeAuthenticationToken(principal);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuthCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }

}

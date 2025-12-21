package dsd.api.cdmsa.security.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import dsd.api.cdmsa.model.UserPrincipal;

public class OAuthCodeAuthenticationToken extends AbstractAuthenticationToken {

    private final String authroizationCode;
    private final UserPrincipal userPrincipal;
    private final String provider;
    
    public OAuthCodeAuthenticationToken(String authorizationCode, String provider) {
        super(null);
        this.authroizationCode = authorizationCode;
        this.provider = provider;
        this.userPrincipal = null;
        setAuthenticated(false);
    }

    public OAuthCodeAuthenticationToken(UserPrincipal principal) {
        super(principal.getAuthorities());
        this.authroizationCode = null;
        this.provider = null;
        this.userPrincipal = principal;
        setAuthenticated(true);       
    }

    @Override
    public Object getCredentials() {
        return authroizationCode;
    }

    @Override
    public Object getPrincipal() {
        return userPrincipal;
    }

    public String getProvider() {
        return provider;
    }

}

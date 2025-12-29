package dsd.api.cdmsa.service;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.net.URL;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;

import dsd.api.cdmsa.dto.UserInfo;

@Service
public class MSAuthService {

    @Value("${microsoft.oauth.client-id}")
    private String clientId;

    @Value("${microsoft.oauth.scope}")
    private String scope;

    @Value("${microsoft.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${microsoft.oauth.client-secret}")
    private String secretId;

    @Value("${microsoft.oauth.token-url}")
    private String tokenUrl;

    // URL with the public keys of MS
    private static final String MS_PUBLIC_KEYS_URL = "https://login.microsoftonline.com/common/discovery/v2.0/keys";

    public String exchangeCodeForTokenResponse(String code) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", secretId);
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                String.class);

        return response.getBody();
    }

    // Validate the Signature of the Token
    private boolean validateSignature(SignedJWT signedJWT) throws Exception {
        // Get the MS public Keys
        JWKSet publicKeys = JWKSet.load(new URL(MS_PUBLIC_KEYS_URL));

        // Try with all the keys
        for (JWK jwk : publicKeys.getKeys()) {
            RSAKey rsaPublicKey = jwk.toRSAKey();
            RSAPublicKey publicKey = rsaPublicKey.toRSAPublicKey();
            JWSVerifier verifier = new RSASSAVerifier(publicKey);

            if (signedJWT.verify(verifier)) {
                return true;
            }
        }
        return false; // If any key verified the signature, is invalid
    }

    private boolean isTokenExpired(JWTClaimsSet claims) {
        return claims.getExpirationTime().before(new Date());
    }

    private boolean isAudienceValid(JWTClaimsSet claims) {
        return claims.getAudience().contains(clientId);
    }

    private boolean isIssuerValid(JWTClaimsSet claims) {
        System.out.println(claims.getIssuer());
        return claims.getIssuer().startsWith("https://login.microsoftonline.com/");
    }

    private void validateClaims(JWTClaimsSet claims) throws Exception {
        // Valid claims (aud, sub, iss)
        if (!isAudienceValid(claims)) {
            throw new Exception("Invalid audience");
        }

        if (!isIssuerValid(claims)) {
            throw new Exception("Invalid issuer");
        }

        if (isTokenExpired(claims)) {
            throw new Exception("Token expired");
        }
    }

    public JWTClaimsSet verifyMSToken(String token) {
        try {
            // Parse token as a SignedJWT
            SignedJWT signedJWT = (SignedJWT) JWTParser.parse(token);

            // Verify the signature
            if (!validateSignature(signedJWT)) {
                throw new Exception("Invalid token signature");
            }

            // Extraer los claims del id_token
            final JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Check the claims
            validateClaims(claims);

            return claims;
        } catch (Exception e) {
            // TODO: handle the exception
            System.err.println("Token validation failed: " + e.getMessage());
            return null;
        }
    }

    public UserInfo extractUserInfo(String code) {
        // Exchange code for token
        String tokenResponse = exchangeCodeForTokenResponse(code);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = null;
        try {
            json = mapper.readTree(tokenResponse);
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String tokenId = json.get("id_token").asText();

        JWTClaimsSet claims = verifyMSToken(tokenId);

        String msId = claims.getSubject();
        try {
            String email = claims.getStringClaim("email");
            String name = claims.getStringClaim("name");
            String firstName, lastName;

            if (name != null) {
                String[] nameparts = splitName(name);
                firstName = nameparts[0];
                lastName = nameparts[1];
            } else {
                firstName = "Unkwon";
                lastName = "";
            }

            return new UserInfo(email, firstName, lastName, "MS", msId);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

    }

    private String[] splitName(String fullName) {
        // Split full name by space
        String[] nameParts = fullName.split(" ");

        // If there are multiple parts, treat the first as first name and rest as last
        // name
        if (nameParts.length > 1) {
            String firstName = nameParts[0]; // First name
            String lastName = String.join(" ", Arrays.copyOfRange(nameParts, 1, nameParts.length)); // Join remaining as
                                                                                                    // last name

            return new String[] { firstName, lastName };
        } else {
            // If only one part, use as first name and leave last name empty
            return new String[] { nameParts[0], "" };
        }
    }

}

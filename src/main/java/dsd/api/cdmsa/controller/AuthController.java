package dsd.api.cdmsa.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dsd.api.cdmsa.dto.LoginRequest;
import dsd.api.cdmsa.dto.LoginResponse;
import dsd.api.cdmsa.dto.MSOrgSignInRequest;
import dsd.api.cdmsa.dto.MSSignInRequest;
import dsd.api.cdmsa.dto.OrgAdminRequest;
import dsd.api.cdmsa.dto.OrgAdminResponse;
import dsd.api.cdmsa.dto.SignInRequest;
import dsd.api.cdmsa.service.OrgService;
import dsd.api.cdmsa.service.UserService;
import jakarta.validation.Valid;

import lombok.AllArgsConstructor;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final OrgService orgService;
    private final UserService userService;

    @PostMapping("/register-org")
    public ResponseEntity<LoginResponse> registerOrg(@Valid @RequestBody OrgAdminRequest newOrg) {
        // Store Org
        OrgAdminResponse dto = orgService.createOrg(newOrg, null);

        // Return org's URI in header and admin's URI in body
        return ResponseEntity.created(linkTo(methodOn(OrgController.class).getOrg(dto.orgId())).toUri())
                .body(dto.admin());
    }

    @PostMapping("/register-org/oauth2/microsoft")
    public ResponseEntity<LoginResponse> registerOrgMS(@RequestBody MSOrgSignInRequest code){
        // Store Org
        OrgAdminResponse dto = orgService.createOrg(null, code);

        // Return org's URI in header and admin's URI in body
        return ResponseEntity.created(linkTo(methodOn(OrgController.class).getOrg(dto.orgId())).toUri())
                .body(dto.admin());
    }

    @PostMapping("/register-invitation")
    ResponseEntity<LoginResponse> register(@Valid @RequestBody SignInRequest newUser, @RequestParam String token) {
        // Store user
        LoginResponse user = userService.createUserByInvitation(newUser, token); //add link invitation
        // Return answer
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest user) {
        return  ResponseEntity.ok(userService.login(user));
    }

    @PostMapping("/oauth2/microsoft")
    public ResponseEntity<LoginResponse> microsoftSignIn(@RequestBody MSSignInRequest code){
        if (code.token() == null || code.token().isBlank()) {
            return ResponseEntity.ok(userService.loginWithMS(code));
        } else {
            LoginResponse user = userService.createUserByMS(code);
            return ResponseEntity.ok(user);            
        }
    }
}

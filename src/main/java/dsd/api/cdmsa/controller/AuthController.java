package dsd.api.cdmsa.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dsd.api.cdmsa.dto.LoginRequest;
import dsd.api.cdmsa.dto.LoginResponse;
import dsd.api.cdmsa.dto.MSSignInRequest;
import dsd.api.cdmsa.dto.OrgAdminRequest;
import dsd.api.cdmsa.dto.OrgAdminResponse;
import dsd.api.cdmsa.dto.SignInRequest;
import dsd.api.cdmsa.model.User;
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
    public ResponseEntity<Map<String, Object>> registerOrg(@Valid @RequestBody OrgAdminRequest newOrg) {
        // Store Org
        OrgAdminResponse dto = orgService.createOrg(newOrg);

        // Return org's URI in header and admin's URI in body
        return ResponseEntity.created(linkTo(methodOn(OrgController.class).getOrg(dto.orgId())).toUri())
                .body(dto.adminUri());
    }

    @PostMapping("/register-invitation")
    ResponseEntity<Void> register(@Valid @RequestBody SignInRequest newUser, @RequestParam String token) {
        // Store user
        User user = userService.createUserByInvitation(newUser, token); //add link invitation
        // Return answer
        return ResponseEntity.created(linkTo(methodOn(UserController.class).getUser(user.getId())).toUri()).build();
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest user) {
        return  userService.login(user);
    }

    @PostMapping("/oauth2/microsoft")
    public LoginResponse microsoftSignIn(@RequestBody MSSignInRequest code){
        if (code.token() == null || code.token().isBlank()) {
            return userService.loginWithMS(code);
        } else {
            userService.createUserByMS(code);
            return null;
            
        }
    }
}

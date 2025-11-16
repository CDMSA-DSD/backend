package dsd.api.cdmsa.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dsd.api.cdmsa.dto.LoginRequest;
import dsd.api.cdmsa.dto.OrgAdminRequest;
import dsd.api.cdmsa.dto.OrgAdminResponse;
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
        return ResponseEntity.created(linkTo(methodOn(OrgController.class).getOrg(dto.id_org())).toUri())
                .body(dto.adminUri());
    }

    @PostMapping("/register-invitation")
    ResponseEntity<Void> register(@Valid @RequestBody User newUser) {
        // Store user
        User user = userService.createUser(newUser); //add link invitation
        // Return answer
        return ResponseEntity.created(linkTo(methodOn(UserController.class).getUser(user.getId())).toUri()).build();
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest user) {
        return userService.verify(user); // Return JWT token con dto or cookie
    }
}

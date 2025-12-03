package dsd.api.cdmsa.controller;

import org.springframework.data.domain.Page;
import org.springframework.hateoas.PagedModel;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import dsd.api.cdmsa.assembler.UserResponseModelAssembler;
import dsd.api.cdmsa.dto.UserResponse;
import dsd.api.cdmsa.exception.*;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.service.UserService;

import lombok.AllArgsConstructor;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {

    private final UserService service;

    private UserResponseModelAssembler userResponseModelAssembler;
    private PagedResourcesAssembler<User> pagedResourcesAssembler;

    // GET user (individual)
    @GetMapping(value = "/{id}")
    @PreAuthorize("@permissionService.canManageOrg(principal)")
    public ResponseEntity<EntityModel<UserResponse>> getUser(@PathVariable Long id) {
        // Search user
        User user = service.searchById(id);
        // Return it with links
        return ResponseEntity.ok(userResponseModelAssembler.toModel(user));
    }

    // GET users (collection)
    @GetMapping
    @PreAuthorize("@permissionService.canManageOrg(principal)")
    public ResponseEntity<PagedModel<EntityModel<UserResponse>>> getAllUsers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "2", required = false) int size) {

        Page<User> users = service.findAllUsers(principal.getOrgId(),page, size);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(users, userResponseModelAssembler));
    }

    /*
     * // PUT user
     * 
     * @PutMapping("users/{id}")
     * public ResponseEntity<Void> replaceUser(@Valid @RequestBody User
     * newUser, @PathVariable Long id) {
     * service.searchById(id).map(User -> {
     * User.setName(newUser.getName());
     * User.setUsername(newUser.getUsername());
     * User.setEmail(newUser.getEmail());
     * User.setPassword(newUser.getPassword());
     * return service.createUser(User);
     * }).orElseThrow(() -> new UserNotFoundException(id));
     * 
     * return ResponseEntity.noContent().build();
     * }
     */

    // DELETE user
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.canManageOrg(principal)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (service.existUserById(id)) {
            service.deleteUser(id);
        } else {
            throw new UserNotFoundException(id);
        }
        return ResponseEntity.noContent().build();
    }

}

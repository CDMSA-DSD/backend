package dsd.api.cdmsa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.List;

import dsd.api.cdmsa.exception.*;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.payload.LoginRequest;
import dsd.api.cdmsa.service.UserService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {

    private final UserService service;

    // POST users
    @PostMapping()
    ResponseEntity<Void> newUser(@Valid @RequestBody User newUser) {
        // Store user
        User user = service.createUser(newUser);
        // Return answer
        return ResponseEntity.created(linkTo(UserController.class).slash(user.getId()).toUri()).build();
    }

    @PostMapping("/login")
    ResponseEntity<String> login(@Valid @RequestBody LoginRequest login) {
        // Checks credentials
        if (service.login(login)) {
            // Return answer
            return ResponseEntity.ok().body("Login successful");
        }

        return ResponseEntity.status(401).body("Invalid username or password");

    }

    // GET user (individual)
    @GetMapping(value = "/{id}", produces = { "application/json" })
    public ResponseEntity<User> getUser(@PathVariable Integer id) {
        User user = service.searchById(id).orElseThrow(() -> new UserNotFoundException(id));
        return ResponseEntity.ok(user);
    }

    // GET users (collection)
    @GetMapping(value = "", produces = { "application/json" })
    public ResponseEntity<List<User>> getUsers() {
        List<User> users = service.findUsers();
        return ResponseEntity.ok(users);
    }

    // PUT user
    @PutMapping("/{id}")
    public ResponseEntity<Void> replaceUser(@Valid @RequestBody User newUser, @PathVariable Integer id) {
        service.searchById(id).map(User -> {
            User.setName(newUser.getName());
            User.setUsername(newUser.getUsername());
            User.setEmail(newUser.getEmail());
            User.setPassword(newUser.getPassword());
            return service.createUser(User);
        }).orElseThrow(() -> new UserNotFoundException(id));

        return ResponseEntity.noContent().build();
    }

    // DELETE user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        if (service.existUserById(id)) {
            service.deleteUser(id);
        } else {
            throw new UserNotFoundException(id);
        }
        return ResponseEntity.noContent().build();
    }

}

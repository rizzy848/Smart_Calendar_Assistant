package api.controller;

import api.dto.UserDTO;
import Framework.UserManager;
import entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {
        "http://localhost:3000",
        "https://smartcalendarassistant-production.up.railway.app/"  // Add actual URL here
})
public class UserController {

    private final UserManager userManager;

    @Autowired
    public UserController(UserManager userManager) {
        this.userManager = userManager;
        System.out.println("✅ UserController initialized with shared UserManager");
    }

    @PostMapping(value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDTO> register(@RequestBody UserDTO dto) {
        System.out.println("📥 Register request: " + dto.getEmail());

        User user = userManager.registerUser(dto.getUsername(), dto.getEmail());

        System.out.println("✅ User registered: " + user.getUserId());
        return ResponseEntity.ok(UserDTO.fromUser(user));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userManager.getAllUsers().stream()
                .map(UserDTO::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}
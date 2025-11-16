package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.model.User;
import com.example.authservice.security.JwtUtil;
import com.example.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "API de autenticación - Login y Registro")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ===== LOGIN =====
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autenticar usuario y generar token JWT")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Autenticar
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Obtener usuario
            User user = authService.findByEmail(loginRequest.getEmail());

            // Actualizar última sesión
            user.setLastLogin(LocalDateTime.now());
            authService.save(user);

            // Generar token
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            // Crear respuesta
            AuthResponse authResponse = new AuthResponse(
                    token,
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole().name()
            );

            response.put("success", true);
            response.put("message", "Login exitoso");
            response.put("data", authResponse);

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            response.put("success", false);
            response.put("message", "Credenciales incorrectas");
            response.put("error", "Email o contraseña inválidos");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error en el login");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===== REGISTER =====
    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crear nueva cuenta de usuario")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Verificar si el email ya existe
            if (authService.existsByEmail(registerRequest.getEmail())) {
                response.put("success", false);
                response.put("message", "El email ya está registrado");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Crear usuario
            User user = new User();
            user.setEmail(registerRequest.getEmail());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setFirstName(registerRequest.getFirstName());
            user.setLastName(registerRequest.getLastName());

            User savedUser = authService.save(user);

            // Generar token automáticamente
            String token = jwtUtil.generateToken(savedUser);

            AuthResponse authResponse = new AuthResponse(
                    token,
                    savedUser.getId(),
                    savedUser.getEmail(),
                    savedUser.getFirstName(),
                    savedUser.getLastName(),
                    savedUser.getRole().name()
            );

            response.put("success", true);
            response.put("message", "Usuario registrado exitosamente");
            response.put("data", authResponse);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al registrar usuario");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===== GET CURRENT USER =====
    @GetMapping("/me")
    @Operation(summary = "Obtener usuario actual", description = "Obtiene la información del usuario autenticado")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Map<String, Object> response = new HashMap<>();

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = authService.findByEmail(email);

            if (user == null) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            UserResponse userResponse = new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole().name(),
                    user.getEnabled()
            );

            response.put("success", true);
            response.put("data", userResponse);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener usuario");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
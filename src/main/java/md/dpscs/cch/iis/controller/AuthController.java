package md.dpscs.cch.iis.controller;

import md.dpscs.cch.iis.dto.AuthResponse;
import md.dpscs.cch.iis.dto.LoginRequest;
import md.dpscs.cch.iis.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseCookie;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        // 1. Extract IP Address from the request
        String ipAddress = request.getRemoteAddr();

        // 2. Pass it to the service
        AuthResponse authResponse = authService.loginUser(loginRequest, ipAddress);

        // Create HttpOnly Cookie
        ResponseCookie jwtCookie = ResponseCookie.from("accessToken", authResponse.getToken())
                .httpOnly(true)
                .secure(true) // MUST be true in Prod (Requires HTTPS)
                .path("/")
                .maxAge(24 * 60 * 60) // 24 hours (match your JWT expiry)
                .sameSite("Strict")
                .build();

        // Remove token from body to prevent frontend storage
        authResponse.setToken(null);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        ResponseCookie cookie = ResponseCookie.from("accessToken", null)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Logged out");
    }
}
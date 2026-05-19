package com.example.taskmanager.service;

import com.example.taskmanager.dto.*;
import com.example.taskmanager.entity.PendingRegistration;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.repository.PendingRegistrationRepository;
import com.example.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String authServerUrl;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${keycloak.client-id:task-manager-client}")
    private String clientId;

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final EmailService emailService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void register(UserRegistrationDTO registrationDTO) {
        // Check if user already exists in local DB
        if (userRepository.findByUsername(registrationDTO.getUsername()).isPresent() ||
            userRepository.findByEmail(registrationDTO.getEmail()).isPresent()) {
            throw new RuntimeException("User with this username or email already exists");
        }

        // Generate OTP
        String otp = String.format("%06d", new Random().nextInt(1000000));

        // Save to pending registrations
        pendingRegistrationRepository.deleteByEmail(registrationDTO.getEmail());
        PendingRegistration pending = PendingRegistration.builder()
                .username(registrationDTO.getUsername())
                .password(registrationDTO.getPassword())
                .email(registrationDTO.getEmail())
                .firstName(registrationDTO.getFirstName())
                .lastName(registrationDTO.getLastName())
                .role(registrationDTO.getRole() != null ? registrationDTO.getRole() : "USER")
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        pendingRegistrationRepository.save(pending);

        // Send OTP email
        emailService.sendOtpEmail(registrationDTO.getEmail(), otp);
    }

    @Transactional
    public void verifyOtp(OtpVerificationRequest verificationRequest) {
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(verificationRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("No pending registration found for this email"));

        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingRegistrationRepository.delete(pending);
            throw new RuntimeException("OTP has expired");
        }

        if (!pending.getOtp().equals(verificationRequest.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        // Register in Keycloak
        registerInKeycloak(pending);

        // Save to local database
        User dbUser = User.builder()
                .username(pending.getUsername())
                .email(pending.getEmail())
                .firstName(pending.getFirstName())
                .lastName(pending.getLastName())
                .role(pending.getRole().toUpperCase())
                .build();
        userRepository.save(dbUser);

        // Remove from pending
        pendingRegistrationRepository.delete(pending);
    }

    private void registerInKeycloak(PendingRegistration pending) {
        String adminToken = getAdminToken();
        String realmName = issuerUri.substring(issuerUri.lastIndexOf("/") + 1);
        String usersUrl = authServerUrl + "/admin/realms/" + realmName + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> user = new HashMap<>();
        user.put("username", pending.getUsername());
        user.put("email", pending.getEmail());
        user.put("firstName", pending.getFirstName());
        user.put("lastName", pending.getLastName());
        user.put("enabled", true);
        user.put("emailVerified", true); // We verified it via OTP

        Map<String, Object> credentials = new HashMap<>();
        credentials.put("type", "password");
        credentials.put("value", pending.getPassword());
        credentials.put("temporary", false);

        user.put("credentials", Collections.singletonList(credentials));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(user, headers);
        try {
            restTemplate.postForEntity(usersUrl, request, Void.class);
        } catch (HttpStatusCodeException e) {
            String errorBody = e.getResponseBodyAsString();
            throw new RuntimeException("Failed to register user in Keycloak. Status: " + e.getStatusCode() + ". Body: " + errorBody, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Keycloak Admin API. Error: " + e.getMessage(), e);
        }
    }

    private String getAdminToken() {
        String url = authServerUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("client_id", "admin-cli");
        map.add("username", "admin");
        map.add("password", "admin");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            if (response == null || !response.containsKey("access_token")) {
                throw new RuntimeException("Keycloak response did not contain access_token");
            }
            return (String) response.get("access_token");
        } catch (HttpStatusCodeException e) {
            String errorBody = e.getResponseBodyAsString();
            throw new RuntimeException("Failed to obtain admin token from Keycloak. " +
                    "Status: " + e.getStatusCode() + ". " +
                    "Body: " + (errorBody.isEmpty() ? "[no body]" : errorBody), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Keycloak at " + url + ". Error: " + e.getMessage(), e);
        }
    }

    public TokenResponse login(LoginRequest loginRequest) {
        String url = issuerUri + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("client_id", clientId);
        map.add("username", loginRequest.getUsername());
        map.add("password", loginRequest.getPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        return restTemplate.postForObject(url, request, TokenResponse.class);
    }

    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String url = issuerUri + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "refresh_token");
        map.add("client_id", clientId);
        map.add("refresh_token", refreshTokenRequest.getRefreshToken());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        return restTemplate.postForObject(url, request, TokenResponse.class);
    }

    public void logout(RefreshTokenRequest logoutRequest) {
        String url = issuerUri + "/protocol/openid-connect/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("refresh_token", logoutRequest.getRefreshToken());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        restTemplate.postForObject(url, request, String.class);
    }
}

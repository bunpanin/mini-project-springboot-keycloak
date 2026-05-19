package com.example.taskmanager.service;

import com.example.taskmanager.dto.LoginRequest;
import com.example.taskmanager.dto.RefreshTokenRequest;
import com.example.taskmanager.dto.TokenResponse;
import com.example.taskmanager.dto.UserRegistrationDTO;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final RestTemplate restTemplate = new RestTemplate();

    public void register(UserRegistrationDTO registrationDTO) {
        String adminToken = getAdminToken();
        String realmName = issuerUri.substring(issuerUri.lastIndexOf("/") + 1);
        String usersUrl = authServerUrl + "/admin/realms/" + realmName + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> user = new HashMap<>();
        user.put("username", registrationDTO.getUsername());
        user.put("email", registrationDTO.getEmail());
        user.put("firstName", registrationDTO.getFirstName());
        user.put("lastName", registrationDTO.getLastName());
        user.put("enabled", true);
        
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("type", "password");
        credentials.put("value", registrationDTO.getPassword());
        credentials.put("temporary", false);
        
        user.put("credentials", Collections.singletonList(credentials));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(user, headers);
        try {
            restTemplate.postForEntity(usersUrl, request, Void.class);
            
            // Save to PostgreSQL after successful Keycloak registration
            String role = registrationDTO.getRole();
            if (role == null || role.isEmpty()) {
                role = "USER";
            }
            
            User dbUser = User.builder()
                    .username(registrationDTO.getUsername())
                    .email(registrationDTO.getEmail())
                    .firstName(registrationDTO.getFirstName())
                    .lastName(registrationDTO.getLastName())
                    .role(role.toUpperCase())
                    .build();
            userRepository.save(dbUser);
            
        } catch (HttpStatusCodeException e) {
            String errorBody = e.getResponseBodyAsString();
            throw new RuntimeException("Failed to register user in Keycloak. " +
                    "Status: " + e.getStatusCode() + ". " +
                    "Body: " + (errorBody.isEmpty() ? "[no body]" : errorBody), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Keycloak Admin API at " + usersUrl + ". Error: " + e.getMessage(), e);
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

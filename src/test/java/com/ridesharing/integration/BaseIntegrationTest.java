package com.ridesharing.integration;

import com.ridesharing.client.GoogleMapsDistanceClient;
import com.ridesharing.client.GoogleMapsGeocodingClient;
import com.ridesharing.client.PaymentGatewayClient;
import com.ridesharing.domain.User;
import com.ridesharing.domain.Vehicle;
import com.ridesharing.dto.AuthResponseDTO;
import com.ridesharing.dto.LoginRequestDTO;
import com.ridesharing.dto.RegisterRequestDTO;
import com.ridesharing.repository.UserRepository;
import com.ridesharing.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(TestConfig.class)
public abstract class BaseIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected VehicleRepository vehicleRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @MockBean
    protected GoogleMapsGeocodingClient geocodingClient;

    @MockBean
    protected GoogleMapsDistanceClient distanceClient;

    @MockBean
    protected PaymentGatewayClient paymentGatewayClient;

    @MockBean
    protected JavaMailSender mailSender;

    @BeforeEach
    void setUpMocks() {
        // Default geocoding stub: return fixed coords for any address
        when(geocodingClient.geocode(anyString())).thenReturn(new double[]{12.9716, 77.5946});
    }

    // ---- Helper: register a user and return their JWT ----

    protected String registerAndLogin(String email, String password, String fullName, String phone) {
        RegisterRequestDTO reg = new RegisterRequestDTO();
        reg.setEmail(email);
        reg.setPassword(password);
        reg.setFullName(fullName);
        reg.setPhone(phone);
        restTemplate.postForEntity("/auth/register", reg, Map.class);

        LoginRequestDTO login = new LoginRequestDTO();
        login.setEmail(email);
        login.setPassword(password);
        ResponseEntity<AuthResponseDTO> resp = restTemplate.postForEntity("/auth/login", login, AuthResponseDTO.class);
        return resp.getBody().getToken();
    }

    // ---- Helper: add vehicle for a user (required before posting trips) ----

    protected void addVehicleForUser(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Vehicle vehicle = Vehicle.builder()
                .user(user)
                .make("Toyota")
                .model("Camry")
                .year(2020)
                .color("White")
                .licensePlate("KA01AB1234")
                .passengerCapacity(4)
                .build();
        vehicleRepository.save(vehicle);
    }

    // ---- Helper: build Authorization header ----

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    protected <T> ResponseEntity<T> get(String url, String token, Class<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), responseType);
    }

    protected <T> ResponseEntity<T> post(String url, Object body, String token, Class<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), responseType);
    }

    protected <T> ResponseEntity<T> delete(String url, String token, Class<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), responseType);
    }

    // ---- Helper: suspend a user directly via DB ----

    protected void suspendUser(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setSuspended(true);
        userRepository.save(user);
    }

    // ---- Helper: make user ADMIN ----

    protected void makeAdmin(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setRole("ADMIN");
        userRepository.save(user);
    }
}

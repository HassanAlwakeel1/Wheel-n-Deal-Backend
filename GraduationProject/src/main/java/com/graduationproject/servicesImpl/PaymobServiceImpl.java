package com.graduationproject.servicesImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduationproject.DTOs.paymobPaymentDTOs.PayResponseDTO;
import com.graduationproject.DTOs.paymobPaymentDTOs.SecondRequest;
import com.graduationproject.DTOs.paymobPaymentDTOs.ThirdRequest;
import com.graduationproject.DTOs.paymobPaymentDTOs.WalletRequest;
import com.graduationproject.entities.PaymobResponse;
import com.graduationproject.entities.User;
import com.graduationproject.mapper.PaymopResponseMapper;
import com.graduationproject.repositories.PaymobResponseRepository;
import com.graduationproject.repositories.UserRepository;
import com.graduationproject.services.PaymobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymobServiceImpl implements PaymobService {
    private final PaymopResponseMapper paymopResponseMapper;
    final private PaymobResponseRepository paymobResponseRepository;
    final private UserRepository userRepository;

    public String getAuthToken(String apiKey) throws JsonProcessingException {
        // API endpoint for obtaining authentication token
        String url = "https://accept.paymob.com/api/auth/tokens";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Construct the JSON request body
        String requestJson = "{\"api_key\": \"" + apiKey + "\"}";

        HttpEntity<String> request = new HttpEntity<>(requestJson, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            // Parse the JSON response to extract the authentication token
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("token").asText();
        } else {
            return "Error occurred: " + response.getStatusCode();
        }
    }
    public String createEcommerceOrder(SecondRequest secondRequest) throws JsonProcessingException {
        String orderUrl = "https://accept.paymob.com/api/ecommerce/orders";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(secondRequest.getAuth_token());

        HttpEntity<SecondRequest> requestEntity = new HttpEntity<>(secondRequest, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(orderUrl, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            // Parse the JSON response to extract the order ID
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("id").asText();
        } else {
            return "Error occurred: " + response.getStatusCode();
        }
    }
    public String sendPaymentKeyRequest(ThirdRequest thirdRequest) throws JsonProcessingException {
        String paymentKeyUrl = "https://accept.paymob.com/api/acceptance/payment_keys";

        // Set request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(thirdRequest.getAuth_token());

        // Create HTTP request entity
        HttpEntity<ThirdRequest> request = new HttpEntity<>(thirdRequest, headers);

        // Send POST request to the API endpoint
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(paymentKeyUrl, request, String.class);

        // Process the response
        if (response.getStatusCode() == HttpStatus.CREATED) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("token").asText();
        } else {
            return "Error occurred: " + response.getStatusCode();
        }
    }
    public void savePayResponse(PayResponseDTO payResponse) {
        PaymobResponse responseEntity = paymopResponseMapper.toEntity(payResponse);
        paymobResponseRepository.save(responseEntity);
    }
    public ResponseEntity<?> sendPaymentRequest(WalletRequest walletRequest) {
        // Validate wallet request and payment token
        if (walletRequest == null || walletRequest.getPayment_token() == null || walletRequest.getPayment_token().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Invalid wallet request or payment token."
            ));
        }

        String paymentUrl = "https://accept.paymob.com/api/acceptance/payments/pay";

        // Set request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(walletRequest.getPayment_token());

        // Create HTTP request entity
        HttpEntity<WalletRequest> request = new HttpEntity<>(walletRequest, headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            // Send POST request to the API endpoint
            ResponseEntity<String> response = restTemplate.postForEntity(paymentUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                // Process the response
                ObjectMapper objectMapper = new ObjectMapper();
                PayResponseDTO walletResponse = objectMapper.readValue(response.getBody(), PayResponseDTO.class);

                String phoneNumber = walletResponse.getSource_data().getPhone_number();
                User user = userRepository.findByPhoneNumber(phoneNumber);

                if (user == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                            "message", "User with phone number " + phoneNumber + " does not exist."
                    ));
                }

                // Update user's wallet amount
                double amountInDollars = walletResponse.getAmount_cents() / 100.0;
                user.setAmount((long) (user.getAmount() + amountInDollars));
                userRepository.save(user);

                savePayResponse(walletResponse);

                return ResponseEntity.ok(Map.of(
                        "message", "Payment request successful.",
                        "data", walletResponse
                ));

            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Map.of(
                        "message", "Payment request failed with status: " + response.getStatusCode()
                ));
            }

        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Failed to process payment response.",
                    "details", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "An error occurred while sending the payment request.",
                    "details", e.getMessage()
            ));
        }
    }

}
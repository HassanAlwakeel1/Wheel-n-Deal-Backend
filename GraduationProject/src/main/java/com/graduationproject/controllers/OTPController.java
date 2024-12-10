package com.graduationproject.controllers;

import com.graduationproject.DTOs.optDTOs.OtpValidationRequest;
import com.graduationproject.services.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("api/v1/OTP")
public class OTPController {

    @Autowired
    private SmsService smsService;

    @PostMapping("send-otp")
    public ResponseEntity<Object> sendOtp(@RequestParam String phoneNumber) {
        return smsService.sendSMS(phoneNumber);
    }

    @PostMapping("validate-otp")
    public ResponseEntity<Object> validateOtp(@RequestBody OtpValidationRequest otpValidationRequest) {
        return smsService.validateOtp(otpValidationRequest);
    }
    @PostMapping("forget-password")
    public ResponseEntity<Object> forgetPassword(@RequestParam String phoneNumber, @RequestParam String newPassword) {
        return smsService.forgetPassword(phoneNumber, newPassword);
    }
}
package com.example.taskmanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@taskmanager.com");
        message.setTo(to);
        message.setSubject("Task Manager - Registration OTP");
        message.setText("Your OTP for registration is: " + otp + ". It will expire in 10 minutes.");
        mailSender.send(message);
    }
}

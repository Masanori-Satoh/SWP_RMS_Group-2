package com.group2.rms.auth.service;

import org.springframework.stereotype.Service;

import com.group2.rms.auth.exception.PasswordRecoveryUnavailableException;

@Service
public class PasswordRecoveryService {
    private final PasswordResetService resetService;
    private final PasswordResetEmailSender emailSender;

    public PasswordRecoveryService(PasswordResetService resetService,
            PasswordResetEmailSender emailSender) {
        this.resetService = resetService;
        this.emailSender = emailSender;
    }

    public void requestReset(String email) {
        //check configure
        if(!emailSender.isConfigured() || !resetService.isConfigured()){
            throw new PasswordRecoveryUnavailableException();
        }


        resetService.request(email).ifPresent(link -> emailSender.send(link.email(), link.token(),link.otp()));
    }
}

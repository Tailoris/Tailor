package com.tailoris.user.service;

public interface MailService {

    void sendVerificationCode(String email, String code);
}

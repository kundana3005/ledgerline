package com.ledgerline.backend.service;

import com.ledgerline.backend.exception.ApiException;
import java.util.regex.Pattern;

public final class PasswordPolicy {
    public static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$";
    private static final Pattern P = Pattern.compile(REGEX);
    private PasswordPolicy() {}

    public static void check(String password, String confirm) {
        if (password == null || !P.matcher(password).matches()) {
            throw ApiException.badRequest("Password must be 8-72 characters with upper and lower case letters, a digit and a symbol");
        }
        if (!password.equals(confirm)) throw ApiException.badRequest("Passwords do not match");
    }
}
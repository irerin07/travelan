package com.irerin.travelan.auth.validation;

import com.irerin.travelan.auth.dto.SignupRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, SignupRequest> {

    @Override
    public boolean isValid(SignupRequest request, ConstraintValidatorContext context) {
        String password = request.getPassword();
        String passwordConfirm = request.getPasswordConfirm();

        if (password == null || passwordConfirm == null) {
            return true; // @NotBlank가 null 처리
        }

        if (!password.equals(passwordConfirm)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("passwordConfirm")
                .addConstraintViolation();
            return false;
        }

        return true;
    }

}

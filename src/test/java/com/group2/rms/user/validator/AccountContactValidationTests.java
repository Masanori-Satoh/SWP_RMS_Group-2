package com.group2.rms.user.validator;

import com.group2.rms.auth.dto.RegisterAccountRequest;
import com.group2.rms.user.dto.CreateAccountRequest;
import com.group2.rms.user.dto.UpdateAccountRequest;
import com.group2.rms.user.dto.UpdateProfileRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Prevents Admin/registration data from becoming invalid solely when editing a profile. */
class AccountContactValidationTests {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeValidator() {
        FACTORY.close();
    }

    static Stream<Arguments> names() {
        return Stream.of(
                Arguments.of("A", true),
                Arguments.of("A1", true),
                Arguments.of("Nguyễn 123", true),
                Arguments.of("A".repeat(100), true),
                Arguments.of("A".repeat(101), false),
                Arguments.of("", false),
                Arguments.of("   ", false),
                Arguments.of(null, false));
    }

    @ParameterizedTest
    @MethodSource("names")
    void fullNameHasSameAcceptanceAcrossAllAccountForms(String name, boolean accepted) {
        var create = new CreateAccountRequest();
        create.setFullName(name);
        var update = new UpdateAccountRequest();
        update.setFullName(name);
        var register = new RegisterAccountRequest();
        register.setFullName(name);
        for (Object request : new Object[] {create, update, register, new UpdateProfileRequest(name, null)}) {
            assertEquals(accepted, VALIDATOR.validateProperty(request, "fullName").isEmpty(),
                    request.getClass().getSimpleName());
        }
    }

    static Stream<Arguments> phones() {
        return Stream.of(
                Arguments.of(null, true),
                Arguments.of("", true),
                Arguments.of("   ", true),
                Arguments.of("0901234567", true),
                Arguments.of("1234567890", false),
                Arguments.of("090123456", false),
                Arguments.of("09012345678", false),
                Arguments.of("+84901234567", false),
                Arguments.of("abcxyz", false),
                Arguments.of("0901234abc", false));
    }

    @ParameterizedTest
    @MethodSource("phones")
    void optionalPhoneHasSameAcceptanceAcrossAdminAndProfile(String phone, boolean accepted) {
        var create = new CreateAccountRequest();
        create.setPhoneNumber(phone);
        var update = new UpdateAccountRequest();
        update.setPhoneNumber(phone);
        for (Object request : new Object[] {create, update, new UpdateProfileRequest("A", phone)}) {
            assertEquals(accepted, VALIDATOR.validateProperty(request, "phoneNumber").isEmpty(),
                    request.getClass().getSimpleName());
        }
    }
}

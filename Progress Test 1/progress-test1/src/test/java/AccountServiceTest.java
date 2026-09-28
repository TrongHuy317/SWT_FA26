import fu.HuyLT.Account;
import fu.HuyLT.AccountService;
import fu.HuyLT.AccountStatus;
import fu.HuyLT.ResultCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AccountService")
class AccountServiceTest {

    static final String USER = "alice_01";
    static final String EMAIL = "alice@example.com";
    static final String PASS = "Secret@123";
    static final String WRONG = "Wrong@123";
    static final LocalDate DOB = LocalDate.of(2000, 1, 15);
    static final String PHONE = "0912345678";

    AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService();
    }

    // =========================================================
    // Helper
    // =========================================================

    void registerDefault() {
        assertEquals(
                ResultCode.SUCCESS,
                service.register(USER, EMAIL, PASS, PASS, DOB, PHONE)
        );
    }

    Account account() {
        return service.findByUsername(USER).orElseThrow();
    }

    void failLogin(int times) {
        for (int i = 0; i < times; i++) {
            service.login(USER, WRONG);
        }
    }

    // =========================================================
    // Register
    // =========================================================

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        void register_ValidData_CreatesActiveAccountWithHashedPassword() {
            ResultCode result =
                    service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);

            assertEquals(ResultCode.SUCCESS, result);

            Account acc = account();

            assertEquals(AccountStatus.ACTIVE, acc.getStatus());
            assertEquals(0, acc.getFailedAttempts());
            assertFalse(acc.isLocked());
            assertNotEquals(PASS, acc.getCurrentPasswordHash());
            assertEquals(64, acc.getCurrentPasswordHash().length());
            assertEquals(1, acc.getPasswordHistory().size());
        }

        @Test
        void register_UpperCaseEmail_StoredAsLowerCase() {
            ResultCode result =
                    service.register(
                            USER,
                            "Alice@Example.COM",
                            PASS,
                            PASS,
                            DOB,
                            PHONE
                    );

            assertEquals(ResultCode.SUCCESS, result);
            assertEquals("alice@example.com", account().getEmail());
        }

        @Test
        void register_TwoAccountsSamePassword_HaveDifferentSaltAndHash() {
            registerDefault();

            assertEquals(
                    ResultCode.SUCCESS,
                    service.register(
                            "bob_02",
                            "bob@example.com",
                            PASS,
                            PASS,
                            DOB,
                            null
                    )
            );

            Account alice = account();
            Account bob = service.findByUsername("bob_02").orElseThrow();

            assertNotEquals(alice.getSalt(), bob.getSalt());
            assertNotEquals(
                    alice.getCurrentPasswordHash(),
                    bob.getCurrentPasswordHash()
            );
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("AccountServiceTest#invalidRegisterInputs")
        void register_InvalidInput_ReturnsExpectedCode(
                String description,
                String username,
                String email,
                String password,
                String confirmPassword,
                LocalDate dob,
                String phone,
                ResultCode expected
        ) {
            ResultCode result =
                    service.register(
                            username,
                            email,
                            password,
                            confirmPassword,
                            dob,
                            phone
                    );

            assertEquals(expected, result);

            if (username != null && !username.isBlank()) {
                assertTrue(
                        service.findByUsername(username).isEmpty(),
                        "Đăng ký thất bại thì không được tạo account"
                );
            }
        }

        @ParameterizedTest(name = "[{index}] username = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void register_UsernameNullEmptyBlank_ReturnsInvalidInput(
                String username
        ) {
            assertEquals(
                    ResultCode.INVALID_INPUT,
                    service.register(
                            username,
                            EMAIL,
                            PASS,
                            PASS,
                            DOB,
                            PHONE
                    )
            );
        }

        @ParameterizedTest(name = "[{index}] email = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void register_EmailNullEmptyBlank_ReturnsInvalidInput(
                String email
        ) {
            assertEquals(
                    ResultCode.INVALID_INPUT,
                    service.register(
                            USER,
                            email,
                            PASS,
                            PASS,
                            DOB,
                            PHONE
                    )
            );
        }

        @ParameterizedTest(name = "[{index}] password = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void register_PasswordNullEmptyBlank_ReturnsInvalidInput(
                String password
        ) {
            assertEquals(
                    ResultCode.INVALID_INPUT,
                    service.register(
                            USER,
                            EMAIL,
                            password,
                            PASS,
                            DOB,
                            PHONE
                    )
            );

            assertEquals(
                    ResultCode.INVALID_INPUT,
                    service.register(
                            USER,
                            EMAIL,
                            PASS,
                            password,
                            DOB,
                            PHONE
                    )
            );
        }

        @ParameterizedTest(name = "[{index}] phone = \"{0}\"")
        @NullAndEmptySource
        void register_PhoneNullOrEmpty_Success(
                String phone
        ) {
            assertEquals(
                    ResultCode.SUCCESS,
                    service.register(
                            USER,
                            EMAIL,
                            PASS,
                            PASS,
                            DOB,
                            phone
                    )
            );
        }

        @ParameterizedTest(name = "[{index}] duplicate username = \"{0}\"")
        @ValueSource(strings = {
                "alice_01",
                "ALICE_01",
                "Alice_01"
        })
        void register_DuplicateUsernameIgnoreCase_ReturnsDuplicateUsername(
                String username
        ) {
            registerDefault();

            assertEquals(
                    ResultCode.DUPLICATE_USERNAME,
                    service.register(
                            username,
                            "other@example.com",
                            PASS,
                            PASS,
                            DOB,
                            null
                    )
            );
        }

        @ParameterizedTest(name = "[{index}] duplicate email = \"{0}\"")
        @ValueSource(strings = {
                "alice@example.com",
                "ALICE@EXAMPLE.COM",
                "Alice@Example.Com"
        })
        void register_DuplicateEmailIgnoreCase_ReturnsDuplicateEmail(
                String email
        ) {
            registerDefault();

            assertEquals(
                    ResultCode.DUPLICATE_EMAIL,
                    service.register(
                            "bob_02",
                            email,
                            PASS,
                            PASS,
                            DOB,
                            null
                    )
            );

            assertTrue(service.findByUsername("bob_02").isEmpty());
        }

        @ParameterizedTest(
                name = "[{index}] today - {0} years + {1} days -> {2}"
        )
        @CsvSource({
                "18,  0, SUCCESS",
                "18,  1, UNDERAGE",
                "18, -1, SUCCESS",
                "0,   0, UNDERAGE",
                "0,   1, INVALID_INPUT"
        })
        void register_AgeBoundary(
                int yearsAgo,
                int plusDays,
                ResultCode expected
        ) {
            LocalDate dob =
                    LocalDate.now()
                            .minusYears(yearsAgo)
                            .plusDays(plusDays);

            assertEquals(
                    expected,
                    service.register(
                            USER,
                            EMAIL,
                            PASS,
                            PASS,
                            dob,
                            null
                    )
            );
        }

        @Test
        void register_DuplicateUsernameButInvalidEmail_ReturnsInvalidEmailFirst() {
            registerDefault();

            assertEquals(
                    ResultCode.INVALID_EMAIL,
                    service.register(
                            USER,
                            "bad-email",
                            PASS,
                            PASS,
                            DOB,
                            null
                    )
            );
        }
    }

    // =========================================================
    // Login - TODO 7
    // =========================================================

    @Nested
    @DisplayName("login()")
    class Login {

        @BeforeEach
        void prepareUser() {
            registerDefault();
        }

        @Test
        void login_CorrectCredentials_Success() {
            ResultCode result = service.login(USER, PASS);

            assertEquals(ResultCode.SUCCESS, result);
            assertEquals(0, account().getFailedAttempts());
            assertFalse(account().isLocked());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Secret@123",
                "Wrong@123"
        })
        void login_UnknownUserAndAnyPassword_ReturnsInvalidCredentials(
                String password
        ) {
            assertEquals(
                    ResultCode.INVALID_CREDENTIALS,
                    service.login("unknown_user", password)
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Secret@123",
                "Wrong@123"
        })
        void login_DisabledAccount_ReturnsAccountDisabled(
                String password
        ) {
            assertEquals(
                    ResultCode.SUCCESS,
                    service.disableAccount(USER)
            );

            assertEquals(
                    ResultCode.ACCOUNT_DISABLED,
                    service.login(USER, password)
            );
        }

        @ParameterizedTest(name = "[{index}] sai {0} lần")
        @ValueSource(ints = {
                1,
                2,
                3,
                4
        })
        void login_WrongPasswordLessThan5Times_IncrementsCounter(
                int attempts
        ) {
            for (int i = 1; i <= attempts; i++) {
                assertEquals(
                        ResultCode.INVALID_CREDENTIALS,
                        service.login(USER, WRONG)
                );
            }

            assertEquals(
                    attempts,
                    account().getFailedAttempts()
            );

            assertFalse(account().isLocked());
        }

        @Test
        void login_WrongPassword5thTime_LocksAccount() {
            failLogin(4);

            assertFalse(account().isLocked());
            assertEquals(4, account().getFailedAttempts());

            assertEquals(
                    ResultCode.ACCOUNT_LOCKED,
                    service.login(USER, WRONG)
            );

            assertTrue(account().isLocked());
            assertEquals(5, account().getFailedAttempts());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Secret@123",
                "Wrong@123"
        })
        void login_WhileLocked_RejectsWithoutIncrement(
                String password
        ) {
            failLogin(5);

            assertTrue(account().isLocked());
            assertEquals(5, account().getFailedAttempts());

            assertEquals(
                    ResultCode.ACCOUNT_LOCKED,
                    service.login(USER, password)
            );

            assertEquals(
                    5,
                    account().getFailedAttempts()
            );
        }

        @ParameterizedTest(
                name = "[{index}] sau {0} lần sai -> {1}, locked={2}"
        )
        @CsvSource({
                "4, SUCCESS,        false",
                "5, ACCOUNT_LOCKED, true",
                "6, ACCOUNT_LOCKED, true"
        })
        void login_CorrectPasswordAfterNFailures(
                int failures,
                ResultCode expected,
                boolean expectedLocked
        ) {
            failLogin(failures);

            ResultCode result =
                    service.login(USER, PASS);

            assertEquals(expected, result);
            assertEquals(
                    expectedLocked,
                    service.isLocked(USER)
            );
        }

        @Test
        void login_SuccessAfterFailures_ResetsCounter() {
            failLogin(3);

            assertEquals(3, account().getFailedAttempts());

            assertEquals(
                    ResultCode.SUCCESS,
                    service.login(USER, PASS)
            );

            assertEquals(
                    0,
                    account().getFailedAttempts()
            );

            assertFalse(account().isLocked());
        }

        @Test
        void login_AfterAdminUnlock_CounterRestartsAndCanLogin() {
            failLogin(5);

            assertTrue(service.isLocked(USER));
            assertEquals(5, account().getFailedAttempts());

            assertEquals(
                    ResultCode.SUCCESS,
                    service.unlockAccount(USER)
            );

            assertFalse(service.isLocked(USER));
            assertEquals(0, account().getFailedAttempts());

            assertEquals(
                    ResultCode.INVALID_CREDENTIALS,
                    service.login(USER, WRONG)
            );

            assertEquals(
                    1,
                    account().getFailedAttempts()
            );

            assertEquals(
                    ResultCode.SUCCESS,
                    service.login(USER, PASS)
            );

            assertEquals(
                    0,
                    account().getFailedAttempts()
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "alice_01",
                "ALICE_01",
                "Alice_01"
        })
        void login_UsernameIgnoreCase_Success(
                String username
        ) {
            assertEquals(
                    ResultCode.SUCCESS,
                    service.login(username, PASS)
            );
        }

        @Test
        void login_PasswordCaseSensitive_ReturnsInvalidCredentials() {
            assertEquals(
                    ResultCode.INVALID_CREDENTIALS,
                    service.login(
                            USER,
                            "secret@123"
                    )
            );

            assertEquals(
                    1,
                    account().getFailedAttempts()
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                " ",
                "   "
        })
        void login_UsernameNullEmptyBlank_ReturnsInvalidInput(
                String username
        ) {
            assertEquals(
                    ResultCode.INVALID_INPUT,
                    service.login(username, PASS)
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                " ",
                "   "
        })
        void login_PasswordNullEmptyBlank_ReturnsInvalidInput(
                String password
        ) {
            assertEquals(
                    ResultCode.INVALID_INPUT,
                    service.login(USER, password)
            );
        }
    }

    // =========================================================
    // MethodSource
    // =========================================================

    static Stream<Arguments> invalidRegisterInputs() {
        LocalDate childDob =
                LocalDate.now().minusYears(10);

        return Stream.of(

                Arguments.of(
                        "DOB null",
                        USER,
                        EMAIL,
                        PASS,
                        PASS,
                        null,
                        PHONE,
                        ResultCode.INVALID_INPUT
                ),

                Arguments.of(
                        "DOB ở tương lai",
                        USER,
                        EMAIL,
                        PASS,
                        PASS,
                        LocalDate.now().plusDays(1),
                        PHONE,
                        ResultCode.INVALID_INPUT
                ),

                Arguments.of(
                        "username sai định dạng",
                        "1alice",
                        EMAIL,
                        PASS,
                        PASS,
                        DOB,
                        PHONE,
                        ResultCode.INVALID_USERNAME
                ),

                Arguments.of(
                        "email sai định dạng",
                        USER,
                        "bad-email",
                        PASS,
                        PASS,
                        DOB,
                        PHONE,
                        ResultCode.INVALID_EMAIL
                ),

                Arguments.of(
                        "password yếu",
                        USER,
                        EMAIL,
                        "weak",
                        "weak",
                        DOB,
                        PHONE,
                        ResultCode.WEAK_PASSWORD
                ),

                Arguments.of(
                        "confirm password không khớp",
                        USER,
                        EMAIL,
                        PASS,
                        "Other@123",
                        DOB,
                        PHONE,
                        ResultCode.PASSWORD_MISMATCH
                ),

                Arguments.of(
                        "chưa đủ 18 tuổi",
                        USER,
                        EMAIL,
                        PASS,
                        PASS,
                        childDob,
                        PHONE,
                        ResultCode.UNDERAGE
                ),

                Arguments.of(
                        "phone sai định dạng",
                        USER,
                        EMAIL,
                        PASS,
                        PASS,
                        DOB,
                        "123456",
                        ResultCode.INVALID_PHONE
                ),

                Arguments.of(
                        "username sai + email sai -> username thắng",
                        "1alice",
                        "bad-email",
                        PASS,
                        PASS,
                        DOB,
                        PHONE,
                        ResultCode.INVALID_USERNAME
                ),

                Arguments.of(
                        "email sai + password yếu -> email thắng",
                        USER,
                        "bad-email",
                        "weak",
                        "weak",
                        DOB,
                        PHONE,
                        ResultCode.INVALID_EMAIL
                ),

                Arguments.of(
                        "password yếu + confirm lệch -> password thắng",
                        USER,
                        EMAIL,
                        "weak",
                        "Different@123",
                        DOB,
                        PHONE,
                        ResultCode.WEAK_PASSWORD
                )
        );
    }
}

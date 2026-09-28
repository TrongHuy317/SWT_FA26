package fu.HuyLT;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AccountService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int PASSWORD_HISTORY_SIZE = 3;
    public static final int MIN_AGE = 18;

    private final Map<String, Account> accountsByUsername = new HashMap<>();
    private final Map<String, String> usernameByEmail = new HashMap<>();

    // Bonus: reset password
    private final Map<String, String> usernameByToken = new HashMap<>();
    private final Map<String, String> tokenByUsername = new HashMap<>();

    public AccountService() {
    }

    // =========================================================
    // REGISTER
    // =========================================================

    public ResultCode register(
            String username,
            String email,
            String password,
            String confirmPassword,
            LocalDate dateOfBirth,
            String phone
    ) {
        LocalDate today = LocalDate.now();

        // BR-REG-01
        if (isBlank(username)
                || isBlank(email)
                || isBlank(password)
                || isBlank(confirmPassword)
                || dateOfBirth == null
                || dateOfBirth.isAfter(today)) {

            return ResultCode.INVALID_INPUT;
        }

        // BR-REG-02
        if (!AccountValidator.isValidUsername(username)) {
            return ResultCode.INVALID_USERNAME;
        }

        // BR-REG-04
        if (!AccountValidator.isValidEmail(email)) {
            return ResultCode.INVALID_EMAIL;
        }

        // BR-REG-06
        if (!AccountValidator.isValidPassword(password, username)) {
            return ResultCode.WEAK_PASSWORD;
        }

        // BR-REG-07
        if (!password.equals(confirmPassword)) {
            return ResultCode.PASSWORD_MISMATCH;
        }

        // BR-REG-08
        if (AccountValidator.calculateAge(dateOfBirth, today) < MIN_AGE) {
            return ResultCode.UNDERAGE;
        }

        // BR-REG-09
        // phone optional: null hoặc "" được chấp nhận
        if (phone != null
                && !phone.isEmpty()
                && !AccountValidator.isValidPhone(phone)) {

            return ResultCode.INVALID_PHONE;
        }

        String userKey = key(username);
        String emailKey = key(email);

        // BR-REG-03
        if (accountsByUsername.containsKey(userKey)) {
            return ResultCode.DUPLICATE_USERNAME;
        }

        // BR-REG-05
        if (usernameByEmail.containsKey(emailKey)) {
            return ResultCode.DUPLICATE_EMAIL;
        }

        // BR-REG-10
        String salt = PasswordHasher.generateSalt();

        Account account = new Account(
                username,
                emailKey,
                dateOfBirth,
                phone,
                salt,
                PasswordHasher.hash(salt, password)
        );

        accountsByUsername.put(userKey, account);
        usernameByEmail.put(emailKey, userKey);

        return ResultCode.SUCCESS;
    }

    // =========================================================
    // LOGIN
    // =========================================================

    public ResultCode login(
            String username,
            String password
    ) {
        // BR-LOG-01
        if (isBlank(username) || isBlank(password)) {
            return ResultCode.INVALID_INPUT;
        }

        Account account =
                accountsByUsername.get(key(username));

        // BR-LOG-02 / BR-LOG-03
        if (account == null) {
            return ResultCode.INVALID_CREDENTIALS;
        }

        // BR-LOG-04
        if (account.getStatus() == AccountStatus.DISABLED) {
            return ResultCode.ACCOUNT_DISABLED;
        }

        // BR-LOG-06
        // Đang bị khóa -> không tăng failedAttempts
        if (account.isLocked()) {
            return ResultCode.ACCOUNT_LOCKED;
        }

        // BR-LOG-03 / BR-LOG-05
        if (!PasswordHasher.matches(
                account.getSalt(),
                password,
                account.getCurrentPasswordHash()
        )) {

            account.incrementFailedAttempts();

            // Lần sai thứ 5 phải khóa ngay
            if (account.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                account.lock();

                return ResultCode.ACCOUNT_LOCKED;
            }

            return ResultCode.INVALID_CREDENTIALS;
        }

        // BR-LOG-08
        // Login đúng -> reset failedAttempts
        account.resetFailedAttempts();

        return ResultCode.SUCCESS;
    }

    // =========================================================
    // ADMIN / QUERY
    // =========================================================

    public ResultCode disableAccount(String username) {
        Optional<Account> account =
                findByUsername(username);

        if (account.isEmpty()) {
            return ResultCode.USER_NOT_FOUND;
        }

        account.get().setStatus(AccountStatus.DISABLED);

        return ResultCode.SUCCESS;
    }

    public ResultCode unlockAccount(String username) {
        Optional<Account> account =
                findByUsername(username);

        if (account.isEmpty()) {
            return ResultCode.USER_NOT_FOUND;
        }

        account.get().unlock();

        return ResultCode.SUCCESS;
    }

    public Optional<Account> findByUsername(String username) {
        if (isBlank(username)) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                accountsByUsername.get(key(username))
        );
    }

    public boolean isLocked(String username) {
        return findByUsername(username)
                .map(Account::isLocked)
                .orElse(false);
    }

    // =========================================================
    // CHANGE PASSWORD - BONUS
    // =========================================================

    public ResultCode changePassword(
            String username,
            String oldPassword,
            String newPassword,
            String confirmPassword
    ) {
        // BR-CHG-01
        if (isBlank(username)
                || isBlank(oldPassword)
                || isBlank(newPassword)
                || isBlank(confirmPassword)) {

            return ResultCode.INVALID_INPUT;
        }

        // BR-CHG-02
        Account account =
                accountsByUsername.get(key(username));

        if (account == null) {
            return ResultCode.USER_NOT_FOUND;
        }

        if (account.getStatus() == AccountStatus.DISABLED) {
            return ResultCode.ACCOUNT_DISABLED;
        }

        // BR-CHG-03
        if (!PasswordHasher.matches(
                account.getSalt(),
                oldPassword,
                account.getCurrentPasswordHash()
        )) {

            return ResultCode.OLD_PASSWORD_INCORRECT;
        }

        // BR-CHG-04 .. BR-CHG-07
        ResultCode check =
                validateNewPassword(
                        account,
                        newPassword,
                        confirmPassword
                );

        if (!check.isSuccess()) {
            return check;
        }

        // BR-CHG-08
        account.changePasswordHash(
                PasswordHasher.hash(
                        account.getSalt(),
                        newPassword
                ),
                PASSWORD_HISTORY_SIZE
        );

        return ResultCode.SUCCESS;
    }

    // =========================================================
    // RESET PASSWORD - BONUS
    // =========================================================

    public TokenResult requestPasswordReset(String email) {
        // BR-RST-01
        if (isBlank(email)) {
            return new TokenResult(
                    ResultCode.INVALID_INPUT,
                    null
            );
        }

        String userKey =
                usernameByEmail.get(key(email));

        if (userKey == null) {
            return new TokenResult(
                    ResultCode.USER_NOT_FOUND,
                    null
            );
        }

        Account account =
                accountsByUsername.get(userKey);

        if (account.getStatus() == AccountStatus.DISABLED) {
            return new TokenResult(
                    ResultCode.ACCOUNT_DISABLED,
                    null
            );
        }

        // BR-RST-03
        String oldToken =
                tokenByUsername.remove(userKey);

        if (oldToken != null) {
            usernameByToken.remove(oldToken);
        }

        String token =
                UUID.randomUUID().toString();

        usernameByToken.put(
                token,
                userKey
        );

        tokenByUsername.put(
                userKey,
                token
        );

        return new TokenResult(
                ResultCode.SUCCESS,
                token
        );
    }

    public ResultCode resetPassword(
            String token,
            String newPassword,
            String confirmPassword
    ) {
        // BR-RST-04
        if (isBlank(token)
                || isBlank(newPassword)
                || isBlank(confirmPassword)) {

            return ResultCode.INVALID_INPUT;
        }

        String userKey =
                usernameByToken.get(token);

        if (userKey == null) {
            return ResultCode.INVALID_TOKEN;
        }

        Account account =
                accountsByUsername.get(userKey);

        // BR-RST-05
        ResultCode check =
                validateNewPassword(
                        account,
                        newPassword,
                        confirmPassword
                );

        if (!check.isSuccess()) {
            return check;
        }

        // BR-RST-06
        account.changePasswordHash(
                PasswordHasher.hash(
                        account.getSalt(),
                        newPassword
                ),
                PASSWORD_HISTORY_SIZE
        );

        account.unlock();

        usernameByToken.remove(token);
        tokenByUsername.remove(userKey);

        return ResultCode.SUCCESS;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private ResultCode validateNewPassword(
            Account account,
            String newPassword,
            String confirmPassword
    ) {
        if (!AccountValidator.isValidPassword(
                newPassword,
                account.getUsername()
        )) {

            return ResultCode.WEAK_PASSWORD;
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResultCode.PASSWORD_MISMATCH;
        }

        String newHash =
                PasswordHasher.hash(
                        account.getSalt(),
                        newPassword
                );

        if (newHash.equals(
                account.getCurrentPasswordHash()
        )) {

            return ResultCode.SAME_AS_OLD_PASSWORD;
        }

        if (account.getPasswordHistory()
                .contains(newHash)) {

            return ResultCode.PASSWORD_REUSED;
        }

        return ResultCode.SUCCESS;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String key(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
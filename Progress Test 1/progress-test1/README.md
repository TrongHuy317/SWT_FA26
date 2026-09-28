# Lab2 – Account Management (JUnit 5)

## Chạy
mvn clean test  → report: target/site/jacoco/index.html

## Kết quả
- Tests: 151 methods, 0 failures
- JaCoCo: AccountValidator  Line 100% / Branch 98%
          AccountService    Line 93% / Branch 95%   (ảnh: docs/jacoco.png)

## Mutation thủ công
| # | Lỗi | Test fail | Hoàn tác |
|---|-----|-----------|----------|
| M1 | >= MAX_FAILED_ATTEMPTS -> > | Login.login_WrongPassword5thTime_LocksAccount | ✅ |
| M2 | bỏ kiểm tra isLocked() trong login() | Login.login_WhileLocked_RejectsWithoutIncrement | ✅ |
| M3 | regex username {4,19} -> {4,20} | Username...BoundaryLength[21] | ✅ |

## Ma trận truy vết
| BR | Test bảo vệ |
|---|---|
| REG-01 | Register.register_UsernameNullEmptyBlank..., ...EmailNullEmptyBlank..., ...PasswordNullEmptyBlank..., invalidRegisterInputs[dob null], register_AgeBoundary |
| REG-02 | AccountValidatorTest.Username.*, invalidRegisterInputs[username sai] |
| REG-03 | register_DuplicateUsernameIgnoreCase_ReturnsDuplicateUsername |
| REG-04 | AccountValidatorTest.Email.*, invalidRegisterInputs[email sai] |
| REG-05 | register_DuplicateEmailIgnoreCase_ReturnsDuplicateEmail |
| REG-06 | AccountValidatorTest.Password.*, invalidRegisterInputs[mật khẩu yếu / chứa username] |
| REG-07 | invalidRegisterInputs[confirm lệch] |
| REG-08 | calculateAge_Boundaries, register_AgeBoundary |
| REG-09 | AccountValidatorTest.Phone.*, register_PhoneNullOrEmpty_Success, invalidRegisterInputs[phone...] |
| REG-10 | register_ValidData_CreatesActiveAccount..., register_UpperCaseEmail_StoredAsLowerCase, register_TwoAccountsSamePassword_HaveDifferentSaltAndHash |
| LOG-01 | login_UsernameNullEmptyBlank..., login_PasswordNullEmptyBlank... |
| LOG-02 | login_UsernameIgnoreCase_Success, login_PasswordCaseSensitive_ReturnsInvalidCredentials |
| LOG-03 | login_UnknownUserAndWrongPassword_ReturnSameCode |
| LOG-04 | login_DisabledAccount_ReturnsAccountDisabled |
| LOG-05 | login_WrongPasswordLessThan5Times_IncrementsCounter, login_WrongPassword5thTime_LocksAccount, login_CorrectPasswordAfterNFailures |
| LOG-06 | login_WhileLocked_RejectsWithoutIncrement |
| LOG-08 | login_CorrectCredentials_Success, login_SuccessAfterFailures_ResetsCounter |
| ADM-01/02 | Admin.disableAccount_*, Admin.findByUsername_BlankOrUnknown_ReturnsEmpty |
| ADM-03 | login_AfterAdminUnlock_CounterRestartsAndCanLogin, Admin.unlockAccount_BlankOrUnknown_ReturnsUserNotFound |

## Nguồn tham khảo
JUnit 5 User Guide – Parameterized Tests

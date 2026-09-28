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
(BR → tên test, xem mục 3 lời giải)

## Nguồn tham khảo
JUnit 5 User Guide – Parameterized Tests

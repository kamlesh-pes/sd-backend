# Phase 11: Configurable Security Settings

## Completed

- Added repository-backed `SystemSetting` storage and migration
- Seeded defaults for token lifetimes, session limits, OTP controls, and login rate-limit configuration
- Added admin-only settings list and update endpoints
- Added per-setting integer validation with explicit minimum and maximum bounds
- Added Redis-backed Spring cache lookup and cache invalidation after updates
- Added audit logging for every setting change with old and new values
- Connected access-token lifetime to runtime settings
- Connected customer refresh-token JWT and database expiry to runtime settings
- Connected OTP expiry and maximum attempts to runtime settings
- Preserved direct-constructor compatibility for existing OTP and JWT unit tests
- Added boundary tests for defaults, valid updates, invalid values, and unknown keys

## Default Settings

- Access token: 15 minutes
- Customer refresh token: 7 days
- Remember-me token: 30 days
- Admin refresh token: 8 hours
- Absolute token cap: 30 days
- Idle timeout: 14 days
- OTP expiry: 5 minutes
- OTP attempts: 5
- Login rate limit: 5 per minute

## Endpoints

- `GET /api/v1/admin/settings`
- `PUT /api/v1/admin/settings/{key}`

Both endpoints are protected by the existing `ROLE_ADMIN` rule for `/api/v1/admin/**`.

## Files Added

- [src/main/java/com/sahastra/backend/domain/repository/SystemSettingRepository.java](src/main/java/com/sahastra/backend/domain/repository/SystemSettingRepository.java)
- [src/main/java/com/sahastra/backend/service/SystemSettingService.java](src/main/java/com/sahastra/backend/service/SystemSettingService.java)
- [src/main/java/com/sahastra/backend/api/controller/AdminSystemSettingController.java](src/main/java/com/sahastra/backend/api/controller/AdminSystemSettingController.java)
- [src/main/resources/db/migration/V8__system_settings.sql](src/main/resources/db/migration/V8__system_settings.sql)
- [src/test/java/com/sahastra/backend/service/SystemSettingServiceTest.java](src/test/java/com/sahastra/backend/service/SystemSettingServiceTest.java)

## Verification

- Main compilation passed with Maven.
- `SystemSettingServiceTest`: 4 tests passed, 0 failures, 0 errors.
- Existing `OTPServiceTest` completed successfully after the settings integration.

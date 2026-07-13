-- Password reset audit rows belong to the doctor account lifecycle.
-- Deleting a doctor/account removes those rows so the normal CRUD delete flow
-- stays atomic and test environments do not accumulate orphan audit records.
USE `medicine`;

ALTER TABLE `password_reset_audit`
  DROP FOREIGN KEY `fk_reset_account`;

ALTER TABLE `password_reset_audit`
  ADD CONSTRAINT `fk_reset_account`
    FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE;

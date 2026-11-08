-- Payment columns hold values added with online payment (REFUNDED, WALLET, OTHER).
-- Databases created earlier stored them as ENUMs of the old values. Safe to run on every start.
ALTER TABLE payments MODIFY status VARCHAR(12) NOT NULL;
ALTER TABLE payments MODIFY method VARCHAR(12) NOT NULL;

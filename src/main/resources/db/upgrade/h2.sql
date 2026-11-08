-- Payment columns hold values added with online payment (REFUNDED, WALLET, OTHER).
-- Databases created earlier stored them as ENUMs of the old values. Safe to run on every start.
ALTER TABLE payments ALTER COLUMN status SET DATA TYPE VARCHAR(12);
ALTER TABLE payments ALTER COLUMN method SET DATA TYPE VARCHAR(12);

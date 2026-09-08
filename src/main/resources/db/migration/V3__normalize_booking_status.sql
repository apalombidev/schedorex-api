-- Normalizza bookings.status a maiuscolo e lo restringe al set
-- {PENDING, CONFIRMED, CANCELLED} — stesso pattern già usato in V2 per
-- services.language/currency (v5).
--
-- Nota: i valori scritti dall'applicazione via BookingStatusConverter sono
-- già in maiuscolo indipendentemente da questa migrazione (il converter
-- chiama sempre attribute.name()); questa migrazione serve a normalizzare
-- eventuali righe storiche inserite prima dell'introduzione del converter
-- e ad aggiungere il CHECK come difesa in profondità a livello DB.
 
UPDATE bookings SET status = UPPER(status);
 
ALTER TABLE bookings ALTER COLUMN status SET DEFAULT 'PENDING';
 
ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'));
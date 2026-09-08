-- Corregge un difetto di design presente fin da V1: il vincolo
-- UNIQUE(availability_slot_id) impediva QUALUNQUE riprenotazione di uno
-- slot, anche dopo che la prenotazione precedente era stata cancellata —
-- contraddicendo la motivazione originale (v2) per aver scelto @ManyToOne
-- invece di @OneToOne: "più flessibili nel tempo (slot cancellato e
-- riprenotato)".
--
-- Fix: un INDICE UNICO PARZIALE, che vincola l'unicità di
-- availability_slot_id solo tra le prenotazioni ATTIVE (PENDING,
-- CONFIRMED). Le righe CANCELLED restano in tabella come storico e non
-- contano ai fini del vincolo — quindi lo stesso slot può avere più righe
-- bookings nel tempo, purché al massimo una sia attiva.
--
-- Bug scoperto durante lo smoke test manuale dello Step 7 (Postman):
-- rieseguire una prenotazione su uno slot già cancellato falliva con lo
-- stesso errore di "slot già prenotato", nonostante lo slot risultasse
-- libero sia nella vista pubblica che in quella admin.
 
ALTER TABLE bookings DROP CONSTRAINT uq_availability_slot_booking;
 
CREATE UNIQUE INDEX uq_availability_slot_booking_active
    ON bookings (availability_slot_id)
    WHERE status IN ('PENDING', 'CONFIRMED');
 
-- Restringe services.language a {IT, EN} e services.currency a {EUR, GBP},
-- coerentemente con la decisione di prodotto (v4). Aggiunge vincoli a
-- livello DB come difesa in profondità, oltre alla validazione Java via enum.
 
-- Normalizza eventuali righe esistenti al formato atteso (maiuscolo)
-- prima di applicare il CHECK, per evitare violazioni su dati già presenti.
UPDATE services SET language = UPPER(language);
UPDATE services SET currency = UPPER(currency);
 
-- Allinea il default della colonna al nuovo formato maiuscolo
ALTER TABLE services ALTER COLUMN language SET DEFAULT 'IT';
ALTER TABLE services ALTER COLUMN currency SET DEFAULT 'EUR';
 
ALTER TABLE services
    ADD CONSTRAINT chk_services_language CHECK (language IN ('IT', 'EN'));
 
ALTER TABLE services
    ADD CONSTRAINT chk_services_currency CHECK (currency IN ('EUR', 'GBP'));
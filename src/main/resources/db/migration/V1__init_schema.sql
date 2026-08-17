-- V1__init_schema.sql
-- Schema iniziale: services, availability_slots, bookings, admin_users
-- Rif: scheduling-engine-architettura-v2.md, sezione "Schema database aggiornato"
 
-- Necessario per gen_random_uuid() usato come default in bookings.cancellation_token
CREATE EXTENSION IF NOT EXISTS pgcrypto;
 
CREATE TABLE services (
  id UUID PRIMARY KEY,
  professional_name VARCHAR(100),
  service_name VARCHAR(100),
  duration_minutes INTEGER,
  price_cents INTEGER,
  currency VARCHAR(3) DEFAULT 'EUR',
  timezone VARCHAR(50), -- es. 'Europe/Rome'
  language VARCHAR(5) DEFAULT 'it', -- 'it' o 'en', lingua interfaccia pubblica di prenotazione
  cancellation_deadline_hours INTEGER DEFAULT 24, -- soglia oltre la quale il cliente NON può più cancellare
  stripe_enabled BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);
 
CREATE TABLE availability_slots (
  id UUID PRIMARY KEY,
  service_id UUID REFERENCES services(id) ON DELETE CASCADE,
  slot_start TIMESTAMP NOT NULL, -- salvato in UTC
  slot_end TIMESTAMP NOT NULL,
  is_blocked BOOLEAN DEFAULT FALSE, -- bloccato manualmente dal professionista (es. pausa, ferie)
  created_at TIMESTAMP DEFAULT NOW(),
 
  -- Previene la generazione di slot duplicati per lo stesso servizio/orario
  CONSTRAINT uq_service_slot_start UNIQUE (service_id, slot_start)
);
 
CREATE TABLE bookings (
  id UUID PRIMARY KEY,
  service_id UUID REFERENCES services(id),
  availability_slot_id UUID REFERENCES availability_slots(id),
  client_name VARCHAR(100) NOT NULL,
  client_email VARCHAR(100) NOT NULL,
  client_notes VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'pending', -- 'pending', 'confirmed', 'cancelled'
  cancellation_token UUID NOT NULL DEFAULT gen_random_uuid(), -- link di cancellazione self-service, no login richiesto
  stripe_checkout_session_id VARCHAR(200), -- solo se stripe_enabled = true
  stripe_payment_status VARCHAR(20), -- 'unpaid', 'paid', 'refunded' — usato dalla riconciliazione periodica
  created_at TIMESTAMP DEFAULT NOW(),
  cancelled_at TIMESTAMP,
 
  -- Vincolo chiave: uno slot può essere associato a UNA SOLA prenotazione attiva.
  -- Garantisce a livello DB l'impossibilità di doppie prenotazioni.
  CONSTRAINT uq_availability_slot_booking UNIQUE (availability_slot_id)
);
 
CREATE TABLE admin_users (
  id UUID PRIMARY KEY,
  email VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL, -- bcrypt/argon2
  created_at TIMESTAMP DEFAULT NOW()
);
 
-- Indice per velocizzare la query "slot liberi per servizio in un range di date"
CREATE INDEX idx_availability_service_start ON availability_slots(service_id, slot_start)
  WHERE is_blocked = FALSE;
 
-- Indice per il lookup del token di cancellazione lato cliente
CREATE INDEX idx_bookings_cancellation_token ON bookings(cancellation_token);
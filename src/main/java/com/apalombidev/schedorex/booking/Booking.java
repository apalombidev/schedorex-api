package com.apalombidev.schedorex.booking;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.apalombidev.schedorex.availability.AvailabilitySlot;
import com.apalombidev.schedorex.service.ProfessionalService;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "bookings")
	
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Booking {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(updatable = false, nullable = false)
	@EqualsAndHashCode.Include
	private UUID id;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    @ToString.Exclude
    private ProfessionalService service;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "availability_slot_id", nullable = false)
    @ToString.Exclude
    private AvailabilitySlot availabilitySlot;
	
	@Column(name="client_name", nullable = false, length = 100)
	private String clientName;
	
	@Column(name="client_email", nullable = false, length = 100)
	private String clientEmail;
	
	@Column(name="client_notes", length = 500)
	private String clientNotes;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private BookingStatus status = BookingStatus.PENDING;
	
	@Column(name = "cancellation_token", nullable = false, unique = true)
	private UUID cancellationToken;
	
	@Column(name = "stripe_checkout_session_id", length = 200)
	private String stripeCheckoutSessionId;
	
	@Column(name = "stripe_payment_status", length = 20)
	private String stripePaymentStatus;
	
	@Column(name = "created_at", nullable = false)
	@CreationTimestamp
	private Instant createdAt;
	
	@Column(name = "cancelled_at")
	private Instant cancelledAt;
	
	@PrePersist
    protected void ensureCancellationToken() {
        if (cancellationToken == null) {
            cancellationToken = UUID.randomUUID();
        }
    }
	

}

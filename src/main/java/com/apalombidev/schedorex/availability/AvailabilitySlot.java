package com.apalombidev.schedorex.availability;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.apalombidev.schedorex.service.ProfessionalService;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "availability_slots",
		uniqueConstraints = @UniqueConstraint(
		name = "uq_service_slot_start",
		columnNames = {"service_id", "slot_start"}
		)
	)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AvailabilitySlot {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(updatable = false, nullable = false)
	@EqualsAndHashCode.Include
	private UUID id;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    @ToString.Exclude
    private ProfessionalService service;
	
	@Column(name = "slot_start", nullable = false)
	@CreationTimestamp
	private Instant slotStart;
	
	@Column(name = "slot_end", nullable = false)
	@CreationTimestamp
	private Instant slotEnd;
	
	@Column(name = "is_blocked", nullable = false)
    @Builder.Default
	boolean isBlocked = false;
	
	@Column(name = "created_at", updatable = false, nullable = false)
	@CreationTimestamp
	private Instant createdAt;

}

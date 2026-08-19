package com.apalombidev.schedorex.service;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProfessionalService {
	
	@Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    @EqualsAndHashCode.Include
	private UUID id;
	
	@Column(name = "professional_name", length = 100)
	private String professionalName;
	
	 @Column(name = "service_name", length = 100)
	private String serviceName;
	
	@Column(name = "duration_minutes")
	private Integer durationMinutes;
	
	@Column(name = "price_cents")
	private Integer priceCents;
	
	@Convert(converter = ServiceCurrencyConverter.class)
    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private ServiceCurrency currency = ServiceCurrency.EUR;
	
	@Column(name = "timezone", length = 50)
	private String timezone;
	
	@Convert(converter = ServiceLanguageConverter.class)
    @Column(name = "language", length = 5, nullable = false)
    @Builder.Default
    private ServiceLanguage language = ServiceLanguage.IT;
	
	@Column(name = "cancellation_deadline_hours")
    @Builder.Default
    private Integer cancellationDeadlineHours = 24;

	@Builder.Default
    @Column(name = "stripe_enabled", nullable = false)
    private Boolean stripeEnabled = false;
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;
	
	@UpdateTimestamp
	@Column(name = "updated_at")
	private Instant updatedAt;


}

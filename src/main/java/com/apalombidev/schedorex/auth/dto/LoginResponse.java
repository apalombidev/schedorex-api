package com.apalombidev.schedorex.auth.dto;

import java.time.Instant;

public record LoginResponse(

		String email, Instant expiredAt

) {

}

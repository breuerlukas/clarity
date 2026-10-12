package de.lukasbreuer.clarity.api.web.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the few endpoints reachable without a session (setup and login).
 * Everything else requires the owner, so a forgotten annotation fails closed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface PublicEndpoint {
}
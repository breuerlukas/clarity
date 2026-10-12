package de.lukasbreuer.clarity.api.hashing;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Hashing {
  private final Argon2 argon2 = Argon2Factory.create();

  public String hash(String input) {
    var password = input.toCharArray();
    try {
      return argon2.hash(3, 65536, 1, password);
    } finally {
      argon2.wipeArray(password);
    }
  }

  public boolean matches(String input, String hash) {
    var password = input.toCharArray();
    try {
      return argon2.verify(hash, password);
    } finally {
      argon2.wipeArray(password);
    }
  }
}

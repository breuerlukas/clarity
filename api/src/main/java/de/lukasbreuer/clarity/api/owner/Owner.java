package de.lukasbreuer.clarity.api.owner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.UUID;

/**
 * The one person allowed to see this instance. There is never more than one
 * confirmed owner: setup refuses to run once an owner has confirmed their
 * authenticator app.
 */
@Entity
@Table(name = "owner")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class Owner {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "name", nullable = false)
  private String name;
  @Column(name = "password", nullable = false)
  private String password;
  @Column(name = "totp_secret", nullable = false)
  private String totpSecret;
  @Column(name = "confirmed", nullable = false)
  private boolean confirmed;
  @Column(name = "last_totp_code")
  private String lastTotpCode;
  @Column(name = "last_totp_at", nullable = false)
  private long lastTotpAt;
  @Column(name = "created_at", nullable = false, updatable = false)
  private long createdAt;

  public void rename(String name) {
    this.name = name;
  }

  /**
   * @param passwordHash The Argon2 hash of the new password, never the
   *                     password itself
   */
  public void changePassword(String passwordHash) {
    this.password = passwordHash;
  }

  public void confirm() {
    this.confirmed = true;
  }

  /**
   * Whether the code was already used in the last 90 seconds. A code stays
   * valid for up to three 30 second windows, so without this a code seen over
   * someone's shoulder could be replayed right after the owner used it.
   */
  public boolean isReplayedTotpCode(String code) {
    return code.equals(lastTotpCode) &&
      System.currentTimeMillis() - lastTotpAt < 90_000;
  }

  public void rememberTotpCode(String code) {
    this.lastTotpCode = code;
    this.lastTotpAt = System.currentTimeMillis();
  }
}
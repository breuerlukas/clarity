package de.lukasbreuer.clarity.api.owner.session;

public enum OwnerSessionStatus {
  ACTIVE,
  CLOSED;

  public boolean isActive() {
    return this == ACTIVE;
  }
}
package de.lukasbreuer.clarity.api.configuration;

import org.apache.commons.configuration2.INIConfiguration;

public interface Configuration {
  /**
   * This function builds the actual objects from the content of the
   * configuration file, which can be used later on
   * @param file The configuration file
   */
  void load(INIConfiguration file);
}

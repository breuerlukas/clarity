/*
 * The Spring Boot plugin brings Jackson 3.1.5 into the build itself; see the
 * Jackson platform in the dependencies below for why it is lifted. Its
 * buildpack support pulls an old commons-lang3 (CVE-2025-48924) in as well.
 */
buildscript {
  dependencies {
    classpath(platform("tools.jackson:jackson-bom:3.2.4"))
    constraints {
      classpath("org.apache.commons:commons-lang3:3.21.0")
    }
  }
}

plugins {
  id("java")
  id("checkstyle")
  id("org.springframework.boot") version "4.1.1"
  id("io.freefair.lombok") version "9.8.2"
  id("com.github.spotbugs") version "6.5.13"
}

group = "de.lukasbreuer.clarity"
version = "1.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_25
java.targetCompatibility = JavaVersion.VERSION_25

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:6.1.3"))
  testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
  testImplementation("org.springframework:spring-test:7.0.9")
  testImplementation("com.icegreen:greenmail:2.1.14")

  implementation("com.google.guava:guava:33.7.2-jre")

  implementation("org.projectlombok:lombok:1.18.48")
  annotationProcessor("org.projectlombok:lombok:1.18.48")
  testImplementation("org.projectlombok:lombok:1.18.48")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.48")

  implementation("org.json:json:20260814")

  implementation("org.apache.commons:commons-configuration2:2.15.1")
  implementation("commons-beanutils:commons-beanutils:1.11.0")

  implementation("org.postgresql:postgresql:42.7.14")
  implementation("org.hibernate.orm:hibernate-core:7.4.13.Final")

  implementation("org.springframework.boot:spring-boot-starter-web:4.1.1")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa:4.1.1")

  implementation("de.mkammerer:argon2-jvm:2.12")

  // The modules instead of the "jjwt" bundle: org.json is already here, and
  // the bundle's Jackson 2 backend brought vulnerable jackson-core along.
  implementation("io.jsonwebtoken:jjwt-api:0.13.0")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
  runtimeOnly("io.jsonwebtoken:jjwt-orgjson:0.13.0")

  implementation("dev.samstevens.totp:totp:1.7.1")

  implementation("jakarta.mail:jakarta.mail-api:2.1.5")
  implementation("org.eclipse.angus:angus-mail:2.0.5")
  implementation("org.apache.pdfbox:pdfbox:3.0.8")

  spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0")

  /*
   * Spring Boot 4.1.1 ships Jackson 3.1.5, whose jackson-core has two High
   * denial of service bugs (GHSA-p6pp-m3f8-5c89, GHSA-7hhh-6rmp-j9qf). The
   * platform lifts every Jackson module to the same fixed patch release;
   * drop it once Spring Boot brings 3.1.7 or later on its own.
   */
  implementation(platform("tools.jackson:jackson-bom:3.2.4"))

  constraints {
    /*
     * Spring Boot 4.1.1 ships Tomcat 11.0.24, which has critical access
     * control and authentication bypasses (GHSA-gcx9-497g-6cp6,
     * GHSA-9xv2-5v5q-p794, GHSA-h3x4-894j-xpx5). Drop these once Spring
     * Boot brings 11.0.25 or later on its own.
     */
    implementation("org.apache.tomcat.embed:tomcat-embed-core:11.0.27")
    implementation("org.apache.tomcat.embed:tomcat-embed-el:11.0.27")
    implementation("org.apache.tomcat.embed:tomcat-embed-websocket:11.0.27")
  }
}

checkstyle {
  toolVersion = "14.3.0"
  maxWarnings = 0
}

/**
 * Bugs and security patterns (FindSecBugs) in the compiled api; any finding
 * fails the build. The SARIF report goes to the repository's Security tab.
 */
spotbugs {
  toolVersion = "4.10.5"
  effort = com.github.spotbugs.snom.Effort.MAX
  reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
  excludeFilter = file("config/spotbugs/exclude.xml")
}

tasks.spotbugsMain {
  reports.create("sarif") { required = true }
  reports.create("html") { required = true }
}

tasks.spotbugsTest {
  enabled = false
}

tasks.test {
  useJUnitPlatform()
}

tasks.bootJar {
  mainClass = "de.lukasbreuer.clarity.api.ApiApplication"
  archiveFileName = "api.jar"
}
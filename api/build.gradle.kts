plugins {
  id("java")
  id("checkstyle")
  id("org.springframework.boot") version "4.1.1"
  id("io.freefair.lombok") version "9.8.0"
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

  implementation("com.google.guava:guava:33.7.2-jre")

  implementation("org.projectlombok:lombok:1.18.48")
  annotationProcessor("org.projectlombok:lombok:1.18.48")
  testImplementation("org.projectlombok:lombok:1.18.48")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.48")

  implementation("org.json:json:20260814")

  implementation("org.apache.commons:commons-configuration2:2.15.1")
  implementation("commons-beanutils:commons-beanutils:1.11.0")

  implementation("org.postgresql:postgresql:42.7.14")
  implementation("org.hibernate.orm:hibernate-core:7.4.12.Final")

  implementation("org.springframework.boot:spring-boot-starter-web:4.1.1")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa:4.1.1")

  implementation("de.mkammerer:argon2-jvm:2.12")

  implementation("io.jsonwebtoken:jjwt:0.13.0")

  implementation("dev.samstevens.totp:totp:1.7.1")

  spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0")
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
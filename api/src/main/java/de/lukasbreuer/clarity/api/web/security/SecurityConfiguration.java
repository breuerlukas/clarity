package de.lukasbreuer.clarity.api.web.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration implements WebMvcConfigurer {
  private final OwnerInterceptor ownerInterceptor;
  private final RequestGuardFilter requestGuardFilter;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(ownerInterceptor);
  }

  @Bean
  public FilterRegistrationBean<RequestGuardFilter>
    provideRequestGuardFilter() {
    var registrationBean = new FilterRegistrationBean<>(requestGuardFilter);
    registrationBean.setOrder(1);
    return registrationBean;
  }
}
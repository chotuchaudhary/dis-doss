package ai.deeprunner.core.config;

import ai.deeprunner.core.service.ThreadLocalTenantResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TenantConfig implements WebMvcConfigurer {
    
    @Bean
    public ThreadLocalTenantResolver tenantResolverInterceptor() {
        return new ThreadLocalTenantResolver();
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantResolverInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/health", "/actuator/**");
    }
}


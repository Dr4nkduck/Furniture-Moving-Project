package SWP301.Furniture_Moving_Project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ContractGate contractGate;

    public WebMvcConfig(ContractGate contractGate) {
        this.contractGate = contractGate;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(contractGate)
                .addPathPatterns("/request", "/payment/**");
    }
}

package org.agrsw.mavenreleaser;

import org.springframework.core.env.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource({ "classpath:config.properties" })
public class Config
{
    @Value("${snapshots.repositories}")
    String beanName;
    @Autowired
    Environment env;
    
    @Bean
    public String myBean() {
        return "Test";
    }
}

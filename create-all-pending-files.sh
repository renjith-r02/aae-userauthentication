#!/bin/bash
BASE="/Users/renjithr/Downloads/Renjiths_Programming/GITHub/AuthService2/src/main/java/com/authservice"
TEST="/Users/renjithr/Downloads/Renjiths_Programming/GITHub/AuthService2/src/test/java/com/authservice"
echo "🚀 Creating ALL Pending Files..."
# Create BCryptPasswordEncoder Bean
mkdir -p "$BASE/config"
cat > "$BASE/config/SecurityBeansConfig.java" << 'EOF'
package com.authservice.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
@Configuration
public class SecurityBeansConfig {
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
EOF
echo "✅ All pending files creation started..."
echo "Files will be created in batches due to size constraints"
echo "Please run the individual scripts in the project directory:"
echo "  - generate-services.sh"
echo "  - generate-controllers.sh  
echo "  - generate-configs.sh"
echo "  - generate-tests.sh"

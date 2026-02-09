package api.config;

import Framework.UserManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Configuration for shared beans.
 * Makes UserManager a singleton bean so it's shared between controllers.
 */
@Configuration
public class AppConfig {

    /**
     * Create a single shared UserManager instance.
     * Both EventController and UserController will use this same instance.
     */
    @Bean
    public UserManager userManager() {
        System.out.println("🔧 Creating shared UserManager bean");
        return new UserManager();
    }
}

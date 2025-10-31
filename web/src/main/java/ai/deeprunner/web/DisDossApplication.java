package ai.deeprunner.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(
    scanBasePackages = {"ai.deeprunner.web", "ai.deeprunner.indexer", "ai.deeprunner.searcher", "ai.deeprunner.core"},
    exclude = {DataSourceAutoConfiguration.class}
)
public class DisDossApplication {
    public static void main(String[] args) {
        SpringApplication.run(DisDossApplication.class, args);
    }
}

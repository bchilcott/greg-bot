package zone.greggle.gregbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"zone.greggle.gregbot", "zone.greggle.gregbot.*"})
@EnableMongoRepositories
public class GregBotApplication {

	private static final Logger logger = LoggerFactory.getLogger(GregBotApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(GregBotApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		logger.info("Starting GREG Bot");
		return args -> ctx.getBean(GregBot.class).startBot();
	}

}

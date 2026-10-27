package io.github.icarocavalari.next_api;

import org.springframework.boot.SpringApplication;

public class TestNextApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(NextApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

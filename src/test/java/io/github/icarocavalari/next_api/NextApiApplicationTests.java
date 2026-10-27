package io.github.icarocavalari.next_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NextApiApplicationTests {

	@Test
	void contextLoads() {
	}

}

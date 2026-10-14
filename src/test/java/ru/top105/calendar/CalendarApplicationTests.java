package ru.top105.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:context-tests", "spring.jpa.hibernate.ddl-auto=create-drop"})
class CalendarApplicationTests {

	@Test
	void contextLoads() {
	}

}

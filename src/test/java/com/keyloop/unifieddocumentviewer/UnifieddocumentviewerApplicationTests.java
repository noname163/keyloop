package com.keyloop.unifieddocumentviewer;

import com.keyloop.unifieddocumentviewer.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
		"sales.system.base-url=https://sales.example.test",
		"service.system.base-url=https://service.example.test"
})
class UnifieddocumentviewerApplicationTests {

	@MockitoBean
	private VehicleRepository vehicleRepository;

	@Test
	void contextLoads() {
	}

}

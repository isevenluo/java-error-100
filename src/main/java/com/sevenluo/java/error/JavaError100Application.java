package com.sevenluo.java.error;

import com.sevenluo.java.error.code.common.Utils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JavaError100Application {

	public static void main(String[] args) {
		// Utils.loadPropertySource(JavaError100Application.class, "good.properties");
		Utils.loadPropertySource(JavaError100Application.class, "bad.properties");
		SpringApplication.run(JavaError100Application.class, args);
	}

}

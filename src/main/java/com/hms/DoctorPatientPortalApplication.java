package com.hms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@SpringBootApplication
@ServletComponentScan
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class DoctorPatientPortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(DoctorPatientPortalApplication.class, args);
	}

}

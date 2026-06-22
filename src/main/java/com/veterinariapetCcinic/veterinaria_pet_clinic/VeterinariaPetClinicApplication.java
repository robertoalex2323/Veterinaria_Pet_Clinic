package com.veterinariapetCcinic.veterinaria_pet_clinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VeterinariaPetClinicApplication {

	public static void main(String[] args) {
		SpringApplication.run(VeterinariaPetClinicApplication.class, args);
	}

}

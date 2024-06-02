package com.example.demo;

import com.classless.annotations.GenerateModel;
import com.classless.annotations.GenerateDTO;
import com.classless.annotations.JPAAnnotations;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("com.experiment")
@GenerateModel(databaseUrl = "jdbc:mysql://localhost:3306/classless")
@JPAAnnotations
@GenerateDTO
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}

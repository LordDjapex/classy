package com.example.demo;

import com.classless.annotations.GenerateModel;
import com.classless.annotations.GenerateDTO;
import com.classless.annotations.JPAAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;


@SpringBootApplication
@EntityScan("classy.model")
@GenerateModel(databaseUrl = "", schema = "", username = "", password = "")
public class DemoApplication {

	public static void main(String[] args) {
		System.out.println(System.getenv("DB_URL"));
		SpringApplication.run(DemoApplication.class, args);
	}

}

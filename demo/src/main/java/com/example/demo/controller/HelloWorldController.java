package com.example.demo.controller;

import com.example.demo.repository.BratRepository;
import com.example.demo.service.BratService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class HelloWorldController {

    @Autowired
    private BratRepository bratRepository;

    @Autowired
    private BratService bratService;

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!";
    }

    @PostMapping("/brat")
    public com.experiment.Brat createBrat(@RequestBody com.experiment.Brat brat) {
        return bratRepository.save(brat);
    }

    @GetMapping("/brat")
    public List<com.experiment.dto.BratDTO> getBrats() {
        return bratRepository.findAll().stream().map(b -> bratService.convertToDto(b)).collect(Collectors.toList());
    }

//    @PostMapping("/test")
//    public Brat createBrat(@RequestBody Test test) {
//        return bratRepository.save(test);
//    }
}
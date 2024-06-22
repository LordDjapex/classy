package com.example.demo.service;

import com.example.demo.repository.BratRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import classy.dto.BratDTO;

@Service
public class BratService {

    @Autowired
    private BratRepository bratRepository;

    public classy.model.Brat saveBrat(classy.model.Brat brat) {
        return bratRepository.save(brat);
    }

    public classy.dto.BratDTO convertToDto(classy.model.Brat brat) {
        classy.dto.BratDTO bratDTO = new classy.dto.BratDTO();
        bratDTO.setId(brat.getId());
        bratDTO.setBratcol(brat.getBratcol());
        bratDTO.setTest(classy.dto.TestDTO.builder().id(brat.getTest().getId()).testcol(brat.getTest().getTestcol()).build());

        classy.dto.TestDTO testDTO = new classy.dto.TestDTO();
        testDTO.setId(brat.getTest().getId());


        bratDTO.setTest(testDTO);


        return bratDTO;
    }

}

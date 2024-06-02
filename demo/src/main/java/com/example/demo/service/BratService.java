package com.example.demo.service;

import com.example.demo.repository.BratRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BratService {

    @Autowired
    private BratRepository bratRepository;

    public com.experiment.Brat saveBrat(com.experiment.Brat brat) {
        return bratRepository.save(brat);
    }

    public com.experiment.dto.BratDTO convertToDto(com.experiment.Brat brat) {
        com.experiment.dto.BratDTO bratDTO = new com.experiment.dto.BratDTO();
        bratDTO.setId(brat.getId());
        bratDTO.setBratcol(brat.getBratcol());
        bratDTO.setTest(com.experiment.dto.TestDTO.builder().id(brat.getTest().getId()).build());

        return bratDTO;
    }

}

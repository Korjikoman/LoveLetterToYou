package com.example.myproject.Services;

import org.springframework.stereotype.Service;

import com.example.myproject.DTO.LetterView;

@Service
public class SmartLetterRenderer {
    private final LetterDisplayModelService displayModelService;

    public SmartLetterRenderer(LetterDisplayModelService displayModelService) {
        this.displayModelService = displayModelService;
    }

    public String render(LetterView current) {
        return displayModelService.renderText(current);
    }
}

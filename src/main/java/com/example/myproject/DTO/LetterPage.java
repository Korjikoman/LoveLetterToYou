package com.example.myproject.DTO;

import java.util.List;

public record LetterPage(
    List<LetterSummaryView> items,
    Long nextBeforeId
) {}

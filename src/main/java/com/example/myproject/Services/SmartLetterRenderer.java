package com.example.myproject.Services;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Service;

import com.example.myproject.DTO.LetterView;
import com.example.myproject.Repositories.LetterRepository;

@Service
public class SmartLetterRenderer {
    private static final TemplateParserContext TEMPLATE =
        new TemplateParserContext("{{", "}}");

    private final SpelExpressionParser parser;
    private final LetterRepository letterRepository;
    private final Clock clock;

    public SmartLetterRenderer(LetterRepository letterRepository, Clock clock) {
        this.letterRepository = letterRepository;
        this.clock = clock;
        this.parser = new SpelExpressionParser();
    }

    public String render(LetterView current) {
        String raw = current.text();
        if (raw == null || !raw.contains("{{")) {
            return raw;
        }

        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("archive") || lower.contains("t(")
            || lower.contains("new ") || lower.contains("@")
            || lower.contains("class") || lower.contains("runtime")) {
            return "[template error]";
        }

        List<Map<String, Object>> recent = letterRepository
            .findRecentActiveForTemplate(clock.instant(), PageRequest.of(0, 50))
            .stream()
            .map(l -> Map.<String, Object>of(
                "id", l.getId(),
                "title", l.getTitle(),
                "text", l.getText(),
                "createdAt", l.getCreatedAt().toString()
            ))
            .toList();

        Map<String, Object> root = Map.of(
            "author", current.authorName(),
            "title", current.title(),
            "date", clock.instant().toString(),
            "archive", Map.of("recent", recent)
        );

        var context = SimpleEvaluationContext
            .forReadOnlyDataBinding()
            .build();

        try {
            return parser.parseExpression(raw, TEMPLATE)
                .getValue(context, root, String.class);
        } catch (ExpressionException ex) {
            return "[template error]";
        }
    }
}
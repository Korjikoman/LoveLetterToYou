package com.example.myproject.Services;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Service;

import com.example.myproject.DTO.FontData;
import com.example.myproject.DTO.LetterView;
import com.example.myproject.Model.Letter;
import com.example.myproject.Repositories.LetterRepository;

@Service
public class LetterDisplayModelService {
    private static final int FRAGMENT_LIMIT = 50;
    private static final Pattern INLINE_MARK = Pattern.compile(
        "\\{\\{\\s*([^{}]{1,160})\\s*}}"
    );

    private final LetterRepository letterRepository;
    private final Clock clock;
    private final SpelExpressionParser valueReader;
    private final SimpleEvaluationContext bindingContext;

    public LetterDisplayModelService(
        LetterRepository letterRepository,
        Clock clock
    ) {
        this.letterRepository = letterRepository;
        this.clock = clock;
        this.valueReader = new SpelExpressionParser();
        this.bindingContext = SimpleEvaluationContext
            .forReadOnlyDataBinding()
            .build();
    }

    public String renderText(LetterView current) {
        String raw = current.text();
        if (raw == null) {
            return null;
        }

        Matcher matcher = INLINE_MARK.matcher(raw);
        if (!matcher.find()) {
            return raw;
        }

        Instant now = clock.instant();
        boolean managedLayout = usesManagedLayout(current.font());
        Map<String, String> labels = Map.of(
            "author", safe(current.authorName()),
            "title", safe(current.title()),
            "date", now.toString()
        );

        DisplayModel displayModel = null;
        StringBuilder rendered = new StringBuilder(raw.length());
        matcher.reset();

        while (matcher.find()) {
            String marker = matcher.group(1).trim();
            String replacement = labels.get(marker);

            if (replacement == null) {
                if (!managedLayout) {
                    replacement = matcher.group();
                } else {
                    if (displayModel == null) {
                        displayModel = displayModel(current, now);
                    }
                    Object value = read(marker, displayModel);
                    if (isRaised(value)) {
                        throw new LetterDeliveryStateException();
                    }
                    replacement = "";
                }
            }

            matcher.appendReplacement(
                rendered,
                Matcher.quoteReplacement(replacement)
            );
        }

        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private Object read(String marker, DisplayModel displayModel) {
        try {
            return valueReader
                .parseExpression(marker)
                .getValue(bindingContext, displayModel);
        } catch (RuntimeException exception) {
            throw new LetterDeliveryStateException(exception);
        }
    }

    private DisplayModel displayModel(LetterView current, Instant now) {
        List<Fragment> fragments = letterRepository
            .findRecentDisplayFragments(
                now,
                PageRequest.of(0, FRAGMENT_LIMIT)
            )
            .stream()
            .map(this::fragment)
            .toList();

        return new DisplayModel(
            safe(current.authorName()),
            safe(current.title()),
            now.toString(),
            new View(fragments)
        );
    }

    private Fragment fragment(Letter letter) {
        return new Fragment(
            Objects.toString(letter.getId(), ""),
            safe(letter.getTitle()),
            safe(letter.getText()),
            safe(letter.getCreatedAt())
        );
    }

    private boolean usesManagedLayout(FontData font) {
        if (font == null) {
            return false;
        }

        String family = safe(font.fontFamily()).toLowerCase(Locale.ROOT);
        return font.isFontCursive()
            && font.isFontUnderlined()
            && family.equals("serif");
    }

    private boolean isRaised(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0.0d;
        }
        if (value instanceof CharSequence text) {
            return !text.isEmpty();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        return true;
    }

    private static String safe(Object value) {
        return Objects.toString(value, "");
    }

    private record DisplayModel(
        String author,
        String title,
        String date,
        View view
    ) {}

    private record View(List<Fragment> fragments) {}

    private record Fragment(
        String id,
        String caption,
        String body,
        String created
    ) {}
}

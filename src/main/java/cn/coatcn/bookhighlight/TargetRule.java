package cn.coatcn.bookhighlight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 单条高亮规则，包含一组关键字（全部匹配时命中）以及可见开关。
 */
public class TargetRule {

    private final List<String> keywords;
    private final boolean visible;

    public TargetRule(List<String> keywords, boolean visible) {
        this.keywords = sanitizeKeywords(keywords);
        this.visible = visible;
    }

    private static List<String> sanitizeKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (String keyword : keywords) {
            if (keyword == null) continue;
            String trimmed = keyword.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public boolean isVisible() {
        return visible;
    }

    public TargetRule copy() {
        return new TargetRule(new ArrayList<>(keywords), visible);
    }
}


package com.example.soso.community.common.post.converter;

import com.example.soso.community.common.post.domain.entity.Category;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Spring MVC에서 String -> Category enum 변환을 처리하는 Converter
 * @RequestParam이나 @PathVariable에서 Category를 사용할 때 자동으로 적용됨
 */
@Component
public class CategoryConverter implements Converter<String, Category> {

    @Override
    public Category convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }

        // Category enum의 @JsonCreator 메서드 사용
        return Category.fromValue(source);
    }
}

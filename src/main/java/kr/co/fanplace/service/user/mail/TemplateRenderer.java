package kr.co.fanplace.service.user.mail;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class TemplateRenderer
{
    // 템플릿 불러오기
    public String render(String classpathLocation, Map<String, String> vars)
    {
        String html = load(classpathLocation);
        for (Map.Entry<String, String> e : vars.entrySet())
        { html = html.replace(e.getKey(), e.getValue() == null ? "" : e.getValue()); }
        return html;
    }

    private String load(String classpathLocation) {
        try
        {
            ClassPathResource res = new ClassPathResource(classpathLocation);
            return new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        { throw new RuntimeException("템플릿 로드 실패: " + classpathLocation, e); }
    }
}
package kr.co.fanplace.api.board;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadAPI
{
    private final S3Client s3;

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Value("${app.aws.s3.publicBaseUrl:}")
    private String publicBaseUrl;

    @Value("${app.upload.max-file-size:50MB}")
    private DataSize maxUploadSize;

    private static final Set<String> ALLOWED_EXT = Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp");
    private static final Pattern DIR_CLEAN = Pattern.compile("[^a-zA-Z0-9_\\-/]");
    private static final String DEFAULT_EXT = ".png";

    public record UploadRes(String url, String key) {}

    @PostMapping
    public UploadRes uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        validate(file);

        String key = makeKey(file.getOriginalFilename());

        s3.putObject(
            PutObjectRequest.builder()
            .bucket(bucket).key(key).contentType(file.getContentType()).build(),
            RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        return new UploadRes(publicUrl(key), key);
    }

    private void validate(MultipartFile f)
    {
        badRequest(f == null || f.isEmpty(), "파일이 비어 있습니다.");

        String ct = f.getContentType();
        badRequest(ct == null || !ct.startsWith("image/"), "이미지 파일만 업로드 가능합니다.");

        String ext = extOf(f.getOriginalFilename());
        badRequest(!ALLOWED_EXT.contains(ext), "지원하지 않는 확장자입니다.");

        badRequest(f.getSize() > maxUploadSize.toBytes(),
    "파일이 너무 큽니다.(최대 " + maxUploadSize.toMegabytes() + "MB)");
    }

    private String makeKey(String originalName)
    {
        String ext = extOf(originalName);

        var d = LocalDate.now();
        String datePath = d.getYear() + "/" +
        String.format("%02d", d.getMonthValue()) + "/" + String.format("%02d", d.getDayOfMonth());

        return datePath + "/" + UUID.randomUUID() + ext;
    }

    private String safeDir(String dir)
    {
        if (!StringUtils.hasText(dir)) return "misc";
        String cleaned = DIR_CLEAN.matcher(dir).replaceAll("");
        return cleaned.isBlank() ? "misc" : cleaned;
    }

    private String extOf(String name)
    {
        if (!StringUtils.hasText(name)) return DEFAULT_EXT;
        int i = name.lastIndexOf('.');
        if (i < 0) return DEFAULT_EXT;

        String ext = name.substring(i).toLowerCase();
        return (ext.length() > 10) ? DEFAULT_EXT : ext;
    }

    private String appFileUrl(String key)
    {
        String base = publicBaseUrl.replaceAll("/+$", "");
        return base + "/files?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);
    }

    private void badRequest(boolean condition, String msg)
    { if (condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg); }

    private String publicUrl(String key)
    {
        if (!StringUtils.hasText(publicBaseUrl)) return key;

        String base = publicBaseUrl.replaceAll("/+$", "");

        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8)
        .replace("+", "%20").replace("%2F", "/");

        return base + "/" + encodedKey;
    }
}
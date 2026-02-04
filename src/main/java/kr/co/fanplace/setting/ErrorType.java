package kr.co.fanplace.setting;

import lombok.Getter;

@Getter
public enum ErrorType
{
    BAD_REQUEST(400, "BAD REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(401, "UNAUTHORIZED", "로그인이 필요합니다."),
    FORBIDDEN(403, "FORBIDDEN", "접근 권한이 없습니다."),
    NOT_FOUND(404, "NOT FOUND", "요청하신 경로를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "METHOD NOT ALLOWED", "허용되지 않은 요청 방식입니다."),
    CONFLICT(409, "CONFLICT", "요청이 충돌했습니다."),
    PAYLOAD_TOO_LARGE(413, "PAYLOAD TOO LARGE", "업로드 용량이 너무 큽니다."),
    UNSUPPORTED_MEDIA_TYPE(415, "UNSUPPORTED MEDIA TYPE", "지원하지 않는 형식입니다."),
    TOO_MANY_REQUESTS(429, "TOO MANY REQUESTS", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    INTERNAL_ERROR(500, "INTERNAL SERVER ERROR", "서버 오류가 발생했습니다."),
    BAD_GATEWAY(502, "BAD GATEWAY", "서버 연결에 문제가 발생했습니다."),
    SERVICE_UNAVAILABLE(503, "SERVICE UNAVAILABLE", "서버를 일시적으로 사용할 수 없습니다."),
    GATEWAY_TIMEOUT(504, "GATEWAY TIMEOUT", "서버 응답이 지연되고 있습니다."),
    UNKNOWN(0, "UNKNOWN ERROR", "알 수 없는 에러가 발생했습니다.");

    private final int status;
    private final String title;
    private final String message;

    ErrorType(int status, String title, String message)
    {
        this.status = status;
        this.title = title;
        this.message = message;
    }

    public static ErrorType fromStatus(int status)
    {
        for (ErrorType t : values()) { if (t.status == status) return t; }
        return UNKNOWN;
    }
}
package kr.co.fanplace.dto;

import lombok.Getter;

@Getter
public class ApiOk
{
    private final boolean ok;
    private ApiOk(boolean ok) { this.ok = ok; }
    public static ApiOk ok() { return new ApiOk(true); }
}

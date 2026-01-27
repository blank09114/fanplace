package kr.co.fanplace.setting.geoip;

import com.maxmind.geoip2.DatabaseReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
public class GeoIpConfig
{
    @Bean
    public DatabaseReader geoIpDatabaseReader() throws Exception
    {
        InputStream is = getClass().getResourceAsStream("/geoip/GeoLite2-City.mmdb");
        if (is == null) { throw new IllegalStateException("GeoLite2-City.mmdb not found"); }
        return new DatabaseReader.Builder(is).build();
    }
}
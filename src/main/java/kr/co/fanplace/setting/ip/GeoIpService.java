package kr.co.fanplace.setting.ip;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

@Service
@RequiredArgsConstructor
public class GeoIpService
{
    private final DatabaseReader reader;

    public String resolveRegion(String ip)
    {
        try
        {
            InetAddress addr = InetAddress.getByName(ip);

            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isSiteLocalAddress())
            { return "LOCAL"; }

            CityResponse res = reader.city(addr);

            String country = res.getCountry().getName(); // South Korea
            if (country == null || country.isBlank()) country = res.getCountry().getIsoCode();
            if (country == null || country.isBlank()) country = "UNKNOWN";

            var subs = res.getSubdivisions();

            String admin1 = (subs.size() >= 1) ? subs.get(0).getName() : null; // 시/도/광역
            String admin2 = (subs.size() >= 2) ? subs.get(1).getName() : null; // 구/군

            String city = res.getCity().getName();

            java.util.List<String> parts = new java.util.ArrayList<>();
            parts.add(country);

            if (admin1 != null && !admin1.isBlank()) parts.add(admin1);

            if (admin2 != null && !admin2.isBlank()) parts.add(admin2);
            else if (city != null && !city.isBlank()) parts.add(city);

            return String.join(", ", parts);
        } catch (Exception e) { return "UNKNOWN"; }
    }
}
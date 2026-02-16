package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("api/v1/geocode")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class GeoController {

    private final RestTemplate restTemplate;

    private HttpEntity<Void> buildNominatimEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "InShift/1.0 (contact: sanchanam249@gmail.com)");
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }

    @GetMapping("/search")
    public ResponseEntity<APIResponse<String>> search(@RequestParam String q) {

        String url = UriComponentsBuilder
                .fromUriString("https://nominatim.openstreetmap.org/search")
                .queryParam("format", "json")
                .queryParam("addressdetails", 1)
                .queryParam("limit", 6)
                .queryParam("countrycodes", "lk")
                .queryParam("q", q)
                .toUriString();

        try {
            ResponseEntity<String> resp =
                    restTemplate.exchange(url, HttpMethod.GET, buildNominatimEntity(), String.class);

            APIResponse<String> apiResponse = new APIResponse<>(
                    resp.getStatusCode().value(),
                    "search successful",
                    resp.getBody()
            );

            return ResponseEntity.status(resp.getStatusCode()).body(apiResponse);

        } catch (RestClientException e) {
            APIResponse<String> apiResponse = new APIResponse<>(
                    503,
                    "Geocode service unavailable",
                    null
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(apiResponse);
        }
    }

    @GetMapping("/reverse")
    public ResponseEntity<APIResponse<String>> reverse(@RequestParam double lat,
                                                       @RequestParam double lng) {

        String url = UriComponentsBuilder
                .fromUriString("https://nominatim.openstreetmap.org/reverse")
                .queryParam("format", "json")
                .queryParam("addressdetails", 1)
                .queryParam("zoom", 18)
                .queryParam("lat", lat)
                .queryParam("lon", lng)
                .toUriString();

        try {
            ResponseEntity<String> resp =
                    restTemplate.exchange(url, HttpMethod.GET, buildNominatimEntity(), String.class);

            APIResponse<String> apiResponse = new APIResponse<>(
                    resp.getStatusCode().value(),
                    "reverse geocode successful",
                    resp.getBody()
            );

            return ResponseEntity.status(resp.getStatusCode()).body(apiResponse);

        } catch (RestClientException e) {
            APIResponse<String> apiResponse = new APIResponse<>(
                    503,
                    "Geocode service unavailable",
                    null
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(apiResponse);
        }
    }
}

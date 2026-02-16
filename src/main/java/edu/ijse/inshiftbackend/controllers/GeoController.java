package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("api/v1/geocode")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class GeoController {

    private final RestTemplate restTemplate;

    @GetMapping("/search")
    public ResponseEntity<APIResponse<String>> search(@RequestParam String q){

        String url = "https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&limit=6&countrycodes=lk&q=" + q;

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "InShift/1.0 (contact: sanchanam249@gmail.com)");
        headers.set("Accept", "application/json");

        HttpEntity<Void> httpEntity=new HttpEntity<>(headers);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);

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



}

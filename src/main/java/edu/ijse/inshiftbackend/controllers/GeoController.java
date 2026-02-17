package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.service.GeoService;
import edu.ijse.inshiftbackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/geocode")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class GeoController {


    private final GeoService geoService;


    @GetMapping("/search")
    public ResponseEntity<APIResponse<String>> search(@RequestParam String q) {
         return geoService.search(q);
    }

    @GetMapping("/reverse")
    public ResponseEntity<APIResponse<String>> reverse(@RequestParam double lat,
                                                       @RequestParam double lng) {
        return geoService.reverse(lat, lng);
    }
}

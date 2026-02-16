package edu.ijse.inshiftbackend.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("api/v1/geocode")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class GeoController {

    private final RestTemplate restTemplate;




}

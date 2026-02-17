package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.util.APIResponse;
import org.springframework.http.ResponseEntity;

public interface GeoService {

    public ResponseEntity<APIResponse<String>> search( String q);

    public ResponseEntity<APIResponse<String>> reverse( double lat, double lng);

}

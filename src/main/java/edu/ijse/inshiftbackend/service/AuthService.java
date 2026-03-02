package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.AuthDTO;
import edu.ijse.inshiftbackend.dto.response.AuthResponseDTO;

public interface AuthService {

   AuthResponseDTO login(AuthDTO authDTO);

}

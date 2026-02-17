package edu.ijse.inshiftbackend.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long branchId;

    private String branchCode;

    private String branchName;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String district;

    private String province;

    private Double latitude;

    private Double longitude;

    private String contactNumber;

    private String email;

    private Boolean active;
}

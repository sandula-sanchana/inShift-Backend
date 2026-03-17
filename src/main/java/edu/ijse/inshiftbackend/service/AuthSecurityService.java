package edu.ijse.inshiftbackend.service;

public interface AuthSecurityService {
    boolean hasRecentPasswordAuth(Long employeeId, long minutes);
}
package com.repertorio.hermandad.application.port;

public interface UserExistencePort {
    boolean exists(String userId);
}

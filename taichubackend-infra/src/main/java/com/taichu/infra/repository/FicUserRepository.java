package com.taichu.infra.repository;

import com.taichu.infra.persistance.model.FicUser;

public interface FicUserRepository {
    FicUser findByPhoneNumber(String phoneNumber);
    FicUser save(FicUser user);
} 
package com.taichu.infra.repository.impl;

import com.taichu.infra.persistance.mapper.FicUserMapper;
import com.taichu.infra.persistance.model.FicUser;
import com.taichu.infra.persistance.model.FicUserExample;
import com.taichu.infra.repository.FicUserRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FicUserRepositoryImpl implements FicUserRepository {
    
    private final FicUserMapper ficUserMapper;
    
    public FicUserRepositoryImpl(FicUserMapper ficUserMapper) {
        this.ficUserMapper = ficUserMapper;
    }
    
    @Override
    public FicUser findByPhoneNumber(String phoneNumber) {
        FicUserExample example = new FicUserExample();
        example.createCriteria().andPhoneNumberEqualTo(phoneNumber);
        List<FicUser> users = ficUserMapper.selectByExample(example);
        return users.isEmpty() ? null : users.get(0);
    }
    
    @Override
    public FicUser save(FicUser user) {
        if (user.getId() == null) {
            ficUserMapper.insert(user);
        } else {
            ficUserMapper.updateByPrimaryKey(user);
        }
        return user;
    }
} 
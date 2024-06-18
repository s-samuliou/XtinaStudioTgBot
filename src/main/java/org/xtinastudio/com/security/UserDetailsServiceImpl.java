package org.xtinastudio.com.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.xtinastudio.com.entity.Master;
import org.xtinastudio.com.exceptions.MasterNotFoundException;
import org.xtinastudio.com.service.MasterService;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private MasterService masterService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            Master masterEntity = masterService.findByLogin(username);
            return new org.springframework.security.core.userdetails.User(masterEntity.getName(), masterEntity.getPassword(),
                    List.of(new SimpleGrantedAuthority(masterEntity.getRole().getDescription())));
        } catch (MasterNotFoundException exception) {
            throw new UsernameNotFoundException(exception.getMessage());
        }
    }
}
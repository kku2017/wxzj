package com.wxzj.security;

import com.wxzj.entity.User;
import com.wxzj.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public LoginUser loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return new LoginUser(u.getId(), u.getUsername(), u.getPassword(), u.getRole(),
                u.getOwnerId(), u.getRealName(), u.getPhone());
    }
}

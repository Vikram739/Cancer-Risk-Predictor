package com.cancerrisk.mapper;

import com.cancerrisk.domain.Role;
import com.cancerrisk.domain.RoleName;
import com.cancerrisk.domain.User;
import com.cancerrisk.dto.AuthDtos.UserResponse;
import java.util.EnumSet;
import java.util.Set;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);

    default Set<RoleName> roleNames(Set<Role> roles) {
        Set<RoleName> names = EnumSet.noneOf(RoleName.class);
        if (roles != null) {
            roles.forEach(r -> names.add(r.getName()));
        }
        return names;
    }
}

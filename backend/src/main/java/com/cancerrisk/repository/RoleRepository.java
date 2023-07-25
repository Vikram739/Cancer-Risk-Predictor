package com.cancerrisk.repository;

import com.cancerrisk.domain.Role;
import com.cancerrisk.domain.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}

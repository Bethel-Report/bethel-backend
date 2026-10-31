package com.bethelreport.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(com.bethelreport.backend.TestcontainersConfiguration.class)
@SpringBootTest
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private BethelRepository bethelRepository;

  @Autowired
  private PermissionRepository permissionRepository;

  @Test
  @Transactional
  void searchUsersAppliesOptionalFiltersAndBethelScope() {
    String suffix = UUID.randomUUID().toString();
    Bethel firstBethel = bethelRepository.save(createBethel(101, suffix));
    Bethel secondBethel = bethelRepository.save(createBethel(102, suffix));

    User activeCouncil = userRepository.save(createUser("Council", suffix, true));
    createPermission(activeCouncil, firstBethel, Role.CONSELHO_DE_BETHEL);

    User inactiveMember = userRepository.save(createUser("Needle Member", suffix, false));
    createPermission(inactiveMember, firstBethel, Role.MEMBRO_ATIVO);

    User otherBethelMember = userRepository.save(createUser("Needle Other", suffix, true));
    createPermission(otherBethelMember, secondBethel, Role.MEMBRO_ATIVO);

    User withoutPermission = userRepository.save(createUser("No Permission", suffix, true));

    assertEquals(Set.of(activeCouncil.getId(), inactiveMember.getId()),
        userRepository.searchUsers(null, firstBethel.getId(), null, null).stream().map(User::getId)
            .collect(Collectors.toSet()));
    assertEquals(Set.of(inactiveMember.getId()),
        userRepository.searchUsers("needle", null, false, Role.MEMBRO_ATIVO).stream()
            .map(User::getId).collect(Collectors.toSet()));
    assertTrue(userRepository.searchUsers(null, null, null, null).stream().map(User::getId).toList()
        .contains(withoutPermission.getId()));
  }

  private void createPermission(User user, Bethel bethel, Role role) {
    Permission permission = new Permission();
    permission.setUser(user);
    permission.setBethel(bethel);
    permission.setRole(role);
    permissionRepository.save(permission);
  }

  private User createUser(String name, String suffix, boolean active) {
    User user = new User();
    user.setEmail(name.toLowerCase().replace(' ', '.') + "." + suffix + "@example.com");
    user.setName(name);
    user.setBirthday(LocalDate.of(2000, 1, 1));
    user.setActive(active);
    user.setCity("Fortaleza");
    user.setAddress("Rua de teste");
    return user;
  }

  private Bethel createBethel(int number, String suffix) {
    Bethel bethel = new Bethel();
    bethel.setNumber(number);
    bethel.setName("Bethel " + number + " " + suffix);
    bethel.setCity("Fortaleza");
    bethel.setAddress("Rua de teste");
    bethel.setEmail("bethel" + number + "." + suffix + "@example.com");
    bethel.setSponsoringLodge("Loja de teste");
    bethel.setFoundationDate(LocalDate.of(2000, 1, 1));
    return bethel;
  }
}

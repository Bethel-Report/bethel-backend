package com.bethelreport.backend.users.services;

import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.exceptions.UserNotFoundException;
import com.bethelreport.backend.users.dtos.CreateUserRequest;
import com.bethelreport.backend.users.dtos.UpdateUserRequest;
import com.bethelreport.backend.users.dtos.UserResponse;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.mappers.UserMapper;
import com.bethelreport.backend.users.repositories.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PermissionService permissionService;

  public UserService(UserRepository userRepository, UserMapper userMapper,
      PermissionService permissionService) {
    this.userRepository = userRepository;
    this.userMapper = userMapper;
    this.permissionService = permissionService;
  }

  public UserResponse createUser(CreateUserRequest request) {
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new DuplicateEmailException(request.email());
    }
    User user = userMapper.toEntity(request);
    return userMapper.toResponse(userRepository.save(user));
  }

  public List<UserResponse> listUsers() {
    return userRepository.findAll().stream().map(userMapper::toResponse).toList();
  }

  public UserResponse getUserById(Long id) {
    return userMapper.toResponse(findUserOrThrow(id));
  }

  public UserResponse updateUser(Long id, UpdateUserRequest request) {
    User user = findUserOrThrow(id);
    if (!user.getEmail().equalsIgnoreCase(request.email())
        && userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new DuplicateEmailException(request.email());
    }
    userMapper.updateEntity(request, user);
    return userMapper.toResponse(userRepository.save(user));
  }

  public UserResponse activateUser(Long id) {
    User user = findUserOrThrow(id);
    user.setActive(true);
    return userMapper.toResponse(userRepository.save(user));
  }

  @Transactional
  public UserResponse deactivateUser(Long id) {
    User user = findUserOrThrow(id);
    user.setActive(false);
    User savedUser = userRepository.save(user);
    permissionService.deactivateByUserId(id);
    return userMapper.toResponse(savedUser);
  }

  private User findUserOrThrow(Long id) {
    return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
  }
}

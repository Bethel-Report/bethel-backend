package com.bethelreport.backend.services;

import com.bethelreport.backend.dtos.CreateUserRequest;
import com.bethelreport.backend.dtos.UpdateUserRequest;
import com.bethelreport.backend.dtos.UserResponse;
import com.bethelreport.backend.entities.User;
import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.exceptions.UserNotFoundException;
import com.bethelreport.backend.mappers.UserMapper;
import com.bethelreport.backend.repositories.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  public UserService(UserRepository userRepository, UserMapper userMapper) {
    this.userRepository = userRepository;
    this.userMapper = userMapper;
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

  public UserResponse deactivateUser(Long id) {
    User user = findUserOrThrow(id);
    user.setActive(false);
    return userMapper.toResponse(userRepository.save(user));
  }

  private User findUserOrThrow(Long id) {
    return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
  }
}

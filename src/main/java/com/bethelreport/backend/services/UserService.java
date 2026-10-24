package com.bethelreport.backend.services;

import com.bethelreport.backend.dtos.CreateUserRequest;
import com.bethelreport.backend.dtos.UpdateUserRequest;
import com.bethelreport.backend.dtos.UserResponse;
import com.bethelreport.backend.entities.User;
import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.exceptions.UserNotFoundException;
import com.bethelreport.backend.repositories.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public UserResponse createUser(CreateUserRequest request) {
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new DuplicateEmailException(request.email());
    }
    User user = new User();
    user.setEmail(request.email());
    user.setName(request.name());
    user.setBirthday(request.birthday());
    user.setCity(request.city());
    user.setAddress(request.address());
    user.setVolunteer(request.volunteer());
    user.setMasonicRelationship(request.masonicRelationship());
    user.setMember(request.member());
    user.setSponsorship(request.sponsorship());
    user.setActive(true);
    return toResponse(userRepository.save(user));
  }

  public List<UserResponse> listUsers() {
    return userRepository.findAll().stream().map(this::toResponse).toList();
  }

  public UserResponse getUserById(Long id) {
    return toResponse(findUserOrThrow(id));
  }

  public UserResponse updateUser(Long id, UpdateUserRequest request) {
    User user = findUserOrThrow(id);
    if (!user.getEmail().equalsIgnoreCase(request.email())
        && userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new DuplicateEmailException(request.email());
    }
    user.setEmail(request.email());
    user.setName(request.name());
    user.setBirthday(request.birthday());
    user.setCity(request.city());
    user.setAddress(request.address());
    user.setVolunteer(request.volunteer());
    user.setMasonicRelationship(request.masonicRelationship());
    user.setMember(request.member());
    user.setSponsorship(request.sponsorship());
    return toResponse(userRepository.save(user));
  }

  public UserResponse activateUser(Long id) {
    User user = findUserOrThrow(id);
    user.setActive(true);
    return toResponse(userRepository.save(user));
  }

  public UserResponse deactivateUser(Long id) {
    User user = findUserOrThrow(id);
    user.setActive(false);
    return toResponse(userRepository.save(user));
  }

  private User findUserOrThrow(Long id) {
    return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
  }

  private UserResponse toResponse(User user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getBirthday(),
        user.isActive(), user.getCity(), user.getAddress(), user.isVolunteer(),
        user.isMasonicRelationship(), user.isMember(), user.isSponsorship());
  }
}

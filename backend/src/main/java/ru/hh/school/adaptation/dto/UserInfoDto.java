package ru.hh.school.adaptation.dto;

import ru.hh.school.adaptation.entities.User;

public class UserInfoDto {
  public String firstName;

  public String lastName;

  public String middleName;

  public String email;

  public String inside;

  public UserInfoDto(User user) {
    firstName = user.getSelf().getFirstName();
    lastName = user.getSelf().getLastName();
    middleName = user.getSelf().getMiddleName();
    email = user.getSelf().getEmail();
    inside = user.getSelf().getInside();
  }
}

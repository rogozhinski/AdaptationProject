package ru.hh.school.adaptation.services;

import org.springframework.transaction.annotation.Transactional;
import ru.hh.school.adaptation.dao.EmployeeDao;
import ru.hh.school.adaptation.dao.UserDao;
import ru.hh.school.adaptation.dto.EmployeeDto;
import ru.hh.school.adaptation.entities.Employee;
import ru.hh.school.adaptation.entities.User;
import ru.hh.school.adaptation.exceptions.EntityDoesNotExistException;

import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class EmployeeService {

  private EmployeeDao employeeDao;
  private UserDao userDao;

  public EmployeeService(EmployeeDao employeeDao, UserDao userDao){
    this.employeeDao = employeeDao;
    this.userDao = userDao;
  }

  @Transactional
  public List<EmployeeDto> getAllEmployees() {
    List<Employee> employeeList = employeeDao.getAllRecords();
    return employeeList.stream().map(EmployeeDto::new).collect(Collectors.toList());
  }

  @Transactional
  public EmployeeDto getEmployee(Integer id) {
    Employee employee = employeeDao.getRecordById(id);
    if (employee == null) {
      throw new EntityDoesNotExistException(String.format("Employee with id = %d does not exist", id));
    }
    return new EmployeeDto(employee);
  }

  @Transactional
  public void saveEmployee(EmployeeDto employeeDto) {
    Employee employee = new Employee();
    employee.setFirstName(employeeDto.firstName);
    employee.setLastName(employeeDto.lastName);
    employee.setMiddleName(employeeDto.middleName);
    employee.setEmail(employeeDto.email);
    employee.setGender(employeeDto.gender);
    employee.setPosition(employeeDto.position);
    employee.setEmploymentTimestamp(employeeDto.employmentTimestamp);
    employee.setMobilePhone(employeeDto.mobilePhone);
    employee.setInternalPhone(employeeDto.internalPhone);

    User mentor = userDao.getRecordById(employeeDto.mentorId);
    if (mentor == null) {
      throw new EntityDoesNotExistException(
          String.format("Mentor with id = %d does not exist", employeeDto.mentorId)
      );
    }
    employee.setMentor(mentor);

    User chief = userDao.getRecordById(employeeDto.chiefId);
    if (chief == null) {
      throw new EntityDoesNotExistException(
          String.format("Chief with id = %d does not exist", employeeDto.chiefId)
      );
    }
    employee.setChief(chief);

    employeeDao.save(employee);
  }

  @Transactional
  public void updateEmployee(EmployeeDto employeeDto) {
    Employee employee = employeeDao.getRecordById(employeeDto.id);
    employee.setFirstName(employeeDto.firstName);
    employee.setLastName(employeeDto.lastName);
    employee.setMiddleName(employeeDto.middleName);
    employee.setEmail(employeeDto.email);
    employee.setGender(employeeDto.gender);
    employee.setPosition(employeeDto.position);
    employee.setEmploymentTimestamp(employeeDto.employmentTimestamp);
    employee.setMobilePhone(employeeDto.mobilePhone);
    employee.setInternalPhone(employeeDto.internalPhone);

    User mentor = userDao.getRecordById(employeeDto.mentorId);
    if (mentor == null) {
      throw new EntityDoesNotExistException(
          String.format("Mentor with id = %d does not exist", employeeDto.mentorId)
      );
    }
    employee.setMentor(mentor);

    employeeDao.update(employee);
  }

}

package ru.hh.school.adaptation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.hh.school.adaptation.AdaptationCommonConfig;
import ru.hh.school.adaptation.entities.Employee;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class EmployeeDto {

  public Integer id;

  public PersonalDto employee;

  public PersonalDto chief;

  public PersonalDto mentor;

  public PersonalDto hr;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AdaptationCommonConfig.JSON_DATE_FORMAT)
  public Date employmentDate;

  public List<WorkflowStepDto> workflow;

  public List<CommentDto> comments;

  public List<LogDto> logs;

  public Boolean currentUserIsHr;

  public EmployeeDto(){
  }

  public EmployeeDto(Employee employee, Integer currentUserId){
    id = employee.getId();
    this.employee = new PersonalDto(employee.getSelf());
    chief = new PersonalDto(employee.getChief());
    if (employee.getMentor() != null){
      mentor = new PersonalDto(employee.getMentor());
    }
    hr = new PersonalDto(employee.getHr());
    employmentDate = employee.getEmploymentDate();
    workflow = employee.getWorkflow().stream().map(WorkflowStepDto::new).collect(Collectors.toList());
    if (employee.getComments() != null) {
      comments = employee.getComments().stream().map(c -> new CommentDto(c, currentUserId)).collect(Collectors.toList());
    }
    if (employee.getLogs() != null) {
      logs = employee.getLogs().stream().map(LogDto::new).collect(Collectors.toList());
    }
    this.currentUserIsHr = currentUserId.equals(employee.getHr().getId());
  }

}

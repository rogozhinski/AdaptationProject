package ru.hh.school.adaptation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.hh.school.adaptation.AdaptationCommonConfig;
import ru.hh.school.adaptation.entities.Employee;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class EmployeeBriefDto {

  public Integer id;

  public PersonalDto employee;

  public PersonalDto chief;

  public PersonalDto mentor;

  public PersonalDto hr;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AdaptationCommonConfig.JSON_DATE_FORMAT)
  public Date employmentDate;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AdaptationCommonConfig.JSON_DATE_FORMAT)
  public Date interimDate;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AdaptationCommonConfig.JSON_DATE_FORMAT)
  public Date finalDate;

  public List<WorkflowStepBriefDto> workflow;

  public Boolean dismissed;

  public EmployeeBriefDto(Employee employee) {
    id = employee.getId();
    this.employee = new PersonalDto(employee.getSelf());
    chief = new PersonalDto(employee.getChief());
    if (employee.getMentor() != null) {
      mentor = new PersonalDto(employee.getMentor());
    }
    hr = new PersonalDto(employee.getHr());
    employmentDate = employee.getEmploymentDate();
    interimDate = employee.getInterimDate();
    finalDate = employee.getFinalDate();
    workflow = employee.getWorkflow().stream().map(WorkflowStepBriefDto::new).collect(Collectors.toList());
    dismissed = employee.getDismissed();
  }
}

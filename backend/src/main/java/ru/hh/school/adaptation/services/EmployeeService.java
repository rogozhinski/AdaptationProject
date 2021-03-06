package ru.hh.school.adaptation.services;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import ru.hh.nab.core.util.FileSettings;
import ru.hh.school.adaptation.dao.EmployeeDao;
import ru.hh.school.adaptation.dao.ScheduledMailDao;
import ru.hh.school.adaptation.dao.UserDao;
import ru.hh.school.adaptation.dto.EmployeeBriefDto;
import ru.hh.school.adaptation.dto.EmployeeCreateDto;
import ru.hh.school.adaptation.dto.EmployeeDto;
import ru.hh.school.adaptation.dto.EmployeeUpdateDto;
import ru.hh.school.adaptation.entities.Comment;
import ru.hh.school.adaptation.entities.Employee;
import ru.hh.school.adaptation.entities.Log;
import ru.hh.school.adaptation.entities.PersonalInfo;
import ru.hh.school.adaptation.entities.ScheduledMail;
import ru.hh.school.adaptation.entities.TaskForm;
import ru.hh.school.adaptation.entities.User;
import ru.hh.school.adaptation.exceptions.EntityNotFoundException;
import ru.hh.school.adaptation.exceptions.RequestValidationException;
import ru.hh.school.adaptation.misc.CommonUtils;
import ru.hh.school.adaptation.services.auth.AuthService;
import ru.hh.school.adaptation.misc.Named;
import ru.hh.school.adaptation.services.documents.ProbationResultDocumentGenerator;

import javax.inject.Singleton;
import java.util.Date;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.stream.Stream;

@Singleton
public class EmployeeService {

  private final String adaptationHost;

  private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

  private EmployeeDao employeeDao;
  private UserDao userDao;
  private PersonalInfoService personalInfoService;
  private TransitionService transitionService;
  private CommentService commentService;
  private ProbationResultDocumentGenerator probationResultDocumentGenerator;
  private AuthService authService;
  private MailService mailService;
  private MeetingService meetingService;
  private TaskService taskService;
  private ScheduledMailDao scheduledMailDao;

  public EmployeeService(FileSettings fileSettings, EmployeeDao employeeDao, UserDao userDao, PersonalInfoService personalInfoService,
                         TransitionService transitionService, ProbationResultDocumentGenerator probationResultDocumentGenerator,
                         CommentService commentService, AuthService authService, MailService mailService, MeetingService meetingService,
                         TaskService taskService, ScheduledMailDao scheduledMailDao) {
    this.employeeDao = employeeDao;
    this.authService = authService;
    this.userDao = userDao;
    this.personalInfoService = personalInfoService;
    this.transitionService = transitionService;
    this.commentService = commentService;
    this.probationResultDocumentGenerator = probationResultDocumentGenerator;
    this.mailService = mailService;
    this.meetingService = meetingService;
    this.taskService = taskService;
    this.scheduledMailDao = scheduledMailDao;

    adaptationHost = fileSettings.getProperties().getProperty("adaptation.host");
  }

  @Transactional(readOnly = true)
  public List<Employee> getAllEmployees() {
    return employeeDao.getAllRecords();
  }

  @Transactional(readOnly = true)
  public Employee getEmployee(Integer id) {
    Employee employee = employeeDao.getRecordById(id);
    if (employee == null) {
      throw new EntityNotFoundException(String.format("Employee with id = %d does not exist", id));
    }
    return employee;
  }

  @Transactional(readOnly = true)
  public List<EmployeeBriefDto> getBriefEmployeesList() {
    return getAllEmployees().stream().map(EmployeeBriefDto::new).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public EmployeeDto getEmployeeDto(Integer id) {
    Employee employee = getEmployee(id);
    return new EmployeeDto(employee, authService.getCurrentUserId());
  }

  @Transactional
  public EmployeeDto createEmployee(EmployeeCreateDto employeeCreateDto){
    Employee employee = new Employee();
    employee.setSelf(personalInfoService.createPersonalInfo(employeeCreateDto.self));
    employee.setChief(personalInfoService.getPersonalInfoById(employeeCreateDto.chiefId)
        .orElseThrow(() -> new EntityNotFoundException(String.format("PersonalInfo with id = %d does not exist", employeeCreateDto.chiefId))));
    if (employeeCreateDto.mentorId != null){
      employee.setMentor(personalInfoService.getPersonalInfoById(employeeCreateDto.mentorId)
          .orElseThrow(() -> new EntityNotFoundException(String.format("PersonalInfo with id = %d does not exist", employeeCreateDto.chiefId))));
    }
    employee.setHr(userDao.getRecordById(employeeCreateDto.hrId));
    employee.setPosition(employeeCreateDto.position);
    employee.setGender(employeeCreateDto.gender);
    employee.setEmploymentDate(employeeCreateDto.employmentDate);
    employee.setInterimDate(employeeCreateDto.interimDate);
    employee.setFinalDate(employeeCreateDto.finalDate);
    employeeDao.save(employee);

    employee.setComments(null);
    employee.setWorkflow(transitionService.createTransitionsForNewEmployee(employee));

    return new EmployeeDto(employee, authService.getCurrentUserId());
  }

  @Transactional
  public EmployeeDto updateEmployee(EmployeeUpdateDto employeeUpdateDto){
    if(employeeUpdateDto.id == null){
      throw new RequestValidationException("Id in update request can't be null");
    } else {
      Employee employee = employeeDao.getRecordById(employeeUpdateDto.id);
      User hr = userDao.getRecordById(employeeUpdateDto.hrId);
      PersonalInfo mentor = null;
      if (employeeUpdateDto.mentorId != null){
        mentor = personalInfoService.getPersonalInfoById(employeeUpdateDto.mentorId)
            .orElseThrow(() -> new EntityNotFoundException(String.format("PersonalInfo with id = %d does not exist", employeeUpdateDto.chiefId)));
      }
      PersonalInfo chief = personalInfoService.getPersonalInfoById(employeeUpdateDto.chiefId)
          .orElseThrow(() -> new EntityNotFoundException(String.format("PersonalInfo with id = %d does not exist", employeeUpdateDto.chiefId)));
      PersonalInfo selfInfo = employee.getSelf();

      logEmployeeUpdate(employee, employeeUpdateDto, hr, mentor, chief);
      personalInfoService.logPersonalInfoUpdate(selfInfo, employeeUpdateDto.self, employee);

      employee.setGender(employeeUpdateDto.gender);
      employee.setEmploymentDate(employeeUpdateDto.employmentDate);
      employee.setInterimDate(employeeUpdateDto.interimDate);
      employee.setFinalDate(employeeUpdateDto.finalDate);

      ScheduledMail scheduledMail = scheduledMailDao.getProbationScheduledMailByEmployeeId(employee.getId());
      scheduledMail.setTriggerDate(employee.getFinalDate());
      scheduledMailDao.update(scheduledMail);

      if (employee.getHr() != hr) {
        notifyNewHr(hr, employee);
      }
      employee.setHr(hr);
      employee.setPosition(employeeUpdateDto.position);
      employee.setMentor(mentor);
      employee.setChief(chief);

      personalInfoService.updatePersonalInfo(selfInfo, employeeUpdateDto.self);
      employeeDao.update(employee);

      return new EmployeeDto(employee, authService.getCurrentUserId());
    }
  }

  private void logEmployeeUpdate(Employee fromEmployee, EmployeeUpdateDto toEmployeeUpdateDto, User hr, PersonalInfo mentor, PersonalInfo chief) {
    String user = authService.getUser().map(u -> u.getSelf().getLastName() + " " + u.getSelf().getFirstName()).orElse("Anonymous");
    Log log = new Log();
    log.setEmployee(fromEmployee);
    log.setAuthor(user);
    log.setEventDate(new Date());

    if (fromEmployee.getEmploymentDate() != toEmployeeUpdateDto.employmentDate) {
      log.setMessage("Дата выхода на работу была изменена с " +
              fromEmployee.getEmploymentDate() +
              " на " +
              toEmployeeUpdateDto.employmentDate);
      commentService.createLog(log);
    }
    if (fromEmployee.getInterimDate() != toEmployeeUpdateDto.interimDate) {
      log.setMessage("Промежуточная дата ИС была изменена с " +
          fromEmployee.getInterimDate() +
          " на " +
          toEmployeeUpdateDto.interimDate);
      commentService.createLog(log);
    }
    if (fromEmployee.getFinalDate() != toEmployeeUpdateDto.finalDate) {
      log.setMessage("Дата окончания ИС была изменена с " +
          fromEmployee.getFinalDate() +
          " на " +
          toEmployeeUpdateDto.finalDate);
      commentService.createLog(log);
    }
    if (fromEmployee.getGender() != toEmployeeUpdateDto.gender) {
      log.setMessage("Пол был изменен с " +
              fromEmployee.getGender() +
              " на " +
              toEmployeeUpdateDto.gender);
      commentService.createLog(log);
    }
    if (fromEmployee.getHr() != hr) {
      log.setMessage("Сопровождающий hr был изменен с " +
              fromEmployee.getHr().getSelf().getFirstName() + " " + fromEmployee.getHr().getSelf().getLastName() +
              " на " +
              hr.getSelf().getFirstName() + " " + hr.getSelf().getLastName());
      commentService.createLog(log);
    }
    if (!fromEmployee.getPosition().equals(toEmployeeUpdateDto.position)) {
      log.setMessage("Позиция была изменена с " +
              fromEmployee.getPosition() +
              " на " +
              toEmployeeUpdateDto.position);
      commentService.createLog(log);
    }
    if (fromEmployee.getMentor() != mentor) {
      if (fromEmployee.getMentor() != null) {
        log.setMessage("Куратор был изменен с " +
            fromEmployee.getMentor().getFirstName() + " " + fromEmployee.getMentor().getLastName() +
            " на " +
            mentor.getFirstName() + " " + mentor.getLastName());
        commentService.createLog(log);
      } else {
        log.setMessage("Назначен куратор: " + mentor.getFirstName() + " " + mentor.getLastName());
        commentService.createLog(log);
      }
    }
    if (fromEmployee.getChief() != chief) {
      log.setMessage("Руководитель был изменен с " +
              fromEmployee.getChief().getFirstName() + " " + fromEmployee.getChief().getLastName() +
              " на " +
              chief.getFirstName() + " " + chief.getLastName());
      commentService.createLog(log);
    }
  }

  private void notifyNewHr(User hr, Employee employee) {
    String hrEmail = hr.getSelf().getEmail();

    mailService.sendMail(
        hrEmail,
        new StringJoiner(" ")
            .add("Привет. Ты был назначен на сопровождение нового сотрудника.")
            .add(String.format("%s %s", employee.getSelf().getFirstName(), employee.getSelf().getLastName()))
            .add("рассчитывает на твою поддержку в прохождении испытательного срока.")
            .add("Более подробную информацию ты можешь получить по ссылке")
            .add(String.format("https://%s/employee/%d", adaptationHost, employee.getId()))
            .toString(),
        "Сопровождение нового сотрудника"
    );

    Date now = new Date();

    String[] attendees = Stream.of(
        hrEmail,
        employee.getSelf().getEmail(),
        employee.getChief().getEmail(),
        Optional.ofNullable(employee.getMentor()).orElse(new PersonalInfo()).getEmail()
    ).filter((str) -> str != null).toArray(String[]::new);

    if (employee.getEmploymentDate().after(now)) {
      meetingService.sendMeeting("Welcome встреча", attendees, employee, employee.getEmploymentDate());
    }
    if (employee.getInterimDate().after(now)) {
      meetingService.sendMeeting("Промежуточная встреча", attendees, employee, employee.getInterimDate());
    }
    if (employee.getFinalDate().after(now)) {
      meetingService.sendMeeting("Итоговая встреча", attendees, employee, employee.getFinalDate());
    }
  }

  @Transactional
  public Response generateProbationResultDoc(Integer employeeId, String userAgent) {
    try {
      Named<byte[]> doc = probationResultDocumentGenerator.generateDoc(employeeDao.getRecordById(employeeId));

      return Response.ok(doc.get()).header(
          "Content-Disposition", String.format("attachment; filename=\"%s.docx\"",
            CommonUtils.getContentDispositionFilename(userAgent, doc.name())
          )
        ).build();
    } catch (InvalidFormatException | IOException | XmlException | NullPointerException e) {
      logger.error("Bad document for employer id {} ", employeeId);
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }

  @Transactional
  public void dismissEmployee(Integer id, String dismissComment) {
    String user = authService.getUser().map(u -> u.getSelf().getLastName() + " " + u.getSelf().getFirstName()).orElse("Anonymous");
    Employee employee = employeeDao.getRecordById(id);
    employee.setDismissed(true);
    employeeDao.update(employee);

    Log log = new Log();
    log.setEmployee(employee);
    log.setAuthor(user);
    log.setEventDate(new Date());
    log.setMessage("Сотрудник был уволен");
    commentService.createLog(log);

    if (dismissComment != null && !dismissComment.equals("")) {
      Comment comment = new Comment();
      comment.setEmployee(employee);
      comment.setAuthor(authService.getUser().get());
      comment.setMessage(dismissComment);
      commentService.createComment(comment);
    }
  }

  @Transactional
  public void undismissEmployee(Integer id, String dismissComment) {
    String user = authService.getUser().map(u -> u.getSelf().getLastName() + " " + u.getSelf().getFirstName()).orElse("Anonymous");
    Employee employee = employeeDao.getRecordById(id);
    employee.setDismissed(false);
    employeeDao.update(employee);

    Log log = new Log();
    log.setEmployee(employee);
    log.setAuthor(user);
    log.setEventDate(new Date());
    log.setMessage("Сотрудник был восстановлен");
    commentService.createLog(log);

    if (dismissComment != null && !dismissComment.equals("")) {
      Comment comment = new Comment();
      comment.setEmployee(employee);
      comment.setAuthor(authService.getUser().get());
      comment.setMessage(dismissComment);
      commentService.createComment(comment);
    }
  }

  public boolean isValidEmployeeCreateDto(EmployeeCreateDto employeeCreateDto) {
    return personalInfoService.isValidPersonalDto(employeeCreateDto.self) &&
        employeeCreateDto.chiefId != null &&
        employeeCreateDto.hrId != null &&
        StringUtils.isNotBlank(employeeCreateDto.position) &&
        employeeCreateDto.gender != null &&
        employeeCreateDto.employmentDate != null &&
        employeeCreateDto.interimDate != null &&
        employeeCreateDto.finalDate != null;
  }

  public boolean isValidEmployeeUpdateDto(EmployeeUpdateDto employeeUpdateDto) {
    return personalInfoService.isValidPersonalDto(employeeUpdateDto.self) &&
        employeeUpdateDto.chiefId != null &&
        StringUtils.isNotBlank(employeeUpdateDto.position) &&
        employeeUpdateDto.gender != null &&
        employeeUpdateDto.employmentDate != null &&
        employeeUpdateDto.interimDate != null &&
        employeeUpdateDto.finalDate != null &&
        employeeUpdateDto.id != null &&
        employeeUpdateDto.hrId != null;
  }

  @Transactional
  public void resendChiefTasks(Integer employeeId) {
    Employee employee = employeeDao.getEmployeeWithTaskForm(employeeId);
    if (employee == null) {
      throw new EntityNotFoundException(String.format("Employee with id = %d does not exist", employeeId));
    }
    if (employee.getTaskForm() == null) {
      TaskForm taskForm = taskService.createTaskForm(employee);
      employee.setTaskForm(taskForm);
    }

    Map<String, String> params = new HashMap<>();
    params.put("{{userName}}", employee.getSelf().getFirstName() + " " + employee.getSelf().getLastName());
    params.put("{{url}}", String.format("https://" + adaptationHost + "/add_tasks/%s", employee.getTaskForm().getKey()));
    mailService.sendMail(employee.getChief().getEmail(), "chief_missions.html", "Задачи на испытательный срок", params);
  }

}

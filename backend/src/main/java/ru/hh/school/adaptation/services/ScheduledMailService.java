package ru.hh.school.adaptation.services;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import ru.hh.nab.core.util.FileSettings;
import ru.hh.school.adaptation.dao.ScheduledMailDao;
import ru.hh.school.adaptation.entities.Employee;
import ru.hh.school.adaptation.entities.Gender;
import ru.hh.school.adaptation.entities.Log;
import ru.hh.school.adaptation.entities.ScheduledMail;
import ru.hh.school.adaptation.entities.TaskForm;
import ru.hh.school.adaptation.misc.CommonUtils;

@Singleton
public class ScheduledMailService {

  private final String adaptHost;
  private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

  private ScheduledMailDao scheduledMailDao;
  private EmployeeService employeeService;
  private MailService mailService;
  private CommentService commentService;
  private TaskService taskService;

  public ScheduledMailService(FileSettings fileSettings, ScheduledMailDao scheduledMailDao, @Lazy EmployeeService employeeService,
                              MailService mailService, CommentService commentService, TaskService taskService) {
    this.scheduledMailDao = scheduledMailDao;
    this.employeeService = employeeService;
    this.mailService = mailService;
    this.commentService = commentService;
    this.taskService = taskService;

    adaptHost = "https://" + fileSettings.getProperties().getProperty("adaptation.host");
  }

  @Transactional
  public void scheduleAllMailFromDb() {
    scheduledMailDao.getAll().forEach(this::scheduleNewMailLogic);
  }

  @Transactional
  public void scheduleNewMail(ScheduledMail scheduledMail) {
    scheduledMailDao.save(scheduledMail);
    scheduleNewMailLogic(scheduledMail);
  }

  private void scheduleNewMailLogic(ScheduledMail scheduledMail) {
    long delay = 1;
    if (scheduledMail.getTriggerDate().after(new Date())) {
      delay = (scheduledMail.getTriggerDate().getTime() - new Date().getTime())/1000;
    }

    scheduledExecutorService.schedule(() -> {
      switch (scheduledMail.getType()) {
        case WELCOME:
          sendWelcomeMail(scheduledMail);
          break;
        case CHIEF_TASK:
          sendTaskMail(scheduledMail);
          break;
        case PROBATION_RESULT:
          sendProbationResultMail(scheduledMail);
          break;
        case CUSTOM:
          sendCustomMail(scheduledMail);
          break;
      }
    }, delay, TimeUnit.SECONDS);
  }

  private void sendWelcomeMail(ScheduledMail scheduledMail) {
    Employee employee = employeeService.getEmployee(scheduledMail.getEmployeeId());

    Map<String, String> params = new HashMap<>();
    params.put("{{userName}}", employee.getSelf().getFirstName());
    params.put("{{gender_provel}}", employee.getGender() == Gender.MALE ? "провел" : "провела");
    params.put("{{gender_samomu}}", employee.getGender() == Gender.MALE ? "самому" : "самой");
    params.put("{{gender_osvoilsya}}", employee.getGender() == Gender.MALE ? "освоился" : "освоилась");
    params.put("{{gender_smog}}", employee.getGender() == Gender.MALE ? "смог" : "смогла");
    mailService.sendMail(employee.getSelf().getEmail(), "welcome_2.html", "Приветственное письмо", params);

    Log log = new Log();
    log.setEmployee(employee);
    log.setAuthor("Adaptation");
    log.setMessage("Сотруднику отправлено welcome-письмо");
    log.setEventDate(new Date());
    commentService.createLog(log);

    scheduledMailDao.delete(scheduledMail);
  }

  private void sendTaskMail(ScheduledMail scheduledMail) {
    Employee employee = employeeService.getEmployee(scheduledMail.getEmployeeId());
    TaskForm taskForm = employee.getTaskForm();

    if (taskForm == null) {
      taskForm = taskService.createTaskForm(employee);
    }

    if (taskForm.getTasks() != null && !taskForm.getTasks().isEmpty()) {
      scheduledMailDao.delete(scheduledMail);
      return;
    }

    String userName = CommonUtils.makeFioFromPersonalInfo(employee.getSelf());
    Map<String, String> params = new HashMap<>();
    params.put("{{userName}}", userName);
    params.put("{{url}}", String.format("%s/add_tasks/%s", adaptHost, taskForm.getKey()));
    mailService.sendMail(
        employee.getChief().getEmail(),
        "chief_missions.html",
        String.format("Задачи на испытательный срок (%s).", userName),
        params
    );
    scheduledMailDao.delete(scheduledMail);

    scheduledMail.setTriggerDate(DateUtils.addDays(scheduledMail.getTriggerDate(), 1));
    scheduleNewMail(scheduledMail);
  }

  private void sendProbationResultMail(ScheduledMail scheduledMail) {
    Employee employee = employeeService.getEmployee(scheduledMail.getEmployeeId());

    String userName = CommonUtils.makeFioFromPersonalInfo(employee.getSelf());
    String resultLink = String.format("%s/api/employee/%s/probation_result", adaptHost, employee.getId());

    Map<String, String> params = new HashMap<>();
    params.put("{{userName}}", userName);
    params.put("{{url}}", resultLink);

    mailService.sendMail(
        employee.getChief().getEmail(),
        "probation_result.html",
        String.format("Итоги испытательного срока (%s).", userName),
        params
    );
    scheduledMailDao.delete(scheduledMail);
  }

  private void sendCustomMail(ScheduledMail scheduledMail) {
    //TODO otpravlyat v scheduledMail.recepients
    //  scheduledMail.subject + scheduledMail.text
  }
}

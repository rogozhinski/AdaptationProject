package ru.hh.school.adaptation.services.workflow;

import org.springframework.context.annotation.Lazy;
import ru.hh.nab.core.util.FileSettings;
import ru.hh.school.adaptation.entities.Employee;
import ru.hh.school.adaptation.entities.Gender;
import ru.hh.school.adaptation.entities.Log;
import ru.hh.school.adaptation.entities.Questionnaire;
import ru.hh.school.adaptation.services.CommentService;
import ru.hh.school.adaptation.services.MailService;
import ru.hh.school.adaptation.services.QuestionnaireService;
import ru.hh.school.adaptation.services.TransitionService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class QuestionnaireStep {

  private String addTaskLink;
  private String dmsEmail1;
  private String dmsEmail2;

  private CommentService commentService;
  private MailService mailService;
  private TransitionService transitionService;
  private QuestionnaireService questionnaireService;

  public QuestionnaireStep(FileSettings fileSettings, MailService mailService, @Lazy TransitionService transitionService,
                           CommentService commentService, QuestionnaireService questionnaireService) {
    this.mailService = mailService;
    this.transitionService = transitionService;
    this.commentService = commentService;
    this.questionnaireService = questionnaireService;

    dmsEmail1 = fileSettings.getProperties().getProperty("adaptation.dms.email1");
    dmsEmail2 = fileSettings.getProperties().getProperty("adaptation.dms.email2");
    addTaskLink = "https://" + fileSettings.getProperties().getProperty("adaptation.host") + "/questionnaire/%s";
  }

  public void onQuestionnaire(Employee employee) {
    Questionnaire questionnaire = questionnaireService.createQuestionnaire(employee);
    Map<String, String> params = new HashMap<>();
    params.put("{{url}}", String.format(addTaskLink, questionnaire.getKey()));
    mailService.sendMail(employee.getSelf().getEmail(), "questionnaire.html", "Опросник для новичка", params);

    params = new HashMap<>();
    params.put("{{userName}}", employee.getSelf().getFirstName() + " " + employee.getSelf().getLastName());
    params.put("{{gender}}", employee.getGender()==Gender.MALE?"прошёл":"прошла");
    mailService.sendMail(dmsEmail1, "create_dms.html", "Оформление ДМС", params);
    mailService.sendMail(dmsEmail2, "create_dms.html", "Оформление ДМС", params);

    Log log = new Log();
    log.setEmployee(employee);
    log.setAuthor("Adaptation");
    log.setMessage("Сотруднику отправлен опросник новичка.");
    log.setEventDate(new Date());
    commentService.createLog(log);

    transitionService.setEmployeeNextTransition(employee);
  }

}

package ru.hh.school.adaptation.services.documents;

import java.text.SimpleDateFormat;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.xmlbeans.XmlException;
import ru.hh.school.adaptation.entities.Employee;
import ru.hh.school.adaptation.entities.Task;
import ru.hh.school.adaptation.entities.TaskForm;
import ru.hh.school.adaptation.misc.CommonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TaskDocumentGenerator extends DocumentGenerator {
  private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

  public TaskDocumentGenerator(String templateName) {
    super(templateName);
  }

  private void addTasksRows(TaskForm taskForm, XWPFTable table, int startFromId) throws IOException, XmlException {
    if (taskForm == null) {
      return;
    }

    List<Task> taskList = taskForm.getTasks();
    String[][] recs = new String[taskList.size()][4];
    for (int i = 0; i < taskList.size(); i++) {
      recs[i][0] = Integer.toString(i + 1);
      recs[i][1] = taskList.get(i).getText();
      recs[i][2] = null;

      if (taskList.get(i).getIsWeeks()) {
        if (taskList.get(i).getDeadline() != null) {
          recs[i][2] = taskList.get(i).getDeadline() + " нед.";
        }
      } else {
        if (taskList.get(i).getDeadline() != null) {
          recs[i][2] = "до " + taskList.get(i).getDeadline();
        }
      }

      recs[i][3] = taskList.get(i).getResources();
    }

    insertRowsInTable(table, startFromId, recs);
  }

  private Map<String, String> constructReplacements(Employee employee) {
    Map<String, String> replacements = new HashMap<>();
    String fio = CommonUtils.makeFioFromPersonalInfo(employee.getSelf());
    String chiefFio = CommonUtils.makeFioFromPersonalInfo(employee.getChief());
    String mentroFio = employee.getMentor() == null ? "" : CommonUtils.makeFioFromPersonalInfo(employee.getMentor());
    replacements.put("{{employee.fullName}}", fio);
    replacements.put("{{employee.employmentDate}}", simpleDateFormat.format(employee.getEmploymentDate()));
    replacements.put("{{employee.endDate}}", simpleDateFormat.format(employee.getFinalDate()));
    replacements.put("{{employee.position}}", employee.getPosition());
    replacements.put("{{employee.chief}}", chiefFio);
    replacements.put("{{employee.mentor}}", mentroFio);
    replacements.put("{{functional.tasks}}", "");

    return replacements;
  }

  public void fillDocWithData(Employee employee, XWPFDocument doc) throws XmlException, IOException {
    for (XWPFTable table : doc.getTables()) {
      for (int i = 0; i < table.getRows().size(); i++) {
        for (XWPFTableCell cell : table.getRow(i).getTableCells()) {
          if (cell.getText().contains("{{functional.tasks}}")) {
            addTasksRows(employee.getTaskForm(), table, i + 1);
          }
          replaceInParagraphs(constructReplacements(employee), cell.getParagraphs());
        }
      }
    }
  }

}

package ru.hh.school.adaptation.entities;

import java.util.Date;
import java.util.List;

import javax.persistence.*;

@Entity
@Table(name = "employee")
public class Employee {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false)
  private Integer id;

  @ManyToOne(cascade = {CascadeType.ALL})
  @JoinColumn(name = "self_id")
  private PersonalInfo self;

  @Column(name = "position", nullable = false)
  private String position;

  @Column(name = "mobile_phone")
  private Long mobilePhone;

  @Column(name = "internal_phone")
  private Integer internalPhone;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender")
  private Gender gender;

  @Column(name = "employment_timestamp")
  @Temporal(TemporalType.DATE)
  private Date employmentTimestamp;

  @ManyToOne(cascade = {CascadeType.ALL})
  @JoinColumn(name = "hr_id")
  private User hr;

  @ManyToOne(cascade = {CascadeType.ALL})
  @JoinColumn(name = "chief_id")
  private PersonalInfo chief;

  @ManyToOne(cascade = {CascadeType.ALL})
  @JoinColumn(name = "mentor_id")
  private PersonalInfo mentor;

  @OneToMany(mappedBy = "employee")
  @OrderBy("step_type")
  private List<Transition> workflow;

  public Integer getId() {
    return id;
  }

  public String getPosition() {
    return position;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  public Date getEmploymentTimestamp() {
    return employmentTimestamp;
  }

  public void setEmploymentTimestamp(Date employmentTimestamp) {
    this.employmentTimestamp = employmentTimestamp;
  }

  public Gender getGender() {
    return gender;
  }

  public void setGender(Gender gender) {
    this.gender = gender;
  }

  public User getHr() {
    return hr;
  }

  public void setHr(User hr) {
    this.hr = hr;
  }

  public Long getMobilePhone() {
    return mobilePhone;
  }

  public void setMobilePhone(Long mobilePhone) {
    this.mobilePhone = mobilePhone;
  }

  public Integer getInternalPhone() {
    return internalPhone;
  }

  public void setInternalPhone(Integer internalPhone) {
    this.internalPhone = internalPhone;
  }

  public PersonalInfo getChief() {
    return chief;
  }

  public void setChief(PersonalInfo chief) {
    this.chief = chief;
  }

  public PersonalInfo getMentor() {
    return mentor;
  }

  public void setMentor(PersonalInfo mentor) {
    this.mentor = mentor;
  }

  public PersonalInfo getSelf() {
    return self;
  }

  public void setSelf(PersonalInfo self) {
    this.self = self;
  }

  public List<Transition> getWorkflow() {
    return workflow;
  }

  public void setWorkflow(List<Transition> workflow) {
    this.workflow = workflow;
  }
}

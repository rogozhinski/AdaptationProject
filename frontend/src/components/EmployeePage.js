import React from 'react';
import ReactDOM from 'react-dom';
import axios from 'axios';
import FaCircle from 'react-icons/lib/fa/circle';
import FaAdjust from 'react-icons/lib/fa/adjust';
import FaExclamationCircle from 'react-icons/lib/fa/exclamation-circle';
import FaCheckCircle from 'react-icons/lib/fa/check-circle';
import EmployeeTasksModal from './tasks/EmployeeTasksModal';

import { Jumbotron, Alert } from 'reactstrap';
import { Container, Row, Col } from 'reactstrap';
import { ListGroup, ListGroupItem } from 'reactstrap';
import { Form, FormGroup, Input } from 'reactstrap';
import { Popover, PopoverHeader, PopoverBody } from 'reactstrap';
import { Button, Modal, ModalHeader, ModalBody, ModalFooter } from 'reactstrap';
import { TabContent, TabPane, Nav, NavItem, NavLink } from 'reactstrap';
import '!style-loader!css-loader!./app.css';
import classnames from 'classnames';

class EmployeePage extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      currentWorkflowType: '',
      alert: {
        status: false,
        message: '',
        color: '',
      },
      activeTab: '1',
      employeeId: this.props.match.params.id,
      data: {
        id: null,
        currentWorkflowStep: '',
        employee: {
          id: null,
          firstName: '',
          lastName: '',
          middleName: '',
          email: '',
          inside: '',
        },
        chief: {
          id: null,
          firstName: '',
          lastName: '',
          middleName: '',
          email: '',
          inside: '',
        },
        mentor: {
          id: null,
          firstName: '',
          lastName: '',
          middleName: '',
          email: '',
          inside: '',
        },
        hr: {
          id: null,
          firstName: '',
          lastName: '',
          middleName: '',
          email: '',
          inside: '',
        },
        employmentDate: null,
        workflow: [
          {
            id: null,
            type: '',
            status: '',
            deadlineDate: null,
            comment: null,
            overdue: false,
          },
        ],
        comments: [
          {
            id: null,
            employeeId: null,
            author: '',
            message: '',
          },
        ],
        logs: [
          {
            id: null,
            employeeId: null,
            author: '',
            message: '',
            link: '',
            eventDate: '',
          },
        ],
      },
      tasksModal: false,
    };

    this.toggleTasksModal = this.toggleTasksModal.bind(this);
    this.toggleAlert = this.toggleAlert.bind(this);
    this.commentRemove = this.commentRemove.bind(this);
    this.commentSubmit = this.commentSubmit.bind(this);
    this.toggleLogBox = this.toggleLogBox.bind(this);

    var nextStep = new NextStep();
  }

  componentDidMount() {
    const url = '/api/employee/' + this.state.employeeId;
    const self = this;
    axios
      .get(url)
      .then(function(response) {
        self.setState({
          data: response.data,
        });
      })
      .catch(function(error) {
        self.toggleAlert({
          status: true,
          message: 'Не удалось установить связь с сервером',
          color: 'danger',
        });
      });
  }

  nextStep(self) {
    const url = '/api/employee/' + self.state.employeeId + '/step/next';
    axios
      .put(url)
      .then(function(response) {
        for (var i = 0; i < self.state.data.workflow.length; i++) {
          if (self.state.data.workflow[i].status == 'CURRENT') {
            self.state.data.workflow[i].status = 'DONE';
            self.state.data.workflow[i].overdue = false;
            if (i < self.state.data.workflow.length - 1) {
              self.state.data.workflow[i + 1].status = 'CURRENT';
              self.state.data.workflow[i + 1].overdue = response.data.overdue;
            }
            break;
          }
        }
        self.forceUpdate();
      })
      .catch(function(error) {
        self.toggleAlert({
          status: true,
          message: 'Не удалось установить связь с сервером',
          color: 'danger',
        });
        //console.log(error);
      });
  }

  toggleTasksModal() {
    //console.log('toggle');
    this.setState({
      tasksModal: !this.state.tasksModal,
    });
  }

  timeLeft(emplDataString) {
    if (emplDataString == null) return;
    var result = 'До конца испытательного срока: ';
    var emplYear = parseInt(emplDataString.split('-')[0]);
    var emplMonth = parseInt(emplDataString.split('-')[1]);
    var emplDay = parseInt(emplDataString.split('-')[2]);

    var emplData = new Date(emplYear, emplMonth + 2, emplDay);
    var delta = emplData - new Date();
    var yearLeft = parseInt(delta / 1000 / 60 / 60 / 24 / 30.4 / 12);
    var monthLeft = parseInt(delta / 1000 / 60 / 60 / 24 / 30.4 - yearLeft * 12);
    var dayLeft = parseInt(delta / 1000 / 60 / 60 / 24 - monthLeft * 30.4 - yearLeft * 30.4 * 12);

    if (yearLeft > 0) {
      result += yearLeft + ' лет ';
    }
    if (monthLeft > 0) {
      result += monthLeft + ' мес. ';
    }
    if (dayLeft > 0) {
      result += dayLeft + ' дн.';
    }
    return result;
  }

  toggleAlert(alertObj) {
    this.setState({
      alert: alertObj,
    });
    setTimeout(() => {
      this.setState({
        alert: {
          status: false,
        },
      });
    }, 2200);
  }

  toggleLogBox(tab) {
    if (this.state.activeTab !== tab) {
      this.setState({
        activeTab: tab,
      });
    }
  }

  commentSubmit(author, message) {
    const url = '/api/comment/create';
    const self = this;

    axios
      .post(url, {
        employeeId: self.state.employeeId,
        author: author,
        message: message,
      })
      .then(function(response) {
        self.state.data.comments.push({
          id: response.data,
          author: author,
          message: message,
        });
        //console.log(self.state.data.comments);
        self.forceUpdate();
        //privet. kak tebe moi kostyl?
        setTimeout(() => {
          document.getElementById('commentBox').scrollTop += 1200;
        }, 0);
      })
      .catch(function(error) {
        self.toggleAlert({
          status: true,
          message: 'Не удалось установить связь с сервером',
          color: 'danger',
        });
        //console.log(error);
      });
  }

  commentRemove(commentId) {
    const url = '/api/comment/remove/' + commentId;
    const self = this;

    axios
      .delete(url)
      .then(function(response) {
        self.state.data.comments = self.state.data.comments.filter((x) => x.id != commentId);
        self.forceUpdate();
      })
      .catch(function(error) {
        self.toggleAlert({
          status: true,
          message: 'Не удалось установить связь с сервером',
          color: 'danger',
        });
        //console.log(error);
      });
  }

  dateFormat(date) {
    if (date == null) {
      return '';
    }
    return date.split('-')[2] + '.' + date.split('-')[1] + '.' + date.split('-')[0];
  }

  render() {
    const {
      firstName: employeeFirstName,
      middleName: employeeMiddleName,
      lastName: employeeLastName,
      email: employeeEmail,
    } = this.state.data.employee;
    const {
      firstName: chiefFirstName,
      middleName: chiefMiddleName,
      lastName: chiefLastName,
    } = this.state.data.chief;
    const mentorFirstName =
      this.state.data.mentor != null ? this.state.data.mentor.firstName : null;
    const mentorMiddleName =
      this.state.data.mentor != null ? this.state.data.mentor.middleName : null;
    const mentorLastName = this.state.data.mentor != null ? this.state.data.mentor.lastName : null;
    const {
      firstName: hrFirstName,
      middleName: hrMiddleName,
      lastName: hrLastName,
    } = this.state.data.hr;
    const employmentDate = this.dateFormat(this.state.data.employmentDate);
    const workflow = this.state.data.workflow;
    const timeLeft = this.timeLeft(this.state.data.employmentDate);

    return (
      <Container>
        <Row className="mb-4">
          <Col sm={{ size: 5, offset: 1 }}>
            <h3 className="mb-0 font-weight-bold">
              {`${employeeFirstName} ${employeeMiddleName} ${employeeLastName}`}
            </h3>
            <div className="mb-1 ml-2 text-info"> {employeeEmail} </div>
          </Col>
        </Row>

        <Row className="mb-2">
          <Col sm={{ size: 5, offset: 1 }}>
            <div className="ml-4">
              <p className="mb-2 text-muted">
                {`Начальник: ${chiefFirstName} ${
                  chiefMiddleName == null ? '' : chiefMiddleName
                } ${chiefLastName}`}
              </p>
              {this.state.data.mentor != null && (
                <p className="mb-2 text-muted">
                  {`Ментор: ${mentorFirstName} ${
                    mentorMiddleName == null ? '' : mentorMiddleName
                  } ${mentorLastName}`}
                </p>
              )}
              <p className="text-muted">
                {`HR: ${hrFirstName} ${hrMiddleName == null ? '' : hrMiddleName} ${hrLastName}`}
              </p>
            </div>
          </Col>
          <Col sm={{ size: 5 }} className="mt-0 ml-5">
            <div className="">
              <p className="mb-2">{`Дата выхода на работу: ${employmentDate}`}</p>
              <p className=""> {timeLeft} </p>
            </div>
          </Col>
        </Row>

        <Row>
          <Col sm={{ size: 5, offset: 1 }} className="mt-4">
            <Workflow data={workflow} parent={this} />
            <Row>
              <NextStep parent={this} />
              <Button outline color="info" className="mt-5 ml-3" onClick={this.toggleTasksModal}>
                Задачи на испытательный срок
              </Button>
              <EmployeeTasksModal
                employeeId={this.state.employeeId}
                isOpen={this.state.tasksModal}
                parentToggle={this.toggleTasksModal}
              />
            </Row>
          </Col>

          <Col sm={{ size: 5 }}>
            <Nav tabs>
              <NavItem
                className={classnames({
                  'cur-default': this.state.activeTab === '1',
                  'cur-pointer': this.state.activeTab !== '1',
                })}
              >
                <NavLink
                  className={classnames({ active: this.state.activeTab === '1' })}
                  onClick={() => {
                    this.toggleLogBox('1');
                  }}
                >
                  <h5>
                    <span className="text-muted">Комментарии</span>
                  </h5>
                </NavLink>
              </NavItem>
              <NavItem
                className={classnames({
                  'cur-default': this.state.activeTab === '2',
                  'cur-pointer': this.state.activeTab !== '2',
                })}
              >
                <NavLink
                  className={classnames({ active: this.state.activeTab === '2' })}
                  onClick={() => {
                    this.toggleLogBox('2');
                  }}
                >
                  <h5>
                    <span className="text-muted">История</span>
                  </h5>
                </NavLink>
              </NavItem>
            </Nav>

            <TabContent activeTab={this.state.activeTab}>
              <TabPane tabId="1">
                <Comments parent={this} />
              </TabPane>
              <TabPane tabId="2">
                <Logs parent={this} />
              </TabPane>
            </TabContent>
          </Col>
        </Row>

        <Row>
          <Col sm={{ size: 12, offset: 0 }} className="mt-0">
            <div>
              <CustomAlert self={this} />
            </div>
          </Col>
        </Row>
      </Container>
    );
  }
}

class NextStep extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      modal: false,
      commentValue: '',
    };

    this.nextStep = this.nextStep.bind(this);
    this.onCommentChange = this.onCommentChange.bind(this);
    this.toggleModal = this.toggleModal.bind(this);
    this.commentModalSubmit = this.commentModalSubmit.bind(this);
  }

  onCommentChange(e) {
    this.setState({
      commentValue: e.target.value,
    });
  }

  commentModalSubmit(e) {
    e.preventDefault();
    this.nextStep();
  }

  nextStep() {
    var parent = this.props.parent;

    if (this.state.commentValue != '') {
      if (this.getCurrentType(parent) == 'INTERIM_MEETING_RESULT') {
        this.props.parent.commentSubmit('Промежуточная встреча', this.state.commentValue);
      }
      if (this.getCurrentType(parent) == 'FINAL_MEETING_RESULT') {
        this.props.parent.commentSubmit('Итоговая встреча', this.state.commentValue);
      }
    }

    this.setState({
      commentValue: '',
    });

    this.toggleModal();
    parent.nextStep(parent);

    /*
    this.props.parent.toggleAlert({
      status: true,
      message: 'Результаты отправлены',
      color: 'success',
    });
    */
  }

  toggleModal() {
    this.setState({
      modal: !this.state.modal,
    });
  }

  getCurrentType(parent) {
    var result = parent.state.data.workflow.filter((x) => x.status == 'CURRENT');
    if (result.length == 0) {
      return 'NONE';
    }
    return result[0].type;
  }

  render() {
    var parent = this.props.parent;
    var currentWorkflowType = this.getCurrentType(parent);
    var isDisabled =
      currentWorkflowType == 'NONE' || currentWorkflowType == 'QUESTIONNAIRE' ? true : false;
    var isResultStep =
      currentWorkflowType == 'INTERIM_MEETING_RESULT' ||
      currentWorkflowType == 'FINAL_MEETING_RESULT'
        ? true
        : false;

    return (
      <div>
        <Button
          outline
          disabled={isDisabled}
          color="secondary"
          className="mt-5"
          onClick={this.toggleModal}
        >
          Перевести далее
        </Button>

        <Modal isOpen={this.state.modal} toggle={this.toggleModal} className={parent.className}>
          <ModalHeader toggle={this.toggleModal}>Перевести на следующий этап?</ModalHeader>
          {isResultStep && (
            <ModalBody>
              <Form onSubmit={(e) => this.commentModalSubmit(e)}>
                <FormGroup>
                  <Input
                    type="text"
                    name="text"
                    placeholder="Написать комментарий"
                    onChange={this.onCommentChange}
                    value={this.state.commentValue}
                  />
                </FormGroup>
              </Form>
            </ModalBody>
          )}
          <ModalFooter>
            <Button outline color="secondary" onClick={this.nextStep}>
              Перевести
            </Button>
            <Button outline color="danger" onClick={this.toggleModal}>
              Отмена
            </Button>
          </ModalFooter>
        </Modal>
      </div>
    );
  }
}

class Workflow extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <div>
        {this.props.data.map((workflowStageData) => (
          <WorkflowStage
            deadlineDate={workflowStageData.deadlineDate}
            status={workflowStageData.status}
            overdue={workflowStageData.overdue}
            type={workflowStageData.type}
            key={workflowStageData.id}
            id={workflowStageData.id}
            parent={this.props.parent}
          />
        ))}
      </div>
    );
  }
}

class WorkflowStage extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      popoverOpen: false,
    };

    this.toggle = this.toggle.bind(this);
    this.selectIcon = this.selectIcon.bind(this);
  }

  selectIcon(status, overdue) {
    switch (status) {
      case 'DONE':
        return <FaCheckCircle size={50} color="#70BD71" />;
      case 'CURRENT':
        return overdue ? (
          <FaExclamationCircle size={50} color="#DF6B62" />
        ) : (
          <FaAdjust size={50} color="#BDB370" />
        );
      default:
        return <FaCircle size={50} color="#C2C2C2" />;
    }
  }

  typeTranslate(type) {
    switch (type) {
      case 'ADD':
        return 'Добавлен в систему';
      case 'TASK_LIST':
        return 'Согласование задач';
      case 'WELCOME_MEETING':
        return 'Welcome-встреча';
      case 'INTERIM_MEETING':
        return 'Промежуточная встреча';
      case 'INTERIM_MEETING_RESULT':
        return 'Результаты промежуточной встречи';
      case 'FINAL_MEETING':
        return 'Итоговая встреча';
      case 'FINAL_MEETING_RESULT':
        return 'Результаты итоговой встречи';
      case 'QUESTIONNAIRE':
        return 'Опросник новичка';
    }
  }

  getDescription(type) {
    switch (type) {
      case 'ADD':
        return 'Начальнику был выслан бланк для заполнения задач на испытательный период.';
      case 'TASK_LIST':
        return 'Необходимо получить задачи от руководителя, распечатать и подписать их.';
      case 'WELCOME_MEETING':
        return 'Ожидается день выхода на работу. Не забудь отправить welcome-письмо и провести welcome-встречу.';
      case 'INTERIM_MEETING':
        return 'Необходимо забронировать перговорную конату и провести промежуточную встречу.';
      case 'INTERIM_MEETING_RESULT':
        return 'Необходимо заполнить результаты промежуточной встречи.';
      case 'FINAL_MEETING':
        return 'Необходимо забронировать перговорную конату и провести итоговую встречу.';
      case 'FINAL_MEETING_RESULT':
        return 'Необходимо заполнить результаты итоговой встречи.';
      case 'QUESTIONNAIRE':
        return 'Сотрудник получил опросник для новичка и работает над его заполнением.';
    }
  }

  toggle() {
    this.setState({
      popoverOpen: !this.state.popoverOpen,
    });
  }

  render() {
    const { deadlineDate, status, overdue, type } = this.props;

    return (
      <div id={'Popover-' + this.props.id} className="workflow-leonid" onClick={this.toggle}>
        {this.selectIcon(status, overdue)}
        <span className="ml-3">{this.typeTranslate(type)}</span>
        <Popover
          placement="bottom"
          isOpen={this.state.popoverOpen}
          target={'Popover-' + this.props.id}
          toggle={this.toggle}
        >
          <PopoverHeader>{this.typeTranslate(type)}</PopoverHeader>
          <PopoverBody>{this.getDescription(type)}</PopoverBody>
        </Popover>
      </div>
    );
  }
}

class Comments extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      commentValue: '',
    };

    this.commentBoxSubmit = this.commentBoxSubmit.bind(this);
    this.onCommentChange = this.onCommentChange.bind(this);
  }

  commentBoxSubmit(e) {
    e.preventDefault();
    if (this.state.commentValue == '') return;
    this.props.parent.commentSubmit('Вы', this.state.commentValue);
    this.setState({
      commentValue: '',
    });
  }

  onCommentChange(e) {
    this.setState({
      commentValue: e.target.value,
    });
  }

  render() {
    var parent = this.props.parent;

    return (
      <div>
        <ListGroup id="commentBox" className="mb-0 info-box">
          {parent.state.data.comments.map((commentsData) => (
            <CommentItem
              message={commentsData.message}
              author={commentsData.author}
              key={commentsData.id}
              id={commentsData.id}
              parent={this.props.parent}
            />
          ))}
        </ListGroup>

        <Form onSubmit={(e) => this.commentBoxSubmit(e)}>
          <FormGroup>
            <Input
              rows="1"
              type="text"
              name="text"
              placeholder="Написать комментарий"
              onChange={this.onCommentChange}
              value={this.state.commentValue}
            />
          </FormGroup>
        </Form>
      </div>
    );
  }
}

class CommentItem extends React.Component {
  constructor(props) {
    super(props);

    this.mouseIn = this.mouseIn.bind(this);
    this.mouseOut = this.mouseOut.bind(this);
    this.state = {
      isHovering: false,
    };
  }

  mouseIn() {
    this.setState({
      isHovering: true,
    });
  }

  mouseOut() {
    this.setState({
      isHovering: false,
    });
  }

  nameWithDots(name) {
    return name + ':';
  }

  render() {
    const { message, author, id } = this.props;

    return (
      <ListGroupItem className="" onMouseEnter={this.mouseIn} onMouseLeave={this.mouseOut}>
        <Row>
          <Col sm={{ size: 8, offset: 0 }}>
            <h6 className="my-0 mb-2">{this.nameWithDots(author)}</h6>
          </Col>
          <Col>
            {this.state.isHovering && (
              <small
                className="text-muted comment-delete"
                onClick={() => this.props.parent.commentRemove(id)}
              >
                ✖
              </small>
            )}
          </Col>
        </Row>
        <Row>
          <span className="ml-2">{message}</span>
        </Row>
      </ListGroupItem>
    );
  }
}

class Logs extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    var parent = this.props.parent;

    return (
      <ListGroup id="logBox" className="mb-0 info-box">
        {parent.state.data.logs.map((logsData) => (
          <LogItem
            message={logsData.message}
            author={logsData.author}
            eventDate={logsData.eventDate}
            link={logsData.link}
            key={logsData.id}
            id={logsData.id}
          />
        ))}
      </ListGroup>
    );
  }
}

class LogItem extends React.Component {
  constructor(props) {
    super(props);
  }

  nameWithDots(name) {
    return name + ':';
  }

  dateFormat(date) {
    if (date == null) {
      return '';
    }
    return date.split('-')[2] + '.' + date.split('-')[1] + '.' + date.split('-')[0];
  }

  render() {
    const { message, link, author, eventDate, id } = this.props;

    return (
      <ListGroupItem className="" onMouseEnter={this.mouseIn} onMouseLeave={this.mouseOut}>
        <Row>
          <Col sm={{ size: 8, offset: 0 }}>
            <h6 className="my-0 mb-2">{this.nameWithDots(author)}</h6>
          </Col>
          <Col>
            <span className="text-muted">{this.dateFormat(eventDate)}</span>
          </Col>
        </Row>
        <Row>
          <span className="ml-2">
            {message}&nbsp;
            {link != null && <a href={link}>ссылка</a>}
          </span>
        </Row>
      </ListGroupItem>
    );
  }
}

class CustomAlert extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    var parent = this.props.self;
    return (
      <Alert color={parent.state.alert.color} isOpen={parent.state.alert.status}>
        {parent.state.alert.message}
      </Alert>
    );
  }
}

export default EmployeePage;

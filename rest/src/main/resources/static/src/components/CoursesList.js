import '../styles/style.css';
import React from 'react';
import ReactDOM from 'react-dom';
import {
    Badge,
    Button,
    ControlLabel,
    DropdownButton,
    FormControl,
    FormGroup,
    HelpBlock,
    Label,
    MenuItem,
    Modal,
    Panel,
    Tab,
    Table,
    Tabs
} from 'react-bootstrap';
import Immutable from 'immutable';
import {credentials} from '../scripts';
import {Api} from '../Api';
import {Notification} from './Notification';

export class CoursesList extends React.Component {

    constructor(props) {
        super(props);

        this.updateCoursesList = this.updateCoursesList.bind(this);
        this.selectCourse = this.selectCourse.bind(this);
        this.deleteCourse = this.deleteCourse.bind(this);

        this.state = {
            account: props.account,
            courses: [],
            selectedCourse: null
        };

        this.updateCoursesList();
    }

    render() {
        if (this.state.selectedCourse == null) {
            const courses = Immutable.List(this.state.courses)
                .map(value => <CourseItem data={value} onSelect={this.selectCourse}/>);

            return (
                <article className="courses-list">
                    <CourseCreation onCourseCreation={this.updateCoursesList}/>
                    {courses.size > 0 ? courses : <p>There are no courses yet.</p>}
                </article>
            );
        } else {
            return (
                <Course data={this.state.selectedCourse}
                        onUpdate={this.updateCoursesList}
                        onDelete={this.deleteCourse}/>
            )
        }
    }

    updateCoursesList() {
        Api.retrieveCourses(
            credentials(),
            this.props.account.nickname,
            courses => {
                const selectedCourse =
                    this.state.selectedCourse != null
                        ? Immutable.List(courses)
                            .find(course => course.name === this.state.selectedCourse.name)
                        : null;

                this.setState({courses, selectedCourse});
            },
            response => ReactDOM.render(
                <Notification message={`Courses retrieving failed.<br/>${response}`}/>,
                document.getElementById('notifications')
            )
        );
    }

    deleteCourse(course) {
        this.setState({selectedCourse: null});
        this.updateCoursesList();
    }

    selectCourse(selectedCourse) {
        this.setState({selectedCourse});
    }

}

class CourseItem extends React.Component {

    constructor(props) {
        super(props);

        this.state = props.data;
    }

    render() {
        return (
            <section className="course-item" onClick={() => this.props.onSelect(this.state)}>
                <Panel>
                    <Panel.Heading>
                        {this.state.name}
                        <Badge>{this.state.tasks.length} tasks</Badge>
                        <Badge>{this.state.students.length} students</Badge>
                    </Panel.Heading>
                    <Panel.Body>
                        <p>Some ambiguous course description.</p>
                        <CourseLabels course={this.state}/>
                    </Panel.Body>
                </Panel>
            </section>
        );
    }
}

class CourseCreation extends React.Component {

    constructor(props) {
        super(props);

        this.getLanguages = this.getLanguages.bind(this);
        this.getTestLanguages = this.getTestLanguages.bind(this);
        this.getTestingFrameworks = this.getTestingFrameworks.bind(this);
        this.handleShow = this.handleShow.bind(this);
        this.handleClose = this.handleClose.bind(this);
        this.handleCourseNameChange = this.handleCourseNameChange.bind(this);
        this.handleLanguageChange = this.handleLanguageChange.bind(this);
        this.handleTestingLanguageChange = this.handleTestingLanguageChange.bind(this);
        this.handleTestingFrameworkChange = this.handleTestingFrameworkChange.bind(this);
        this.handleNumberOfTasksChange = this.handleNumberOfTasksChange.bind(this);
        this.createCourse = this.createCourse.bind(this);

        this.state = {
            show: false,
            courseName: null,
            language: null,
            testLanguage: null,
            testingFramework: null,
            numberOfTasks: null,
            flaxoLanguages: []
        };

        Api.retrieveLanguages(
            languages => {
                this.setState(
                    {
                        flaxoLanguages: languages,
                        language: languages[0].name,
                        testLanguage: languages[0].compatibleTestingLanguages[0],
                        testingFramework: languages[0].compatibleTestingFrameworks[0]
                    }
                )
            }
        );
    }

    render() {
        return (
            <div>
                <Button onClick={this.handleShow}>
                    Create course
                </Button>
                <Modal show={this.state.show} onHide={this.handleClose}>
                    <form>
                        <Modal.Header>
                            <Modal.Title>Create course from scratch</Modal.Title>
                        </Modal.Header>
                        <Modal.Body>
                            <FormGroup>
                                <ControlLabel>Course name</ControlLabel>
                                <FormControl
                                    type="text"
                                    placeholder="Course name"
                                    onChange={this.handleCourseNameChange}/>
                                <HelpBlock>Course name should be a valid git repository name</HelpBlock>
                            </FormGroup>
                            <FormGroup>
                                <ControlLabel>language</ControlLabel>
                                <FormControl
                                    componentClass="select"
                                    placeholder="language"
                                    onChange={this.handleLanguageChange}>
                                    {this.getLanguages()}
                                </FormControl>
                                <HelpBlock>Language solutions will be written on</HelpBlock>
                            </FormGroup>
                            <FormGroup>
                                <ControlLabel>testing language</ControlLabel>
                                <FormControl
                                    componentClass="select"
                                    placeholder="testing language"
                                    onChange={this.handleTestingLanguageChange}>
                                    {this.getTestLanguages()}
                                </FormControl>
                                <HelpBlock>Language tests will be written on</HelpBlock>
                            </FormGroup>
                            <FormGroup>
                                <ControlLabel>testing framework</ControlLabel>
                                <FormControl
                                    componentClass="select"
                                    placeholder="testing framework"
                                    onChange={this.handleTestingFrameworkChange}>
                                    {this.getTestingFrameworks()}
                                </FormControl>
                                <HelpBlock>Test framework to use in course</HelpBlock>
                            </FormGroup>
                            <FormGroup>
                                <ControlLabel>Number of tasks</ControlLabel>
                                <FormControl
                                    type="text"
                                    placeholder="Number of tasks"
                                    onChange={this.handleNumberOfTasksChange}/>
                                <HelpBlock>Number of git branches for tasks</HelpBlock>
                            </FormGroup>
                        </Modal.Body>
                        <Modal.Footer>
                            <Button onClick={this.handleClose}>Close</Button>
                            <Button bsStyle="primary" onClick={this.createCourse}>Create</Button>
                        </Modal.Footer>
                    </form>
                </Modal>
            </div>
        );
    }

    getLanguages() {
        return (
            Immutable.List(this.state.flaxoLanguages)
                .map(language => language.name)
                .map(name => <option value={name}
                                     selected={name === this.state.language ? "selected" : ""}>{name}</option>
                )
        );
    }

    getTestLanguages() {
        return (
            Immutable.List(this.state.flaxoLanguages)
                .filter(language => language.name === this.state.language)
                .flatMap(language => language.compatibleTestingLanguages)
                .map(name => <option value={name}
                                     selected={name === this.state.testLanguage ? "selected" : ""}>{name}</option>
                )
        );
    }

    getTestingFrameworks() {
        return (
            Immutable.List(this.state.flaxoLanguages)
                .filter(language => language.name === this.state.testLanguage)
                .flatMap(language => language.compatibleTestingFrameworks)
                .map(name => <option value={name}
                                     selected={name === this.state.testingFramework ? "selected" : ""}>{name}</option>
                )
        );
    }

    handleClose() {
        this.setState({show: false});
    }

    handleShow() {
        this.setState({show: true});
    }

    handleCourseNameChange(event) {
        this.setState({courseName: event.target.value});
    }

    handleLanguageChange(event) {
        const selectedLanguage = event.target.value;

        const defaultTestLanguage =
            Immutable.List(this.state.flaxoLanguages)
                .find(language => language.name === selectedLanguage)
                .compatibleTestingLanguages[0];

        const defaultTestingFramework =
            Immutable.List(this.state.flaxoLanguages)
                .find(language => language.name === defaultTestLanguage)
                .compatibleTestingFrameworks[0];

        this.setState({
            language: selectedLanguage,
            testLanguage: defaultTestLanguage,
            testingFramework: defaultTestingFramework
        });
    }

    handleTestingLanguageChange(event) {
        const selectedTestLanguage = event.target.value;

        const defaultTestingFramework =
            Immutable.List(this.state.flaxoLanguages)
                .find(language => language.name === selectedTestLanguage)
                .compatibleTestingFrameworks[0];

        this.setState({
            testLanguage: selectedTestLanguage,
            testingFramework: defaultTestingFramework
        });
    }

    handleTestingFrameworkChange(event) {
        this.setState({testingFramework: event.target.value});
    }

    handleNumberOfTasksChange(event) {
        this.setState({numberOfTasks: event.target.value});
    }

    createCourse() {
        Api.createCourse(credentials(),
            {
                courseName: this.state.courseName,
                language: this.state.language,
                testLanguage: this.state.testLanguage,
                testingFramework: this.state.testingFramework,
                numberOfTasks: parseInt(this.state.numberOfTasks)
            },
            () => {
                ReactDOM.render(
                    <Notification succeed message={`Course ${this.state.courseName} has been created`}/>,
                    document.getElementById('notifications')
                );

                this.props.onCourseCreation();
            },
            response => ReactDOM.render(
                <Notification message={`Course ${this.state.courseName} creation failed.<br/>${response}`}/>,
                document.getElementById('notifications')
            )
        );

        this.handleClose();
    }
}

class Course extends React.Component {

    constructor(props) {
        super(props);

        this.startCourse = this.startCourse.bind(this);
        this.analysePlagiarism = this.analysePlagiarism.bind(this);
        this.downloadStats = this.downloadStats.bind(this);
        this.deleteCourse = this.deleteCourse.bind(this);

        this.state = props.data;
    }

    render() {
        return (
            <section className="selected-course">
                <h2>
                    {this.state.name}
                    <small>#{this.state.id}</small>
                    <CourseLabels course={this.state}/>
                </h2>
                <Button onClick={this.startCourse} disabled={this.state.status === "running" ? "disabled" : ""}>
                    Start course
                </Button>
                <Button onClick={this.analysePlagiarism} disabled={this.state.status !== "running" ? "disabled" : ""}>
                    Analyse plagiarism
                </Button>
                <DropdownButton title="Download as">
                    <MenuItem eventKey="json" onSelect={this.downloadStats}>json</MenuItem>
                    <MenuItem eventKey="2" disabled>excel</MenuItem>
                    <MenuItem eventKey="3" disabled>csv</MenuItem>
                </DropdownButton>
                <Button bsStyle="danger" onClick={this.deleteCourse}>
                    Delete course
                </Button>
                <CourseStatsTable course={this.state}/>
            </section>
        );
    }

    startCourse() {
        Api.startCourse(credentials(), this.state.name,
            () => {
                this.state.status = "running";
                this.props.onUpdate();

                ReactDOM.render(
                    <Notification succeed message={`Course ${this.state.name} has started successful.`}/>,
                    document.getElementById('notifications')
                );
            },
            response => ReactDOM.render(
                <Notification message={`Course ${this.state.name} starting went bad.<br/>${response}`}/>,
                document.getElementById('notifications')
            )
        );
    }

    analysePlagiarism() {
        Api.analysePlagiarism(credentials(), this.state.name,
            scheduledTasks => {
                const tasks = Immutable.List(scheduledTasks).join(", ");

                if (tasks) {
                    ReactDOM.render(
                        <Notification succeed
                                      message={`Plagiarism analysis for course ${this.state.name}
                                                has been scheduled for ${tasks}.`}/>,
                        document.getElementById('notifications')
                    );
                } else {
                    ReactDOM.render(
                        <Notification info
                                      message={`Plagiarism analysis for course hasn't been scheduled.
                                                There aren't enough students solutions.`}/>,
                        document.getElementById('notifications')
                    );
                }
            },
            response => {
                ReactDOM.render(
                    <Notification succeed
                                  message={`Plagiarism analysis for course ${this.state.name}
                                            hasn't been scheduled.<br/>${response}`}/>,
                    document.getElementById('notifications')
                );
            }
        );
    }

    downloadStats(eventKey) {
        //todo: impl
    }

    deleteCourse() {
        Api.deleteCourse(credentials(), this.state.name,
            () => {
                this.props.onDelete(this.state.name);

                ReactDOM.render(
                    <Notification succeed message={`Course ${this.state.name} has been deleted.`}/>,
                    document.getElementById('notifications')
                );
            },
            response => ReactDOM.render(
                <Notification message={`Course ${this.state.name} deletion went bad.<br/>` + response}/>,
                document.getElementById('notifications')
            )
        );
    }
}

class CourseStatsTable extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            perStudentStats: {},
            perTaskStats: {}
        };

        Api.retrieveCourseStatistics(credentials(), props.course.user, props.course.name,
            statistics => this.setState({
                perStudentStats: statistics.perStudentStats,
                perTaskStats: statistics.perTaskStats
            }),
            response => ReactDOM.render(
                <Notification message={"Course statistics retrieving failed.<br/>" + response}/>,
                document.getElementById('notifications')
            )
        )
    }

    render() {
        const tasksNames =
            Immutable.List(this.props.course.tasks)
                .sort();

        const toStudentResultCell = (studentTasks, taskName) => {
            const studentTask =
                Immutable.List(studentTasks)
                    .find(studentTask => studentTask.task === taskName);

            return (
                <td className={
                    `${studentTask.built ? "solution-built" : ""} ${studentTask.succeed ? "solution-succeed" : ""}`
                }/>
            );
        };

        const studentsResults =
            Immutable.Map(this.state.perStudentStats)
                .map((studentTasks, student) =>
                    <tr>
                        <td>{student}</td>
                        {tasksNames.map(taskName => toStudentResultCell(studentTasks, taskName))}
                        <td>0</td>
                    </tr>
                )
                .valueSeq();

        const tasks =
            Immutable.Map(this.state.perTaskStats)
                .map((taskData, taskName) => {
                    return {
                        name: taskName,
                        mossResultUrl: taskData.mossResultUrl,
                        mossPlagiarismMatches: taskData.mossPlagiarismMatches
                    }
                })
                .valueSeq()
                .sortBy(task => task.name)
                .map((task, index) =>
                    <Tab eventKey={index + 1} title={task.name}>
                        <CourseTask user={this.props.course.userGithubId}
                                    courseName={this.props.course.name}
                                    data={task}/>
                    </Tab>
                );

        return (
            <section className="course-stats-tabs">
                <Tabs defaultActiveKey={0}>
                    <Tab eventKey={0} title="Course stats">
                        <Table striped bordered condensed hover>
                            <thead>
                            <tr>
                                <th>Student</th>
                                {tasksNames.map(name => <th>{name}</th>)}
                                <th>Score</th>
                            </tr>
                            </thead>
                            <tbody>
                            {
                                studentsResults.size > 0
                                    ? studentsResults
                                    : <td colSpan={tasksNames.size + 2}>There are no students on the course yet.</td>
                            }
                            </tbody>
                        </Table>
                    </Tab>
                    {tasks}
                </Tabs>
            </section>
        );
    }
}

class CourseTask extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const gitLink =
            `https://github.com/${this.props.user}/${this.props.courseName}/tree/${this.props.data.name}`;

        const mossLink =
            this.props.data.mossResultUrl
                ? this.props.data.mossResultUrl
                : null;

        return (
            <section className="course-task">
                <Button href={gitLink} bsStyle="link" bsSize="small">Git branch</Button>
                {
                    mossLink
                        ? <Button href={mossLink} bsStyle="link" bsSize="small">Moss analysis results</Button>
                        : <Button bsStyle="link" bsSize="small" disabled>Moss analysis results</Button>
                }
                <PlagiarismStats data={this.props.data.mossPlagiarismMatches}/>
            </section>
        );
    }
}

class PlagiarismStats extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const matches =
            Immutable.List(this.props.data)
                .map(match => <PlagiarismMatch data={match}/>);

        return (
            <section className="task-plagiarism">
                {
                    matches.size > 0
                        ? matches
                        : <p>No plagiarism matches were detected.</p>
                }
            </section>
        );
    }
}

class PlagiarismMatch extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <section className="plagiarism-match">
                <p>
                    <b>students: </b>
                    {this.props.students.first}
                    and
                    {this.props.students.second}
                </p>
                <p>
                    <b>lines: </b>
                    {this.props.lines}
                </p>
                <p>
                    <b>percentage: </b>
                    {this.props.percentage}
                </p>
                <p>
                    <b>moss link: </b>
                    <a href={this.props.link}>{this.props.link}</a>
                </p>
            </section>
        )
    }
}

function CourseLabels(props) {
    const statusLabel = <Label bsStyle="primary">{props.course.status}</Label>;

    const techLabels =
        Immutable.Set([props.course.language, props.course.testingLanguage, props.course.testingFramework])
            .map(value => <Label bsStyle="info">{value}</Label>);

    return (
        <section className="course-labels">
            {statusLabel}
            {techLabels}
        </section>
    );
}
package root.client.controllers;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import ocsf.client.ObservableClient;
import root.client.managers.DataKeepManager;
import root.client.managers.ScreensManager;
import root.dao.app.CsvDetails;
import root.dao.app.Question;
import root.dao.app.SolvedExams;
import root.dao.app.Subject;
import root.dao.app.User;
import root.dao.app.UserInfo;
import root.dao.message.CourseMessage;
import root.dao.message.CsvMessage;
import root.dao.message.ErrorMessage;
import root.dao.message.MessageFactory;
import root.dao.message.QuestionsMessage;
import root.dao.message.SimpleMessage;
import root.dao.message.UserInfoMessage;
import root.dao.message.UserSolvedExamsMessage;
import root.util.log.Log;
import root.util.log.LogLine;

/**
 * 
 * 
 * this class handle all that related to student's solved exams
 * 
 * @author Gal Brandwine
 *
 */
public class SolvedExamsController implements Observer {

	// FXML variables **********************************************

	@FXML
	private AnchorPane rootPane;

	@FXML
	private TableView<SolvedExams> tblSolvedExams;

	@FXML
	private TableColumn<SolvedExams, String> tbcId;

	@FXML
	private TableColumn<SolvedExams, String> tbcCourse;

	@FXML
	private TableColumn<SolvedExams, String> tbcExecutingDate;

	@FXML
	private TableColumn<SolvedExams, String> tbcTeacherNotes;

	@FXML
	private TableColumn<SolvedExams, String> tbcAlteredHradeExplenation;

	@FXML
	private TableColumn<SolvedExams, String> tbcApprovingTeacherName;

	@FXML
	private TableColumn<SolvedExams, String> tbcGetCopy;

	@FXML
	private TableColumn<SolvedExams, Integer> tbcIdGrade;

	@FXML
	private TextField txtFieldId;

	@FXML
	private TextField txtFieldName;

	@FXML
	private TextField txtFieldSolvedExams;

	@FXML
	private ComboBox<String> courseCombobox;

	@FXML
	private Button newQuestion;

	@FXML
	private Button btnSearch;

	@FXML
	private Button editQuestion;

	@FXML
	private Button deleteQuestion;

	@FXML
	private Label fstNameLbl;

	@FXML
	private Label lstNamelbl;

	@FXML
	private Label teacherIDLbl;

	@FXML
	private Label TeacherPremissionLbl;

	// Instance variables **********************************************

	/**
	 * the relevant coursese to be shown in combobox
	 */
	private ObservableList<String> observableCourses;

	/**
	 * this observabale shows the solves exam in the tableView
	 */
	private ObservableList<SolvedExams> observabaleSolvedExams;

	/**
	 * the students solved exams
	 */
	private ArrayList<SolvedExams> solvedExams;

	/**
	 * the client of this student
	 */
	private ObservableClient client;

	/**
	 * the message factory to get our dao messages
	 */
	private MessageFactory message;

	/**
	 * the logged in user
	 */
	private User user;

	/**
	 * the screen manager to pass bewteen screens
	 */
	private ScreensManager screenManager;

	/**
	 * the hashMap to hold courseId with relevant course name
	 */
	private HashMap<String, String> courseMap; // key = subjectID, value = subject name

	/**
	 * the hashMap to hold relevant teacher id with teacher name
	 */
	private HashMap<String, String> teachersMap; // key = teacherID, value = teacher full name.

	/**
	 * a specific solved exams CSV
	 */
	private ArrayList<String[]> csvData; // csvData contains per solvedExam questions: {questionID, selectedQuestion,
											// question Weight}

	/**
	 * this strudent solved exams
	 */
	private ArrayList<Question> solvedExamQuestions;

	/**
	 * a selected solved exam for downloading
	 */
	private SolvedExams solvedExam;

	/**
	 * 
	 * A log file that is responsible for documenting the actions performed in the
	 * application
	 */
	Log log = Log.getInstance();

	// CLASS METHODS *************************************************

	public SolvedExamsController() {
		super();
	}

	/**
	 * This method fires when theres a selection from combobox
	 * 
	 * @param event
	 */
	@FXML
	void selectFromCombobox(ActionEvent event) {
		String selectedCourse = courseCombobox.getSelectionModel().getSelectedItem();
		observabaleSolvedExams.clear();
		if (selectedCourse.equals("show all exams"))
			observabaleSolvedExams.addAll(solvedExams);
		else {
			for (SolvedExams solveExam : solvedExams) {
				if (solveExam.getExamCourse().equals(selectedCourse)) {
					observabaleSolvedExams.add(solveExam);
				}
			}
		}
	}

	/**
	 * This method occurs when the window is shown up.
	 * 
	 * @throws IOException
	 *             if the window cannot be shown
	 */
	@FXML
	public void initialize() throws IOException {
		Platform.runLater(() -> rootPane.requestFocus());
		message = MessageFactory.getInstance();
		screenManager = ScreensManager.getInstance();

		client = (ObservableClient) DataKeepManager.getInstance().getObject_NoRemove("client"); // get the client from
																								// DataKeep, but dont
																								// remove it from there,
																								// for later use.
		client.deleteObservers();
		client.addObserver(this); // add THIS to clinet's observer, so THIS.update will be triggered when server
									// send messages.

		user = (User) DataKeepManager.getInstance().getUser();
		solvedExams = new ArrayList<SolvedExams>();
		teachersMap = new HashMap<String, String>();
		courseMap = new HashMap<String, String>();
		csvData = new ArrayList<String[]>();
		solvedExamQuestions = new ArrayList<Question>();

		// Listen for selection changes and show the person details when changed.
		txtFieldId.setOnMouseClicked(e -> {
			btnSearch.setDisable(false);
		});
		// Listen for selection changes and show the person details when changed.
		txtFieldName.setOnMouseClicked(e -> {
			btnSearch.setDisable(false);
		});
		// Listen for selection changes and show the person details when changed.
		txtFieldSolvedExams.setOnMouseClicked(e -> {
			btnSearch.setDisable(false);
		});

		btnSearch.setDisable(true);

		setUserDetails(user);
		getUserSolvedExamsByUserID();
		initSolvedExamTable();
	}

	/**
	 * this method is called when need to fill courseMap, first get all solvedExams
	 * course ID, from examID. Than get all course names
	 * 
	 * @param solvedExams2
	 */
	private void getSolvedExamsCourse(ArrayList<SolvedExams> solvedExams2) {
		for (SolvedExams solvedExam : solvedExams) {
			courseMap.put(solvedExam.getExamID().substring(2, 4), "");
		}
		CourseMessage newCourseMessage = (CourseMessage) message.getMessage("get-coursesbyid", courseMap);
		try {
			client.sendToServer(newCourseMessage);
		} catch (IOException e) {
			String setTitle = "IOException";
			String errorHeader = "In SolvedExamsController, getSolvedExamsCourse()";
			String errorText = e.getMessage();
			showErrorDialog(setTitle, errorHeader, errorText);
		}
	}

	private void getUserSolvedExamsByUserID() {
		if (solvedExams.isEmpty() != true)
			return; // if solvedExams has been loaded already, do nothing
		UserSolvedExamsMessage userSolvedExamMessage = (UserSolvedExamsMessage) message
				.getMessage("get-solvedExams-user", user);
		try {
			client.sendToServer(userSolvedExamMessage);
		} catch (IOException e) {
			e.printStackTrace();
			log.writeToLog(LogLine.LineType.ERROR, e.getMessage());
		}
	}

	/**
	 * this method fires when SolvedExamsController is initialized
	 */
	private void initSolvedExamTable() {
		tbcId.setCellValueFactory(new PropertyValueFactory<SolvedExams, String>("examID"));
		tbcCourse.setCellValueFactory(new PropertyValueFactory<SolvedExams, String>("examCourse"));
		tbcExecutingDate.setCellValueFactory(new PropertyValueFactory<SolvedExams, String>("examDateTime"));
		tbcTeacherNotes.setCellValueFactory(new PropertyValueFactory<SolvedExams, String>("teacherNotes"));
		tbcAlteredHradeExplenation
				.setCellValueFactory(new PropertyValueFactory<SolvedExams, String>("gradeAlturationExplanation"));
		tbcApprovingTeacherName
				.setCellValueFactory(new PropertyValueFactory<SolvedExams, String>("approvingTeacherName"));
		tbcIdGrade.setCellValueFactory(new PropertyValueFactory<SolvedExams, Integer>("examGrade"));
		/**
		 * all this, is pre-settings for adding a button into the column
		 */
		tbcGetCopy.setCellValueFactory(new PropertyValueFactory<SolvedExams, String>("action"));

		Callback<TableColumn<SolvedExams, String>, TableCell<SolvedExams, String>> cellFactory = //
				new Callback<TableColumn<SolvedExams, String>, TableCell<SolvedExams, String>>() {
					@Override
					public TableCell call(final TableColumn<SolvedExams, String> param) {

						final TableCell<SolvedExams, String> cell = new TableCell<SolvedExams, String>() {

							final Button btn = new Button("Download copy");

							@Override
							public void updateItem(String item, boolean empty) {
								super.updateItem(item, empty);
								if (empty) {
									setGraphic(null);
									setText(null);
								} else {
									btn.setOnAction(event -> {
										solvedExam = getTableView().getItems().get(getIndex());
										perapeDownload(solvedExam);
									});
									setGraphic(btn);
									setText(null);
								}
							}

						};
						return cell;
					}
				};
		tbcGetCopy.setCellFactory(cellFactory);
	}

	private void setUserDetails(User user1) {
		teacherIDLbl.setText(user.getUserID());
		fstNameLbl.setText(user.getUserFirstName());
		lstNamelbl.setText(user.getUserLastName());
		TeacherPremissionLbl.setText(user.getUserPremission());
	}

	/**
	 * this method is called when student pressed the "Download copy" button.
	 * 
	 * @param solvedExam
	 */
	private void perapeDownload(SolvedExams solvedExam) {
		System.out.println(solvedExam);
		/**
		 * get CSV of this solved exam, from server
		 */
		CsvDetails csv = new CsvDetails(solvedExam, solvedExam.getSovingStudentID());
		CsvMessage newMessage = (CsvMessage) message.getMessage("get-csvFromServer", csv);
		try {
			client.sendToServer(newMessage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			String setTitle = "IOException";
			String errorHeader = "In SolvedExamsController, perapeDownload()";
			String errorText = e.getMessage();
			showErrorDialog(setTitle, errorHeader, errorText);
		}

	}

	/**
	 * this method get all solvedExam's questions
	 */
	private void getExamQuestions() {
		ArrayList<String> questionIDList = new ArrayList<String>();
		for (String[] csvLine : csvData) {
			String questionID = csvLine[0];
			if (questionID.equals("QuestionID") == false) {// do this check to pass the first line of the csvData file.
				questionIDList.add(questionID);
			}
		}
		QuestionsMessage newQuestionMessage = (QuestionsMessage) message.getMessage("get-Questions", questionIDList);
		try {
			client.sendToServer(newQuestionMessage);
		} catch (IOException e) {
			String setTitle = "IOException";
			String errorHeader = "In SolvedExamsController, getExamQuestions()";
			String errorText = e.getMessage();
			showErrorDialog(setTitle, errorHeader, errorText);
		}
	}

	/**
	 * this method create the solvedExam's PDF.
	 */
	private void createWord() {
		/**
		 * Word formation:
		 * 
		 * SolvingDate Exam ID StudentID Approving teacher full name Exam Grade
		 *
		 * question (from Questions) Question weight selected answer
		 *
		 * question (from Questions) Question weight selected answer * * *
		 *
		 */

		Platform.runLater(() -> {
			System.out.println("opening fileChooser");
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save solvedExam");
			fileChooser.setInitialFileName(solvedExam.getExamID() + "-" + solvedExam.getSovingStudentID() + ".docx");
			File file = fileChooser.showSaveDialog(screenManager.getPrimaryStage());
			if (file != null) {
				try {
					// Blank Document
					XWPFDocument document = new XWPFDocument();
					// Write the Document in file system
					FileOutputStream out = new FileOutputStream(file);
					/**
					 * Printing: SolvingDate Exam ID StudentID Approving teacher full name Exam
					 * Grade
					 */
					// solving date
					XWPFParagraph dateParagraph = document.createParagraph();
					XWPFRun runDate = dateParagraph.createRun();
					runDate.setBold(true);
					runDate.setItalic(true);
					SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
					String solvedExsamDate = dateFormat.format(solvedExam.getExamDateTime());
					runDate.setText(solvedExsamDate);
					// Exam ID + Exam course
					XWPFParagraph titleParagraph = document.createParagraph();
					titleParagraph.setAlignment(ParagraphAlignment.CENTER);
					XWPFRun runTitle = titleParagraph.createRun();
					runTitle.setBold(true);
					runTitle.setItalic(true);
					runTitle.setText("ExamID: " + solvedExam.getExamID());
					runTitle.addBreak();
					runTitle.setText("Exam course: " + solvedExam.getExamCourse());
					runTitle.addBreak();
					runTitle.setText("Strudet ID: " + solvedExam.getSovingStudentID());
					runTitle.addBreak();
					runTitle.setText("Approving Teacher: " + solvedExam.getApprovingTeacherName());
					runTitle.addBreak();
					runTitle.setText("Exam grade: " + solvedExam.getExamGrade());
					runTitle.addBreak();
					/**
					 * printing Questions and selected answeres
					 */
					for (Question question : solvedExamQuestions) {
						// create Paragraph
						XWPFParagraph questionParagraph = document.createParagraph();
						XWPFRun runQuestions = questionParagraph.createRun();
						runQuestions.setText(question.getQuestionText() + "\n");
						runQuestions.addBreak();
						runQuestions.addTab();
						if (question.getIdquestionIntruction() != null) {
							runQuestions.setText(question.getIdquestionIntruction() + "\n");
							runQuestions.addBreak();
							runQuestions.addTab();
						}
						runQuestions.setText("Possible answeres:");
						runQuestions.addBreak();
						runQuestions.addTab();
						runQuestions.setText("1) " + question.getAns1());
						runQuestions.addBreak();
						runQuestions.addTab();
						runQuestions.setText("2) " + question.getAns2());
						runQuestions.addBreak();
						runQuestions.addTab();
						runQuestions.setText("3) " + question.getAns3());
						runQuestions.addBreak();
						runQuestions.addTab();
						runQuestions.setText("4) " + question.getAns4());
						runQuestions.addBreak();
						runQuestions.addTab();
						runQuestions.setText("Correct answer: " + question.getCorrectAns());
						runQuestions.addBreak();
						runQuestions.addTab();
						// getting the selected answer from csvData.
						for (String[] csvLine : csvData) {
							String slectedQuestionID = csvLine[0].replaceAll("\"", "");
							String questionID = question.getQuestionId();
							int correctAnswer = question.getCorrectAns();
							if (slectedQuestionID.equals(questionID)) {
								runQuestions.setText("Your selected answer: " + csvLine[1].replaceAll("\"", ""));
								runQuestions.addBreak();
								if (Integer.parseInt(csvLine[1].replaceAll("\"", "")) == correctAnswer) {
									runQuestions.setText("Question points: " + csvLine[2].replaceAll("\"", "") + "/"
											+ csvLine[2].replaceAll("\"", ""));
								} else
									runQuestions
											.setText("Recieved points: " + "0" + "/" + csvLine[2].replaceAll("\"", ""));
							}
						}
						runQuestions.addBreak();
					} // end of printing questions
					document.enforceReadonlyProtection();
					document.write(out);

					// Close document
					out.close();
					System.out.println(solvedExam.getExamID() + "-" + solvedExam.getSovingStudentID() + ".docx"
							+ " written successfully");

				} catch (IOException e) {
					String setTitle = "IOException";
					String errorHeader = "In SolvedExamsController, createWord()";
					String errorText = e.getMessage();
					showErrorDialog(setTitle, errorHeader, errorText);
				} catch (Exception e) {
					String setTitle = "Exception";
					String errorHeader = "In SolvedExamsController, createWord()";
					String errorText = e.getMessage();
					showErrorDialog(setTitle, errorHeader, errorText);
				}
			}
		});
	}

	/**
	 * This method called when we need to update in tblQuestions the TeacherName
	 * column
	 */
	private void updateTeacherAssemblerFullName(HashMap<String, String> userInfo) {
		for (SolvedExams solvedExam : solvedExams) { // update the questions array, to keep updated
			String tempTeacherAssembeledID = solvedExam.getApprovingTeacherID();
			if (userInfo.containsKey(tempTeacherAssembeledID)) {
				solvedExam.setApprovingTeacherName(userInfo.get(tempTeacherAssembeledID));
			}
		}
		for (SolvedExams obsSolvedExam : observabaleSolvedExams) { // update observableQuestion to update the
																	// tblQuestions
			String tempTeacherAssembeledID = obsSolvedExam.getApprovingTeacherID();
			if (userInfo.containsKey(tempTeacherAssembeledID)) {
				obsSolvedExam.setApprovingTeacherName(userInfo.get(tempTeacherAssembeledID));
			}
		}
	}

	/**
	 * This method is called in order to fill theacherMap, for each solvedExam in
	 * the subject this teacher teaches, we need the teacher assembled name.
	 * 
	 * @param solvedExams2
	 */
	void getTeachersMap(ArrayList<SolvedExams> solvedExams2) {
		// by sending all solvedExams of THIS, well loop over all solvedExams and get
		// the relevant ApprovringteacherName Full name
		for (SolvedExams solvedExam : solvedExams) {
			teachersMap.put(solvedExam.getApprovingTeacherID(), "");
		}
		UserInfo teachersInfo = new UserInfo(teachersMap, null);
		teachersInfo.setSolvedExams(solvedExams2);
		UserInfoMessage teacehrInfoMessage = (UserInfoMessage) message.getMessage("get-user-name", teachersInfo); // we
																													// can
																													// send
																													// the
																													// specific
																													// question
																													// because
																													// we
																													// have
																													// table
																													// "Questions"
		try {
			client.sendToServer(teacehrInfoMessage);
		} catch (IOException e) {
			e.printStackTrace();
			log.writeToLog(LogLine.LineType.ERROR, e.getMessage());
		}
	}

	private void fillCombobox(HashMap<String, String> subjectMap) {
		if (courseCombobox.getItems().isEmpty()) {
			observableCourses = FXCollections.observableArrayList(subjectMap.values());
			courseCombobox.getItems().add("show all exams");
			courseCombobox.getItems().addAll(observableCourses);
		}
	}

	/**
	 * This method is called when there's a need to ErrorDialog
	 * 
	 * @param HeaderTitle
	 * @param HeaderText
	 * @param Errormessage
	 */
	private void showErrorDialog(String HeaderTitle, String HeaderText, String Errormessage) {
		Platform.runLater(() -> { // In order to run javaFX thread.(we recieve from server a java thread)
			// Show the error message.
			Alert alert = new Alert(AlertType.ERROR);
			alert.initOwner(screenManager.getPrimaryStage());
			alert.setTitle(HeaderTitle);// "ServerIP error");
			alert.setHeaderText(HeaderText);// "Please contact system administrator");
			alert.setContentText(Errormessage);
			alert.showAndWait();
			log.writeToLog(LogLine.LineType.ERROR, Errormessage);
		});
	}

	/**
	 * after getting all courses relevant for these solved exams, i want to update
	 * solvedExam.examcourse field, for further use
	 */
	private void updateSolvedExamsCourse() {
		for (SolvedExams solvedExam : solvedExams) {
			String tempCourseIDinSolvedExam = solvedExam.getExamID().substring(2, 4);
			if (courseMap.containsKey(tempCourseIDinSolvedExam)) {
				solvedExam.setExamCourse(courseMap.get(tempCourseIDinSolvedExam));

			}
		}
		for (SolvedExams solvedExam : observabaleSolvedExams) {
			String tempCourseIDinSolvedExam = solvedExam.getExamID().substring(2, 4);
			if (courseMap.containsKey(tempCourseIDinSolvedExam)) {
				solvedExam.setExamCourse(courseMap.get(tempCourseIDinSolvedExam));

			}
		}
	}

	/**
	 * this method fires when pressing the search button
	 * 
	 * @param event
	 */
	@FXML
	void searchQuestion(ActionEvent event) {
		String errorMessage = "";
		if (txtFieldSolvedExams.getText().length() != 0) {
			String solvedExamID = txtFieldSolvedExams.getText();
			observabaleSolvedExams.clear();
			for (SolvedExams solvedExam : solvedExams) {
				if (solvedExam.getExamID().equals(solvedExamID)) {
					observabaleSolvedExams.add(solvedExam);
				}
			}
			txtFieldSolvedExams.clear();
			return;
		}
		if (txtFieldId.getText().length() != 0) {
			String teacherID = txtFieldId.getText();
			observabaleSolvedExams.clear();
			for (SolvedExams solvedExam : solvedExams) {
				if (solvedExam.getApprovingTeacherID().equals(teacherID)) {
					observabaleSolvedExams.add(solvedExam);
				}
			}
			txtFieldId.clear();
			return;
		}
		if (txtFieldName.getText().length() != 0) {
			String teacherName = txtFieldName.getText();
			observabaleSolvedExams.clear();
			for (SolvedExams solvedExam : solvedExams) {
				if (solvedExam.getApprovingTeacherName().equals(teacherName)) {
					observabaleSolvedExams.add(solvedExam);
				}
			}
			txtFieldName.clear();
			return;
		} else {
			// Nothing selected.
			String setTitle = "No selection";
			String errorHeader = "No field Selected";
			String errorText = "Please fill selected field";
			showErrorDialog(setTitle, errorHeader, errorText);
		}
	}

	/**
	 * This method happens when the server send an message
	 */
	@Override
	public void update(Observable arg0, Object arg1) {
		if (arg1 instanceof QuestionsMessage) {
			this.solvedExamQuestions = ((QuestionsMessage) arg1).getQuestions();
			createWord();
		}
		if (arg1 instanceof CsvMessage) {
			ArrayList<String[]> tempCsv = ((CsvMessage) arg1).getCsvDetailofSolvedExam();
			this.csvData = tempCsv;
			getExamQuestions();
		}
		if (arg1 instanceof UserSolvedExamsMessage) {
			ArrayList<SolvedExams> tempSolvedExams = ((UserSolvedExamsMessage) arg1).getUserSolvedExams();
			for (SolvedExams solvedExam : tempSolvedExams) {
				if (solvedExam.getCalculatedGradeApprovalStateByTeacher().equals("approved"))
					this.solvedExams.add(solvedExam);
			}
			observabaleSolvedExams = FXCollections.observableArrayList(); // add new solvedExams to ObservebaleList
			for (SolvedExams solvedExam : solvedExams) {
				observabaleSolvedExams.add(solvedExam);
			}
			getTeachersMap(solvedExams); // add newly teacher's ID to teacherMap
			getSolvedExamsCourse(solvedExams);
			tblSolvedExams.setItems(observabaleSolvedExams); // insert newly fetched question's to tblQuestion
		}
		if (arg1 instanceof CourseMessage) {
			if (this.courseMap.isEmpty())
				this.courseMap = ((CourseMessage) arg1).getCourseMap();
			this.courseMap.putAll(((CourseMessage) arg1).getCourseMap());
			fillCombobox(this.courseMap);
			updateSolvedExamsCourse();
		}
		if (arg1 instanceof UserInfoMessage) {
			for (SolvedExams solvedExam : solvedExams) {
				String tempApprovingTeaherId = solvedExam.getApprovingTeacherID();
				if (((UserInfoMessage) arg1).getUserInfo().getTeachersMap().containsKey(tempApprovingTeaherId))
					solvedExam.setApprovingTeacherName(
							((UserInfoMessage) arg1).getUserInfo().getTeachersMap().get(tempApprovingTeaherId));
				teachersMap = ((UserInfoMessage) arg1).getUserInfo().getTeachersMap(); // update teacherMap to hold new
																						// <teacherID,teacherFullName>.
			}
			updateTeacherAssemblerFullName(teachersMap);
		}
		if (arg1 instanceof SimpleMessage) {
			log.writeToLog(LogLine.LineType.INFO, "simpleMessage Recieved in SolvedExamController,  what the Fuck ?!");
		} else if (arg1 instanceof ErrorMessage) {
			String errorHeader = ((ErrorMessage) arg1).getMsg();
			String errorText = arg1.toString();
			showErrorDialog("Server error", errorHeader, errorText);
		}
	}
}

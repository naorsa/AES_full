package root.client.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ocsf.client.ObservableClient;
import root.client.Main;
import root.client.managers.DataKeepManager;
import root.client.managers.ScreensManager;
import root.dao.app.Question;
import root.dao.app.Subject;
import root.dao.app.User;
import root.dao.app.UserInfo;
import root.dao.message.ErrorMessage;
import root.dao.message.MessageFactory;
import root.dao.message.QuestionsMessage;
import root.dao.message.SimpleMessage;
import root.dao.message.UserInfoMessage;
import root.dao.message.UserSubjectMessage;
import root.util.log.Log;
import root.util.log.LogLine;
//import tableButtonTest.Main.Record;
//import tableButtonTest.Main.ButtonCell;
//import tableButtonTest.Mai//n.Record;
import root.util.properties.PropertiesFile;

/**
 * 
 * 
 * This class handle with questions window
 * 
 * @author gal
 */
public class QuestionsController implements Observer {

	// FXML variables **********************************************

	@FXML // fx:id="rootPane"
	private AnchorPane rootPane; // Value injected by FXMLLoader

	@FXML // fx:id="tblQuestions"
	private TableView<Question> tblQuestions; // Value injected by FXMLLoader

	@FXML // fx:id="tbcId"
	private TableColumn<Question, String> tbcId; // Value injected by FXMLLoader

	@FXML // fx:id="tbcIdNum"
	private TableColumn<Question, String> tbcIdInstruction; // Value injected by FXMLLoader

	@FXML // fx:id="tbcName"
	private TableColumn<Question, String> tbcTeacherName; // Value injected by FXMLLoader

	@FXML // fx:id="tbcIdText"
	private TableColumn<Question, String> tbcIdText; // Value injected by FXMLLoader

	@FXML // fx:id="tbcAns1"
	private TableColumn<Question, String> tbcAns1; // Value injected by FXMLLoader

	@FXML // fx:id="tbcAns2"
	private TableColumn<Question, String> tbcAns2; // Value injected by FXMLLoader

	@FXML // fx:id="tbcAns3"
	private TableColumn<Question, String> tbcAns3; // Value injected by FXMLLoader

	@FXML // fx:id="tbcAns4"
	private TableColumn<Question, String> tbcAns4; // Value injected by FXMLLoader

	@FXML // fx:id="tbcCorr"
	private TableColumn<Question, Integer> tbcCorr; // Value injected by FXMLLoader

	@FXML // fx:id="txtFieldId"
	private TextField txtFieldId; // Value injected by FXMLLoader

	@FXML // fx:id="txtFieldName"
	private TextField txtFieldName; // Value injected by FXMLLoader

	@FXML // fx:id="txtFieldQuestion"
	private TextField txtFieldQuestion; // Value injected by FXMLLoader

	@FXML // fx:id="subjectCombobox"
	private ComboBox<Subject> subjectCombobox; // Value injected by FXMLLoader

	@FXML // fx:id="btnSearch"
	private Button btnSearch; // Value injected by FXMLLoader

	@FXML // fx:id="rstNameLbl"
	private Label fstNameLbl; // Value injected by FXMLLoader

	@FXML // fx:id="lstNamelbl"
	private Label lstNamelbl; // Value injected by FXMLLoader

	@FXML // fx:id="teacherIDLbl"
	private Label teacherIDLbl; // Value injected by FXMLLoader

	@FXML // fx:id="TeacherPremissionLbl"
	private Label TeacherPremissionLbl; // Value injected by FXMLLoader

	@FXML
	private Button deleteQuestion;

	@FXML
	private Button newQuestion;

	@FXML
	private Button editQuestion;

	// Instance variables **********************************************
	/**
	 * The manager that responsible for transmit data between windows in the system
	 */
	private DataKeepManager dkm = DataKeepManager.getInstance();
	/**
	 * The observable that contain the Subject, to show in the combobox
	 */
	private ObservableList<Subject> observableSubjects;
	/**
	 * The observable that contain the Questions, to show in the questionTable
	 */
	private ObservableList<Question> observabaleQuestions;
	/**
	 * The observable that contain the Questions, to show in the addQuestionWizzard
	 */
	private ObservableList<Question> observebaleNewQuestion;
	/**
	 * 
	 * Keeps our client in order to communicate with the server
	 */
	private ObservableClient client;
	/**
	 * The Arraylist that contains all question's that relevant to this teacher
	 * teaching subjects
	 */
	private ArrayList<Question> questions;
	/**
	 * Generates new communications between server and client
	 */
	private MessageFactory message;
	/**
	 * The login teacher
	 */
	private User user;
	/**
	 * The manager that responsible for switching between windows in the system
	 */
	private ScreensManager screenManager;
	/**
	 * The Arraylist that contain all theacher teaching subjects
	 */
	private ArrayList<Subject> userSubjects;
	/**
	 * The hashMap that contain theacherID and its corresponding teacherName
	 */
	private HashMap<String, String> teachersMap; // key = teacherID, value = teacher full name.
	/**
	 * 
	 * A log file that is responsible for documenting the actions performed in the
	 * application
	 */
	Log log = Log.getInstance();

	// CLASS METHODS *************************************************

	public QuestionsController() {
		super();
	}

	/**
	 * 
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

		client = (ObservableClient) dkm.getObject_NoRemove("client"); // get the client from DataKeep, but dont remove
																		// it from there, for later use.
		client.addObserver(this); // add THIS to clinet's observer, so THIS.update will be triggered when server
									// send messages.
		user = (User) dkm.getUser();// loggedInManager.getUser();

		// Listen for selection changes and show the person details when changed.
		txtFieldId.setOnMouseClicked(e -> {
			btnSearch.setDisable(false);
		});
		// Listen for selection changes and show the person details when changed.
		txtFieldName.setOnMouseClicked(e -> {
			btnSearch.setDisable(false);
		});
		// Listen for selection changes and show the person details when changed.
		txtFieldQuestion.setOnMouseClicked(e -> {
			btnSearch.setDisable(false);
		});
		// Listen for selection changes and show the person details when changed.
		tblQuestions.setOnMouseClicked(e -> {
			editQuestion.setDisable(false);
		});
		// Listen for selection changes and show the person details when changed.
		tblQuestions.setOnMouseClicked(e -> {
			deleteQuestion.setDisable(false);
		});

		btnSearch.setDisable(true);
		deleteQuestion.setDisable(true);
		questions = new ArrayList<Question>();
		userSubjects = new ArrayList<Subject>();
		teachersMap = new HashMap<String, String>();
		setUserDetails(user);
		getUserSubjects(user);
		initQuestionsTable();
	}

	/**
	 * 
	 * A method that initiate tableView every time window is initialized
	 * 
	 * 
	 * 
	 * 
	 */
	private void initQuestionsTable() {
		tbcId.setCellValueFactory(new PropertyValueFactory<Question, String>("questionId"));
		tbcIdText.setCellValueFactory(new PropertyValueFactory<Question, String>("questionText"));
		tbcIdInstruction.setCellValueFactory(new PropertyValueFactory<Question, String>("idquestionIntruction"));
		tbcAns1.setCellValueFactory(new PropertyValueFactory<Question, String>("ans1"));
		tbcAns2.setCellValueFactory(new PropertyValueFactory<Question, String>("ans2"));
		tbcAns3.setCellValueFactory(new PropertyValueFactory<Question, String>("ans3"));
		tbcAns4.setCellValueFactory(new PropertyValueFactory<Question, String>("ans4"));
		tbcCorr.setCellValueFactory(new PropertyValueFactory<Question, Integer>("correctAns"));
		tbcTeacherName.setCellValueFactory(new PropertyValueFactory<Question, String>("teacherFullName"));
	}

	/**
	 * 
	 * A method that allows the teacher to select a subject
	 * 
	 * @param event
	 * 
	 *            An event that happens when a teacher select from subject combo box
	 */
	@FXML
	void selectFromCombobox(ActionEvent event) {
		Subject selectedSucjet = subjectCombobox.getSelectionModel().getSelectedItem();
		observabaleQuestions.clear();
		if (selectedSucjet.getSubjectID().equals("00"))
			observabaleQuestions.addAll(questions);
		else {
			for (Question question : questions) {
				if (question.getQuestionId().substring(0, 2).equals(selectedSucjet.getSubjectID())) {
					observabaleQuestions.add(question);
				}
			}
		}

	}

	/**
	 * 
	 * A method that allows the teacher to search a question
	 * 
	 * @param event
	 * 
	 *            An event that happens when a teacher press "search"
	 */
	@FXML
	void searchQuestion(ActionEvent event) {
		String errorMessage = "";
		if (txtFieldQuestion.getText().length() != 0) {
			String questionID = txtFieldQuestion.getText();
			observabaleQuestions.clear();
			for (Question question : questions) {
				if (question.getQuestionId().equals(questionID)) {
					observabaleQuestions.add(question);
				}
			}
			txtFieldQuestion.clear();
			unselectSelectionFromTable();
			return;
		}
		if (txtFieldId.getText().length() != 0) {
			String teacherID = txtFieldId.getText();
			observabaleQuestions.clear();
			for (Question question : questions) {
				if (question.getTeacherAssembeld().equals(teacherID)) {
					observabaleQuestions.add(question);
				}
			}
			txtFieldId.clear();
			unselectSelectionFromTable();
			return;
		}
		if (txtFieldName.getText().length() != 0) {
			String teacherName = txtFieldName.getText();
			observabaleQuestions.clear();
			for (Question question : questions) {
				if (question.getTeacherFullName().equals(teacherName)) {
					observabaleQuestions.add(question);
				}
			}
			txtFieldName.clear();
			unselectSelectionFromTable();
			return;
		} else {
			// Nothing selected.
			String setTitle = "No selection";
			String errorHeader = "No field Selected";
			String errorText = "Please fill selected field";
			showErrorDialog(setTitle, errorHeader, errorText);
			unselectSelectionFromTable();

		}
	}

	/**
	 * This method is called when theres a need to ErrrorDialog
	 * 
	 * @param HeaderTitle
	 *            the text to show in the window header
	 * @param HeaderText
	 *            the test to show in the window upper body
	 * @param Errormessage
	 *            the text to show in the window lower body
	 * 
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
	 * 
	 * This method will set a edit option
	 * 
	 * @param event
	 * 
	 *            An event that happens when a teacher press "edit".
	 */
	@FXML
	void editSelectedQuestion(ActionEvent event) {
		int selectedIndex = tblQuestions.getSelectionModel().getSelectedIndex();
		if (selectedIndex >= 0) {
			runNewQuestionWizzard(tblQuestions.getSelectionModel().getSelectedItem());
		} else {
			// Nothing selected.
			String setTitle = "No selection";
			String errorHeader = "No field Selected";
			String errorText = "Please fill selected field";
			showErrorDialog(setTitle, errorHeader, errorText);
			unselectSelectionFromTable();
		}
	}

	/**
	 * 
	 * This method happens when the user press on the delete button
	 * 
	 * @param event
	 * 
	 *            An event that happens when a teacher press "delete".
	 */
	@FXML
	void deleteSelectedQuestion(ActionEvent event) {
		Question questionToDelete;
		int selectedIndex = tblQuestions.getSelectionModel().getSelectedIndex();
		if (selectedIndex >= 0) {
			questionToDelete = tblQuestions.getSelectionModel().getSelectedItem();
			deleteQuestionFromDB(questionToDelete); // remove question from DB
			tblQuestions.getItems().remove(selectedIndex); // remove question from tableview
			questions.remove(questionToDelete); // remove question from THIS.questions
			unselectSelectionFromTable();
		} else {
			// Nothing selected.
			String setTitle = "No selection";
			String errorHeader = "No question Selected";
			String errorText = "Please select a question in the table.";
			showErrorDialog(setTitle, errorHeader, errorText);
			unselectSelectionFromTable();
		}
	}

	/**
	 * 
	 * This method is called in order to fill theacherMap, for each question in the
	 * subject this teacher teaches, we need the teacher assembled name.
	 * 
	 * @param questions2
	 *            this param is the questions recieved from the server
	 */
	void getTeachersMap(ArrayList<Question> questions2) {
		// by sending all question of THIS teacher teaching subject, well loop over all
		// user and get the relevant users Full name
		for (Question question : questions) {
			teachersMap.put(question.getTeacherAssembeld(), "");
		}
		UserInfo teachersInfo = new UserInfo(teachersMap, questions);
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
			String setTitle = "IOException";
			String errorHeader = "In QuestionsController, getTeachersMap()";
			String errorText = e.getMessage();
			showErrorDialog(setTitle, errorHeader, errorText);
		}
	}

	/**
	 * 
	 * this method called when deleting Question from DB
	 * 
	 * @param questionToDelete
	 *            the question teacher want to delete
	 */
	void deleteQuestionFromDB(Question questionToDelete) {
		QuestionsMessage questionDeleteMessage = (QuestionsMessage) message.getMessage("delete-Questions",
				questionToDelete); // we can send the specific question because we have table "Questions"
		try {
			client.sendToServer(questionDeleteMessage);
		} catch (IOException e) {
			String setTitle = "IOException";
			String errorHeader = "In QuestionsController, deleteQuestionFromDB()";
			String errorText = e.getMessage();
			showErrorDialog(setTitle, errorHeader, errorText);
		}
	}

	/**
	 * 
	 * if NEW Question pressed, open NewQuestionWizzard, than, after pressing "Save
	 * and Close" add newly created question to Questions in THIS, and to DB.
	 * 
	 * @param event
	 *            An event that happens when a teacher press "newQuestion".
	 * 
	 * @throws IOException
	 * 
	 */
	@FXML
	void newQuestionDialog(ActionEvent event) throws IOException {
		runNewQuestionWizzard(null);
	}

	/**
	 * This method is called when pressing New-Question, or Edit-Question buttons.
	 * It opens a new window
	 * 
	 * @param selectedQuestionToEdit
	 *            the selected question to edit, if null - then assume
	 *            "New-Question" pressed, proceed accordingly.
	 */
	private void runNewQuestionWizzard(Question selectedQuestionToEdit) {
		unselectSelectionFromTable();
		Platform.runLater(() -> { // In order to run javaFX thread.(we receive from server a java thread)
			try {
				observebaleNewQuestion = FXCollections.observableArrayList();
				FXMLLoader fxmlLoader;
				String questionId;
				String addNewQuestionfxmlPath;// = "../resources/view/AddQuestionWizzard.fxml";
				CodeSource src = Main.class.getProtectionDomain().getCodeSource();
				URL jarLocation = src.getLocation();
				// InputStream jarPath;
				if (jarLocation.toString().startsWith("jar") || jarLocation.toString().endsWith("jar")) {
					fxmlLoader = new FXMLLoader(
							getClass().getResource("/root/client/resources/view/AddQuestionWizzard.fxml"));
					System.out.println("loading jar's FXML");

				} else {
					/* SHOULD ONLY BE HERE IF IN IDE */
					addNewQuestionfxmlPath = "../resources/view/AddQuestionWizzard.fxml";
					fxmlLoader = new FXMLLoader(getClass().getResource(addNewQuestionfxmlPath));
				}

				Parent parent = fxmlLoader.load();
				AddNewQuestionController dialogController = fxmlLoader.<AddNewQuestionController>getController();
				dialogController.setAppMainObservableList(observebaleNewQuestion);
				dialogController.setUser(this.user);
				dialogController.setUserSubjects(this.userSubjects);
				dialogController.setMainApp(screenManager.getPrimaryStage());
				if (selectedQuestionToEdit != null) {
					dialogController.setQuestion(selectedQuestionToEdit);
				}
				Scene scene = new Scene(parent);
				Stage stage = new Stage();
				stage.initModality(Modality.APPLICATION_MODAL);
				stage.setScene(scene);
				stage.setTitle("New question wizzard");
				stage.showAndWait();

				if (observebaleNewQuestion.isEmpty() == false) { // if false, than no new question created, that means a
																	// question was updated
					if (selectedQuestionToEdit != null) { // if 'selectedQuestionToEdit' changed, remove old question
															// form list's
						String tempOldQID = selectedQuestionToEdit.getQuestionId();
						observabaleQuestions.remove(selectedQuestionToEdit);
						questions.remove(selectedQuestionToEdit);
						questionId = prepareQuestionID(observebaleNewQuestion.get(0).getQuestionId());
						observebaleNewQuestion.get(0).setQuestionId(questionId);
						observabaleQuestions.add(observebaleNewQuestion.get(0));
						questions.add(observebaleNewQuestion.get(0));
						if (tempOldQID.equals(questionId.substring(0, 2))) { // if equal, then updated question didn't
																				// changed its subject
							setChangedQuestion(observebaleNewQuestion.get(0));
						}
						deleteQuestionFromDB(selectedQuestionToEdit); // delete from DB, old question
						putNewQuestion(observebaleNewQuestion.get(0)); // insert updated question
						updateTeacherAssemblerFullName(teachersMap);
					} else { // if "selectedQuestionToEdit" == null, than it means a "NEW Question" pressed.
						questionId = prepareQuestionID(observebaleNewQuestion.get(0).getQuestionId());
						observebaleNewQuestion.get(0).setQuestionId(questionId);
						observabaleQuestions.add(observebaleNewQuestion.get(0));
						questions.add(observebaleNewQuestion.get(0));
						updateTeacherAssemblerFullName(teachersMap);
						putNewQuestion(observebaleNewQuestion.get(0));
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				String setTitle = "IOException";
				String errorHeader = "In QuestionsController, runNewQuestionWizzard()";
				String errorText = e.getMessage();
				showErrorDialog(setTitle, errorHeader, errorText);
			}
		});
	}

	/**
	 *
	 * A method that is responsible for handling messages sent from the server
	 */
	@Override
	public void update(Observable arg0, Object arg1) {
		if (arg1 instanceof QuestionsMessage) {
			if (this.getQuestions().size() == 0)
				this.setQuestions(((QuestionsMessage) arg1).getQuestions()); // only when there no question's - at first
																				// load or a new Teacher.
			else
				addQuestions(((QuestionsMessage) arg1).getQuestions()); // add new questions to a teacher.
			observabaleQuestions = FXCollections.observableArrayList(); // add new question to ObservebaleList
			for (Question question : questions) {
				observabaleQuestions.add(question);
			}
			getTeachersMap(questions); // add newly teacher's ID to teacherMap
			tblQuestions.setItems(observabaleQuestions); // insert newly fetched question's to tblQuestion
		}
		if (arg1 instanceof UserSubjectMessage) {
			this.setUserSubjects(((UserSubjectMessage) arg1).getSubjects());
			fillCombobox(this.userSubjects);
			getUserQuestions(this.userSubjects);
		}
		if (arg1 instanceof UserInfoMessage) {
			for (Question question : questions) {
				String tempTeacherAssembeledID = question.getTeacherAssembeld();
				if (((UserInfoMessage) arg1).getUserInfo().getTeachersMap().containsKey(tempTeacherAssembeledID))
					question.setTeacherFullName(
							((UserInfoMessage) arg1).getUserInfo().getTeachersMap().get(tempTeacherAssembeledID));
				teachersMap = ((UserInfoMessage) arg1).getUserInfo().getTeachersMap(); // update teacherMap to hold new
																						// <teacherID,teacherFullName>.
			}
			updateTeacherAssemblerFullName(teachersMap);
		}
		if (arg1 instanceof SimpleMessage) {
			SimpleMessage simple = (SimpleMessage) arg1;
			log.writeToLog(LogLine.LineType.INFO, "Question deleted");
		}
		if (arg1 instanceof ErrorMessage) {
			String setTitle = "Server error";
			String errorHeader = ((ErrorMessage) arg1).getMsg();
			String errorText = arg1.toString();
			showErrorDialog(setTitle, errorHeader, errorText);
		}
	}

	/**
	 * 
	 * This method called when we need to update in tblQuestions the TeacherName
	 * column
	 */
	private void updateTeacherAssemblerFullName(HashMap<String, String> userInfo) {
		for (Question question : questions) { // update the questions array, to keep updated
			String tempTeacherAssembeledID = question.getTeacherAssembeld();
			if (userInfo.containsKey(tempTeacherAssembeledID)) {
				question.setTeacherFullName(userInfo.get(tempTeacherAssembeledID));
			}
		}
		for (Question obsQuestion : observabaleQuestions) { // update observableQuestion to update the tblQuestions
			String tempTeacherAssembeledID = obsQuestion.getTeacherAssembeld();
			if (userInfo.containsKey(tempTeacherAssembeledID)) {
				obsQuestion.setTeacherFullName(userInfo.get(tempTeacherAssembeledID));
			}
		}
	}

	/**
	 * 
	 * function to create a valid Question ID
	 * 
	 * @param subjectID
	 *            subjectID needed to retrieve all relevant questions
	 * 
	 * @return String a Question valid ID
	 */
	private String prepareQuestionID(String subjectID) {
		String newId = subjectID;
		int newQuestionID = 0;
		for (Question question : this.getQuestions()) {
			String questionID = question.getQuestionId();
			if (subjectID.equals(questionID.substring(0, 2))) {
				int tempId = Integer.parseInt(questionID.substring(2));
				if (newQuestionID <= tempId)
					newQuestionID = tempId;
			}
		}
		newQuestionID++;
		int temp = 100;
		while (temp > newQuestionID) {
			newId = newId.concat("0");
			temp = temp / 10;
		}
		newId = newId.concat(String.valueOf(newQuestionID));
		System.out.println("My tst " + newId);
		return newId;
	}

	/**
	 * 
	 * Here well prepare a message with {"put-new-Question", Question } in order to
	 * insert new Question to DB.
	 * 
	 * @param question
	 *            the question we want to put in DB
	 */
	private void putNewQuestion(Question question) {
		QuestionsMessage newQuestionMessage = (QuestionsMessage) message.getMessage("put-Questions", question); // we
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
			client.sendToServer(newQuestionMessage);
		} catch (IOException e) {
			e.printStackTrace();
			log.writeToLog(LogLine.LineType.ERROR, e.getMessage());
		}
	}

	/**
	 * 
	 * this method is called when updating existing message. here well prepare a
	 * message with {"set-new-Question", Question }
	 * 
	 * @param question
	 *            updated question needed to be inserted the DB
	 */
	private void setChangedQuestion(Question question) {
		QuestionsMessage updatedQuestionMessage = (QuestionsMessage) message.getMessage("set-Questions", question); // we
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
			client.sendToServer(updatedQuestionMessage);
		} catch (IOException e) {
			String setTitle = "IOException";
			String errorHeader = "In QuestionsController, setChangedQuestion()";
			String errorText = e.getMessage();
			showErrorDialog(setTitle, errorHeader, errorText);
		}
	}

	/**
	 * 
	 * Here well get all question that in the same subject of the user
	 * 
	 * @param userSubjects
	 *            this param relevant in order to get relevant teacher questions
	 */
	private void getUserQuestions(ArrayList<Subject> userSubjects) {
		for (Subject subject : userSubjects) {
			QuestionsMessage newQuestionMessage = (QuestionsMessage) message.getMessage("get-Questions", subject);
			try {
				client.sendToServer(newQuestionMessage);
			} catch (IOException e) {
				String setTitle = "IOException";
				String errorHeader = "In QuestionsController, getUserQuestions()";
				String errorText = e.getMessage();
				showErrorDialog(setTitle, errorHeader, errorText);
			}
		}
	}

	/**
	 * 
	 * This method is called in order to get all user teaching subjects - nessecary
	 * to get relevant questions
	 * 
	 * @param user
	 *            the user that needed to get his teaching subjects
	 */
	private void getUserSubjects(User user) {
		UserSubjectMessage newUserSubjectMessage = (UserSubjectMessage) message.getMessage("get-UserSubjects", user);
		try {
			client.sendToServer(newUserSubjectMessage);
		} catch (IOException e) {
			String setTitle = "IOException";
			String errorHeader = "In QuestionsController, getUserSubjects()";
			String errorText = e.getMessage();
			showErrorDialog(setTitle, errorHeader, errorText);
		}
	}

	public ArrayList<Subject> getUserSubjects() {
		return userSubjects;
	}

	public void setUserSubjects(ArrayList<Subject> userSubjects) {
		this.userSubjects = userSubjects;
	}

	/**
	 * this moethod filling the teacher teaching subjects combobox
	 * 
	 * @param teacherSubject
	 *            the subjects to enter the combobox
	 */
	private void fillCombobox(ArrayList<Subject> teacherSubject) {
		observableSubjects = FXCollections.observableArrayList(teacherSubject);
		subjectCombobox.getItems().add(new Subject("00", "Show all Questions")); // DUMMY subject, for showing all table
																					// rows
		subjectCombobox.getItems().addAll(observableSubjects);
	}

	/**
	 * @return the questions
	 */
	public ArrayList<Question> getQuestions() {
		return questions;
	}

	/**
	 * @param questions
	 *            the questions to set
	 */
	public void setQuestions(ArrayList<Question> questions) {
		this.questions = questions;
	}

	/**
	 *
	 * A method that is responsible for adding questions recieved from server into
	 * this.questions
	 * 
	 * @param questions
	 *            recieved from server
	 */
	public void addQuestions(ArrayList<Question> questions) {
		Boolean identFlag = false;
		for (Question question : questions) {
			for (Question question2 : this.questions) {
				if (question.getQuestionId().equals(question2.getQuestionId())) {
					identFlag = true;
				}
			}
			if (identFlag == false) {
				this.getQuestions().add(question);
			}
		}
	}

	/**
	 * 
	 * 
	 * This method called when one want's to deSelect selection from table
	 */
	private void unselectSelectionFromTable() {
		int selectedIndex = tblQuestions.getSelectionModel().getSelectedIndex();
		tblQuestions.getSelectionModel().clearSelection(selectedIndex);
		btnSearch.setDisable(true);
		deleteQuestion.setDisable(true);
	}

	/**
	 * 
	 * 
	 * This method called when initializing this window.
	 */
	private void setUserDetails(User user1) {
		teacherIDLbl.setText(user.getUserID());
		fstNameLbl.setText(user.getUserFirstName());
		lstNamelbl.setText(user.getUserLastName());
		TeacherPremissionLbl.setText(user.getUserPremission());
	}

}
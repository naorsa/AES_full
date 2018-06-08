package root.client.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.util.converter.IntegerStringConverter;
import ocsf.client.ObservableClient;
import root.client.managers.DataKeepManager;
import root.client.managers.ScreensManager;
import root.dao.app.Exam;
import root.dao.app.QuestionInExam;
import root.dao.app.Question;
import root.dao.message.ExamMessage;
import root.dao.message.MessageFactory;
import root.dao.message.QuestionInExamMessage;
import root.dao.message.SimpleMessage;
import root.util.log.Log;
import root.util.log.LogLine;

public class UpdateExamController implements Observer {

	@FXML
	private AnchorPane rootPane;

	@FXML
	private TableColumn<root.dao.app.QuestionInExam, Integer> tbcPoints;

	@FXML
	private TableColumn<root.dao.app.QuestionInExam, Question> tbcQuestion;

	@FXML
	private TableView<root.dao.app.QuestionInExam> tblQuestion;

	private MessageFactory messageFact;
	private ObservableClient client;
	private ScreensManager screenManager;
	private Log log;
	private DataKeepManager dbk;
	private Exam updateExam;
	private ObservableList<root.dao.app.QuestionInExam> examQuestions;

	/**
	 * Update the question in the specific exam
	 * 
	 * @param event
	 */
	@FXML
	void UpdateQuestionInExam(ActionEvent event) {
		ArrayList<QuestionInExam> examInQuestions = new ArrayList<QuestionInExam>();
		for (QuestionInExam q : examQuestions) {
			examInQuestions.add(q);
		}
		QuestionInExamMessage sendMessage = (QuestionInExamMessage) messageFact.getMessage("put-questioninexam",
				examInQuestions);
		sendMessage.setExamId(updateExam.getExamId());
		try {
			client.sendToServer(sendMessage);
		} catch (IOException e) {
			log.writeToLog(LogLine.LineType.ERROR, e.getMessage());
			e.printStackTrace();
		}

	}

	/**
	 * Delete question from the exam
	 * 
	 * @param event
	 */
	@FXML
	void RemoveQuestionFromExam(ActionEvent event) {
		ObservableList<QuestionInExam> questionSelected;
		questionSelected = tblQuestion.getSelectionModel().getSelectedItems();
		QuestionInExam q = questionSelected.get(0);
		examQuestions.remove(q);
		tblQuestion.setItems(examQuestions);

	}

	/**
	 * This method occurs when the window is shown up.
	 * 
	 * @throws IOException
	 *             if the window cannot be shown
	 */
	@FXML
	public void initialize() throws IOException {
		messageFact = MessageFactory.getInstance();
		screenManager = ScreensManager.getInstance();
		log = Log.getInstance();
		dbk = DataKeepManager.getInstance();
		client = new ObservableClient("localhost", 8000);
		client.addObserver(this);
		client.openConnection();
		tbcPoints.setCellValueFactory(new PropertyValueFactory("questionGrade"));
		tbcQuestion.setCellValueFactory(new PropertyValueFactory("question"));
		tblQuestion.setEditable(true);
		tbcPoints.setCellFactory(
				TextFieldTableCell.<root.dao.app.QuestionInExam, Integer>forTableColumn(new IntegerStringConverter()));
		updateExam = (Exam) dbk.getObject("updateExam");
		examQuestions = FXCollections.observableArrayList();
		ArrayList<root.dao.app.QuestionInExam> questionList = updateExam.getExamQuestions();
		for (root.dao.app.QuestionInExam q : questionList) {
			examQuestions.add(q);
		}
		tblQuestion.setItems(examQuestions);

	}
	
    @FXML
    void updatePoints(TableColumn.CellEditEvent<QuestionInExam, Integer> pointEditEvent) {
    	int i =0;
    	QuestionInExam question = tblQuestion.getSelectionModel().getSelectedItem();
		Integer newValue = pointEditEvent.getNewValue();
		question.setQuestionGrade(newValue);
		for(QuestionInExam q: examQuestions){
			if(q.getQuestion().getQuestionId().equals(question.getQuestion().getQuestionId()))
			{
				examQuestions.set(i, question);
				break;
			}
			i++;
		}
		
    }

	/**
	 * This method occurs when the server send message to the client
	 */
	@Override
	public void update(Observable arg0, Object arg1) {
		if (arg1 instanceof SimpleMessage) {
			SimpleMessage simple = (SimpleMessage) arg1;
			log.writeToLog(LogLine.LineType.INFO, "Exam updated");
			Platform.runLater(() -> { // In order to run javaFX thread.(we recieve from server a java thread)
				try {
					screenManager.activate("home");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.writeToLog(LogLine.LineType.ERROR, e.getMessage());
				}
			});
		}
	}

}
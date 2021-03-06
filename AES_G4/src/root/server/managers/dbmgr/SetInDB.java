package root.server.managers.dbmgr;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import root.dao.app.AlterDuration;
import root.dao.app.Course;
import root.dao.app.Exam;
import root.dao.app.ExecuteExam;
import root.dao.app.Question;
import root.dao.app.QuestionInExam;
import root.dao.app.SolvedExams;
import root.dao.app.Statistic;
import root.dao.app.Subject;
import root.dao.app.User;
import root.dao.message.AbstractMessage;
import root.dao.message.MessageFactory;
import root.dao.message.QuestionsMessage;
import root.server.AES_Server;
import root.server.managers.SolvedExamsFinishedStatistics;
import root.util.log.Log;
import root.util.log.LogLine;
import root.util.log.LogLine.LineType;

/**
 * The SetInDB is a class that responsible for modify, or add data to the
 * database
 * 
 * @author Omer Haimovich
 *
 */
public class SetInDB {
	// Instance variables **********************************************

	/**
	 * A sentence designed for queries
	 */
	private java.sql.Statement stmt;

	/**
	 * A sentence designed for queries and has dynamic variables
	 */
	private PreparedStatement newStmt;
	/**
	 * 
	 * Connection between the database and the server
	 */
	private Connection conn;
	/**
	 * 
	 * A log file that is responsible for documenting the actions performed in the
	 * application
	 */
	private Log log;
	/**
	 * 
	 * Generates new messages that link the server to the client
	 */
	private MessageFactory message;

	// CONSTRUCTORS *****************************************************

	/**
	 * Constructs the SetInDB
	 */
	public SetInDB() {
		super();
		conn = AES_Server.getConnection();
		log.getInstance();
		message = MessageFactory.getInstance();
	}

	// CLASS METHODS *************************************************

	/**
	 * 
	 * A method that update in the database in the solved exam table the cheating
	 * flag column
	 * 
	 * @author Amit Molek
	 * @param grade
	 *            the id of the teacher who executed the exam
	 * @param expl
	 *            the exam id
	 * @param teacher_id
	 *            true if the student cheated or false if not cheated
	 * @param se
	 *            teacher_id true if the student cheated or false if not cheated
	 * @return true if the updated successful and false otherwise
	 */
	public boolean updateSolvedExamGrade_Approval_Explenation_ApprovingTeacherID(int grade, String expl,
			String teacher_id, SolvedExams se) {
		String updateQuery = "UPDATE `solved exams` SET solved_exam_grade = ?, grade_alteration_explanation = ?,"
				+ "approving_teacher_id = ?, grade_approval_by_teacher = ? WHERE User_ID = ? AND exam_ID = ? AND exam_executing_Date = ?;";

		try {
			newStmt = conn.prepareStatement(updateQuery);
			newStmt.setInt(1, grade);
			newStmt.setString(2, expl);
			newStmt.setString(3, teacher_id);
			newStmt.setString(4, "approved");
			newStmt.setString(5, se.getSovingStudentID());
			newStmt.setString(6, se.getExamID());
			newStmt.setTimestamp(7, se.getExamDateTime());
			newStmt.execute();

			new SolvedExamsFinishedStatistics(se.getExamID());
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 
	 * A method that update in the database in the solved exam table the cheating
	 * flag column
	 * 
	 * @author Amit Molek
	 * @param user_id
	 *            the id of the teacher who executed the exam
	 * @param exam_id
	 *            the exam id
	 * @param cheated
	 *            true if the student cheated or false if not cheated
	 * @return true if the updated successful and false otherwise
	 */
	public boolean updateSolvedExamCheatingFlag(String user_id, String exam_id, boolean cheated) {
		String cheatedStr = "no";
		if (cheated)
			cheatedStr = "yes";
		String updateQuery = "UPDATE `solved exams` SET cheating_flag = ? WHERE User_ID = ? AND exam_ID = ?;";

		try {
			newStmt = conn.prepareStatement(updateQuery);
			newStmt.setString(1, cheatedStr);
			newStmt.setString(2, user_id);
			newStmt.setString(3, exam_id);
			newStmt.execute();

			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * A method that adds a exam to the database to the exam table
	 * 
	 * @author Omer Haimovich
	 * 
	 * @param exam
	 *            the exam that should be added
	 * @return abstract message if the exam added successful
	 */
	public AbstractMessage AddExam(Exam exam) {
		String insertExam = "insert into exams (exam_id, teacher_assembler_id, exam_original_allocated_duration, exams_state, lock_flag)"
				+ " values (?, ?, ?, ?, ?);";
		try {
			newStmt = conn.prepareStatement(insertExam);
			newStmt.setString(1, exam.getExamId());
			newStmt.setString(2, exam.getAuthor().getUserID());
			newStmt.setInt(3, exam.getExamDuration());
			newStmt.setString(4, "clean");
			newStmt.setString(5, "locked");
			newStmt.execute();
			String insertQuestionInExam = "insert into `questions in exam` (exam_ID, Question_ID, Question_Grade, Question_Free_text_Student, Question_Free_text_Teacher)"
					+ " values (?, ?, ?, ?, ?);";
			newStmt = conn.prepareStatement(insertQuestionInExam);
			ArrayList<QuestionInExam> examQuestions = exam.getExamQuestions();
			for (QuestionInExam q : examQuestions) {
				newStmt.setString(1, exam.getExamId());
				newStmt.setString(2, q.getQuestion().getQuestionId());
				newStmt.setInt(3, q.getQuestionGrade());
				newStmt.setString(4, q.getFreeTextForStudent());
				newStmt.setString(5, q.getFreeTextForTeacher());
				newStmt.execute();
			}
			return message.getMessage("ok-put-exams", null); // because we didnt needed to get from DB theres nothing to
																// send back to client but the confirmation

		} catch (SQLException e) {
			// log.writeToLog(LogLine.LineType.ERROR, e.getMessage());
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * Add statistics about the solved exam to the database
	 * 
	 * @param examStatistic
	 *            the Statistics object filled with all the exam statistical data
	 */
	public void addSolvedExamStatistics(Statistic examStatistic) {
		String insertQuery = "INSERT INTO `exams stats`("
				+ "exam_ID, exam_date, real_time_duration, submitted_students_counter, interrupted_students_counter, students_started_counter,"
				+ "exams_avg, exams_median, grade_derivative_0_10, grade_derivative_11_20, grade_derivative_21_30, grade_derivative_31_40,"
				+ "grade_derivative_41_50, grade_derivative_51_60, grade_derivative_61_70, grade_derivative_71_80, grade_derivative_81_90,"
				+ "grade_derivative_91_100)" + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

		try {
			newStmt = conn.prepareStatement(insertQuery);

			newStmt.setString(1, examStatistic.getExam_ID());
			newStmt.setTimestamp(2, examStatistic.getDateTime());
			newStmt.setInt(3, Integer.parseInt(examStatistic.getReal_time_duration()));
			newStmt.setInt(4, examStatistic.getSubmitted_students_counter());
			newStmt.setInt(5, examStatistic.getInterrupted_students_counter());
			newStmt.setInt(6, examStatistic.getStudents_started_counter());
			newStmt.setDouble(7, examStatistic.getExams_avg());
			newStmt.setDouble(8, examStatistic.getExams_median());

			newStmt.setDouble(9, examStatistic.getGrade_derivative_0_10());
			newStmt.setDouble(10, examStatistic.getGrade_derivative_11_20());
			newStmt.setDouble(11, examStatistic.getGrade_derivative_21_30());
			newStmt.setDouble(12, examStatistic.getGrade_derivative_31_40());
			newStmt.setDouble(13, examStatistic.getGrade_derivative_41_50());
			newStmt.setDouble(14, examStatistic.getGrade_derivative_51_60());
			newStmt.setDouble(15, examStatistic.getGrade_derivative_61_70());
			newStmt.setDouble(16, examStatistic.getGrade_derivative_71_80());
			newStmt.setDouble(17, examStatistic.getGrade_derivative_81_90());
			newStmt.setDouble(18, examStatistic.getGrade_derivative_91_100());
			newStmt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
	}

	/**
	 * A method that adds a question to the database to the question table
	 * 
	 * @author gal
	 * @param newQuestionTooAdd
	 *            the question that should be added
	 * @return abstract message if the question added successful
	 */
	public AbstractMessage AddNewQuestion(Question newQuestionTooAdd) {
		String insertQuestion = "INSERT INTO `aes`.`questions`(`question_id`,`question_text`,`question_instruction`,`question_answer_1`,`question_answer_2`,`question_answer_3`,`question_answer_4`,`correct_question`,`teacher_assembeld_id`)"
				+ "VALUES(?,?,?,?,?,?,?,?,?);";

		try {
			newStmt = conn.prepareStatement(insertQuestion);
			newStmt.setString(1, newQuestionTooAdd.getQuestionId());
			newStmt.setString(2, newQuestionTooAdd.getQuestionText());
			newStmt.setString(3, newQuestionTooAdd.getIdquestionIntruction());
			newStmt.setString(4, newQuestionTooAdd.getAns1());
			newStmt.setString(5, newQuestionTooAdd.getAns2());
			newStmt.setString(6, newQuestionTooAdd.getAns3());
			newStmt.setString(7, newQuestionTooAdd.getAns4());
			newStmt.setInt(8, newQuestionTooAdd.getCorrectAns());
			newStmt.setString(9, newQuestionTooAdd.getTeacherAssembeld());
			newStmt.execute();
			return message.getMessage("ok-put-questions", null); // because we didnt needed to get from DB theres
																	// nothing to send back to client but the
																	// confirmation

		} catch (SQLException e) {
			// log.writeToLog(LogLine.LineType.ERROR, e.getMessage());
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * A method that deletes an exam from a database
	 * 
	 * @author Omer Haimovich
	 * @param exam
	 *            the exam that should be deleted
	 * @return abstract message if the exam deleted successful
	 */
	public AbstractMessage deleteTheExam(Exam exam) {
		String deleteExam = "delete from exams where exam_id = " + exam.getExamId();
		try {
			newStmt = conn.prepareStatement(deleteExam + ";");
			newStmt.execute();
			return message.getMessage("ok-delete-exams", null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * A method that deletes a question from a database
	 * 
	 * @author gal
	 * @param Question
	 *            the question that should be deleted
	 * @return abstract message if the question deleted successful
	 */
	public AbstractMessage deleteTheQuestion(Question Question) {
		String deleteQuestion = "delete from questions where question_id = " + Question.getQuestionId();
		try {
			newStmt = conn.prepareStatement(deleteQuestion + ";");
			newStmt.execute();
			return message.getMessage("ok-delete-questions", null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * A method that update in the database in the question table
	 * 
	 * @author gal
	 * @param questionMessage
	 *            the question that should be updated
	 * @return abstract message if the question updated successful
	 */
	public AbstractMessage updateExistingQuestion(Question questionMessage) {
		String updateQuestion = "UPDATE questions SET " + " `question_id` = ?," + " `question_text` = ?,"
				+ " `question_instruction` = ?," + " `question_answer_1` = ?," + " `question_answer_2` = ?,"
				+ " `question_answer_3` = ?," + " `question_answer_4` = ?," + " `correct_question` = ?,"
				+ " `teacher_assembeld_id` = ?" + " WHERE `question_id` = ?;";
		System.out.println(questionMessage);
		System.out.println(updateQuestion);

		// +"SELECT * FROM aes.questions;";
		try {
			newStmt = conn.prepareStatement(updateQuestion);
			newStmt.setString(1, questionMessage.getQuestionId());
			newStmt.setString(2, questionMessage.getQuestionText());
			newStmt.setString(3, questionMessage.getIdquestionIntruction());
			newStmt.setString(4, questionMessage.getAns1());
			newStmt.setString(5, questionMessage.getAns2());
			newStmt.setString(6, questionMessage.getAns3());
			newStmt.setString(7, questionMessage.getAns4());
			newStmt.setInt(8, questionMessage.getCorrectAns());
			newStmt.setString(9, questionMessage.getTeacherAssembeld());
			newStmt.setString(10, questionMessage.getQuestionId());
			newStmt.execute();
			return message.getMessage("ok-set-questions", null); // because we didnt needed to get from DB theres
																	// nothing to send back to client but the
																	// confirmation

		} catch (SQLException e) {
			// log.writeToLog(LogLine.LineType.ERROR, e.getMessage());
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * A method that add to the database in a question in exam table
	 * 
	 * @author Omer Haimovich
	 * @param id
	 *            the exam id
	 * @param quest
	 *            list of all question in the exam
	 * @return abstract message if the question added successful
	 */
	public AbstractMessage addQuestionToExam(String id, ArrayList<QuestionInExam> quest) {
		deleteQuestionInExam(id);
		String insertQuestionInExam = "insert into `questions in exam` (exam_ID, Question_ID, Question_Grade, Question_Free_text_Student, Question_Free_text_Teacher)"
				+ " values (?, ?, ?, ?, ?);";
		try {
			newStmt = conn.prepareStatement(insertQuestionInExam);
			for (QuestionInExam q : quest) {
				newStmt.setString(1, id);
				newStmt.setString(2, q.getQuestion().getQuestionId());
				newStmt.setInt(3, q.getQuestionGrade());
				newStmt.setString(4, q.getFreeTextForStudent());
				newStmt.setString(5, q.getFreeTextForTeacher());
				newStmt.execute();
			}
			return message.getMessage("ok-put-questioninexam", null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * A method that deletes from the database in a question in exam table
	 * 
	 * @author Omer Haimovich
	 * @param id
	 *            the exam id
	 * @return abstract message if the question deleted successful
	 */
	public AbstractMessage deleteQuestionInExam(String id) {
		String deleteExam = "delete from `questions in exam` where exam_ID = " + id;
		try {
			newStmt = conn.prepareStatement(deleteExam + ";");
			newStmt.execute();
			return message.getMessage("ok-delete-questioninexam", null);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * A method that add to the database in a solved exams table
	 * 
	 * @author Naor Saadia
	 * @param newExam
	 *            the solved exam
	 * @return abstract message if the solved exam added successful
	 */
	public AbstractMessage addSolvedExam(SolvedExams newExam) {
		String insertSolvedExam = "insert into `solved exams` (User_ID, exam_ID, solved_exam_grade, solve_duration_timer, submitted_or_interrupted_Flag, exam_executing_Date)"
				+ " values (?, ?, ?, ?, ?, ?);";
		try {
			newStmt = conn.prepareStatement(insertSolvedExam);
			java.sql.Date dateDB = new java.sql.Date(newExam.getDate().getTime());
			newStmt.setString(1, newExam.getSovingStudentID());
			newStmt.setString(2, newExam.getExamID());
			newStmt.setInt(3, newExam.getExamGrade());
			newStmt.setInt(4, newExam.getSolveDurationTime());
			newStmt.setString(5, newExam.getSubmittedOrInterruptedFlag());
			newStmt.setDate(6, newExam.getDate());
			newStmt.execute();
			return message.getMessage("ok-put-solvedexams", null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * A method that add to the database in a execute exams table
	 * 
	 * @author Naor Saadia
	 * @param newExam
	 *            the execute exam
	 * @return abstract message if the execute exam added successful
	 */
	public AbstractMessage addExecuteExam(ExecuteExam newExam) {
		String insertExecuteExam = "insert into `execute exams` (exam_id, exam_date_start, four_Digit, exam_type,executining_teacher_ID)"
				+ " values (?, ?, ?, ?, ?);";
		try {
			newStmt = conn.prepareStatement(insertExecuteExam);
			newStmt.setString(1, newExam.getExamId());
			newStmt.setString(2, newExam.getStartTime());
			newStmt.setString(3, newExam.getExamPassword());
			newStmt.setString(4, newExam.getExamType());
			newStmt.setString(5, newExam.getTeacherId());
			newStmt.execute();
			return message.getMessage("ok-put-executeexam", null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * A method that update in the database in the exams table the lock flag column
	 * and the exam state column
	 * 
	 * @param newExam
	 *            the exam that should be updated
	 * @return abstract message if the exam updated successful
	 */
	public AbstractMessage updateExam(Exam newExam) {
		String updateExam = "UPDATE exams SET exams_state = ?, lock_flag = ? WHERE exam_id = ?;";
		try {
			newStmt = conn.prepareStatement(updateExam);
			newStmt.setString(1, "dirty");
			newStmt.setString(2, "unlocked");
			newStmt.setString(3, newExam.getExamId());
			newStmt.execute();
			return message.getMessage("ok-set-exams", null); // because we didnt needed to get from DB theres
																// nothing to send back to client but the
																// confirmation

		} catch (SQLException e) {
			// log.writeToLog(LogLine.LineType.ERROR, e.getMessage());
			e.printStackTrace();
		}

		return null;

	}

	/**
	 * A method that deletes from the database in a solved exam table
	 * 
	 * @author Naor Saadia
	 * @param examId
	 *            the solved exam id
	 */
	public void deleteSolvedExam(String examId) {
		String deleteExam = "delete from `solved exams` where exam_id = " + examId;
		try {
			newStmt = conn.prepareStatement(deleteExam);
			newStmt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * A method that deletes from the database in a execute exams table
	 * 
	 * @author Naor Saadia
	 * @param examId
	 *            the execute exam id
	 */
	public void deleteExecutedExam(String examId) {
		String deleteExam = "delete from `execute exams` where exam_id = " + examId;
		try {
			newStmt = conn.prepareStatement(deleteExam);
			newStmt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * A method that update in the database in the exams table the lock flag column
	 * 
	 * @param examId
	 *            the id of the exam that should be updated
	 */
	public void lockExam(String examId) {
		String updateExam = "UPDATE exams SET lock_flag = ? WHERE exam_id = ?;";

		try {
			newStmt = conn.prepareStatement(updateExam);
			newStmt.setString(1, "locked");
			newStmt.setString(2, examId);
			newStmt.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
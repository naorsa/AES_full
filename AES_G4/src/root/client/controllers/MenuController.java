package root.client.controllers;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import root.client.managers.DataKeepManager;
import root.client.managers.ScreensManager;
import root.dao.app.User;
import root.util.log.Log;
import root.util.log.LogLine.LineType;

/**
 * The controller class of the menu screen b
 * @author Amit Molek
 *
 */

public class MenuController {

    @FXML
    private MenuBar menuBar;

    private User user;
    
    private ScreensManager screensMgr;
    private Log log;
  
    private final String mainPath = "/root/client";
    private final String homeIconPath = mainPath + "/resources/images/icons/home.png";
    private final String returnIconPath = mainPath + "/resources/images/icons/back_arrow.png";
    private final String helpIconPath = mainPath + "/resources/images/icons/help.png";
    private final String statisticsIconPath = mainPath + "/resources/images/icons/history.png";
    private final String questionsIconPath = mainPath + "/resources/images/icons/aspect_ratio.png";
    private final String examsIconPath = mainPath + "/resources/images/icons/collections.png";
    private final String executeExamIconPath = mainPath + "/resources/images/icons/play.png";
    
    private final String aboutUsNames = "Group 4\nGal Brandwine\nAlon Ben-yosef\nNaor Saadia\nAmit Molek\nOmer Haimovich";
    
    /**
     * FXML init func
     * Used to init the whole screen
     * @throws IOException if the menu could not be initialized, image could not be loaded
     */
    @FXML
	public void initialize() throws IOException{
    	user = DataKeepManager.getInstance().getUser();
    	screensMgr = ScreensManager.getInstance();
    	log = Log.getInstance();

    	initMenuBtn();
    }
    
    /**
     * Calls all the creation functions of the menu buttons
     */
    public void initMenuBtn() {
    	initReturnMenu();
    	initGoHome();
    	initExamsMenu();
    	if (user.getUserPremission().equals("Teacher")) {
        	initQuestionsMenu();
    	}
    	initExecuteExamMenu();
    	initStatisticsMenu();
    	initGoHelp();
    }
    
    /**
     * Used to init the execute exam menu
     */
    public void initExecuteExamMenu() {
    	if (!user.getUserPremission().equals("Principal")) {
    		Image img = new Image(getClass().getResourceAsStream(executeExamIconPath));
	    	Menu executeMenu = new Menu();
	    	createMenuItem(executeMenu, img, "Execute Exam", 115, 25);
	    	
	    	if (user.getUserPremission().equals("Student")) {
	    		executeMenu.getItems().add(createMenuItem("Solve exam", getChangeScreenActionEvent("Enter4digitPassword")));
	    	}else if (user.getUserPremission().equals("Teacher")) {
	    		executeMenu.getItems().add(createMenuItem("Start exam", getChangeScreenActionEvent("PrepareExam")));
	    		executeMenu.getItems().add(createMenuItem("Change exam's duration", getChangeScreenActionEvent("changeDurations")));
	    	}
    	}
    }
    
    /**
     * Used to init the exams menu
     */
    public void initExamsMenu() {
    	Image img = new Image(getClass().getResourceAsStream(examsIconPath));
    	
    	Menu examsMenu = new Menu();
    	createMenuItem(examsMenu, img, "Exams", 70, 25);
    	
    	if (user.getUserPremission().equals("Student")) {
    		examsMenu.getItems().add(createMenuItem("View exams", getChangeScreenActionEvent("solvedExams")));
    	}else if (user.getUserPremission().equals("Teacher")) {
        	examsMenu.getItems().add(createMenuItem("Add exam", getChangeScreenActionEvent("addExam")));
        	examsMenu.getItems().add(createMenuItem("Update exam", getChangeScreenActionEvent("updateDeleteExam")));
        	examsMenu.getItems().add(createMenuItem("View solved exams", getChangeScreenActionEvent("teacherExamsView")));
    	}
    }
    
    /**
     * Used to init the questions menu
     */
    public void initQuestionsMenu() {
    	Image img = new Image(getClass().getResourceAsStream(questionsIconPath));
    	
    	EventHandler<MouseEvent> e = getChangeScreenMouseEvent("questions");
    	createMenuItem(new Menu(), img, "Questions", 100, 25, e);
    }
    
    /**
     * Used to init the statistics menu
     */
    public void initStatisticsMenu() {
    	String per = user.getUserPremission();
    	Image img = new Image(getClass().getResourceAsStream(statisticsIconPath));
    	
    	Menu statisticsMenu = new Menu();
    	
    	String name = "Grade stats";
    	if (per.equals("Principal")) {
    		statisticsMenu.getItems().add(createMenuItem(name, getChangeScreenActionEvent("testPrincipalGradesStats")));
    	}else if (per.equals("Teacher")) {
    		statisticsMenu.getItems().add(createMenuItem(name, getChangeScreenActionEvent("testTeacherGradesStats")));
    	}
    	
    	createMenuItem(statisticsMenu, img, "Statistics", 90, 25);
    }
    
    /**
     * Creates the return menu object
     */
    public void initReturnMenu() {
    	Image img = new Image(getClass().getResourceAsStream(returnIconPath));
    	
    	EventHandler<MouseEvent> e = new EventHandler<MouseEvent>() {
    		@Override
    		public void handle(MouseEvent event) {
    			try {
    				screensMgr.returnToPrevScreen();
				} catch (IOException e) {
					log.writeToLog(LineType.ERROR, "Failed returning to previous screen");
					e.printStackTrace();
				}
    		}
		};
    	
		Menu returnMenu = new Menu();
    	createMenuItem(returnMenu, img, "", 30, 25, e);
    	if (screensMgr.getScreenStackSize() < 1) {
    		returnMenu.setDisable(true);
    	}
    }
    
    /**
     * Creates the home menu object
     */
    public void initGoHome() {
    	Image img = new Image(getClass().getResourceAsStream(homeIconPath));
    	EventHandler<MouseEvent> e = getChangeScreenMouseEvent("home");
    	createMenuItem(new Menu(), img, "Home", 70, 25, e);
    }
  
    /**
     * Init the help menu object, with about us
     */
    public void initGoHelp() {
    	Menu help = new Menu();
    	
    	Image img = new Image(getClass().getResourceAsStream(helpIconPath));
    	createMenuItem(help, img, "Help", 70, 25);
    	
    	EventHandler<ActionEvent> e = new EventHandler<ActionEvent>() {
    		@Override
    		public void handle(ActionEvent event) {
    			Stage aboutStage = new Stage();
    			VBox comp = new VBox();
    			
    			Image img = new Image(getClass().getResourceAsStream(helpIconPath));
    			
    			Label aboutGroup = new Label();
    			aboutGroup.setText(aboutUsNames);
    			aboutGroup.setTextAlignment(TextAlignment.CENTER);
    			comp.setAlignment(Pos.CENTER);
    			comp.getChildren().add(aboutGroup);
    			
    			Scene aboutStageScene = new Scene(comp, 300, 125);
    			aboutStage.setScene(aboutStageScene);
    			aboutStage.setTitle("About Us");
    			aboutStage.getIcons().add(img);
    			aboutStage.setResizable(false);
    			aboutStage.show();
    		}
		};
    	
    	help.getItems().add(createMenuItem("About Us", e));
    }
    
    /**
     * Returns a MenuItem with txtLbl text, and a event when you action click it
     * @param txtLbl the text you want the menu item to display
     * @param e the event you want to happen when you click the menu item
     * @return the MenuItem with txtLbl and e event
     */
    private MenuItem createMenuItem(String txtLbl, EventHandler<ActionEvent> e) {
    	MenuItem item = new MenuItem(txtLbl);
    	item.setOnAction(e);
    	return item;
    } 
    
    /**
     * Build a new FlowPane with an img and text (sets the menu object design)
     * @param menu the menu you want to "design"
     * @param btnImg the icon you want to display in the menu object
     * @param txtLbl the text you want the menu to display
     * @param maxWidth the maximum width of the menu object
     * @param imgFitSize the size of the icon
     * @return FlowPane with all the objects attached to it
     */
    private FlowPane createMenu_Structure(Menu menu, Image btnImg, String txtLbl, double maxWidth, double imgFitSize) {
    	FlowPane menuPane = new FlowPane();
    	menuPane.setMaxWidth(maxWidth);
    	
    	ImageView img = new ImageView(btnImg);
    	img.setFitHeight(imgFitSize);
    	img.setFitWidth(imgFitSize);
    	
    	Label lbl = new Label(" " + txtLbl);
    	lbl.getStyleClass().add("label");
    	
    	menuPane.getChildren().add(img);
    	menuPane.getChildren().add(lbl);
    	menu.setGraphic(menuPane);
    	return menuPane;
    }
    
    /**
     * Creates a menu object that and adds it to the menu bar (not a clickable menu object)
     * @param menu the menu object you want to add to the menu bar
     * @param btnImg the icon you want to display in the menu object
     * @param txtLbl the text you want the menu to display
     * @param maxWidth the maximum width of the menu object
     * @param imgFitSize the size of the icon
     */
    private void createMenuItem(Menu menu, Image btnImg, String txtLbl, double maxWidth, double imgFitSize) {
    	createMenu_Structure(menu, btnImg, txtLbl, maxWidth, imgFitSize);
    	menuBar.getMenus().add(menu);
    }
    
    /**
     * Creates a clickable menu object and adds it to the menu bar
     * @param menu the menu object you want to add to the menu bar
     * @param btnImg the icon you want to display in the menu object
     * @param txtLbl the text you want the menu to display
     * @param maxWidth the maximum width of the menu object
     * @param imgFitSize the size of the icon
     * @param e the event you want to happen when you click the menu object
     */
    private void createMenuItem(Menu menu, Image btnImg, String txtLbl, double maxWidth, double imgFitSize, EventHandler<MouseEvent> e) {
    	FlowPane menuPane = createMenu_Structure(menu, btnImg, txtLbl, maxWidth, imgFitSize);
    	
    	menuPane.setOnMouseClicked(e);
    	menuBar.getMenus().add(menu);
    }
    
    /**
     * Return an action event that switches between screens
     * @param screenName the screen name you want to switch to
     * @return action event of the screen switching
     */
    private EventHandler<ActionEvent> getChangeScreenActionEvent(String screenName){
    	EventHandler<ActionEvent> e = new EventHandler<ActionEvent>() {
    		@Override
    		public void handle(ActionEvent event) {
    			screensMgr.switchScreen(screenName);
    		}
		};
		return e;
    }
    
    /**
     * Returns an mouse event that switches between screens
     * @param screenName the screen name you want to switch to
     * @return mouse event of the screen switching
     */
    private EventHandler<MouseEvent> getChangeScreenMouseEvent(String screenName){
    	EventHandler<MouseEvent> e = new EventHandler<MouseEvent>() {
    		@Override
    		public void handle(MouseEvent event) {
    			screensMgr.switchScreen(screenName);
    		}
		};
		return e;
    }
    
}
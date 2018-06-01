package root.dao.message;


import root.dao.app.LoginInfo;
import root.dao.app.User;

public class LoginMessage extends AbstractMessage {
	private LoginInfo user;
	
	public LoginInfo getUser() {
		return user;
	}

	public void setUser(LoginInfo user) {
		this.user = user;
	}

	public LoginMessage(LoginInfo user) {
		super("login");
		this.user = user;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return "Login";
	}

}

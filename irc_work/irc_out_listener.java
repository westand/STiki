package irc_work;

import org.schwering.irc.lib.IRCEventAdapter;
import org.schwering.irc.lib.IRCModeParser;
import org.schwering.irc.lib.IRCUser;

/**
 * Andrew G. West - irc_out_listener.java - An IRCEventListener customized
 * for STiki output feeds. The issuing of initialization commands at rapid
 * speed is unsufficient -- sometimes one must wait for a response -- or
 * subsequent commands will break. Thus, via this class, we install handlers
 * for those responses -- and handlers that spin until they are received.
 * 
 * There are undoubtedly more elegant ways to do this. However, given
 * our minimal needs for this class -- this seems sufficient.
 */
public class irc_out_listener extends IRCEventAdapter{
	
	// ***************************** PUBLIC FIELDS ***************************
	
	/**
	 * Number of milliseconds to wait between repetitive checks to see
	 * if a desired response has been received from the IRC server.
	 */
	public final int SPIN_INTERVAL_MS = 10;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Has a "mode" message been received since last reset of this var?
	 */
	private boolean have_mode = false;
	
	/**
	 * Has a "notice" message been received since last reset of this var?
	 */
	private boolean have_notice = false;
	
	/**
	 * Last numerical "reply" code received, or -1 if none since last reset.
	 */
	private int last_reply_code = -1;
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Spin on this method until a "reply" is received with some integer code.
	 * @param code Integer code of "reply" to break loop.
	 */
	public void await_reply(int code){
		try{while(last_reply_code != code)
			Thread.sleep(SPIN_INTERVAL_MS);
		reset_flags();
		} catch(Exception e){} // Only if interrupted
	}
		 
	/**
	 * Spin on this method until a "notice" messsage is received.
	 */
	public void await_notice(){
		try{while(!have_notice)
			Thread.sleep(SPIN_INTERVAL_MS);
		reset_flags();
		} catch(Exception e){} // Only if interrupted
	}
	 
	/**
	 * Spin on this method until a "notice" or "mode" message is received.
	 */
	public void await_notice_or_mode(){
		try{while(!have_notice && !have_mode)
			Thread.sleep(SPIN_INTERVAL_MS);
		reset_flags();
		} catch(Exception e){} // Only if interrupted
	}
		
	/**
	 * Reset all listener flags to an "off" state.
	 */
	public void reset_flags(){
		have_mode = false;
		have_notice = false;
		last_reply_code = -1;
	}
	
	
	// ************* ADAPTED EVENTS *************
	
		// All code here is overriding, no need for javadoc
	
	public void onMode(String chan, IRCUser u, IRCModeParser mp){
		have_mode = true;
	}
	
	public void onNotice(String target, IRCUser u, String msg){
		have_notice = true;
	}
	
	public void onReply(int num, String value, String msg){
		last_reply_code = num;
	}
	
}

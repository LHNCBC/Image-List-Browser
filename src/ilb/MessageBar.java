package ilb;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * The Message bar is a simple panel with a progress bar for showing the loading
 * status of the current background thread, and a JLabel for relevant errors
 * that users might like to know, like if FaceMatch didn't load
 * 
 * @author bonifantmc
 *
 */
@SuppressWarnings("serial")
public class MessageBar extends JPanel {
	/** display loading status of threads here */
	public JProgressBar prog = new JProgressBar();
	/** display pertinent messages to users here */
	public JLabel message = new JLabel();

	/** Init the Message bar and set the progress and message to visible. */
	public MessageBar() {
		add(this.prog);
		add(this.message);
		this.prog.setVisible(true);
		this.message.setVisible(true);
	}
}

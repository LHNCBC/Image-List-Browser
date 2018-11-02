package ilb;

import java.awt.EventQueue;
import java.io.File;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Launch and run the ILB
 * 
 * @author bonifantmc
 * 
 */
public class Main {
	// static final int TIP_DURATION = 10000;

	/**
	 * Sets the UI look & feel to match current OS as best as possible. Then
	 * launches the application and loads the last session.
	 * 
	 * @param args
	 *            Ignored
	 */
	public static void main(String[] args) {
		// check that needed directories exist
		File tempdir = new File(ILB.TMP);
		if (!tempdir.exists())
			tempdir.mkdirs();

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		// ToolTipManager.sharedInstance().setDismissDelay(TIP_DURATION);

		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					ILB fml = new ILB();
					fml.setVisible(true);
					fml.getHandler().load();

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
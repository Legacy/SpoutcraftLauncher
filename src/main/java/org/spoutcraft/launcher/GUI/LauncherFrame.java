package org.spoutcraft.launcher.GUI;

import org.spoutcraft.launcher.Launcher;
import org.spoutcraft.launcher.MinecraftAppletEnglober;
import org.spoutcraft.launcher.PlatformUtils;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class LauncherFrame extends Frame implements WindowListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4524937541564722358L;
	private MinecraftAppletEnglober minecraft;
	
	public LauncherFrame() {
		super("Spoutcraft Launcher");
		super.setVisible(true);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation((dim.width-870)/2, (dim.height-518)/2);
		this.setSize(new Dimension(870, 518));
		this.setResizable(true);
		this.addWindowListener(this);
		setIconImage(Toolkit.getDefaultToolkit().getImage(LoginForm.class.getResource("/org/spoutcraft/launcher/favicon.png")));
	}
	
	public void runGame(String user, String session, String downloadTicket, String mcpass) {

		Applet applet = Launcher.getMinecraftApplet();

		minecraft = new MinecraftAppletEnglober(applet);

		minecraft.addParameter("username", user);
		minecraft.addParameter("sessionid", session);
		minecraft.addParameter("downloadticket", downloadTicket);
		minecraft.addParameter("mppass", mcpass);
		minecraft.addParameter("spoutcraftlauncher", "true");
		minecraft.addParameter("portable", PlatformUtils.isPortable() + "");

		applet.setStub(minecraft);

		this.add(minecraft);
		validate();

		minecraft.init();
		minecraft.setSize(getWidth(), getHeight());

		minecraft.start();
		
		this.setVisible(true);
	}

	public void windowActivated(WindowEvent e) {		
	}

	
	public void windowClosed(WindowEvent e) {
	}

	
	public void windowClosing(WindowEvent e) {
		if (LauncherFrame.this.minecraft != null) {
			LauncherFrame.this.minecraft.stop();
			LauncherFrame.this.minecraft.destroy();
		}
		try {
			Thread.sleep(10000L);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		System.out.println("Exiting Spoutcraft");
		System.exit(0);
	}

	
	public void windowDeactivated(WindowEvent e) {
	}

	
	public void windowDeiconified(WindowEvent e) {
	}

	
	public void windowIconified(WindowEvent e) {
	}

	
	public void windowOpened(WindowEvent e) {
	}
}

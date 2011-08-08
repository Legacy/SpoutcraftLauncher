package org.spoutcraft.launcher;

import org.spoutcraft.launcher.GUI.LoginForm;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;

public class Main {
	
	@SuppressWarnings("static-access")
	public Main() throws Exception {
		main(new String[0]);
	}
	
	@SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void main(String[] args) throws Exception {
        if (Arrays.asList(args).contains("--portable")) {
            PlatformUtils.setPortable(true);
        }
		PlatformUtils.getWorkingDirectory().mkdir();
		new File(PlatformUtils.getWorkingDirectory(), "spoutcraft").mkdir();
		
		try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("[WARNING] Can't get system LnF: " + e);
        }
		
		LoginForm login = new LoginForm();
		login.setVisible(true);
	}
	
}

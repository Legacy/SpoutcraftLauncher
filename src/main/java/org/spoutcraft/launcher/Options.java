/*
 * This file is part of Spoutcraft Launcher (http://www.spout.org/).
 *
 * Spoutcraft Launcher is licensed under the SpoutDev License Version 1.
 *
 * Spoutcraft Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * Spoutcraft Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev license version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package org.spoutcraft.launcher;

import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;

public class Options {
	@Parameter
	private List<String> parameters = Lists.newArrayList();

	@Parameter(names = { "-username", "-user", "-u" }, description = "Minecraft username")
	private String user = null;

	@Parameter(names = { "-password", "-pass", "-p" }, description = "Minecraft password")
	private String pass = null;

	@Parameter(names = { "-server", "-host", "-join", "-j", "-h", "-s" }, description = "Minecraft server to join")
	private String server = null;

	@Parameter(names = { "-portable", "--portable", "-pmode", "-portable_mode", "-pm" }, description = "Portable mode")
	private boolean portable = false;

	@Parameter(names = { "-safe", "-smode", "-safe_mode", "-sm" }, description = "Safe mode preventing addons from loading")
	private boolean safe_mode = false;

	@Parameter(names = {"-nr", "-norelaunch"}, description = "Don't relaunch the launcher when memory settings are different")
	private boolean noRelaunch = false;
	
	public List<String> getParameters() {
		return parameters;
	}

	public String getUser() {
		return user;
	}

	public String getPass() {
		return pass;
	}

	public String getServer() {
		if (server == null) {
			return null;
		}
		if (server.contains(":")) {
			return server.substring(0, server.indexOf(":"));
		}
		return server;
	}

	public String getPort() {
		if (server == null) return null;
		if (server.contains(":")) {
			return server.substring(server.indexOf(":") + 1);
		}
		return null;
	}

	public boolean isPortable() {
		return portable;
	}

	public boolean isSafe_mode() {
		return safe_mode;
	}
	
	public boolean noRelaunch() {
		return noRelaunch;
	}
}

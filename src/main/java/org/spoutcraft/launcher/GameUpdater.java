/*
 * This file is part of Spoutcraft Launcher (http://wiki.getspout.org/).
 * 
 * Spoutcraft Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Spoutcraft Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.spoutcraft.launcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.spoutcraft.launcher.async.Download;
import org.spoutcraft.launcher.async.DownloadListener;
import org.spoutcraft.launcher.exception.UnsupportedOSException;
import org.spoutcraft.launcher.logs.SystemConsoleListener;

import SevenZip.LzmaAlone;

public class GameUpdater implements DownloadListener {
	/* Minecraft Updating Arguments */
	public long latestVersion;
	public String user = "Player";
	public String downloadTicket = "1";

	/* General Updating Settings */
	public boolean devmode = false;

	/* Files */
	public static final File binDir = new File(PlatformUtils.getWorkingDirectory().getPath() + File.separator + "bin");
	public static final File binCacheDir = new File(PlatformUtils.getWorkingDirectory().getPath() + File.separator + "bin" + File.separator + "cache");
	public static final File updateDir = new File(PlatformUtils.getWorkingDirectory().getPath() + File.separator + "temp");
	public static final File backupDir = new File(PlatformUtils.getWorkingDirectory().getPath() + File.separator + "backups");
	public static final File spoutcraftDir = new File(PlatformUtils.getWorkingDirectory().getPath() + File.separator + "spoutcraft");
	public static final File savesDir = new File(PlatformUtils.getWorkingDirectory().getPath() + File.separator + "saves");

	/* Minecraft Updating Arguments */
	public final String baseURL = "http://s3.amazonaws.com/MinecraftDownload/";
	public final String latestLWJGLURL = "http://www.minedev.net/spout/lwjgl/";
	// public final String spoutcraftDownloadURL = "http://ci.getspout.org/view/SpoutDev/job/Spoutcraft/promotion/latest/Recommended/artifact/target/spoutcraft-dev-SNAPSHOT.zip";
	// public final String spoutcraftDownloadDevURL = "http://ci.getspout.org/job/Spoutcraft/lastSuccessfulBuild/artifact/target/spoutcraft-dev-SNAPSHOT.zip";
	public final String spoutcraftMirrors = "http://cdn.getspout.org/mirrors.html";
	private SettingsHandler settings = new SettingsHandler("defaults/spoutcraft.properties", new File(PlatformUtils.getWorkingDirectory(), "spoutcraft" + File.separator + "spoutcraft.properties"));
	private DownloadListener listener;
	private byte allowUpdates = -1;
	private ArrayList<String> mirrors = new ArrayList<String>();
	private final Random rand = new Random();

	public GameUpdater() {
	}

	public void updateMC() throws Exception {

		binDir.mkdir();
		binCacheDir.mkdir();
		updateDir.mkdir();

		// Processs minecraft.jar \\
		File mcCache = new File(binCacheDir, "minecraft_1.8.1.jar");
		if (mcCache.length() < 1000) {
			downloadFile(baseURL + "minecraft.jar?user=" + user + "&ticket=" + downloadTicket, updateDir + File.separator + "minecraft.jar", "minecraft_1.8.1.jar");
		} else {
			copy(mcCache, new File(updateDir, "minecraft.jar"));
		}

		File nativesDir = new File(binDir.getPath() + File.separator + "natives");
		nativesDir.mkdir();

		// Process other Downloads
		mcCache = new File(binCacheDir, "jinput.jar");
		if (!mcCache.exists()) {
			downloadFile(getNativesUrl() + "jinput.jar", binDir.getPath() + File.separator + "jinput.jar", "jinput.jar");
		} else {
			copy(mcCache, new File(updateDir, "jinput.jar"));
		}

		mcCache = new File(binCacheDir, "lwjgl.jar");
		if (!mcCache.exists()) {
			downloadFile(getNativesUrl() + "lwjgl.jar", binDir.getPath() + File.separator + "lwjgl.jar", "lwjgl.jar");
		} else {
			copy(mcCache, new File(updateDir, "lwjgl.jar"));
		}

		mcCache = new File(binCacheDir, "lwjgl_util.jar");
		if (!mcCache.exists()) {
			downloadFile(getNativesUrl() + "lwjgl_util.jar", binDir.getPath() + File.separator + "lwjgl_util.jar", "lwjgl_util.jar");
		} else {
			copy(mcCache, new File(updateDir, "lwjgl_util.jar"));
		}

		getNatives();

		stateChanged("Extracting Files...", 0);
		// Extract Natives \\
		extractNatives(nativesDir, new File(GameUpdater.updateDir.getPath() + File.separator + "natives.zip"));

		writeVersionFile(new File(GameUpdater.binDir + File.separator + "version"), Long.toString(this.latestVersion));
	}

	public String getBuildUrl(String mirrorURI, String jenkinsURL) {
		try {
			if (mirrors.size() == 0) {
				updateMirrors();
			}
			int random = rand.nextInt(10 * mirrors.size());
			int index = random / 10;
			float progress = 0F;
			// Test for bad, down mirrors
			for (int i = index; i < mirrors.size() + index; i++) {
				int j = i;
				if (j >= mirrors.size())
					j -= mirrors.size();
				String mirror = "http://" + mirrors.get(j) + "/" + mirrorURI;
				if (isAddressReachable(mirror)) {
					System.out.println("Using mirror: " + mirror);
					stateChanged("Contacting Mirrors...", 100F);
					return mirror;
				} else {
					progress += 100F / mirrors.size();
					stateChanged("Contacting Mirrors...", progress);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.err.println("All mirrors failed, defaulting to jenkins");
		return jenkinsURL;
	}

	public boolean isAddressReachable(String url) {
		try {
			URL test = new URL(url);
			HttpURLConnection.setFollowRedirects(false);
			HttpURLConnection urlConnect = (HttpURLConnection) test.openConnection();
			urlConnect.setRequestMethod("HEAD");
			return (urlConnect.getResponseCode() == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			return false;
		}
	}

	public void updateMirrors() throws IOException {
		URL url = new URL("http://cdn.getspout.org/mirrors.html");
		HttpURLConnection con = (HttpURLConnection) (url.openConnection());
		System.setProperty("http.agent", ""); // Spoofing the user agent is required to track stats
		con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.100 Safari/534.30");
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		mirrors.clear();
		String data = "";
		while ((data = in.readLine()) != null) {
			mirrors.add(data);
		}
		in.close();
	}

	public String getNativesUrl() {
		if (settings.checkProperty("latestLWJGL")) {
			if (settings.getPropertyBoolean("latestLWJGL")) {
				return latestLWJGLURL;
			}
		}
		return baseURL;
	}

	public String getNativesUrl(String fileName) {
		if (settings.checkProperty("latestLWJGL")) {
			if (settings.getPropertyBoolean("latestLWJGL")) {
				return latestLWJGLURL + fileName + ".zip";
			}
		}
		return baseURL + fileName + ".jar.lzma";
	}

	public boolean isLZMANatives() {
		if (settings.checkProperty("latestLWJGL")) {
			if (settings.getPropertyBoolean("latestLWJGL")) {
				return false;
			}
		}
		return true;
	}

	public String readVersionFile(File file) throws Exception {
		DataInputStream dis = new DataInputStream(new FileInputStream(file));
		String version = dis.readUTF();
		dis.close();
		return version;
	}

	public void writeVersionFile(File file, String version) throws Exception {
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
		dos.writeUTF(version);
		dos.close();
	}

	public Boolean checkMCUpdate(File versionFile) throws Exception {
		if (!GameUpdater.binDir.exists())
			return true;
		if (!new File(binDir, "natives").exists())
			return true;
		if (!versionFile.exists())
			return true;
		if ((new File(binDir, "minecraft.jar").length() < 1500))
			return true;
		long currentVersion = Long.parseLong(this.readVersionFile(versionFile));
		return this.latestVersion > currentVersion;
	}

	private void extractNatives(File nativesDir, File nativesJar) throws Exception {

		if (!nativesDir.exists())
			nativesDir.mkdir();

		JarFile jar = new JarFile(nativesJar);
		Enumeration<JarEntry> entries = jar.entries();

		float progressStep = 100F / jar.size();
		float progress = 0;

		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String name = entry.getName();
			if (entry.isDirectory())
				continue;
			if (name.startsWith("META-INF"))
				continue;
			InputStream inputStream = jar.getInputStream(entry);
			File outFile = new File(nativesDir.getPath() + File.separator + name);
			if (!outFile.exists())
				outFile.createNewFile();
			OutputStream out = new FileOutputStream(new File(nativesDir.getPath() + File.separator + name));

			int read;
			byte[] bytes = new byte[1024];

			while ((read = inputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}

			progress += progressStep;
			stateChanged("Extracting Files...", progress);

			inputStream.close();
			out.flush();
			out.close();
		}

	}

	private File getNatives() throws Exception {
		String osName = System.getProperty("os.name").toLowerCase();
		String fname;

		if (osName.contains("win")) {
			fname = "windows_natives";
		} else if (osName.contains("mac")) {
			fname = "macosx_natives";
		} else if (osName.contains("solaris") || osName.contains("sunos")) {
			fname = "solaris_natives";
		} else if (osName.contains("linux") || osName.contains("unix")) {
			fname = "linux_natives";
		} else {
			throw new UnsupportedOSException();
		}

		if (!updateDir.exists())
			updateDir.mkdir();

		this.downloadFile(getNativesUrl(fname), updateDir.getPath() + File.separator + (isLZMANatives() ? "natives.jar.lzma" : "natives.zip"));

		if (isLZMANatives())
			extractLZMA(GameUpdater.updateDir.getPath() + File.separator + "natives.jar.lzma", GameUpdater.updateDir.getPath() + File.separator + "natives.zip");

		return new File(updateDir.getPath() + File.separator + "natives.jar.lzma");
	}

	public void updateSpout() throws Exception {
		performBackup();
		String newversion = getSpoutVersion();

		updateDir.mkdirs();
		binCacheDir.mkdirs();

		File mcCache = new File(binCacheDir, "minecraft_1.8.1.jar");
		File updateMC = new File(updateDir.getPath() + File.separator + "minecraft.jar");
		if (mcCache.exists()) {
			copy(mcCache, updateMC);
		}

		if (!updateMC.exists())
			downloadFile(baseURL + "minecraft.jar?user=" + user + "&ticket=" + downloadTicket, updateMC.getPath(), "minecraft_1.8.1.jar");

		File spout = new File(GameUpdater.updateDir.getPath() + File.separator + "spoutcraft.zip");

		stateChanged("Looking Up Mirrors...", 0F);
		downloadFile(getBuildUrl("Spoutcraft/" + newversion + "/spoutcraft-dev-SNAPSHOT.zip", "http://ci.getspout.org/job/Spoutcraft/" + (devmode ? "lastSuccessfulBuild/artifact/target/spoutcraft-dev-SNAPSHOT.zip" : "promotion/latest/Recommended/artifact/target/spoutcraft-dev-SNAPSHOT.zip")), spout.getPath());

		stateChanged("Unzipping Spoutcraft Files...", 0F);

		unzipArchive(new File(updateDir.getPath() + File.separator + "spoutcraft.zip"), new File(updateDir + File.separator + "spoutcraft"));

		String rootDir = PlatformUtils.getWorkingDirectory() + File.separator + "temp" + File.separator + "spoutcraft" + File.separator;
		Set<ClassFile> spoutMod = this.getFiles(new File(updateDir.getPath() + File.separator + "spoutcraft"), rootDir);

		stateChanged("Merging Spoutcraft Files Into Minecraft Jar...", 0F);

		this.addFilesToExistingZip(updateMC, spoutMod, rootDir, true);

		File mcJar = new File(binDir, "minecraft.jar");
		mcJar.delete();

		// Move file
		updateMC.renameTo(mcJar);

		if (GameUpdater.spoutcraftDir.exists())
			GameUpdater.spoutcraftDir.mkdir();

		File spoutVersion = new File(GameUpdater.spoutcraftDir.getPath() + File.separator + "versionSpoutcraft");
		if (spoutVersion.exists())
			spoutVersion.delete();

		this.writeFile(spoutVersion.getPath(), newversion);
	}

	public String getSpoutVersion() throws Exception {
		String version;
		URL url;
		if (devmode) {
			url = new URL("http://ci.getspout.org/job/Spoutcraft/lastSuccessfulBuild/buildNumber");
		} else {
			url = new URL("http://ci.getspout.org/job/Spoutcraft/Recommended/buildNumber");
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		String str = in.readLine();
		if (str != null) {
			version = str;
			return version;
		}
		in.close();
		return null;
	}

	public boolean checkSpoutUpdate() throws Exception {
		if (!PlatformUtils.getWorkingDirectory().exists())
			return true;
		if (!GameUpdater.spoutcraftDir.exists())
			return true;
		File spoutcraftVersion = new File(GameUpdater.spoutcraftDir.getPath() + File.separator + "versionSpoutcraft");
		if (!spoutcraftVersion.exists())
			return true;
		BufferedReader br = new BufferedReader(new FileReader(spoutcraftVersion));
		String line;
		String version = null;
		if ((line = br.readLine()) != null) {
			version = line;
		}

		String latest = this.getSpoutVersion();

		if (latest == null)
			return false;
		if (version == null)
			return true;
		if (version.contains("."))
			return true;

		int c = Integer.parseInt(version);
		int l = Integer.parseInt(latest);

		return c < l || (c > l && !devmode);

	}

	public boolean allowUpdate() {
		if (allowUpdates == -1) {
			try {
				String version = null;
				URL url = new URL("http://dl.dropbox.com/u/27798409/AllowMC.txt");
				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				String str = in.readLine();
				if (str != null) {
					version = str;
				}
				in.close();

				if (version == null) {
					allowUpdates = 0;
					return false;
				}

				if (version.equalsIgnoreCase("true")) {
					allowUpdates = 1;
					return true;
				}
			} catch (Exception ex) {

			}
			allowUpdates = 0;
			return false;
		}
		return allowUpdates != 0;
	}

	public void unzipArchive(File archive, File outputDir) throws ZipException, IOException {
		ZipFile zipfile = new ZipFile(archive);
		float progress = 0F;
		float progressStep = 100F / zipfile.size();
		for (Enumeration<? extends ZipEntry> e = zipfile.entries(); e.hasMoreElements();) {
			ZipEntry entry = (ZipEntry) e.nextElement();
			unzipEntry(zipfile, entry, outputDir);

			progress += progressStep;
			stateChanged("Unzipping Spoutcraft Files...", progress);
		}
		stateChanged("Unzipping Spoutcraft Files...", 100F);
	}

	private void unzipEntry(ZipFile zipfile, ZipEntry entry, File outputDir) throws IOException {

		if (entry.isDirectory()) {
			(new File(outputDir, entry.getName())).mkdirs();
			return;
		}

		File outputFile = new File(outputDir, entry.getName());
		if (!outputFile.getParentFile().exists()) {
			(new File(outputDir, entry.getName())).mkdirs();
		}
		if (outputFile.isDirectory()) {
			outputFile.delete();
		}
		InputStream inputStream = zipfile.getInputStream(entry);
		OutputStream outputStream = new FileOutputStream(outputFile);

		try {
			copy(inputStream, outputStream);
		} finally {
			outputStream.close();
			inputStream.close();
		}
	}

	public static long copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[1024 * 4];
		long count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	public static void copy(File input, File output) throws IOException {
		FileInputStream inputStream = null;
		FileOutputStream outputStream = null;
		try {
			inputStream = new FileInputStream(input);
			outputStream = new FileOutputStream(output);
			copy(inputStream, outputStream);
		} finally {
			if (inputStream != null)
				inputStream.close();
			if (outputStream != null)
				outputStream.close();
		}
	}

	public void performBackup() throws Exception {
		File spoutVersion = new File(GameUpdater.spoutcraftDir.getPath() + File.separator + "versionSpoutcraft");
		if (!spoutVersion.exists())
			return;

		BufferedReader br;
		br = new BufferedReader(new FileReader(spoutVersion));
		String line;
		String version = null;

		if ((line = br.readLine()) != null) {
			version = line;
		}

		if (version == null)
			return;

		if (!backupDir.exists())
			backupDir.mkdir();

		File zip = new File(GameUpdater.backupDir, version + "-backup.zip");

		if (zip.exists())
			return;

		String rootDir = PlatformUtils.getWorkingDirectory() + File.separator;
		HashSet<File> exclude = new HashSet<File>();
		exclude.add(GameUpdater.backupDir);
		if (!(settings.checkProperty("worldbackup") && settings.getPropertyBoolean("worldbackup"))) {
			exclude.add(GameUpdater.savesDir);
		}
		exclude.add(GameUpdater.updateDir);
		exclude.add(SystemConsoleListener.logDir);

		zip.createNewFile();

		addFilesToExistingZip(zip, getFiles(PlatformUtils.getWorkingDirectory(), exclude, rootDir), rootDir, false);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean canPlayOffline() {
		try {
			String path = (String) AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws Exception {
					return PlatformUtils.getWorkingDirectory() + File.separator + "bin" + File.separator;
				}
			});
			File dir = new File(path);
			if (!dir.exists())
				return false;

			dir = new File(dir, "version");
			if (!dir.exists())
				return false;

			if (dir.exists()) {
				String version = readVersionFile(dir);
				if ((version != null) && (version.length() > 0))
					return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}

	public void addFilesToExistingZip(File zipFile, Set<ClassFile> files, String rootDir, boolean progressBar) throws IOException {
		File tempFile = File.createTempFile(zipFile.getName(), null, zipFile.getParentFile());
		tempFile.delete();

		copy(zipFile, tempFile);
		boolean renameOk = zipFile.renameTo(tempFile);
		;
		if (!renameOk) {
			if (tempFile.exists()) {
				zipFile.delete();
			} else {
				throw new RuntimeException("could not rename the file " + zipFile.getAbsolutePath() + " to " + tempFile.getAbsolutePath());
			}
		}
		byte[] buf = new byte[1024];

		float progress = 0F;
		float progressStep = 0F;
		if (progressBar) {
			int jarSize = new JarFile(tempFile).size();
			progressStep = 100F / (files.size() + jarSize);
		}

		ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(tempFile)));
		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
		ZipEntry entry = zin.getNextEntry();
		while (entry != null) {
			String name = entry.getName();
			ClassFile entryFile = new ClassFile(name);
			if (!name.contains("META-INF") && !files.contains(entryFile)) {
				out.putNextEntry(new ZipEntry(name));
				int len;
				while ((len = zin.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			}
			entry = zin.getNextEntry();

			progress += progressStep;
			if (progressBar) {
				stateChanged("Merging Spoutcraft Files Into Minecraft Jar...", progress);
			}
		}
		zin.close();
		for (ClassFile file : files) {
			try {
				InputStream in = new FileInputStream(file.getFile());

				String path = file.getPath();
				path = path.replace(rootDir, "");
				path = path.replaceAll("\\\\", "/");
				out.putNextEntry(new ZipEntry(path));

				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}

				progress += progressStep;
				if (progressBar) {
					stateChanged("Merging Spoutcraft Files Into Minecraft Jar...", progress);
				}

				out.closeEntry();
				in.close();
			} catch (Exception e) {
			}
		}

		out.close();
	}

	// I know that is is not the best method but screw it, I am tired of trying to do it myself :P
	private void extractLZMA(String in, String out) throws Exception {
		String[] args = { "d", in, out };
		LzmaAlone.main(args);
	}

	@SuppressWarnings("unused")
	private void extractPack(String in, String out) throws Exception {
		File f = new File(in);
		if (!f.exists())
			return;

		FileOutputStream fostream = new FileOutputStream(out);
		JarOutputStream jostream = new JarOutputStream(fostream);

		Pack200.Unpacker unpacker = Pack200.newUnpacker();
		unpacker.unpack(f, jostream);
		jostream.close();

		f.delete();
	}

	public void writeFile(String out, String contents) {
		FileWriter fWriter;
		BufferedWriter writer;
		try {
			fWriter = new FileWriter(out);
			writer = new BufferedWriter(fWriter);
			writer.write(contents);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void downloadFile(String url, String output, String cacheName) throws IOException {
		int tries = settings.getPropertyBoolean("retryLogins") ? 3 : 1;
		File outputFile = null;
		while (tries > 0) {
			System.out.println("Starting download of " + url + ", with " + tries + " tries remaining");
			tries--;
			Download download = new Download(url, output);
			download.setListener(this);
			download.run();
			if (!download.isSuccess()) {
				if (download.getOutFile() != null) {
					download.getOutFile().delete();
				}
				System.err.println("Download of " + url + " Failed!");
				stateChanged("Download Failed, retries remaining: " + tries, 0F);
			} else {
				outputFile = download.getOutFile();
				break;
			}
		}
		if (outputFile == null) {
			throw new IOException("Failed to download " + url);
		}
		if (cacheName != null) {
			copy(outputFile, new File(binCacheDir, cacheName));
		}
	}

	private void downloadFile(String url, String output) throws IOException {
		downloadFile(url, output, null);
	}

	public Set<ClassFile> getFiles(File dir, String rootDir) {
		return getFiles(dir, new HashSet<File>(), rootDir);
	}

	public Set<ClassFile> getFiles(File dir, Set<File> exclude, String rootDir) {
		HashSet<ClassFile> result = new HashSet<ClassFile>();
		for (File file : dir.listFiles()) {
			if (!exclude.contains(dir)) {
				if (file.isDirectory()) {
					result.addAll(this.getFiles(file, exclude, rootDir));
					continue;
				}
				result.add(new ClassFile(file, rootDir));
			}
		}
		return result;
	}

	public void stateChanged(String fileName, float progress) {
		this.listener.stateChanged(fileName, progress);
	}

	public void setListener(DownloadListener listener) {
		this.listener = listener;
	}
}

package org.spoutcraft.launcher;

import SevenZip.LzmaAlone;
import org.spoutcraft.launcher.AsyncDownload.Download;
import org.spoutcraft.launcher.AsyncDownload.DownloadListener;
import org.spoutcraft.launcher.Exceptions.UnsupportedOSException;

import java.io.*;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored", "ResultOfMethodCallIgnored"})
public class GameUpdater implements DownloadListener {
	/* Minecraft Updating Arguments */
	public long latestVersion;
	public String user = "Player";
	public String downloadTicket = "1";
	
	/* General Updating Settings */
	public boolean devmode = false;
	public Boolean force = false;
	
	/* Files */
	public final File binDir = new File(PlatformUtils.getWorkingDirectory().getPath() +  File.separator + "bin");
	public final File updateDir = new File(PlatformUtils.getWorkingDirectory().getPath() +  File.separator + "updateFolder");
	public final File backupDir = new File(PlatformUtils.getWorkingDirectory().getPath() +  File.separator + "backups");
	public final File spoutDir = new File(PlatformUtils.getWorkingDirectory().getPath() +  File.separator + "spoutcraft");
	public final File savesDir = new File(PlatformUtils.getWorkingDirectory().getPath() +  File.separator + "saves");
	
	/* Minecraft Updating Arguments */
	public final String backupbaseURL = "http://s3.amazonaws.com/MinecraftDownload/";
	public final String baseURL = "http://www.getspout.org/lwjgl/";
	public final String spoutDownloadURL = "http://ci.getspout.org/view/SpoutDev/job/Spoutcraft/promotion/latest/Recommended/artifact/target/spoutcraft-dev-SNAPSHOT-MC-1.7.3.zip";
	public final String spoutDownloadDevURL = "http://ci.getspout.org/job/Spoutcraft/lastSuccessfulBuild/artifact/target/spoutcraft-dev-SNAPSHOT-MC-1.7.3.zip";
	private SettingsHandler settings = new SettingsHandler("defaults/spoutcraft.properties", new File(PlatformUtils.getWorkingDirectory(), "spoutcraft" + File.separator + "spoutcraft.properties"));
    private DownloadListener listener;

    public GameUpdater() {
    }

    public void updateMC() throws Exception {
		this.purgeDir(binDir);
		this.purgeDir(updateDir);
		
		binDir.mkdir();
		updateDir.mkdir();
		
		// Processs minecraft.jar \\
		downloadFile(backupbaseURL + "minecraft.jar?user=" + user + "&ticket=" + downloadTicket, this.updateDir + File.separator + "minecraft.jar");
		
		File nativesDir = new File(binDir.getPath() + File.separator + "natives");
		nativesDir.mkdir();
		
		// Process other Downloads
		try {
			downloadFile(baseURL + "jinput.jar", this.binDir.getPath() + File.separator + "jinput.jar");
		} catch (Exception e) {
			downloadFile(backupbaseURL + "jinput.jar", this.binDir.getPath() + File.separator + "jinput.jar");
		}
		try {
			downloadFile(baseURL + "lwjgl.jar", this.binDir.getPath() + File.separator + "lwjgl.jar");
		} catch (Exception e) {
			downloadFile(backupbaseURL + "lwjgl.jar", this.binDir.getPath() + File.separator + "lwjgl.jar");
		}
		try {
			downloadFile(baseURL + "lwjgl_util.jar", this.binDir.getPath() + File.separator + "lwjgl_util.jar");
		} catch (Exception e) {
			downloadFile(backupbaseURL + "lwjgl_util.jar", this.binDir.getPath() + File.separator + "lwjgl_util.jar");
		}
		getNatives();
		
		// Extract Natives \\
		extractNatives(nativesDir, new File(this.updateDir.getPath() + File.separator + "natives.zip"));
		
		writeVersionFile(new File(this.binDir + File.separator + "version"), Long.toString(this.latestVersion));
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
		if (!this.binDir.exists()) return true;
		if (!new File(binDir, "natives").exists()) return true;
		if (!versionFile.exists()) return true;
		long currentVersion = Long.parseLong(this.readVersionFile(versionFile));
        return this.latestVersion > currentVersion;
    }
	
	private void extractNatives(File nativesDir, File nativesJar) throws Exception {

		if (!nativesDir.exists())nativesDir.mkdir();

		JarFile jar = new JarFile(nativesJar);
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String name = entry.getName();
			if (entry.isDirectory()) continue;
			if (name.startsWith("META-INF")) continue;
			InputStream inputStream = jar.getInputStream(entry);
			File outFile = new File(nativesDir.getPath() + File.separator  + name);
			if (!outFile.exists()) outFile.createNewFile();
			OutputStream out = new FileOutputStream(new File(nativesDir.getPath() + File.separator  + name));

			int read;
			byte[] bytes = new byte[1024];

			while((read = inputStream.read(bytes))!= -1){
				out.write(bytes, 0, read);
			}

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

		if (!updateDir.exists()) updateDir.mkdir();
		try {
			this.downloadFile(baseURL + fname + ".zip", updateDir.getPath() + File.separator + "natives.zip");
		} catch (Exception e) {
			// Failed to download from spoutcraft, try mc.net
			this.downloadFile(backupbaseURL + fname + ".jar.lzma", updateDir.getPath() + File.separator + "natives.jar.lzma");
			extractLZMA(this.updateDir.getPath() + File.separator + "natives.jar.lzma", this.updateDir.getPath() + File.separator + "natives.zip");
		}

		return new File (updateDir.getPath() + File.separator + "natives.jar.lzma");
	}
	
	public void updateSpout() throws Exception {
		performBackup();
		
		if (this.updateDir.exists()) this.purgeDir(updateDir);
		this.updateDir.mkdirs();
		
		File updateMC = new File(updateDir.getPath() + File.separator + "minecraft.jar");
		
		if (!updateMC.exists()) downloadFile(backupbaseURL + "minecraft.jar?user=" + user + "&ticket=" + downloadTicket, updateMC.getPath());
		
		File spout = new File(this.updateDir.getPath() + File.separator + "Spout.zip");
		
		if (devmode) {
			downloadFile(spoutDownloadDevURL, spout.getPath());
		} else {
			downloadFile(spoutDownloadURL, spout.getPath());
		}
		
		this.unzipSpout();
		
		ArrayList<File> spoutMod = this.getFiles(new File(updateDir.getPath() + File.separator + "Spout"));
		
		this.addFilesToExistingZip(updateMC, spoutMod, PlatformUtils.getWorkingDirectory() + File.separator + "updateFolder" + File.separator + "Spout" + File.separator);
		
		File mcJar = new File(binDir, "minecraft.jar");
		mcJar.delete();
		
		//Move file
		updateMC.renameTo(mcJar);
		
		if (this.spoutDir.exists()) this.spoutDir.mkdir();
		
		File spoutVersion = new File(this.spoutDir.getPath() + File.separator + "versionSpoutcraft");
		if (spoutVersion.exists()) spoutVersion.delete();
		
		this.writeFile(spoutVersion.getPath(), this.getSpoutVersion());
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
		if (!PlatformUtils.getWorkingDirectory().exists()) return true;
		if (!this.spoutDir.exists()) return true;
		File bcVersion = new File(this.spoutDir.getPath() + File.separator + "versionSpoutcraft");
		if (!bcVersion.exists()) return true;
		BufferedReader br = new BufferedReader(new FileReader(bcVersion));
		String line;
		String version = null;
		if((line = br.readLine()) != null) {
			version = line;
		}
		
		String latest = this.getSpoutVersion();

		if (latest == null) return false;
        if (version == null) return true;
		if (version.contains(".")) return true;
		
		int c = Integer.parseInt(version);
		int l = Integer.parseInt(latest);

        return c < l || (c > l && !devmode);

    }
	
	public void unzipSpout() throws Exception {
		final int BUFFER = 2048;
		BufferedOutputStream dest;
		FileInputStream fis = new FileInputStream(new File(this.updateDir.getPath() + File.separator + "Spout.zip"));
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
        ZipEntry entry;
		File dir = new File(updateDir + File.separator + "Spout");
		if (dir.exists()) {
			this.purgeDir(dir);
		}

		dir.mkdir();
		while ((entry = zis.getNextEntry()) != null) {
			int count;
			byte data[] = new byte[BUFFER];
			if(entry.isDirectory()) {
				File f2 = new File(dir.getPath() + File.separator + entry.getName());
				f2.mkdir();
			} else {
				FileOutputStream fos = new FileOutputStream(new File(dir.getPath() + File.separator + entry.getName()));
				dest = new BufferedOutputStream(fos, BUFFER);
				while ((count = zis.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
		}
		zis.close();
		fis.close();
	}
	
	public void performBackup() throws Exception {
		File spoutVersion = new File(this.spoutDir.getPath() + File.separator + "versionSpoutcraft");
		if (!spoutVersion.exists()) return;
		
		BufferedReader br;
		br = new BufferedReader(new FileReader(spoutVersion));
		String line;
		String version = null;
		
		if((line = br.readLine()) != null) {
			version = line;
		}
		
		if (version == null) return;
		
		if (!backupDir.exists()) backupDir.mkdir();
		
		File zip = new File(this.backupDir, version + "-backup.zip");
		
		if (zip.exists()) return;
		
		ArrayList<File> exclude = new ArrayList<File>();
		exclude.add(this.backupDir);
		if (!(settings.checkProperty("worldbackup") && settings.getPropertyBoolean("worldbackup"))) {
			exclude.add(this.savesDir);
		}
		exclude.add(this.updateDir);

		zip.createNewFile();
		
		addFilesToExistingZip(zip, getFiles(PlatformUtils.getWorkingDirectory(), exclude), PlatformUtils.getWorkingDirectory() + File.separator);
		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean canPlayOffline()
	{
		try
		{
			String path = (String)AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws Exception {
					return PlatformUtils.getWorkingDirectory() + File.separator + "bin" + File.separator;
				}
			});
			File dir = new File(path);
			if (!dir.exists()) return false;

			dir = new File(dir, "version");
			if (!dir.exists()) return false;

			if (dir.exists()) {
				String version = readVersionFile(dir);
				if ((version != null) && (version.length() > 0))
					return true;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	public void addFilesToExistingZip(File zipFile, ArrayList<File> files, String rootDir) throws IOException {
        File tempFile = File.createTempFile(zipFile.getName(), null, zipFile.getParentFile());
		tempFile.delete();

		boolean renameOk=zipFile.renameTo(tempFile);
		if (!renameOk)
		{
			throw new RuntimeException("could not rename the file "+zipFile.getAbsolutePath()+" to "+tempFile.getAbsolutePath());
		}
		byte[] buf = new byte[1024];
		
		ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
		ZipEntry entry = zin.getNextEntry();
		while (entry != null) {
			String name = entry.getName();
			boolean notInFiles = true;
			for (File f : files) {
				String path = f.getPath();
				path = path.replace(rootDir, "");
				path = path.replaceAll("\\\\","/");
				if (path.equals(name) || name.contains("META-INF")) {
					notInFiles = false;
					break;
				}
			}
			if (notInFiles) {
				out.putNextEntry(new ZipEntry(name));
				int len;
				while ((len = zin.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			}
			entry = zin.getNextEntry();
		}	
		zin.close();
		for (File file : files) {
			InputStream in = new FileInputStream(file);

			String path = file.getPath();
			path = path.replace(rootDir, "");
			path = path.replaceAll("\\\\","/");
			out.putNextEntry(new ZipEntry(path));

			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}

			out.closeEntry();
			in.close();
		}

		out.close();
	}
	
	//I know that is is not the best method but screw it, I am tired of trying to do it myself :P
	private void extractLZMA(String in, String out) throws Exception {
		String[] args = { "d", in, out };
		LzmaAlone.main(args);
	}

	@SuppressWarnings("unused")
	private void extractPack(String in, String out) throws Exception {
		File f = new File(in);
		if (!f.exists()) return;

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
		  System.out.print(contents);
		  writer.write(contents);
		   writer.close();
		} catch (Exception e) {
            e.printStackTrace();
		}
	}
	
	private void downloadFile(String url, String outPut) throws Exception {
        Download download = new Download(url, outPut);
        download.setListener(this);
        download.run();
	}
		
	
	public ArrayList<File> getFiles(File dir) {
		return getFiles(dir, new ArrayList<File>());
	}
	
	public ArrayList<File> getFiles(File dir, ArrayList<File> exclude) {
		ArrayList<File> result = new ArrayList<File>();
		for (File file : dir.listFiles()) {
			if (!exclude.contains(dir)) {
				if (file.isDirectory()) {
					result.addAll(this.getFiles(file, exclude));
					continue;
				}
				result.add(file);
			}
		}
		return result;
	}
	
	private void purgeDir(File file) {
		if (file.exists()) {
			if (file.isDirectory()) deleteSubDir(file);
			file.delete();
		}
	}
	
	private void deleteSubDir(File argFile) {
		for (File file : argFile.listFiles()) {
			if (file.isDirectory()) {
				this.deleteSubDir(file);
			}
			file.delete();
		}
	}

    public void stateChanged(String fileName, float progress) {
        this.listener.stateChanged(fileName, progress);
    }

    public void setListener(DownloadListener listener) {
        this.listener = listener;
    }
}

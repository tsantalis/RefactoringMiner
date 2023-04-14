package org.refactoringminer.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class ExternalProcess {

	public static String execute(File workingDir, String ... commandAndArgs) {
		try {
			Process p = new ProcessBuilder(commandAndArgs)
			.directory(workingDir)
			.redirectErrorStream(true)
			.start();
			try {
				StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream());
				outputGobbler.run();
				//Thread outputGobblerThread = new Thread(outputGobbler);
				//outputGobblerThread.start();
				p.waitFor();

				if (p.exitValue() == 0) {
					return outputGobbler.getOutput();
				} else {
					throw new RuntimeException("Error executing command " + commandAndArgs + ":\n" + outputGobbler.getOutput());
				}
			}
			finally {
				close(p.getInputStream());
				close(p.getOutputStream());
				//p.destroy();
			}
		} catch (IOException e) {
			throw new RuntimeException("Error executing command " + commandAndArgs, e);
		} catch (InterruptedException e) {
			throw new RuntimeException("Error executing command " + commandAndArgs, e);
		}
	}

	private static void close(Closeable closeable) throws IOException {
		if (closeable != null) {
			closeable.close();
		}
	}
	
	private static class StreamGobbler implements Runnable {
		private final InputStream is;
		private final StringBuffer output = new StringBuffer();
		
		StreamGobbler(InputStream is) {
			this.is = is;
		}
		
		public void run() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				String line = null;
				while ((line = br.readLine()) != null) {
					output.append(line + '\n');
				}
			} catch (IOException e) {
				throw new RuntimeException(e); 
			}
		}
		
		public String getOutput() {
			return this.output.toString();
		}
	}
}


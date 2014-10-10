package ca.ualberta.cs.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class AntBuilder implements Builder {

	@Override
	public boolean build(File directory) {
		boolean successfullyCompiled = false;
		try {
			Map<String, String> env = System.getenv();

			ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "ant");
			Map<String, String> environment = pb.environment();
			for(String key : env.keySet()) {
				environment.put(key, env.get(key));
			}
			pb.directory(directory);
			Process p = pb.start();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			String s = null;
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
				if(s.contains("BUILD SUCCESSFUL"))
					successfullyCompiled = true;
			}
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return successfullyCompiled;
	}

}

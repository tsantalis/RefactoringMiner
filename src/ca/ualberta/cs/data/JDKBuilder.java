package ca.ualberta.cs.data;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JDKBuilder implements Builder {

	@Override
	public boolean build(File directory) {
		boolean successfullyCompiled = false;
		try {
			SourceCompiler compiler = new SourceCompiler();
			compiler.compile(directory);
			List<String> errors = compiler.getErrors();
			if(errors.isEmpty())
				successfullyCompiled = true;
			else
				System.err.println(compiler.getErrors());
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return successfullyCompiled;
	}

}

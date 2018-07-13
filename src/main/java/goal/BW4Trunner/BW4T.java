package goal.BW4Trunner;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import events.Channel;
import events.Channel.ChannelState;
import goal.preferences.DebugPreferences;
import goal.preferences.LoggingPreferences;
import goal.tools.Run;
import languageTools.analyzer.FileRegistry;
import languageTools.analyzer.mas.MASValidator;
import languageTools.errors.Message;
import languageTools.errors.mas.MASError;
import languageTools.program.mas.MASProgram;

public class BW4T {
	public static void main(final String[] args) {
		final String target = (args.length > 1) ? args[1] : "BW4Thuman";

		// ------------------------------------------------------------
		// Find directories for the MAS2G and the JAR file
		// ------------------------------------------------------------
		final String working = System.getProperty("user.dir");
		final File bw4t = new File(working + File.separator + target);
		final File client = new File(working + File.separator + "BW4T3" + File.separator + "bw4t-client-3.9.2.jar");

		// ------------------------------------------------------------
		// Fetch all MAS2G files, and parse them into runnable MASPrograms
		// ------------------------------------------------------------
		final Map<String, MASProgram> programs = new LinkedHashMap<>();
		for (final File assignment : bw4t.listFiles()) {
			final String name = assignment.getParentFile().getName() + "\\" + assignment.getName();
			final Set<File> mas2g = Run.getMASFiles(assignment, true);
			if (mas2g.size() != 1) {
				System.err.println("Found " + mas2g.size() + " mas2g files in " + name);
			} else {
				final FileRegistry registry = new FileRegistry();
				final MASProgram mas = parseMAS(mas2g.iterator().next(), registry);
				// Check for any parsing errors (but ignore the environment JAR path)
				final Set<Message> errors = registry.getAllErrors();
				final Iterator<Message> errorcheck = errors.iterator();
				while (errorcheck.hasNext()) {
					final Message error = errorcheck.next();
					if (error.getType() == MASError.ENVIRONMENT_COULDNOT_FIND
							|| error.getType() == MASError.ENVIRONMENT_NOT_JAR) {
						errorcheck.remove();
					}
				}
				// If ok, set the correct environment file and store the result
				if (errors.isEmpty()) {
					mas.setEnvironmentfile(client);
					programs.put(name, mas);
					System.out.println("Success! " + name);
				} else {
					System.err.println("Found " + errors.size() + " errors in " + name + ": " + errors);
				}
			}
		}

		// ------------------------------------------------------------
		// Tweak the GOAL logging (i.e. only log executed actions to a file)
		// ------------------------------------------------------------
		DebugPreferences.setDefault(Run.getDefaultPrefs());
		LoggingPreferences.setLogToFile(true);
		LoggingPreferences.setLogDirectory(working + File.separator + "logs");
		for (Channel channel : Channel.values()) {
			switch (channel) {
			case ACTION_EXECUTED_BUILTIN:
			case ACTION_EXECUTED_USERSPEC:
				DebugPreferences.setChannelState(channel, ChannelState.VIEW);
				break;
			default:
				DebugPreferences.setChannelState(channel, ChannelState.NONE);
				break;
			}
		}

		// ------------------------------------------------------------
		// Run all the MASPrograms
		// ------------------------------------------------------------
		Runner runner = null;
		try {
			if (!programs.isEmpty()) {
				runner = new Runner(client.getParentFile());
			}
			for (final String assignment : programs.keySet()) {
				if (programs.containsKey(assignment)) {
					final MASProgram mas = programs.get(assignment);
					runner.process(mas);
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (runner != null) {
				runner.stop();
			}
			System.exit(0);
		}
	}

	private static MASProgram parseMAS(final File masFile, final FileRegistry registry) {
		final MASValidator validator = new MASValidator(masFile.getPath(), registry);
		validator.validate();
		validator.process();
		return validator.getProgram();
	}
}

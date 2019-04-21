package goal.BW4Trunner;

import java.io.File;
import java.io.IOException;

import goal.tools.SingleRun;
import goal.tools.logging.Loggers;
import languageTools.program.mas.MASProgram;

public class Runner {
	private final File working;
	private final int timeout;
	private final int repeats;
	private final Process server;

	public Runner(final File working, final int timeout, final int repeats) throws IOException {
		this.working = working;
		this.timeout = timeout;
		this.repeats = repeats;
		this.server = startServer();
		Loggers.addConsoleLogger();
		try {
			Thread.sleep(3000); // FIXME
		} catch (final InterruptedException ignore) {
		}
	}

	public void stop() {
		Loggers.removeConsoleLogger();
		this.server.destroy();
	}

	public void process(final MASProgram mas) {
		for (int i = 0; i < this.repeats; i++) {
			try {
				final SingleRun run = new SingleRun(mas);
				run.setTimeOut(this.timeout);
				run.setDebuggerOutput(true);
				run.run(true);
				Thread.sleep(1000); // FIXME
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Process startServer() throws IOException {
		// Create a command that starts a server in a separate Java process,
		// and redirect its output and set the correct working directory.
		final ProcessBuilder builder = new ProcessBuilder(new String[] { "java", "-jar", "bw4t-server-3.9.3.jar" });
		builder.inheritIO();
		builder.directory(this.working);
		// Actually start the server
		return builder.start();
	}
}

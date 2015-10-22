package com.clarkparsia.pellet.server;

import javax.servlet.ServletException;

import com.clarkparsia.pellet.server.handlers.ServerShutdownHandler;
import com.clarkparsia.pellet.server.model.ServerState;
import com.clarkparsia.pellet.server.servlets.MessageServlet;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

/**
 * Pellet PelletServer implementation with Undertow.
 *
 * @author Edgar Rodriguez-Diaz
 * @see <a href="http://undertow.io">undertow.io</a>
 */
public final class PelletServer {

	private static final String HOST = "localhost";
	private static final int PORT = 8080;

	private static final String DEPLOYMENT_NAME = "pellet-server.war";
	private static final String ROOT_PATH = "/api";

	private Undertow server;
	private boolean isRunning = false;

	private final Injector serverInjector;

	public PelletServer(final Injector theInjector) {
		serverInjector = theInjector;
	}

	public void start() throws ServletException {
		DeploymentInfo servletBuilder = Servlets.deployment()
		                                        .setClassLoader(getClass().getClassLoader())
		                                        .setContextPath(ROOT_PATH)
		                                        .setDeploymentName(DEPLOYMENT_NAME)
		                                        .addServlets(Servlets.servlet("MyServlet", MessageServlet.class)
		                                                             .addInitParam("message", "MyServlet")
		                                                             .addMapping("/myservlet"),
		                                                     Servlets.servlet("MessageServlet", MessageServlet.class)
		                                                             .addInitParam("message", "Hello World")
		                                                             .addMapping("/*"));

		DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
		manager.deploy();

		// Servlets are attached to ROOT_PATH
		PathHandler path = Handlers.path(Handlers.redirect(ROOT_PATH))
		                           .addPrefixPath(ROOT_PATH, manager.start());

		// Exceptions handler
		ExceptionHandler aExceptionHandler = Handlers.exceptionHandler(path);

		// Shutdown handler
		GracefulShutdownHandler aShutdownHandler = Handlers.gracefulShutdown(aExceptionHandler);

		// add shutdown path
		path.addExactPath("/shutdown", ServerShutdownHandler.newInstance(this, aShutdownHandler));

		server = Undertow.builder()
		                 .addHttpListener(8080, "localhost")
		                 .setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
		                 .setHandler(aShutdownHandler)
		                 .build();

		isRunning = true;
		server.start();
	}

	public void stop() {
		if (server != null && isRunning) {
			System.out.println("Received request to shutdown");
			System.out.println("System is shutting down...");

			server.stop();
			isRunning = false;
		}
	}

	public static void main(String[] args) throws Exception {
		PelletServer aPelletServer = new PelletServer(Guice.createInjector(new PelletServerModule()));

		System.out.println(String.format("Listening at: http://%s:%s", HOST, PORT));
		aPelletServer.start();
	}
}

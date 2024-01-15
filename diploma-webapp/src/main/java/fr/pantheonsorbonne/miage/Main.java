package fr.pantheonsorbonne.miage;

 import java.io.IOException;
 import java.io.InputStream;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.ArrayList;
 import java.util.NoSuchElementException;
 import java.util.logging.Level;
 import java.util.logging.Logger;

 import org.glassfish.grizzly.http.io.NIOOutputStream;
 import org.glassfish.grizzly.http.server.HttpHandler;
 import org.glassfish.grizzly.http.server.HttpServer;
 import org.glassfish.grizzly.http.server.Request;
 import org.glassfish.grizzly.http.server.Response;

 import com.google.common.collect.Iterables;
 import com.google.common.io.ByteStreams;

 /**
 * Main class.
 *
 */
public class Main {
	public static final String HOST = "localhost";
 	public static final int PORT = 7000;
 	private static final Logger logger = Logger.getLogger(Main.class.getName());
 	private static StudentRepository studentRepo = StudentRepository.withDB("src/main/resources/students.db");
 	private static String webAppURL = "http://localhost:8080/home";

 	public static void main(String[] args) throws IOException, URISyntaxException {

 		HttpServer server = HttpServer.createSimpleServer();
 		addRootPath(server, "/home");
 		addDiplomaPath(server, "/diploma/*");
 		try

 		{
 			server.start();
 			java.awt.Desktop.getDesktop().browse(new URI(webAppURL));
 			logger.log(Level.FINE, "Press any key to stop the server...");
 			System.in.read();
 		} catch (Exception e) {
 			logger.log(Level.SEVERE, e.toString());
 		}
 	}

 	protected static Student getStudentData(int studentId, StudentRepository repo) {
 		// create an arrayList of the students, because iterables are too hard
 		ArrayList<Student> students = new ArrayList<>();
 		Iterables.addAll(students, repo);
 		for (Student student : students) {
 			if (student.getId() == studentId) {
 				return student;
 			}
 		}

 		throw new NoSuchElementException();
 	}
 	protected static void handleResponse(Response response, int studentId) throws IOException {
 		response.setContentType("application/pdf");
 		Student student = getStudentData(studentId, studentRepo);
 		DiplomaGenerator generator = new MiageDiplomaGenerator(student);
 		try (InputStream is = generator.getContent()) {
 			try (NIOOutputStream os = response.createOutputStream()) {
 				ByteStreams.copy(is, os);
 			}

 		}
 	}

 	protected static void addDiplomaPath(HttpServer server, String path) {
 		server.getServerConfiguration().addHttpHandler(new HttpHandler() {
 			public void service(Request request, Response response) throws Exception {
 				// get the id of the student
 				int id = Integer.parseInt(request.getPathInfo().substring(1));
 				handleResponse(response, id);
 				response.setContentType("text/html; charset=utf-8");
 				response.setStatus(404);
 				response.getWriter().write("Erreur 404 : Le diplome n'existe pas pour cet utilisateur");
 			}
 		}, path);
 	}
 	protected static void addRootPath(HttpServer server, String path) {
 		server.getServerConfiguration().addHttpHandler(new HttpHandler() {
 			public void service(Request request, Response response) throws Exception {
 				StringBuilder sb = new StringBuilder();
 				sb.append("<!DOCTYPE html><head><meta charset='utf-8'></head><body><h1>Liste des diplômés</h1><ul>");
 				for (Student stu : StudentRepository.withDB("src/main/resources/students.db")) {
					sb.append("<li>");
					sb.append(
 							"<a href='/diploma/" + stu.getId() + "'>" + stu.getTitle() + ' ' + stu.getName() + "</a>");
 					sb.append("</li>");
 				}
 				sb.append("</ul></body></html>");
 				response.setContentType("text/html; charset=utf-8");
 				response.setContentLength(sb.length());
 				response.getWriter().write(sb.toString());
 			}
 		}, path);
 	}
 }
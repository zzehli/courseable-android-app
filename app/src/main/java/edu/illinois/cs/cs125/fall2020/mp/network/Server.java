package edu.illinois.cs.cs125.fall2020.mp.network;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.cs.cs125.fall2020.mp.application.CourseableApplication;
import edu.illinois.cs.cs125.fall2020.mp.models.Rating;
import edu.illinois.cs.cs125.fall2020.mp.models.Summary;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Development course API server.
 *
 * <p>Normally you would run this server on another machine, which the client would connect to over
 * the internet. For the sake of development, we're running the server right alongside the app on
 * the same device. However, all communication between the course API client and course API server
 * is still done using the HTTP protocol. Meaning that eventually it would be straightforward to
 * move this server to another machine where it could provide data for all course API clients.
 *
 * <p>You will need to add functionality to the server for MP1 and MP2.
 */
public final class Server extends Dispatcher {
  @SuppressWarnings({"unused", "RedundantSuppression"})
  private static final String TAG = Server.class.getSimpleName();

  private final Map<String, String> summaries = new HashMap<>();

  private MockResponse getSummary(@NonNull final String path) {
    String[] parts = path.split("/");
    if (parts.length != 2) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    String summary = summaries.get(parts[0] + "_" + parts[1]);
    if (summary == null) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
    }
    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(summary);
  }

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<Summary, String> courses = new HashMap<>();

  // course/YEAR/SEMESTER/DEPARTMENT/NUMBER
  private MockResponse getCourse(@NonNull final String path) {
    String[] parts = path.split("/");

    final int length = 4;
    if (parts.length != length) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    Summary courseSummary = new Summary(parts[0], parts[1], parts[2], parts[3], null);

    String course = courses.get(courseSummary);

    if (course == null) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
    }
    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(course);
  }

  // The map take clientID as key and find collection of ratings corresponds to different courses
  private final Map<String, Map<Summary, Rating>> ratingDict = new HashMap<>();

  // "rating/YEAR/SEMESTER/DEPARTMENT/NUMBER?client=CLIENTID"
  private MockResponse getRating(
      @NonNull final String path, @NonNull final RecordedRequest request) {
    final int id = 36;
    final int length = 4;
    // test
    String[] parts = path.split("/");

    if (parts.length != length) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    String[] subparts = parts[3].split("\\?");

    if (subparts.length != 2) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    String[] categoryID = subparts[1].split("=");
    if (categoryID[1].length() != id) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    Summary courseSummary = new Summary(parts[0], parts[1], parts[2], subparts[0], null);
    if (courses.get(courseSummary) == null) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
    }

    ObjectMapper map = new ObjectMapper();
    // choose between GET and POST
    if (request.getMethod().equals("GET")) {
      // for GET method serialization
      String json = new String();
      Rating rating = null;
      try {
        // find the rating in the dictionary
        Map<Summary, Rating> individualRatings = ratingDict.get(categoryID[1]);
        if (individualRatings == null) {
          rating = new Rating(categoryID[1]);
        } else {
          rating = individualRatings.get(courseSummary);
        }

        if (rating == null) {
          rating = new Rating(categoryID[1]);
        }

        json = map.writeValueAsString(rating);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }

      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(json);

    } else if (request.getMethod().equals("POST")) {
      // for POST retrieval
      Rating courseRating;
      try {
        String message = request.getBody().readUtf8();
        // deserialized rating information
        courseRating = map.readValue(message, Rating.class);
        // update the rating in dictionary
        Map<Summary, Rating> individualRatings = ratingDict.get(categoryID[1]);
        if (individualRatings == null) {
          // create new rating record for the client

          Map<Summary, Rating> clientRecord = new HashMap<>();
          clientRecord.put(courseSummary, courseRating);
          ratingDict.put(categoryID[1], clientRecord);
        } else {
          // put entry to the existing record
          ratingDict.get(categoryID[1]).put(courseSummary, courseRating);
        }
        // redirect POST to send a request to GET
        return new MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
            .setHeader("Location", parts[3]);

      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }

    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
  }

  // for demo
  private String testString;
  // for demo
  private MockResponse testPost(@NonNull final RecordedRequest request) {
    if (request.getMethod().equals("GET")) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(testString);
    } else if (request.getMethod().equals(("POST"))) {
      testString = request.getBody().readUtf8();
      // redirects the client to a new get request
      return new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
          .setHeader("Location", "/test/");
    }
    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
  }

  @NonNull
  @Override
  public MockResponse dispatch(@NonNull final RecordedRequest request) {
    try {
      String path = request.getPath();

      if (path == null || request.getMethod() == null) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
      } else if (path.equals("/") && request.getMethod().equalsIgnoreCase("HEAD")) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK);
      } else if (path.startsWith("/summary/")) {
        return getSummary(path.replaceFirst("/summary/", ""));
      } else if (path.startsWith("/course/")) {
        return getCourse(path.replaceFirst("/course/", ""));
      } else if (path.startsWith("/rating/")) {
        return getRating(path.replaceFirst("/rating/", ""), request);
      } else if (path.startsWith("/test/")) {
        return testPost(request);
      }
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
    } catch (Exception e) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
    }
  }

  private static boolean started = false;

  /**
   * Start the server if has not already been started.
   *
   * <p>We start the server in a new thread so that it operates separately from and does not
   * interfere with the rest of the app.
   */
  public static void start() {
    if (!started) {
      new Thread(Server::new).start();
      started = true;
    }
  }

  private final ObjectMapper mapper = new ObjectMapper();

  private Server() {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    loadSummary("2020", "fall");
    loadCourses("2020", "fall");

    try {
      MockWebServer server = new MockWebServer();
      server.setDispatcher(this);
      server.start(CourseableApplication.SERVER_PORT);

      String baseUrl = CourseableApplication.SERVER_URL; // server.url("").toString(); //
      if (!CourseableApplication.SERVER_URL.equals(baseUrl)) {
        throw new IllegalStateException("Bad server URL: " + baseUrl);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  @SuppressWarnings("SameParameterValue")
  private void loadSummary(@NonNull final String year, @NonNull final String semester) {
    String filename = "/" + year + "_" + semester + "_summary.json";
    String json =
        new Scanner(Server.class.getResourceAsStream(filename), "UTF-8").useDelimiter("\\A").next();
    summaries.put(year + "_" + semester, json);
  }

  @SuppressWarnings("SameParameterValue")
  private void loadCourses(@NonNull final String year, @NonNull final String semester) {
    String filename = "/" + year + "_" + semester + ".json";
    String json =
        new Scanner(Server.class.getResourceAsStream(filename), "UTF-8").useDelimiter("\\A").next();
    try {
      JsonNode nodes = mapper.readTree(json);
      for (Iterator<JsonNode> it = nodes.elements(); it.hasNext(); ) {
        JsonNode node = it.next();
        Summary course = mapper.readValue(node.toString(), Summary.class);
        courses.put(course, node.toPrettyString());
      }
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
